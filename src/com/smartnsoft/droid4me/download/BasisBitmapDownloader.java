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
import android.os.Handler;
import android.view.View;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Responsible for downloading the bitmaps in dedicated threads and to bind them to Android {@link View Views}.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public abstract class BasisBitmapDownloader
    extends DownloadInstructions
{

  protected static final Logger log = LoggerFactory.getInstance("BitmapDownloader");

  public final static DownloadInstructions.Instructions ABSTRACT_INSTRUCTIONS = new DownloadInstructions.AbstractInstructions();

  protected final class UsedBitmap
      implements Comparable<BasisBitmapDownloader.UsedBitmap>
  {

    private final Reference<Bitmap> bitmapReference;

    private Bitmap bitmap;

    private final int memoryConsumption;

    private int accessCount = 0;

    private int bindingCount = 0;

    /**
     * Redundant but there for optimization reasons.
     */
    public final String url;

    public UsedBitmap(Bitmap bitmap, String url)
    {
      if (useReferences == true)
      {
        // We use a soft reference, because we do not want our reference to be garbaged automatically at next garbage collection (which is the case
        // with the WeakReference)
        bitmapReference = new SoftReference<Bitmap>(bitmap);
        keepBitmap();
      }
      else
      {
        bitmapReference = null;
        this.bitmap = bitmap;
      }
      this.url = url;
      memoryConsumption = (bitmap == null ? 0 : bitmap.getWidth() * bitmap.getHeight() * 4);
    }

    public Bitmap getBitmap()
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

    public void rememberBinding(View view)
    {
      if (recycleMap == false)
      {
        return;
      }
      final BasisBitmapDownloader.UsedBitmap otherUsedBitmap = (BasisBitmapDownloader.UsedBitmap) view.getTag();
      if (otherUsedBitmap != null)
      {
        otherUsedBitmap.bindingCount--;
        // if (log.isDebugEnabled())
        // {
        // log.debug("The bitmap corresponding to the URL '" + url + "' has not been bound for the first time with a view");
        // }
      }
      else
      {
        // if (log.isDebugEnabled())
        // {
        // log.debug("The bitmap corresponding to the URL '" + url + "' is bound for the first time with an view");
        // }
      }
      bindingCount++;
      view.setTag(this);
    }

    public void rememberAccessed()
    {
      accessCount++;
    }

    public int compareTo(BasisBitmapDownloader.UsedBitmap another)
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
  protected final Map<String, BasisBitmapDownloader.UsedBitmap> cache = new HashMap<String, BasisBitmapDownloader.UsedBitmap>();

  private long memoryConsumption = 0;

  private boolean cleanUpInProgress;

  protected BasisBitmapDownloader(String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
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
   *          the view that will be bound with the retrieved {@link Bitmap}
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
  public abstract void get(View view, String bitmapUid, Object imageSpecs, Handler handler, DownloadInstructions.Instructions instructions);

  /**
   * Provides the same feature as {@link #computeAndGetUrl(View, String, Object, Handler, BasisBitmapDownloader.Instructions)}, except that it may be
   * called from the GUI thread and blocking.
   * 
   * @param isBlocking
   *          indicates whether the call is blocking. It <code>true</code>, the call must be done from the GUI thread!
   */
  public abstract void get(boolean isBlocking, View view, String bitmapUid, Object imageSpecs, Handler handler, DownloadInstructions.Instructions instructions);

  /**
   * Empties the cache, so that no more bitmap is available in memory.
   * 
   * <p>
   * All operations in progress should be canceled.
   * </p>
   */
  public abstract void empty();

  protected final BasisBitmapDownloader.UsedBitmap getUsedBitmapFromCache(String url)
  {
    synchronized (cache)
    {
      final BasisBitmapDownloader.UsedBitmap usedBitmap = cache.get(url);
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

  protected final BasisBitmapDownloader.UsedBitmap putInCache(String url, Bitmap bitmap)
  {
    // if (log.isDebugEnabled())
    // {
    // log.debug("The thread '" + Thread.currentThread().getName() + "' put in cache the bitmap with the URL '" + url + "' (size=" + cache.size() +
    // ")");
    // }
    final BasisBitmapDownloader.UsedBitmap usedBitmap = new BasisBitmapDownloader.UsedBitmap(bitmap, url);
    synchronized (cache)
    {
      final UsedBitmap previousUsedBitmap = cache.put(url, usedBitmap);
      if (previousUsedBitmap != null)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Putting twice in cache the bitmap corresponding to the URL '" + url + "'!");
        }
        // We can consider the previous entry as released
        memoryConsumption -= previousUsedBitmap.getMemoryConsumption();
      }
    }
    final int bitmapSize = usedBitmap.getMemoryConsumption();
    // log.debug("The bitmap consumes " + bitmapSize + " (" + memoryConsumption + ") bytes and corresponds to the url '" + url + "'");
    memoryConsumption += bitmapSize;
    usedBitmap.rememberAccessed();
    cleanUpCacheIfNecessary();
    return usedBitmap;
  }

  private final void cleanUpCacheIfNecessary()
  {
    if (memoryConsumption > maxMemoryInBytes)
    {
      cleanUpCache();
    }
  }

  protected final void cleanUpCache()
  {
    if (cleanUpInProgress == true)
    {
      // This cancels all the forthcoming calls, as long as the clean-up is not over, and we do not want the caller to be hanging
      return;
    }
    cleanUpInProgress = true;
    try
    {
      synchronized (cache)
      {
        int discardedCount = 0;
        int recycledCount = 0;
        final List<BasisBitmapDownloader.UsedBitmap> toBeDiscardedUsedBitmaps = new ArrayList<BasisBitmapDownloader.UsedBitmap>(cache.values());
        if (recycleMap == true)
        {
          // First, we clean up the references to all garbage-collected bitmaps
          final Iterator<UsedBitmap> iterator = toBeDiscardedUsedBitmaps.iterator();
          while (iterator.hasNext())
          {
            final BasisBitmapDownloader.UsedBitmap usedBitmap = (BasisBitmapDownloader.UsedBitmap) iterator.next();
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
          final BasisBitmapDownloader.UsedBitmap discardedUsedCache = cache.remove(toBeDiscardedUsedBitmaps.get(index).url);
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
          // if (log.isDebugEnabled())
          // {
          // log.debug("Removed from the cache the URL " + discardedUsedCache.url + "' accessed " + discardedUsedCache.accessCount +
          // " time(s) and currently bound " + discardedUsedCache.bindingCount + " time(s)");
          // }
          index++;
        }
        // We reset the remaining usages
        for (BasisBitmapDownloader.UsedBitmap uriUsage : cache.values())
        {
          uriUsage.accessCount = 0;
        }
        if (log.isInfoEnabled())
        {
          log.info("The bitmap cache '" + name + "' has been cleaned-up (" + discardedCount + " discarded and " + recycledCount + " recycled) and it now contains " + cache.size() + " item(s), and it now consumes " + memoryConsumption + " bytes");
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
