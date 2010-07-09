/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.smartnsoft.droid4me.bo.Business;

/**
 * Enables to store on the internal/external device "hard-drive" and cache in memory some contents.
 * 
 * @author Édouard Mercier
 * @since 2009.03.26
 */
public final class FilePersistence
    extends Persistence
{

  public static int[] CACHE_FILE_COUNT_LIMITS = new int[] { Integer.MAX_VALUE };

  public static String CACHE_INDEX_FILE_NAME = "index";

  private Properties properties;

  private File indexFile;

  private final int storageLimitFilesCount;

  private boolean fileIndexNeedsSaving;

  public FilePersistence(String storageDirectoryPath, int instanceIndex)
  {
    super(storageDirectoryPath, instanceIndex);
    this.storageLimitFilesCount = FilePersistence.CACHE_FILE_COUNT_LIMITS[instanceIndex];
  }

  private boolean isStorageLimited()
  {
    return storageLimitFilesCount != Integer.MAX_VALUE;
  }

  @Override
  protected void initialize()
  {
    properties = new Properties();
    indexFile = new File(getStorageDirectoryPath(), CACHE_INDEX_FILE_NAME);
    // We make sure that the parent directory exists
    final File storageDirectory = indexFile.getParentFile();
    storageDirectory.mkdirs();
    if (storageDirectory.exists() == false)
    {
      if (log.isErrorEnabled())
      {
        log.error("The back-end directory '" + storageDirectory.getAbsolutePath() + "' is not available: will not cache the streams");
      }
      return;
    }
    else
    {
      storageBackendAvailable = true;
    }
    if (indexFile.exists() == false)
    {
      // There is no index file yet
      return;
    }
    if (log.isInfoEnabled())
    {
      log.info("Reading the index file '" + indexFile + "'");
    }
    FileInputStream inputStream = null;
    try
    {
      inputStream = new FileInputStream(indexFile);
      properties.load(inputStream);
      final Enumeration<?> propertyNames = properties.propertyNames();
      while (propertyNames.hasMoreElements() == true)
      {
        final String uri = (String) propertyNames.nextElement();
        uriUsages.put(uri, new Persistence.UriUsage(properties.getProperty(uri), uri));
      }
    }
    catch (Exception exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot properly read the index file at '" + indexFile + "'", exception);
      }
    }
    finally
    {
      if (inputStream != null)
      {
        try
        {
          inputStream.close();
        }
        catch (IOException exception)
        {
          // Does not matter
        }
      }
    }
  }

  private synchronized void saveIndexFileIfNecessary()
  {
    if (fileIndexNeedsSaving == false)
    {
      return;
    }
    final File indexFile = getIndexFile();
    try
    {
      final FileOutputStream outputStream = new FileOutputStream(indexFile);
      try
      {
        properties.store(outputStream, "The cache index file");
        fileIndexNeedsSaving = false;
        // log.debug("Saved the index file");
      }
      finally
      {
        try
        {
          outputStream.close();
        }
        catch (IOException exception)
        {
          if (log.isWarnEnabled())
          {
            log.warn("Could not properly close the file index output stream while saving ", exception);
          }
        }
      }
    }
    catch (Exception exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot write the cache index file at '" + indexFile + "'", exception);
      }
    }
  }

  private File getIndexFile()
  {
    return indexFile;
  }

  // TODO: implement that method
  public Date getLastUpdate(String uri)
  {
    if (storageBackendAvailable == false)
    {
      return null;
    }
    throw new UnsupportedOperationException("FilePersistence::getLastUpdate() is not implemented!");
  }

  public InputStream getRawInputStream(String uri)
      throws Persistence.PersistenceException
  {
    final Business.InputAtom atom = readInputStream(uri);
    return (atom == null ? null : atom.inputStream);
  }

  // TODO: put the URI usage in the parent class
  public Business.InputAtom readInputStream(String uri)
      throws Persistence.PersistenceException
  {
    // log.debug("Asking for the input stream related to the URI '" + uri + "'");
    final Persistence.UriUsage uriUsage;
    synchronized (uriUsages)
    {
      uriUsage = uriUsages.get(uri);
    }
    if (uriUsage == null)
    {
      return null;
    }
    synchronized (beingProcessed)
    {
      if (beingProcessed.contains(uri) == true)
      {
        // There is already a thread asking for that input stream of writing the corresponding input stream
        synchronized (beingProcessed)
        {
          try
          {
            beingProcessed.wait();
          }
          catch (InterruptedException exception)
          {
            if (log.isWarnEnabled())
            {
              log.warn("An interruption occurred while waiting for the stream linked to the URI '" + uri + "' to be released", exception);
            }
          }
        }
      }
      beingProcessed.add(uri);
    }
    try
    {
      try
      {
        // log.debug("Reusing the cached data for the URI '" + uri + "', stored in the file '" + uriUsage.storageFilePath + "'");
        rememberUriUsed(uri);
        if (storageBackendAvailable == true)
        {
          // TODO: compute the time stamp via the file last modification date
          return new Business.InputAtom(new Date(), new FileInputStream(uriUsage.storageFilePath));
        }
        else
        {
          return null;
        }
      }
      catch (FileNotFoundException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot find anymore the file '" + uriUsage.storageFilePath + "' corresponding to the URI '" + uri + "'", exception);
        }
        synchronized (uriUsages)
        {
          uriUsages.remove(uri);
          properties.remove(uri);
        }
        return null;
      }
    }
    finally
    {
      synchronized (beingProcessed)
      {
        beingProcessed.remove(uri);
        beingProcessed.notify();
      }
    }
  }

  @Override
  public InputStream cacheInputStream(String uri, InputStream inputStream)
      throws Persistence.PersistenceException
  {
    return cacheInputStream(uri, inputStream, false);
  }

  public void remove(String uri)
  {
    throw new UnsupportedOperationException("FilePersistence::remove() is not implemented!");
  }

  public InputStream writeInputStream(String uri, Business.InputAtom atom)
      throws Persistence.PersistenceException
  {
    return cacheInputStream(uri, atom.inputStream, false);
  }

  protected void empty()
      throws Persistence.PersistenceException
  {
    final Enumeration<?> propertyNames = properties.propertyNames();
    // We delete all the cached files
    while (propertyNames.hasMoreElements() == true)
    {
      final String uri = (String) propertyNames.nextElement();
      new File(properties.getProperty(uri)).delete();
    }
    getIndexFile().delete();
  }

  // TODO: think of making the processing in another thread
  public InputStream cacheInputStream(String uri, InputStream inputStream, boolean closeInput)
      throws Persistence.PersistenceException
  {
    synchronized (beingProcessed)
    {
      if (beingProcessed.contains(uri) == true)
      {
        try
        {
          beingProcessed.wait();
        }
        catch (InterruptedException exception)
        {
          if (log.isWarnEnabled())
          {
            log.warn("An interruption occurred while waiting for the stream linked to the URI '" + uri + "' to be released", exception);
          }
        }
      }
      beingProcessed.add(uri);
    }
    try
    {
      if (storageBackendAvailable == true)
      {
        final String filePath = computeUriFilePath(uri);
        // log.debug("Caching the stream for the URI '" + uri + "' to the file '" + filePath + "'");
        // We store the contents of the input stream on the SD card
        final InputStream newInputStream = Persistence.storeInputStreamToFile(filePath, inputStream, closeInput);
        rememberUriUsed(uri);
        saveIndexFileIfNecessary();
        return newInputStream;
      }
      else
      {
        // There is no use trying to cache the stream, since the back-end storage is not available
        return inputStream;
      }
    }
    finally
    {
      synchronized (beingProcessed)
      {
        beingProcessed.remove(uri);
        beingProcessed.notify();
      }
    }
  }

  private void registerUri(String uri, String filePath)
  {
    Persistence.UriUsage uriUsage;
    synchronized (uriUsages)
    {
      uriUsage = uriUsages.get(uri);
      if (uriUsage == null)
      {
        uriUsage = new Persistence.UriUsage(filePath, uri);
        uriUsage.accessCount++;
        uriUsages.put(uri, uriUsage);
        properties.put(uri, filePath);
        // log.debug("Registered the URI '" + uri + "' with the file '" + filePath + "'");
      }
    }
  }

  private void rememberUriUsed(String uri)
  {
    if (isStorageLimited() == true)
    {
      final Persistence.UriUsage uriUsage = uriUsages.get(uri);
      uriUsage.accessCount++;
      // log.debug("The URI '" + uri + "' has been accessed " + uriUsage.accessCount + " time(s)");
    }
  }

  private synchronized String computeUriFilePath(String uri)
  {
    Persistence.UriUsage uriUsage;
    synchronized (uriUsages)
    {
      uriUsage = uriUsages.get(uri);
    }
    if (uriUsage != null)
    {
      // We URI has already been cached
      return uriUsage.storageFilePath;
    }
    // We test whether we have reached the storage limit
    if (isStorageLimited() == true && uriUsages.size() >= storageLimitFilesCount)
    {
      // We need to discard some cached URIs
      if (log.isInfoEnabled())
      {
        log.info("The cache storage limit " + storageLimitFilesCount + " has been reached");
      }
      cleanUpUris();
    }
    synchronized (uriUsages)
    {
      final String filePath = getStorageDirectoryPath() + "/" + uriUsages.size();
      registerUri(uri, filePath);
      fileIndexNeedsSaving = true;
      return filePath;
    }
  }

  private void cleanUpUris()
  {
    synchronized (uriUsages)
    {
      final List<Persistence.UriUsage> toBeDiscardedUriUsages = new ArrayList<Persistence.UriUsage>(uriUsages.values());
      Collections.sort(toBeDiscardedUriUsages);
      final int toBeDiscardedCount = toBeDiscardedUriUsages.size() / 2;
      for (int index = 0; index < toBeDiscardedCount; index++)
      {
        final Persistence.UriUsage discardedUriUsage = uriUsages.remove(toBeDiscardedUriUsages.get(index).uri);
        properties.remove(discardedUriUsage.uri);
        // log.debug("Removed from the cache the URI " + discardedUriUsage.uri + "' accessed " + discardedUriUsage.accessCount +
        // " time(s), corresponding to the file '" + discardedUriUsage.storageFilePath);
      }
      // We reset the remaining usages
      for (Persistence.UriUsage uriUsage : uriUsages.values())
      {
        uriUsage.accessCount = 0;
      }
      if (log.isInfoEnabled())
      {
        log.info("The web cache has been cleaned-up and it now contains " + uriUsages.size() + " item(s)");
      }
    }
  }

}
