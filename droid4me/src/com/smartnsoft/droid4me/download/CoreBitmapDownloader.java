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

package com.smartnsoft.droid4me.download;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.view.View;

import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Responsible for downloading the bitmaps in dedicated threads and to bind them to Android {@link View Views}.
 *
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public abstract class CoreBitmapDownloader<BitmapClass extends Bitmapable, ViewClass extends Viewable, HandlerClass extends Handlerable>
{

  /**
   * Contains information about a {@link CoreBitmapDownloader} instance internal state.
   *
   * @since 2012.06.07
   */
  public static class CoreAnalyticsData
  {

    public final int bitmapsCount;

    public final int cleanUpsCount;

    public final int outOfMemoryOccurences;

    protected CoreAnalyticsData(int bitmapsCount, int cleanUpsCount, int outOfMemoryOccurences)
    {
      this.bitmapsCount = bitmapsCount;
      this.cleanUpsCount = cleanUpsCount;
      this.outOfMemoryOccurences = outOfMemoryOccurences;
    }

  }

  /**
   * When defining the {@link CoreBitmapDownloader#ANALYTICS_LISTENER} variable with an implementation of this interface, it will be notified as soon
   * as the cache internal state changes.
   *
   * @since 2012.06.07
   */
  public interface AnalyticsListener
  {

    /**
     * Is invoked every time the underlying attached {@link CoreBitmapDownloader} internal state changes.
     * <p/>
     * <p>
     * In particular, this method will be invoked when the instance cache is {@link CoreBitmapDownloader#cleanUpCache() cleaned up}, or when an
     * {@link OutOfMemoryError} occurs.
     * </p>
     *
     * @param coreBitmapDownloader the instance involved in the internal state change
     * @param analyticsData        the information about the instance internal state
     */
    void onAnalytics(CoreBitmapDownloader<?, ?, ?> coreBitmapDownloader,
        CoreBitmapDownloader.CoreAnalyticsData analyticsData);

  }

  /**
   * Enables to remember the bound bitmaps.
   */
  protected final class UsedBitmap
      implements Comparable<UsedBitmap>
  {

    private final Reference<BitmapClass> bitmapReference;

    private BitmapClass bitmap;

    private final int memoryConsumption;

    private int accessCount = 0;

    private int bindingCount = 0;

    /**
     * Redundant but there for optimization reasons.
     */
    public final String url;

    public UsedBitmap(BitmapClass bitmap, String url)
    {
      if (useReferences == true)
      {
        // We use a soft reference, because we do not want our reference to be garbaged automatically at next garbage collection (which is the case
        // with the WeakReference)
        bitmapReference = new SoftReference<BitmapClass>(bitmap);
        keepBitmap();
      }
      else
      {
        bitmapReference = null;
        this.bitmap = bitmap;
      }
      this.url = url;
      memoryConsumption = (bitmap == null ? 0 : bitmap.getSizeInBytes());
    }

    public BitmapClass getBitmap()
    {
      if (useReferences == true)
      {
        return bitmapReference.get();
      }
      else
      {
        return bitmap;
      }
    }

    public void keepBitmap()
    {
      if (useReferences == true)
      {
        bitmap = bitmapReference.get();
      }
    }

    public void forgetBitmap()
    {
      if (useReferences == true)
      {
        bitmap = null;
      }
    }

    public void rememberBinding(ViewClass view)
    {
      if (recycleMap == false)
      {
        return;
      }
      @SuppressWarnings("unchecked") final UsedBitmap otherUsedBitmap = (UsedBitmap) view.getTag();
      if (otherUsedBitmap != null)
      {
        otherUsedBitmap.bindingCount--;
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug("The bitmap corresponding to the URL '" + url + "' has not been bound for the first time with a view");
        }
      }
      else
      {
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug("The bitmap corresponding to the URL '" + url + "' is bound for the first time with an view");
        }
      }
      bindingCount++;
      view.setTag(this);
    }

    public void rememberAccessed()
    {
      accessCount++;
    }

    public void rememberAccessed(UsedBitmap otherUsedBitmap)
    {
      accessCount += otherUsedBitmap.accessCount;
    }

    public int compareTo(UsedBitmap another)
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

    public int getMemoryConsumption()
    {
      return memoryConsumption;
    }

    protected int getAccessCount()
    {
      return accessCount;
    }

    protected int getBindingCount()
    {
      return bindingCount;
    }

  }

  protected static final Logger log = LoggerFactory.getInstance("BitmapDownloader");

  /**
   * A flag which enables to get debug information for the "BitmapDownloader" component: only defined for development purposes. Defaults to
   * {@code false}.
   */
  public static boolean IS_DEBUG_TRACE = false;

  /**
   * A flag which enables to log dump details about "BitmapDownloader" component, which will have an effect only provided the
   * {@link CoreBitmapDownloader#IS_DEBUG_TRACE} is set to {@code true}: only defined for development purposes. Defaults to {@code false}.
   */
  public static boolean IS_DUMP_TRACE = false;

  /**
   * When set, i.e. not {@code null} (which is the default), each instance will be notified as soon as its internal state changes.
   *
   * @see #notifyAnalyticsListener()
   */
  public static CoreBitmapDownloader.AnalyticsListener ANALYTICS_LISTENER;

  /**
   * The index of the instance, mostly useful for the logs and the analytics.
   */
  public final int instanceIndex;

  /**
   * The name of the instance, mostly useful for the logs.
   */
  public final String name;

  /**
   * Indicates the upper limit of memory that the cache is allowed to reach.
   */
  public final long highLevelMemoryWaterMarkInBytes;

  /**
   * When the cache is being cleaned-up, indicates the lower limit of memory that the cache is allowed to reach.
   */
  public final long lowLevelMemoryWaterMarkInBytes;

  /**
   * A flag which states whether the instance is enabled: if not, the commands do nothing. Defaults to {@code true}.
   */
  private boolean isEnabled = true;

  /**
   * A flag which states whether the device currently has some Internet connectivity. Defaults to {@code true}.
   */
  private boolean isConnected = true;

  /**
   * Indicates whether the instance should let the Java garbage collector handle bitmap soft references.
   */
  public final boolean useReferences;

  public final boolean recycleMap;

  /**
   * Holds the instance bitmap memory cache. The {@link Map} {@link String} entry if the
   * {@link BasisDownloadInstructions.Instructions#computeUrl(String, Object) URL} of a cached bitmap.
   * <p/>
   * <p>
   * A {@link HashMap} is used instead of a {@link java.util.Hashtable}, because we want to allow null values.
   * </p>
   */
  protected final Map<String, UsedBitmap> cache = new HashMap<String, UsedBitmap>();

  /**
   * How much memory is currently used by the bitmap memory cache.
   */
  private long memoryConsumptionInBytes = 0;

  /**
   * Indicates whether a {@link #cleanUpCache() clean up} is already running.
   */
  private boolean cleanUpInProgress;

  /**
   * The number of times the {@link #cleanUpCache()} method has actually cleaned up the cache.
   */
  protected int cleanUpsCount = 0;

  /**
   * The number of times an {@link OutOfMemoryError} has occurred for this instance.
   */
  protected int outOfMemoryOccurences = 0;

  /**
   * @param instanceIndex                   the index of the current instance. When creating multiple instances, it is important to provide a unique index, so that the
   *                                        {@link CoreBitmapDownloader.AnalyticsListener} knows what instance is at stake
   * @param name                            the name of the instance, used for the logs and the analytics
   * @param highLevelMemoryWaterMarkInBytes the number of maximum bytes space that this instance will allow cached bitmaps to take in memory
   * @param lowLevelMemoryWaterMarkInBytes  when the instance has been {@link #cleanUpCache() cleaned up}, how much space the cached bitmap will take at most
   * @param useReferences
   * @param recycleMap
   */
  protected CoreBitmapDownloader(int instanceIndex, String name, long highLevelMemoryWaterMarkInBytes,
      long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    this.instanceIndex = instanceIndex;
    this.name = name;
    this.recycleMap = recycleMap;
    this.highLevelMemoryWaterMarkInBytes = highLevelMemoryWaterMarkInBytes;
    this.lowLevelMemoryWaterMarkInBytes = lowLevelMemoryWaterMarkInBytes;
    this.useReferences = useReferences;
  }

  /**
   * By default, an instance is enabled.
   *
   * @return {@code true} if and only if the current instance is enabled, i.e. the commands will do something
   * @see #setEnabled(boolean)
   */
  public final boolean isEnabled()
  {
    return isEnabled;
  }

  /**
   * Changes the instance enabled state. This method is useful for disable an instance, without changing much code.
   *
   * @param isEnabled when set to {@code false}, any call to the
   *                  {@link #get(Viewable, String, Object, Handlerable, com.smartnsoft.droid4me.download.BasisDownloadInstructions.Instructions)} or
   *                  {@link #get(boolean, boolean, Viewable, String, Object, Handlerable, com.smartnsoft.droid4me.download.BasisDownloadInstructions.Instructions)}
   *                  methods will do nothing
   * @see #isEnabled()
   */
  public final void setEnabled(boolean isEnabled)
  {
    if (log.isInfoEnabled())
    {
      log.info("Marking the BitmapDownloader instance '" + name + "' as " + (isEnabled == true ? "enabled" : "disabled"));
    }
    this.isEnabled = isEnabled;
  }

  /**
   * Indicates the instance assumption regarding the device Internet current connectivity.
   *
   * @return {@code true} if and only if the current instance considers that the device has Internet connectivity
   * @see #setConnected(boolean)
   * @see BasisDownloadInstructions.Instructions#downloadInputStream(String, Object, String)
   */
  public final boolean isConnected()
  {
    return isConnected;
  }

  /**
   * Notifies the instance about Internet connectivity. When set to {@code false}, the
   * {@link BasisDownloadInstructions.Instructions#downloadInputStream(String, Object, String)} will not be invoked, and not bitmap will be
   * downloaded.
   *
   * @param isConnected indicates the device current Internet connectivity
   */
  public final void setConnected(boolean isConnected)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Marking the BitmapDownloader instance '" + name + "' as " + (isConnected == true ? "connected" : "disconnected"));
    }
    this.isConnected = isConnected;
  }

  /**
   * @return the current cache memory consumption, expressed in bytes
   */
  public final long getMemoryConsumptionInBytes()
  {
    return memoryConsumptionInBytes;
  }

  /**
   * This method will retrieve the bitmap corresponding to the provided identifier along with the provided instructions, and bind it to the provided
   * {@link View}. This instruction is considered as a download and binding job.
   * <p/>
   * <p>
   * Can be invoked from any thread, not only the GUI's! The method is non-blocking, which means that the job is done in an asynchronous way.
   * </p>
   * <p/>
   * Here are some noteworthy contracts that the component enforces:
   * <ul>
   * <li>The component ensures that any {@link OutOfMemoryError} exception thrown during the commands of the job are caught properly, so that the
   * caller does not have to deal with potential memory saturation problems.</li>
   * <li>The component will only download once the bitmap corresponding to a given
   * {@link BasisDownloadInstructions.Instructions#computeUrl(String, Object) URL}, even if several commands are run successively for the same URL, in
   * order to save battery and network band usage.</li>
   * </ul>
   *
   * @param view         the view that will be bound with the retrieved {@link Bitmap}. May be {@code null}, which enables to simply download a bitmap and put it
   *                     into the cache
   * @param bitmapUid    the identifier of the bitmap to retrieve. Most of the time, this is the URL of the bitmap on Internet, but it can serve as a basis to
   *                     {@link BasisBitmapDownloader.Instructions#computeUrl(String, Object) compute} the actual URL
   * @param imageSpecs   a free object that will be passed along during the bitmap retrieval workflow
   * @param handler      will be used for performing GUI-thread operations
   * @param instructions the instructions that will be invoked during the retrieval workflow
   * @see #get(boolean, boolean, Viewable, String, Object, Handlerable, com.smartnsoft.droid4me.download.BasisDownloadInstructions.Instructions)
   */
  public abstract void get(ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions);

  /**
   * Provides the same feature as {@link #computeAndGetUrl(View, String, Object, HandlerClass, BasisBitmapDownloader.Instructions)}, except that it
   * may be called from the GUI thread and blocking.
   * <p/>
   * <p>
   * Invoking that method with the two {@code isPreBlocking} and {@code isDownloadBlocking} parameters set to {@code true} will execute the command
   * from the calling thread, which involves that the caller should be the UI thread, or that the provided {@code view} parameter is {@code null}.
   * This type of invocation is especially useful when the download and bind command must be run in a synchronous way.
   * </p>
   *
   * @param isPreBlocking      indicates whether the generated command first step will be executed directly from the calling thread. If {@code true}, the call must be
   *                           done from the GUI thread, or the provided {@code view} must be {@code null}!
   * @param isDownloadBlocking indicates whether the generated command second step will be executed directly from the calling thread. If the previous
   *                           {@code isPreBlocking} argument is set to {@code false}, this parameter is ignored. If {@code true}, the call must be done from the GUI
   *                           thread, or the provided {@code view} must be {@code null}!
   * @see #get(Viewable, String, Object, Handlerable, com.smartnsoft.droid4me.download.BasisDownloadInstructions.Instructions)
   */
  public abstract void get(boolean isPreBlocking, boolean isDownloadBlocking, ViewClass view, String bitmapUid,
      Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions);

  /**
   * Empties the cache, so that no more bitmap is available in memory.
   * <p/>
   * <p>
   * All operations in progress should be canceled.
   * </p>
   */
  public abstract void clear();

  protected final UsedBitmap getUsedBitmapFromCache(String url)
  {
    synchronized (cache)
    {
      final UsedBitmap usedBitmap = cache.get(url);
      if (usedBitmap != null)
      {
        if (usedBitmap.getBitmap() != null)
        {
          usedBitmap.keepBitmap();
          return usedBitmap;
        }
        else
        {
          // The underlying bitmap has been garbaged, and we discard it
          memoryConsumptionInBytes -= usedBitmap.getMemoryConsumption();
          cache.remove(url);
        }
      }
    }
    return null;
  }

  protected final UsedBitmap putInCache(String url, BitmapClass bitmap)
  {
    final UsedBitmap usedBitmap = new UsedBitmap(bitmap, url);
    synchronized (cache)
    {
      final UsedBitmap previousUsedBitmap = cache.put(url, usedBitmap);
      if (previousUsedBitmap != null)
      {
        // This may happen if the same bitmap URL has been asked several times while being downloaded
        // We can consider the previous entry as released
        memoryConsumptionInBytes -= previousUsedBitmap.getMemoryConsumption();
        usedBitmap.rememberAccessed(previousUsedBitmap);
      }
    }
    if (IS_DEBUG_TRACE && log.isDebugEnabled())
    {
      log.debug("The thread '" + Thread.currentThread().getName() + "' put in cache the bitmap with the URL '" + url + "'");
    }
    final int bitmapSize = usedBitmap.getMemoryConsumption();
    if (IS_DEBUG_TRACE && log.isDebugEnabled())
    {
      log.debug("The bitmap consumes " + bitmapSize + " (" + memoryConsumptionInBytes + ") bytes and corresponds to the url '" + url + "'");
    }
    memoryConsumptionInBytes += bitmapSize;
    usedBitmap.rememberAccessed();

    // If the cache water mark upper limit has been reached, the cache is cleared
    if (memoryConsumptionInBytes > highLevelMemoryWaterMarkInBytes)
    {
      cleanUpCache();
    }

    notifyAnalyticsListener();
    return usedBitmap;
  }

  /**
   * Is responsible for cleaning up the cache, until the low-level memory water mark is reached.
   * <p/>
   * <p>
   * If o clean-up is already {@link #cleanUpInProgress in progress}, the method returns immediately without processing anything.
   * </p>
   *
   * @see #cleanUpCacheInstance()
   */
  protected final void cleanUpCache()
  {
    if (cleanUpInProgress == true)
    {
      // This cancels all the forthcoming calls, as long as the clean-up is not over, and we do not want the caller to be hanging
      return;
    }
    final long start = System.currentTimeMillis();
    cleanUpInProgress = true;
    if (IS_DEBUG_TRACE && log.isDebugEnabled())
    {
      log.debug("Running a cache clean-up from thread '" + Thread.currentThread().getName() + "'");
    }
    try
    {
      synchronized (cache)
      {
        cleanUpCacheInstance();
      }
      if (log.isInfoEnabled())
      {
        log.info("The bitmap cache '" + name + "' has been cleaned-up in " + (System.currentTimeMillis() - start) + " ms and it now contains " + cache.size() + " item(s), and it now consumes " + memoryConsumptionInBytes + " byte(s)");
      }
      cleanUpsCount++;
    }
    finally
    {
      cleanUpInProgress = false;
    }
  }

  /**
   * This method is responsible for actually cleaning up the memory cache, by enforcing the {@link #highLevelMemoryWaterMarkInBytes} and
   * {@link #lowLevelMemoryWaterMarkInBytes} limits. This method is aimed at being overridden, so as to set up another clean up policy.
   * <p/>
   * <p>
   * It is responsible for cleaning up the {@link #cache} attribute and update the {@link #memoryConsumptionInBytes} attribute accordingly.
   * </p>
   *
   * @see #cleanUpCache()
   */
  protected void cleanUpCacheInstance()
  {
    int discardedCount = 0;
    int recycledCount = 0;
    final List<UsedBitmap> toBeDiscardedUsedBitmaps = new ArrayList<UsedBitmap>(cache.values());
    if (recycleMap == true)
    {
      // First, we clean up the references to all garbage-collected bitmaps
      final Iterator<UsedBitmap> iterator = toBeDiscardedUsedBitmaps.iterator();
      while (iterator.hasNext())
      {
        final UsedBitmap usedBitmap = iterator.next();
        if (usedBitmap.getBitmap() == null)
        {
          iterator.remove();
        }
      }
    }
    Collections.sort(toBeDiscardedUsedBitmaps);
    int index = 0;
    while (memoryConsumptionInBytes > lowLevelMemoryWaterMarkInBytes && index < toBeDiscardedUsedBitmaps.size())
    {
      final UsedBitmap discardedUsedCache = cache.remove(toBeDiscardedUsedBitmaps.get(index).url);
      memoryConsumptionInBytes -= discardedUsedCache.getMemoryConsumption();
      // We make the bitmap as recycled, so that it is actually removed from memory, only if it not being used
      if (recycleMap == true && discardedUsedCache.bindingCount <= 0)
      {
        if (discardedUsedCache.getBitmap() != null)
        {
          discardedUsedCache.getBitmap().recycle();
          recycledCount++;
        }
      }
      discardedCount++;
      if (IS_DEBUG_TRACE && log.isDebugEnabled())
      {
        log.debug("Removed from the cache the bitmap with URL '" + discardedUsedCache.url + "' accessed " + discardedUsedCache.accessCount + " time(s) and currently bound " + discardedUsedCache.bindingCount + " time(s)");
      }
      index++;
    }
    // We reset the remaining usages
    for (UsedBitmap uriUsage : cache.values())
    {
      uriUsage.accessCount = 0;
    }
    if (log.isInfoEnabled())
    {
      log.info("The cleaning up the bitmap cache '" + name + "' statistics are: " + discardedCount + " discarded and " + recycledCount + " recycled");
    }
  }

  /**
   * Dumps the analytics about the current state of the instance.
   */
  protected void dump()
  {
    if (IS_DUMP_TRACE && IS_DEBUG_TRACE && log.isDebugEnabled())
    {
      log.debug("'" + name + "' statistics: " + "cache.size()=" + cache.size());
    }
    notifyAnalyticsListener();
  }

  /**
   * Forces the BitmapDownloader to send its analytics. If the {@link CoreBitmapDownloader#ANALYTICS_LISTENER} is not {@code null}, it will be
   * notified with the latest analytics.
   */
  public final void notifyAnalyticsListener()
  {
    if (CoreBitmapDownloader.ANALYTICS_LISTENER != null)
    {
      CoreBitmapDownloader.ANALYTICS_LISTENER.onAnalytics(this, computeAnalyticsData());
    }
  }

  /**
   * @return the analytics currently available
   */
  protected CoreAnalyticsData computeAnalyticsData()
  {
    return new CoreBitmapDownloader.CoreAnalyticsData(cache.size(), cleanUpsCount, outOfMemoryOccurences);
  }

}
