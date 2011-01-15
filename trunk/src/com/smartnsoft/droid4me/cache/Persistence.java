/*
 * (C) Copyright 2009-2011 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Enables to store persistently on the internal/external device "hard-drive" some contents.
 * 
 * @author Édouard Mercier
 * @since 2009.03.26
 */
// TODO: turn the derived classes exception into actual Persistence.PersistenceException instances!
public abstract class Persistence
    implements Business.IOStreamer<String, Persistence.PersistenceException>
{

  /**
   * The exception thrown when an error occurs during a persistence operation.
   */
  public final static class PersistenceException
      extends Error
  {

    private static final long serialVersionUID = -5246441820601050842L;

  }

  protected final static class UriUsage
      implements Comparable<Persistence.UriUsage>
  {

    public int accessCount = 0;

    public final String storageFilePath;

    /**
     * Redundant but there for optimization reasons.
     */
    public final String uri;

    public UriUsage(String storageFilePath, String uri)
    {
      this.storageFilePath = storageFilePath;
      this.uri = uri;
    }

    public int compareTo(Persistence.UriUsage another)
    {
      if (accessCount > another.accessCount)
      {
        return -1;
      }
      else if (accessCount < another.accessCount)
      {
        return 1;
      }
      return 0;
    }

  }

  protected final static Logger log = LoggerFactory.getInstance(Persistence.class);

  private static volatile Persistence[] instances;

  public static String[] CACHE_DIRECTORY_PATHS = new String[] { "/sdcard" };

  public static int CACHES_COUNT = 1;

  /**
   * The fully qualified name of the {@link Persistence} instances implementation.
   */
  public static String IMPLEMENTATION_FQN;

  public static int MAXIMUM_URI_CONTENTS_SIZE_IN_BYTES = 512 * 1024;

  protected boolean storageBackendAvailable;

  private final String storageDirectoryPath;

  protected final Map<String, Persistence.UriUsage> uriUsages;

  protected final Set<String> beingProcessed = new HashSet<String>();

  public static Persistence getInstance()
  {
    return Persistence.getInstance(0);
  }

  /**
   * This is a lazy instantiation.
   */
  // We accept the "out-of-order writes" case
  @SuppressWarnings("unchecked")
  public static Persistence getInstance(int position)
  {
    if (Persistence.instances == null)
    {
      synchronized (Persistence.class)
      {
        if (Persistence.instances == null)
        {
          try
          {
            // We do that way, because the Persistence initialization may be long, and we want the "instances" attribute to be atomic
            final Class<? extends Persistence> implementationClass = (Class<? extends Persistence>) Class.forName(Persistence.IMPLEMENTATION_FQN);
            final Persistence[] newInstances = (Persistence[]) Array.newInstance(implementationClass, Persistence.CACHES_COUNT);
            final Constructor<? extends Persistence> constructor = implementationClass.getDeclaredConstructor(String.class, int.class);
            for (int index = 0; index < Persistence.CACHES_COUNT; index++)
            {
              newInstances[index] = constructor.newInstance(Persistence.CACHE_DIRECTORY_PATHS[index], index);
              newInstances[index].initialize();
            }
            // We only assign the instances class variable here, once all instances have actually been created
            Persistence.instances = newInstances;
          }
          catch (Exception exception)
          {
            if (log.isFatalEnabled())
            {
              log.fatal("Cannot instantiate properly the persistence instances", exception);
            }
          }
        }
      }
    }
    return Persistence.instances[position];
  }

  /**
   * @param outputStream
   *          it is closed by this method
   */
  // TODO: simplify all this!
  public static InputStream storeInputStream(OutputStream outputStream, InputStream inputStream, boolean closeInput, String logMessageSuffix)
  {
    final InputStream actualInputStream;
    final BufferedInputStream bufferedInputStream;
    final ByteArrayInputStream byteArrayInputStream;
    if (inputStream == null)
    {
      bufferedInputStream = null;
      byteArrayInputStream = new ByteArrayInputStream(new byte[0]);
      actualInputStream = byteArrayInputStream;
    }
    else if (closeInput == false)
    {
      byteArrayInputStream = null;
      bufferedInputStream = new BufferedInputStream(inputStream, 8192);
      bufferedInputStream.mark(MAXIMUM_URI_CONTENTS_SIZE_IN_BYTES);
      actualInputStream = bufferedInputStream;
    }
    else
    {
      bufferedInputStream = null;
      byteArrayInputStream = null;
      actualInputStream = inputStream;
    }
    try
    {
      final byte buffer[] = new byte[8092];
      int length;
      while ((length = actualInputStream.read(buffer)) > 0)
      {
        outputStream.write(buffer, 0, length);
      }
      if (bufferedInputStream != null)
      {
        try
        {
          bufferedInputStream.reset();
        }
        catch (IOException exception)
        {
          if (log.isErrorEnabled())
          {
            log.error("Could not reset the buffered input stream" + logMessageSuffix, exception);
          }
          return inputStream;
        }
        return bufferedInputStream;
      }
      else if (byteArrayInputStream != null)
      {
        byteArrayInputStream.close();
        return null;
      }
      else
      {
        // And we do not forget to close the original input stream
        inputStream.close();
        return null;
      }
    }
    catch (OutOfMemoryError exception)
    {
      if (log.isWarnEnabled())
      {
        log.warn("Not enough memory for properly writing to the output stream" + logMessageSuffix, exception);
      }
      System.gc();
      return inputStream;
    }
    catch (IOException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Could not properly write to the output stream" + logMessageSuffix, exception);
      }
      return inputStream;
    }
    finally
    {
      if (outputStream != null)
      {
        try
        {
          outputStream.close();
        }
        catch (IOException exception)
        {
          if (log.isWarnEnabled())
          {
            log.warn("Could not properly close the output stream" + logMessageSuffix, exception);
          }
        }
      }
    }
  }

  public static InputStream storeInputStreamToFile(String filePath, InputStream inputStream, boolean closeInput)
  {
    try
    {
      return Persistence.storeInputStream(new FileOutputStream(filePath), inputStream, closeInput, " corresponding to the file '" + filePath + "'");
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

  /**
   * The unique constructor.
   * 
   * @param storageDirectoryPath
   *          the location where the persistence should be performed
   * @param instanceIndex
   *          the ordinal of the instance which is bound to be created. Starts with <code>0</code>.
   */
  protected Persistence(String storageDirectoryPath, int instanceIndex)
  {
    this.storageDirectoryPath = storageDirectoryPath;
    uriUsages = new HashMap<String, Persistence.UriUsage>();
  }

  protected abstract void initialize();

  public abstract InputStream getRawInputStream(String uri)
      throws Persistence.PersistenceException;

  public abstract InputStream cacheInputStream(String uri, InputStream inputStream)
      throws Persistence.PersistenceException;

  /**
   * Empties the specific part of the persistence.
   * 
   * When called, the persistence storage is ensured to be available.
   */
  protected abstract void empty()
      throws Persistence.PersistenceException;

  public final String getStorageDirectoryPath()
  {
    return storageDirectoryPath;
  }

  /**
   * Totally clears the cache related to the current instance.
   */
  public final synchronized void clear()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Emptying the persistence instance");
    }
    if (storageBackendAvailable == true)
    {
      empty();
    }
    uriUsages.clear();
    beingProcessed.clear();
  }

  /**
   * Totally clears all caches.
   */
  public static synchronized void clearAll()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Emptying all persistence instances");
    }
    for (int index = 0; index < Persistence.CACHES_COUNT; index++)
    {
      Persistence.instances[index].clear();
    }
  }

}
