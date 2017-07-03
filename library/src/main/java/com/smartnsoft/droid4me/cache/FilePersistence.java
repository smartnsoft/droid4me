// The MIT License (MIT)
//
// Copyright (c) 2017 Smart&Soft
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.smartnsoft.droid4me.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.InputAtom;

/**
 * Enables to store on the internal/external device "hard-drive" and cache in memory some contents.
 *
 * @author Édouard Mercier
 * @since 2009.03.26
 */
public final class FilePersistence
    extends Persistence
{

  private static final String INDEX_KEY = "index";

  /**
   * An array, which indicates for each instance, the maximum number of URIs which can be persisted.
   */
  public static int[] CACHE_FILE_COUNT_LIMITS = new int[] { Integer.MAX_VALUE };

  /**
   * The name of the file which will store the index of the persisted files.
   */
  public static String CACHE_INDEX_FILE_NAME = "index";

  private static InputStream storeInputStreamToFile(String filePath, Business.InputAtom inputAtom, boolean closeInput)
  {
    try
    {
      return Persistence.storeInputStream(new FileOutputStream(filePath), inputAtom.inputStream, closeInput, " corresponding to the file '" + filePath + "'");
    }
    catch (FileNotFoundException exception)
    {
      if (log.isWarnEnabled())
      {
        log.warn("Could not access to the file '" + filePath + "' for writing", exception);
      }
      return null;
    }
  }

  private final int storageLimitFilesCount;

  private Properties properties;

  private File indexFile;

  private boolean fileIndexNeedsSaving;

  /**
   * {@inheritDoc}
   */
  public FilePersistence(String storageDirectoryPath, int instanceIndex)
  {
    super(storageDirectoryPath, instanceIndex);
    this.storageLimitFilesCount = FilePersistence.CACHE_FILE_COUNT_LIMITS[instanceIndex];
  }

  @Override
  protected void initializeInstance()
      throws Persistence.PersistenceException
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
      throw new Persistence.PersistenceException("Cannot initialize properly: the back-end directory '" + storageDirectory.getAbsolutePath() + "' is not available");
    }
    if (indexFile.exists() == false)
    {
      // There is no index file yet
      setStorageBackendAvailable(true);
      return;
    }
    if (log.isInfoEnabled())
    {
      log.info("Reading the index file '" + indexFile.getAbsolutePath() + "'");
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
        if (uri.equals(FilePersistence.INDEX_KEY) == true)
        {
          uriUsages.setIndex(Integer.parseInt(properties.getProperty(uri)));
        }
        else
        {
          uriUsages.put(uri, new Persistence.UriUsage(properties.getProperty(uri), uri));
        }
      }
    }
    catch (Exception exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot properly read the index file at '" + indexFile.getAbsolutePath() + "'", exception);
      }
      throw new Persistence.PersistenceException("Cannot initialize properly: the index file at '" + indexFile.getAbsolutePath() + "'", exception);
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
    setStorageBackendAvailable(true);
  }

  @Override
  protected List<String> getUrisInstance()
      throws Persistence.PersistenceException
  {
    return uriUsages.getUris();
  }

  @Override
  protected Date getLastUpdateInstance(String uri)
      throws Persistence.PersistenceException
  {
    final Persistence.UriUsage uriUsage;
    synchronized (uriUsages)
    {
      uriUsage = uriUsages.get(uri);
    }
    if (uriUsage == null)
    {
      return null;
    }
    else
    {
      return new Date(new File(uriUsage.storageFilePath).lastModified());
    }
  }

  @Override
  protected Business.InputAtom readInputStreamInstance(String uri)
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Asking for the input stream related to the URI '" + uri + "'");
    }
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
        if (log.isDebugEnabled())
        {
          log.debug("Reusing the cached data for the URI '" + uri + "', stored in the file '" + uriUsage.storageFilePath + "'");
        }
        rememberUriUsed(uri);
        final File file = new File(uriUsage.storageFilePath);
        final long lastModified = file.lastModified();
        return new Business.InputAtom(new Date(lastModified), new FileInputStream(file));
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
  protected Business.InputAtom flushInputStreamInstance(String uri, Business.InputAtom inputAtom)
      throws Persistence.PersistenceException
  {
    return cacheInputStream(uri, inputAtom, false);
  }

  @Override
  protected void removeInstance(String uri)
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Removing from the persistence the contents related to the URI '" + uri + "'");
    }
    final Persistence.UriUsage uriUsage;
    synchronized (uriUsages)
    {
      uriUsage = uriUsages.get(uri);
    }
    if (uriUsage == null)
    {
      return;
    }
    else
    {
      unregisterUri(uriUsage);
      saveIndexFileIfNecessary();
    }
  }

  @Override
  protected InputStream writeInputStreamInstance(String uri, Business.InputAtom inputAtom, boolean returnStream)
      throws Persistence.PersistenceException
  {
    final InputAtom newInputAtom = cacheInputStream(uri, inputAtom, returnStream == false);
    return (returnStream == false || newInputAtom == null || newInputAtom.inputStream == null) ? null : newInputAtom.inputStream;
  }

  /**
   * Does nothing.
   */
  @Override
  protected void computePolicyAndCleanUpInstance()
      throws Persistence.PersistenceException
  {
  }

  /**
   * @return {@code null}
   */
  @Override
  protected <CleanUpPolicyClass extends Persistence.CleanUpPolicy> CleanUpPolicyClass computeCleanUpPolicy()
  {
    return null;
  }

  /**
   * This implementation does nothing.
   */
  @Override
  protected <CleanUpPolicyClass extends Persistence.CleanUpPolicy> void cleanUpInstance(
      CleanUpPolicyClass cleanUpPolicy)
      throws PersistenceException
  {
  }

  protected void clearInstance()
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
    properties.clear();
  }

  @Override
  protected void closeInstance()
      throws Persistence.PersistenceException
  {
    properties = null;
    indexFile = null;
    fileIndexNeedsSaving = false;
  }

  // TODO: think of making the processing in another thread
  public Business.InputAtom cacheInputStream(String uri, Business.InputAtom inputAtom, boolean closeInput)
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
      final String filePath = computeUriFilePath(uri);
      if (log.isDebugEnabled())
      {
        log.debug("Caching the stream for the URI '" + uri + "' to the file '" + filePath + "'");
      }
      // We store the contents of the input stream on the SD card
      final InputStream newInputStream = FilePersistence.storeInputStreamToFile(filePath, inputAtom, closeInput);
      rememberUriUsed(uri);
      saveIndexFileIfNecessary();
      return new Business.InputAtom(new Date(), newInputStream, inputAtom.context);
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

  private boolean isStorageLimited()
  {
    return storageLimitFilesCount != Integer.MAX_VALUE;
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
        properties.setProperty(FilePersistence.INDEX_KEY, Integer.toString(uriUsages.getIndex()));
        properties.store(outputStream, "The cache index file");
        fileIndexNeedsSaving = false;
        if (log.isDebugEnabled())
        {
          log.debug("Saved the index file");
        }
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

  private void registerUri(String uri, String filePath)
  {
    Persistence.UriUsage uriUsage;
    synchronized (uriUsages)
    {
      uriUsage = uriUsages.get(uri);
      if (uriUsage == null)
      {
        uriUsage = new Persistence.UriUsage(filePath, uri);
        uriUsage.accessed();
        uriUsages.put(uri, uriUsage);
        properties.put(uri, filePath);
        fileIndexNeedsSaving = true;
        if (log.isDebugEnabled())
        {
          log.debug("Registered the URI '" + uri + "' with the file '" + filePath + "'");
        }
      }
    }
  }

  private void unregisterUri(Persistence.UriUsage uriUsage)
  {
    synchronized (uriUsages)
    {
      new File(uriUsage.storageFilePath).delete();
      synchronized (uriUsages)
      {
        uriUsages.remove(uriUsage.uri);
      }
      properties.remove(uriUsage.uri);
      fileIndexNeedsSaving = true;
      if (log.isDebugEnabled())
      {
        log.debug("Unregistered the URI '" + uriUsage.uri + "' with the file '" + uriUsage.storageFilePath + "'");
      }
    }
  }

  private void rememberUriUsed(String uri)
  {
    if (isStorageLimited() == true)
    {
      final Persistence.UriUsage uriUsage = uriUsages.get(uri);
      uriUsage.accessed();
      if (log.isDebugEnabled())
      {
        log.debug("The URI '" + uri + "' has been accessed " + uriUsage.getAccessCount() + " time(s)");
      }
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
      final String filePath = getStorageDirectoryPath() + "/" + uriUsages.getIndex();
      registerUri(uri, filePath);
      return filePath;
    }
  }

  private void cleanUpUris()
  {
    synchronized (uriUsages)
    {
      final List<Persistence.UriUsage> toBeDiscardedUriUsages = uriUsages.getUriUsages();
      Collections.sort(toBeDiscardedUriUsages);
      final int toBeDiscardedCount = toBeDiscardedUriUsages.size() / 2;
      for (int index = 0; index < toBeDiscardedCount; index++)
      {
        final Persistence.UriUsage discardedUriUsage = uriUsages.remove(toBeDiscardedUriUsages.get(index).uri);
        properties.remove(discardedUriUsage.uri);
        if (log.isDebugEnabled())
        {
          log.debug("Removed from the cache the URI " + discardedUriUsage.uri + "' accessed " + discardedUriUsage.getAccessCount() + " time(s), corresponding to the file '" + discardedUriUsage.storageFilePath);
        }
      }
      // We reset the remaining usages
      uriUsages.resetAccessCount();
      if (log.isInfoEnabled())
      {
        log.info("The web cache has been cleaned-up and it now contains " + uriUsages.size() + " item(s)");
      }
    }
  }

}
