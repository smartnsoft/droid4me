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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.InputAtom;
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
   * Defines a basis policy/strategy interface used when {@link Persistence#cleanUp() cleaning up} the persistence instance.
   *
   * @see Persistence#cleanUp()
   * @since 2012.03.13
   */
  public interface CleanUpPolicy
  {

  }

  /**
   * The exception thrown when an error occurs during a persistence operation.
   * <p/>
   * <p>
   * For the time being, only the classes in the current package can create such exception.
   * </p>
   */
  public static final class PersistenceException
      extends Error
  {

    private static final long serialVersionUID = -5246441820601050842L;

    PersistenceException()
    {
    }

    PersistenceException(Throwable throwable)
    {
      super(throwable);
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
  protected static final class UriUsage
      implements Comparable<Persistence.UriUsage>
  {

    /**
     * How many times the URI has been accessed.
     */
    private int accessCount = 0;

    /**
     * If applicable, the file path of the persisted data.
     */
    public final String storageFilePath;

    /**
     * Redundant but there for optimization reasons.
     */
    public final String uri;

    protected UriUsage(String storageFilePath, String uri)
    {
      this.storageFilePath = storageFilePath;
      this.uri = uri;
    }

    /**
     * @return how many times the underlying URI has been accessed. Starts from {@code 0}
     * @see #accessed()
     */
    protected final int getAccessCount()
    {
      return accessCount;
    }

    /**
     * Indicates that the underlying URI has been accessed once again.
     *
     * @return how many time the URI has been accessing, including the current call
     * @see #getAccessCount()
     */
    public int accessed()
    {
      return ++accessCount;
    }

    @Override
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

  /**
   * Used by the classes overriding the {@link Persistence} class, so as to keep track of how many time each persisted URI has been accessed.
   */
  protected static final class UriUsages
  {

    /**
     * The map which contains the persisted URIs as keys, and their usage as a value.
     */
    private final Map<String, Persistence.UriUsage> uriUsages = new HashMap<String, Persistence.UriUsage>();

    /**
     * A counter which is incremented every time a URI is being declared {@link #put(String, UriUsage) persisted}.
     */
    private int index = 0;

    /**
     * Only a {@link Persistence} instance should be allowed to create such an instance.
     */
    private UriUsages()
    {
    }

    public int getIndex()
    {
      return index;
    }

    protected void setIndex(int index)
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

  /**
   * All the {@link Persistence} instances which have been creating once the {@link Persistence#getInstance(int)} method has been invoked.
   */
  private static volatile Persistence[] instances;

  /**
   * The directory paths of the instances.
   * <p/>
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
   * <p/>
   * <p>
   * Think of tuning this parameter in case the application stores large data.
   * </p>
   */
  public static int MAXIMUM_URI_CONTENTS_SIZE_IN_BYTES = 512 * 1024;

  /**
   * Remembers whether the instance is currently initialized.
   */
  private boolean isInitialized;

  /**
   * Remembers whether the storage back-end is currently available.
   */
  private boolean storageBackendAvailable;

  /**
   * The location of the folder where the data are being persisted, if applicable.
   */
  private final String storageDirectoryPath;

  /**
   * The persistence index among all instances.
   */
  protected final int instanceIndex;

  /**
   * Indicates how the persisted URI are being accessed.
   * <p/>
   * <p>
   * All implementations are not required to update this field, it is just here to help.
   * </p>
   */
  protected final UriUsages uriUsages = new UriUsages();

  /**
   * Holds all the URIs which are currently being persisted.
   * <p/>
   * <p>
   * All implementations are not required to update this field, it is just here to help.
   * </p>
   */
  protected final Set<String> beingProcessed = new HashSet<String>();

  /**
   * @param outputStream it is closed by this method
   */
  // TODO: simplify all this!
  public static InputStream storeInputStream(OutputStream outputStream, InputStream inputStream, boolean closeInput,
      String logMessageSuffix)
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
   * Equivalent to calling {@link #getInstance(int)} with the position equals to 0.
   */
  public static Persistence getInstance()
  {
    return Persistence.getInstance(0);
  }

  /**
   * Enables to access the various persistence instances. The number of instantiated occurrences is defined by the {@link Persistence#INSTANCES_COUNT}
   * variable, which must have been set beforehand.
   * <p/>
   * <p>
   * This will cause the {@link Persistence#initialize()} method to be invoked for every instance.
   * </p>
   * <p/>
   * <p>
   * Note that the instantiation is lazy.
   * </p>
   *
   * @throws Persistence.PersistenceException in case a problem occurred while initializing the persistence
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
              // The initializing is lazily done at runtime, when necessary
              // newInstances[index].initialize();
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
   * The unique constructor.
   * <p/>
   * <p>
   * The {@link #initialize()} method does not need to be explicitly invoked before using the instance, as long as the {@code XXXInstance} methods are
   * not invoked, because the other methods will invoke it.
   * </p>
   *
   * @param storageDirectoryPath the location where the persistence should be performed
   * @param instanceIndex        the ordinal of the instance which is bound to be created. Starts with {@code 0}
   * @see #initialize()
   */
  protected Persistence(String storageDirectoryPath, int instanceIndex)
  {
    this.instanceIndex = instanceIndex;
    this.storageDirectoryPath = storageDirectoryPath;
  }

  /**
   * Indicates whether the underlying back-end storage is available.
   *
   * @param storageBackendAvailable {@code true} if and only if the back-end storage is available
   */
  protected final void setStorageBackendAvailable(boolean storageBackendAvailable)
  {
    this.storageBackendAvailable = storageBackendAvailable;
  }

  /**
   * Indicates whether the current instance has already been initialized.
   *
   * @return {@code true} if and only if the current instance is currently initialized
   * @see #initializeInstance()
   */
  public final boolean isInitialized()
  {
    return isInitialized;
  }

  /**
   * @return the directory path of the persistence provided in the {@link #Persistence(String, int) constructor}
   */
  public final String getStorageDirectoryPath()
  {
    return storageDirectoryPath;
  }

  /**
   * Initializes the persistence instance. If the instance is already {@link #isInitialized() initialized}, the method does nothing.
   * <p/>
   * <p>
   * It will be lazily invoked when the object is used for the first time when using the {@link #getInstance(int)} method.
   * </p>
   *
   * @throws Persistence.PersistenceException if something went wrong during the initialization
   */
  public final synchronized void initialize()
      throws Persistence.PersistenceException
  {
    if (isInitialized == true)
    {
      return;
    }
    initializeInstance();
    isInitialized = true;
  }

  /**
   * Is responsible for performing the {@link #initialize()} method job.
   * <p/>
   * <p>
   * The implementation must set the {@link #storageBackendAvailable} flag accordingly.
   * </p>
   *
   * @throws Persistence.PersistenceException if something went wrong during the initialization
   * @see #isInitialized
   */
  protected abstract void initializeInstance()
      throws Persistence.PersistenceException;

  /**
   * Enables to access all the stored URIs. Each persistent entry is represented by a a local URI, and this method returns all of them.
   * <p/>
   * <p>
   * Note that the ordering of the returned list is not guaranteed to be stable.
   * </p>
   *
   * @return the list of the stored URIs stored in the persistence instance
   * @throws Persistence.PersistenceException in case an error occurred while computing the URIs or if the storage back-end is not available
   * @see #getUrisInstance()
   */
  public final List<String> getUris()
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    return getUrisInstance();
  }

  /**
   * Is responsible for performing the {@link #getUris()} method job.
   *
   * @return the list of the stored URIs stored in the persistence instance
   * @throws Persistence.PersistenceException in case an error occurred while computing the URIs
   * @see #getUris()
   */
  protected abstract List<String> getUrisInstance()
      throws Persistence.PersistenceException;

  /**
   * Indicates the latest update timestamp corresponding to an URI persistent entry.
   *
   * @return the date when the underlying persistent entry has been updated ; {@code null} if no persistent entry exists for the provided URI
   * @throws Persistence.PersistenceException in case an error occurred while processing the request or if the storage back-end is not available
   * @see #getLastUpdateInstance(String)
   */
  @Override
  public final Date getLastUpdate(String uri)
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    return getLastUpdateInstance(uri);
  }

  /**
   * Is responsible for performing the {@code getLastUpdate()}} method job.
   *
   * @return the date when the underlying persistent entry has been updated ; {@code null} if no persistent entry exists for the provided URI
   * @throws Persistence.PersistenceException in case an error occurred while processing the request
   * @see #getLastUpdate(String)
   * @see #getLastUpdate(Object)
   */
  protected abstract Date getLastUpdateInstance(String uri)
      throws Persistence.PersistenceException;

  /**
   * Is responsible for extracting an input stream from the persistence related to the provided URI.
   *
   * @param uri the URI which identifies the stream to extract
   * @return {@code null} if and only if there is no data associated to the given URI; otherwise, its related wrapped input stream is returned
   * @throws Persistence.PersistenceException in case an error occurred while extracting the input stream or if the storage back-end is not available
   * @see #readInputStreamInstance(String)
   * @see #readInputStream(String)
   * @see #readInputStreamInstance(String)
   * @see #readInputStream(Object)
   */
  public final Business.InputAtom extractInputStream(String uri)
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    return readInputStreamInstance(uri);
  }

  /**
   * Is responsible for writing persistently the stream related to the given URI.
   * <p/>
   * <p>
   * Beware: the {@link Business.InputAtom#inputStream} of the provided parameter is consumed by the method!
   * </p>
   *
   * @param uri       the URI which identifies the stream to persist
   * @param inputAtom the wrapper which contains the stream to write
   * @return a new stream wrapper, which is operational, and in particular not {@null}
   * @throws Persistence.PersistenceException if a problem occurred while persisting the data or if the storage back-end is not available
   * @see #flushInputStreamInstance(String, InputAtom)
   */
  public final Business.InputAtom flushInputStream(String uri, Business.InputAtom inputAtom)
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    return flushInputStreamInstance(uri, inputAtom);
  }

  /**
   * Is responsible for performing the {@link #flushInputStream(String, InputAtom)}} method job.
   *
   * @param uri       the URI which identifies the stream to persist
   * @param inputAtom the wrapper which contains the stream to write
   * @return a new stream wrapper, which is operational
   * @throws Persistence.PersistenceException if a problem occurred while persisting the data
   * @see #flushInputStream(String, InputAtom)
   */
  protected abstract Business.InputAtom flushInputStreamInstance(String uri, Business.InputAtom inputAtom)
      throws Persistence.PersistenceException;

  /**
   * @throws Persistence.PersistenceException if a problem occurred while reading the data or if the storage back-end is not available
   * @see #readInputStreamInstance(String)
   */
  @Override
  public final Business.InputAtom readInputStream(String uri)
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    return readInputStreamInstance(uri);
  }

  /**
   * Is responsible for performing the {@code readInputStream()} method job.
   *
   * @param uri the URI which identifies the stream to persist
   * @throws Persistence.PersistenceException if a problem occurred while reading the data
   * @see #readInputStream(String)
   * @see #readInputStream(String)
   * @see #readInputStream(String)
   */
  protected abstract Business.InputAtom readInputStreamInstance(String uri)
      throws Persistence.PersistenceException;

  /**
   * @throws Persistence.PersistenceException if a problem occurred while writing the data or if the storage back-end is not available
   * @see #writeInputStreamInstance(String, InputAtom, boolean)
   */
  @Override
  public final InputStream writeInputStream(String uri, Business.InputAtom inputAtom, boolean returnStream)
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    return writeInputStreamInstance(uri, inputAtom, returnStream);
  }

  /**
   * Is responsible for performing the {@code writeInputStream()} method job.
   *
   * @param uri          the URI which identifies the stream to persist
   * @param inputAtom    the binary form of the data to store for the provided URI
   * @param returnStream if set {@code true}, a valid {@link InputStream} corresponding to the provided {@link Business.InputAtom#inputStream} must be returned ;
   *                     if set to {@code false}, {@code null} must be returned
   * @return {@code null} if {@code returnStream} is set to {@code false} ; if {@code returnStream} is set to {@code true}, an input stream which
   * holds the same data as the provided {@code inputAtom} {@link Business.InputAtom#inputStream}
   * @throws Persistence.PersistenceException if a problem occurred while writing the data
   * @see #writeInputStream(String, InputAtom, boolean)
   * @see #writeInputStream(Object, InputAtom, boolean)
   */
  protected abstract InputStream writeInputStreamInstance(String uri, Business.InputAtom inputAtom,
      boolean returnStream)
      throws Persistence.PersistenceException;

  /**
   * @throws Persistence.PersistenceException if a problem occurred while erasing the data or if the storage back-end is not available
   * @see #removeInstance(String)
   */
  @Override
  public final void remove(String uri)
      throws Persistence.PersistenceException
  {
    checkAndInitializeIfNecessary();
    removeInstance(uri);
  }

  /**
   * Is responsible for performing the {@code remove()} method job.
   *
   * @param uri the URI which identifies the stream to delete
   * @throws Persistence.PersistenceException if a problem occurred while erasing the data
   * @see #remove(String)
   * @see #remove(Object)
   */
  protected abstract void removeInstance(String uri)
      throws Persistence.PersistenceException;

  /**
   * Enables to define the way the persistence cache clean up is performed.
   * <p/>
   * <p>
   * This method is supposed to be invoked by the {@link #cleanUpInstance(CleanUpPolicy)} method, when it attempts to determine the
   * {@link Persistence.CleanUpPolicy} to use.
   * </p>
   *
   * @return a clean-up policy ; may be {@code null}, and in that case, the {@link #computePolicyAndCleanUpInstance()} should not run the
   * {@link #cleanUpInstance(CleanUpPolicy)} method
   */
  protected abstract <CleanUpPolicyClass extends Persistence.CleanUpPolicy> CleanUpPolicyClass computeCleanUpPolicy();

  /**
   * Cleans-up the persistence.
   * <p/>
   * <p>
   * This will remove persistent entries depending on the provided {@code cleanUpPolicy}.
   * </p>
   *
   * @param cleanUpPolicy the policy to use for cleaning up the instance
   * @throws if any problem occurs while cleaning up the instance
   * @see #cleanUp()
   */
  protected abstract <CleanUpPolicyClass extends Persistence.CleanUpPolicy> void cleanUpInstance(
      CleanUpPolicyClass cleanUpPolicy)
      throws Persistence.PersistenceException;

  /**
   * Empties the persistence.
   * <p/>
   * <p>
   * Once called, the persistence storage will be emptied, but the instance can keep on being used as is.
   * </p>
   *
   * @throws if any problem occurs while emptying the persistence
   * @see #clear()
   */
  protected abstract void clearInstance()
      throws Persistence.PersistenceException;

  /**
   * Closes the persistence.
   * <p/>
   * <p>
   * Once called, the persistence will need to be {@link #initialize() initialized again}, before being used.
   * </p>
   *
   * @throws if any problem occurs while closing the persistence
   * @see #close()
   */
  protected abstract void closeInstance()
      throws Persistence.PersistenceException;

  /**
   * Cleans up the cache related to the current instance. This will remove persistent entries depending on the computed
   * {@link #computeCleanUpPolicy()} : if this policy is {@code null}, nothing is done. The method will invoke the
   * {@link #computePolicyAndCleanUpInstance()} method.
   * <p/>
   * <p>
   * During this operation, the instance should not be accessed, and the implementation is not responsible for ensuring that: it is up to the caller
   * to make sure that no other instance method is being invoked during its execution!
   * </p>
   *
   * @throws if any problem occurs while cleaning up the instance
   * @see #cleanUpInstance(CleanUpPolicy)
   * @see #clear()
   * @see #close()
   */
  public final synchronized void cleanUp()
      throws Persistence.PersistenceException
  {
    final long start = System.currentTimeMillis();
    if (log.isInfoEnabled())
    {
      log.info("Cleaning up the persistence instance " + instanceIndex);
    }
    checkAndInitializeIfNecessary();
    if (storageBackendAvailable == true)
    {
      computePolicyAndCleanUpInstance();
    }
    uriUsages.clear();
    beingProcessed.clear();
    if (log.isInfoEnabled())
    {
      log.info("Cleaning up the persistence instance " + instanceIndex + " took " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  /**
   * Is responsible for invoking the {@link #computeCleanUpPolicy()} and then, if the returned value not {@code null}, invoke the
   * {@link #cleanUpInstance(CleanUpPolicy)} method.
   *
   * @throws Persistence.PersistenceException if something goes wrong during the {@link #cleanUpInstance(CleanUpPolicy)} method execution
   */
  protected abstract void computePolicyAndCleanUpInstance()
      throws Persistence.PersistenceException;

  /**
   * Totally clears the cache related to the current instance. This will delete all the entries. The method will invoke the {@link #clearInstance()}
   * method.
   * <p/>
   * <p>
   * Once cleared, the current instance can be used as is.
   * </p>
   *
   * @throws if any problem occurs while clearing the persistence
   * @see #clearInstance()
   * @see #close()
   * @see #clearAll()
   */
  public final synchronized void clear()
      throws Persistence.PersistenceException
  {
    if (log.isInfoEnabled())
    {
      log.info("Emptying the persistence instance " + instanceIndex);
    }
    checkAndInitializeIfNecessary();
    if (storageBackendAvailable == true)
    {
      clearInstance();
    }
    uriUsages.clear();
    beingProcessed.clear();
  }

  /**
   * Closes the current instance. The method will invoke the {@link #closeInstance()} method.
   * <p/>
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
    if (log.isInfoEnabled())
    {
      log.info("Closing the persistence instance " + instanceIndex);
    }
    if (storageBackendAvailable == true)
    {
      closeInstance();
    }
    uriUsages.clear();
    beingProcessed.clear();
    storageBackendAvailable = false;
    isInitialized = false;
  }

  /**
   * Cleans up all persistence instances. The method will invoke the {@link #cleanUp()} method on each instance.
   */
  public static synchronized void cleanUpAll()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Cleaning up all persistence instances");
    }
    for (int index = 0; index < Persistence.INSTANCES_COUNT; index++)
    {
      Persistence.getInstance(index).cleanUp();
    }
  }

  /**
   * Clears all persistence instances. The method will invoke the {@link #clear()} method on each instance.
   */
  public static synchronized void clearAll()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Clearing all persistence instances");
    }
    for (int index = 0; index < Persistence.INSTANCES_COUNT; index++)
    {
      Persistence.getInstance(index).clear();
    }
  }

  /**
   * Closes all persistence instances. The method will invoke the {@link #close()} method on each instance.
   */
  public static synchronized void closeAll()
      throws Persistence.PersistenceException
  {
    if (log.isDebugEnabled())
    {
      log.debug("Closing all persistence instances");
    }
    for (int index = 0; index < Persistence.INSTANCES_COUNT; index++)
    {
      Persistence.getInstance(index).close();
    }
  }

  private void checkAndInitializeIfNecessary()
      throws Persistence.PersistenceException
  {
    initialize();
    if (storageBackendAvailable == false)
    {
      throw new Persistence.PersistenceException("Unailable back-end storage");
    }
  }

}
