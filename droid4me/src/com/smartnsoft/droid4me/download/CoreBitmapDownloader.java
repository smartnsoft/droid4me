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
      @SuppressWarnings("unchecked")
      final UsedBitmap otherUsedBitmap = (UsedBitmap) view.getTag();
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

  }

  /**
   * The name of the instance, mostly useful for the logs.
   */
  public final String name;

  /**
   * Indicates the upper limit of memory that the cache is allowed to reach.
   */
  public final long maxMemoryInBytes;

  /**
   * When the cache is being cleaned-up, indicates the lower limit of memory that the cache is allowed to reach.
   */
  public final long lowLevelMemoryWaterMarkInBytes;

  /**
   * Indicates whether the instance should let the Java garbage collector handle bitmap soft references.
   */
  public final boolean useReferences;

  public final boolean recycleMap;

  /**
   * A {@link HashMap} is used instead of a {@link java.util.Hashtable}, because we want to allow null values.
   */
  protected final Map<String, UsedBitmap> cache = new HashMap<String, UsedBitmap>();

  private long memoryConsumption = 0;

  private boolean cleanUpInProgress;

  protected CoreBitmapDownloader(String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    this.name = name;
    this.recycleMap = recycleMap;
    this.maxMemoryInBytes = maxMemoryInBytes;
    this.useReferences = useReferences;
    this.lowLevelMemoryWaterMarkInBytes = lowLevelMemoryWaterMarkInBytes;
  }

  /**
   * This method will retrieve the bitmap corresponding to the provided identifier along with the provided instructions, and bind it to the provided
   * {@link View}.
   * 
   * <p>
   * Can be invoked from any thread, not only the GUI's! The method is non-blocking.
   * </p>
   * 
   * @param view
   *          the view that will be bound with the retrieved {@link Bitmap}. May be {@code null}, which enables to simply download a bitmap and put it
   *          into the cache
   * @param bitmapUid
   *          the identifier of the bitmap to retrieve. Most of the time, this is the URL of the bitmap on Internet, but it can serve as a basis to
   *          {@link BasisBitmapDownloader.Instructions#computeUrl(String, Object) compute} the actual URL
   * @param imageSpecs
   *          a free object that will be passed along during the bitmap retrieval workflow
   * @param handler
   *          will be used for performing GUI-thread operations
   * @param instructions
   *          the instructions that will be invoked during the retrieval workflow
   */
  public abstract void get(ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions);

  /**
   * Provides the same feature as {@link #computeAndGetUrl(View, String, Object, HandlerClass, BasisBitmapDownloader.Instructions)}, except that it
   * may be called from the GUI thread and blocking.
   * 
   * @param isBlocking
   *          indicates whether the call is blocking. It {@code true}, the call must be done from the GUI thread!
   */
  public abstract void get(boolean isBlocking, ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions);

  /**
   * Empties the cache, so that no more bitmap is available in memory.
   * 
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
          memoryConsumption -= usedBitmap.getMemoryConsumption();
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
        memoryConsumption -= previousUsedBitmap.getMemoryConsumption();
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
      log.debug("The bitmap consumes " + bitmapSize + " (" + memoryConsumption + ") bytes and corresponds to the url '" + url + "'");
    }
    memoryConsumption += bitmapSize;
    usedBitmap.rememberAccessed();

    // If the cache water mark upper limit has been reached, the cache is cleared
    if (memoryConsumption > maxMemoryInBytes)
    {
      cleanUpCache();
    }

    return usedBitmap;
  }

  /**
   * Is responsible for emptying the cache, until the low-level memory water mark is reached.
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
        int discardedCount = 0;
        int recycledCount = 0;
        final List<UsedBitmap> toBeDiscardedUsedBitmaps = new ArrayList<UsedBitmap>(cache.values());
        if (recycleMap == true)
        {
          // First, we clean up the references to all garbage-collected bitmaps
          final Iterator<UsedBitmap> iterator = toBeDiscardedUsedBitmaps.iterator();
          while (iterator.hasNext())
          {
            final UsedBitmap usedBitmap = (UsedBitmap) iterator.next();
            if (usedBitmap.getBitmap() == null)
            {
              iterator.remove();
            }
          }
        }
        Collections.sort(toBeDiscardedUsedBitmaps);
        int index = 0;
        while (memoryConsumption > lowLevelMemoryWaterMarkInBytes && index < toBeDiscardedUsedBitmaps.size())
        {
          final UsedBitmap discardedUsedCache = cache.remove(toBeDiscardedUsedBitmaps.get(index).url);
          memoryConsumption -= discardedUsedCache.getMemoryConsumption();
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
          log.info("The bitmap cache '" + name + "' has been cleaned-up in " + (System.currentTimeMillis() - start) + " ms (" + discardedCount + " discarded and " + recycledCount + " recycled) and it now contains " + cache.size() + " item(s), and it now consumes " + memoryConsumption + " bytes");
        }
      }
    }
    finally
    {
      cleanUpInProgress = false;
      System.gc();
    }
  }

}
