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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    PersistenceException()
    {
    }

    PersistenceException(String message)
    {
      super(message);
    }

    PersistenceException(String message, Throwable throwable)
    {
      super(message, throwable);
    }

  }

  /**
   * Defined in order to count how many times an URI has been consumed.
   */
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

  protected static final class UriUsages
  {

    private final Map<String, Persistence.UriUsage> uriUsages = new HashMap<String, Persistence.UriUsage>();

    private int index = 0;

    public int getIndex()
    {
      return index;
    }

    public void setIndex(int index)
    {
      this.index = index;
    }

    public void clear()
    {
      uriUsages.clear();
      index = 0;
    }

    public void put(String uri, UriUsage uriUsage)
    {
      uriUsages.put(uri, uriUsage);
      index++;
    }

    public UriUsage get(String uri)
    {
      return uriUsages.get(uri);
    }

    public UriUsage remove(String uri)
    {
      return uriUsages.remove(uri);
    }

    public int size()
    {
      return uriUsages.size();
    }

    public void resetAccessCount()
    {
      for (Persistence.UriUsage uriUsage : uriUsages.values())
      {
        uriUsage.accessCount = 0;
      }
    }

    public List<UriUsage> getUriUsages()
    {
      return new ArrayList<Persistence.UriUsage>(uriUsages.values());
    }

    public List<String> getUris()
    {
      final List<String> uris = new ArrayList<String>();
      for (Entry<String, Persistence.UriUsage> entry : uriUsages.entrySet())
      {
        uris.add(entry.getKey());
      }
      return uris;
    }

  }

  protected final static Logger log = LoggerFactory.getInstance(Persistence.class);

  private static volatile Persistence[] instances;

  /**
   * The directory paths of the instances.
   * 
   * <p>
   * The number of elements in this array must be equal to {@link Persistence#INSTANCES_COUNT}.
   * </p>
   */
  public static String[] CACHE_DIRECTORY_PATHS = new String[] { "/sdcard" };

  /**
   * The number of instances.
   */
  public static int INSTANCES_COUNT = 1;

  /**
   * The fully qualified name of the {@link Persistence} instances implementation.
   */
  public static String IMPLEMENTATION_FQN;

  /**
   * The maximum size, expressed in bytes, of the data associated to an URI.
   * 
   * <p>
   * Think of tuning this parameter in case the application stores large data.
   * </p>
   */
  public static int MAXIMUM_URI_CONTENTS_SIZE_IN_BYTES = 512 * 1024;

  /**
   * Remembers whether the storage back-end is currently available.
   */
  protected boolean storageBackendAvailable;

  private final String storageDirectoryPath;

  protected final UriUsages uriUsages;

  protected final Set<String> beingProcessed = new HashSet<String>();

  /**
   * Equivalent to calling {@link #getInstance(0)}.
   */
  public static Persistence getInstance()
  {
    return Persistence.getInstance(0);
  }

  /**
   * Enables to access the various persistence instances. The number of instantiated occurrences is defined by the {@link Persistence#INSTANCES_COUNT}
   * variable, which must have been set beforehand.
   * 
   * <p>
   * This will cause the {@link Persistence#initialize()} method to be invoked for every instance.
   * </p>
   * 
   * <p>
   * Note that the instantiation is lazy.
   * </p>
   * 
   * @throws Persistence.PersistenceException
   *           in case a problem occurred while initializing the persistence
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
          if (Persistence.IMPLEMENTATION_FQN == null)
          {
            throw new Persistence.PersistenceException("The persistence implementation class has not been set!");
          }
          try
          {
            // We do that way, because the Persistence initialization may be long, and we want the "instances" attribute to be atomic
            final Class<? extends Persistence> implementationClass = (Class<? extends Persistence>) Class.forName(Persistence.IMPLEMENTATION_FQN);
            final Persistence[] newInstances = (Persistence[]) Array.newInstance(implementationClass, Persistence.INSTANCES_COUNT);
            final Constructor<? extends Persistence> constructor = implementationClass.getDeclaredConstructor(String.class, int.class);
            for (int index = 0; index < Persistence.INSTANCES_COUNT; index++)
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
            throw new Persistence.PersistenceException("Cannot instantiate properly the persistence instances", exception);
          }
        }
      }
    }
    return Persistence.instances[position];
  }

  /**
   * Enables to access all the stored URIs.
   * 
   * @return the list of the stored URIs stored in the persistence instance. The ordering is not guaranteed to be stable
   */
  public abstract List<String> getUris();

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
      bufferedInputStream.mark(Persistence.MAXIMUM_URI_CONTENTS_SIZE_IN_BYTES);
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
    uriUsages = new UriUsages();
  }

  /**
   * Initializes the persistence instance.
   * 
   * <p>
   * It will be Invoked when the object is used for the first time when using the {@link #getInstance(int)} method.
   * </p>
   */
  public abstract void initialize();

  /**
   * Is responsible for extracting an input stream from the persistence related to the provided URI.
   * 
   * @param uri
   *          the URI which identifies the stream to extract
   * @return {@code null} if and only if there is no data associated to the given URI; otherwise, its related wrapped input stream is returned
   */
  public abstract Business.InputAtom extractInputStream(String uri)
      throws Persistence.PersistenceException;

  /**
   * Is responsible for writing persistently the stream related to the given URI.
   * 
   * <p>
   * Beware: the {@link Business.InputAtom#inputStream} of the provided parameter is consumed by the method!
   * </p>
   * 
   * @param uri
   *          the URI which identifies the stream to persist
   * @param inputAtom
   *          the wrapper which contains the stream to write
   * @return a new stream wrapper, which is operational
   * @throws Persistence.PersistenceException
   *           if a problem occurred while persisting the data
   */
  public abstract Business.InputAtom flushInputStream(String uri, Business.InputAtom inputAtom)
      throws Persistence.PersistenceException;

  /**
   * Empties the persistence.
   * 
   * <p>
   * Once called, the persistence storage will be emptied, but the instance can keep on being used as is.
   * </p>
   * 
   * @throws if
   *           any problem occurs while emptying the persistence
   * @see #clear()
   */
  protected abstract void clearInstance()
      throws Persistence.PersistenceException;

  /**
   * Closes the persistence.
   * 
   * <p>
   * Once called, the persistence will need to be {@link #initialize() initialized again}, before being used.
   * </p>
   * 
   * @throws if
   *           any problem occurs while closing the persistence
   * @see #close()
   */
  protected abstract void closeInstance()
      throws Persistence.PersistenceException;

  /**
   * @return the directory path of the persistence provided in the {@link #Persistence(String, int) constructor}
   */
  public final String getStorageDirectoryPath()
  {
    return storageDirectoryPath;
  }

  /**
   * Totally clears the cache related to the current instance. This will delete all the entries. The method will invoke the {@link #clearInstance()}
   * method.
   * 
   * <p>
   * Once cleared, the current instance can be used as is.
   * </p>
   * 
   * @throws if
   *           any problem occurs while clearing the persistence
   * @see #clearInstance()
   * @see #close()
   * @see #clearAll()
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
      clearInstance();
    }
    uriUsages.clear();
    beingProcessed.clear();
  }

  /**
   * Closes the current instance. The method will invoke the {@link #closeInstance()} method.
   * 
   * <p>
   * Once closed, the current instance cannot be used until an explicit {@link #initialize()} call is performed.
   * </p>
   * 
   * @see #closeInstance()
   * @see #clear()
   * @see #closeAll()
   */
  public final synchronized void close()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Closing the persistence instance");
    }
    if (storageBackendAvailable == true)
    {
      closeInstance();
    }
    uriUsages.clear();
    beingProcessed.clear();
    storageBackendAvailable = false;
  }

  /**
   * Clears all persistence instances. The method will invoke the {@link #clear()} on each instance.
   */
  public static synchronized void clearAll()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Clearing all Persistence instances");
    }
    for (int index = 0; index < Persistence.INSTANCES_COUNT; index++)
    {
      Persistence.getInstance(index).clear();
    }
  }

  /**
   * Closes all persistence instances. The method will invoke the {@link #close()} on each instance.
   */
  public static synchronized void closeAll()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Closing all Persistence instances");
    }
    for (int index = 0; index < Persistence.INSTANCES_COUNT; index++)
    {
      Persistence.getInstance(index).close();
    }
  }

}
