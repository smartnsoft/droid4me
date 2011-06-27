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

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Responsible for downloading the bitmaps in dedicated threads and to bind them to Android {@link View Views}.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public abstract class BasisBitmapDownloader
{

  protected static final Logger log = LoggerFactory.getInstance(BasisBitmapDownloader.class);

  /**
   * When the input stream related to an image needs to be downloaded asynchronously, this interface enables to indicate it, and to notify the owner
   * when the input stream has been actually downloaded.
   * 
   * @since 2010.05.01
   */
  public static interface InputStreamDownloadInstructor
  {

    /**
     * Indicates that the input stream should be asynchronous.
     */
    public void setAsynchronous();

    /**
     * Should be called when the input stream has just been downloaded.
     * 
     * @param inputStream
     *          should not be <code>null</code>
     */
    public void onDownloaded(InputStream inputStream);

  }

  /**
   * Enables the image downloader to know whether an image can be taken locally from a resource.
   * 
   * @since 2009.03.04
   */
  public static interface Instructions
  {

    /**
     * Given the image identifier, its size and its extra specifications, returns its URL, which will be used for downloading it.
     */
    String computeUrl(String bitmapUid, Object imageSpecs);

    /**
     * @return <code>true</code> if and only if a temporary bitmap should be used for the underlying image: in that case, the
     *         {@link Instructions#onBindTemporaryBitmap(View, String, Object)} will be invoked
     */
    boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs);

    /**
     * When a temporary image usage has been specified, is responsible for binding the image view with that temporary image.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     */
    void onBindTemporaryBitmap(View view, String bitmapUid, Object imageSpecs);

    /**
     * @return <code>true</code> if and only the image should be taken locally, instead of running into the downloading process: in that case, the
     *         {@link Instructions#onBindLocalBitmap(View, String, Object)} will be invoked
     */
    boolean hasLocalBitmap(String bitmapUid, Object imageSpecs);

    /**
     * When a local image usage has been specified, is responsible for binding the image view with that local image.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     */
    void onBindLocalBitmap(View view, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked every time once the underlying image bitmap is either not <code>null</code> ready in memory (the <code>allright</code> parameter is
     * set to <code>true</code>), or when it occurs that the bitmap could not be downloaded or is not well formed (the <code>allright</code> parameter
     * is set to <code>false</code>).
     * 
     * <p>
     * It is NOT ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param allright
     *          indicates whether the bitmap is actually available and well formed
     * @param view
     *          will be <code>null</code> if, and only if the provided {@link View} is <code>null</code>
     * @param bitmap
     *          cannot be <code>null</code> if and only if <code>allright</code> is <code>true</code>
     */
    void onBitmapReady(boolean allright, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked once the image bitmap is ready when the provided {@link View} is not <code>null</code>, whether it has been downloaded from Internet
     * or retrieved from the cache.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param downloaded
     *          indicates whether the bitmap has actually been downloaded from the network or retrieved locally
     * @return <code>false</code> if and only if the current instance did not perform the binding
     */
    boolean onBindBitmap(boolean downloaded, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked once the given the bitmap identifier, its specifications and either:
     * <ul>
     * <li>the underlying bitmap has actually been downloaded, and attached to its {@link View},</li>
     * <li>or, the underlying bitmap download was failure (wrong bitmap URL, connectivity problem).</li>
     * </ul>
     * 
     * <p>
     * Also invoked when the bitmap has been taken from a local resource, or when taken from the cache.
     * </p>
     * <p>
     * It is ensured that this notification will be invoked from the GUI thread.
     * </p>
     * 
     * @param result
     *          <code>true</code> if and only if the image bitmap has actually been downloaded and attached to its view
     */
    void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs);

    /**
     * Invoked in a non-deterministic thread, just before the image gets downloaded via the provided {@link URLConnection}.
     * 
     * <p>
     * This is a place where it is possible to tweak the urlConnection.
     * </p>
     */
    void onBeforeBitmapDownloaded(String bitmapUid, Object imageSpecs, URLConnection urlConnection);

    /**
     * Is invoked just before the stream corresponding to the image is started, provided its computed URL is not null nor empty.
     * 
     * <p>
     * This is a good place for fetching from a local cache the input stream related to the image.
     * </p>
     * 
     * @param instructor
     *          if you want the input stream download to be asynchronous, call
     *          {@link BasisBitmapDownloader.InputStreamDownloadInstructor#setAsynchronous()} and return <code>null</code> ; once the input stream has
     *          been download, invoke {@link BasisBitmapDownloader.InputStreamDownloadInstructor#onDownloaded(InputStream)}.
     * @return if <code>null</code> and {@link BasisBitmapDownloader.InputStreamDownloadInstructor#setAsynchronous()} has not been invoked, the
     *         framework will download the input from Internet; otherwise that input stream will be used for populating the image view
     */
    InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, BasisBitmapDownloader.InputStreamDownloadInstructor instructor)
        throws IOException;

  }

  public static abstract class SimpleInstructions
      implements BasisBitmapDownloader.Instructions
  {

    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, BasisBitmapDownloader.InputStreamDownloadInstructor instructor)
        throws IOException
    {
      return null;
    }

  }

  /**
   * An implementation of the {@link BasisBitmapDownloader.Instructions}, which returns the <code>bitmapUid</code> as an URL, and which does not
   * present any temporary nor local image.
   * 
   * <p>
   * Caution: this implementation supposes that the provided {@View view} is actually an {@ImageView} in the
   * {@link BasisBitmapDownloader.AbstractInstructions#onBindBitmap(boolean, View, Bitmap, String, Object)} method.
   * </p>
   */
  public static class AbstractInstructions
      extends BasisBitmapDownloader.SimpleInstructions
  {

    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      return bitmapUid;
    }

    public boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs)
    {
      return false;
    }

    public void onBindTemporaryBitmap(View view, String bitmapUid, Object imageSpecs)
    {
    }

    public boolean hasLocalBitmap(String bitmapUid, Object imageSpecs)
    {
      return false;
    }

    public void onBindLocalBitmap(View view, String bitmapUid, Object imageSpecs)
    {
    }

    public void onBeforeBitmapDownloaded(String bitmapUid, Object imageSpecs, URLConnection urlConnection)
    {
    }

    public void onBitmapReady(boolean allright, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    public boolean onBindBitmap(boolean downloaded, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs)
    {
      ((ImageView) (view)).setImageBitmap(bitmap);
      return true;
    }

    public void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs)
    {
    }

  }

  public final static BasisBitmapDownloader.Instructions ABSTRACT_INSTRUCTIONS = new BasisBitmapDownloader.AbstractInstructions();

  final class UsedBitmap
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
        // log.debug("The bitmap corresponding to the URL '" + url + "' has not been bound for the first time with an image");
        // }
      }
      else
      {
        // if (log.isDebugEnabled())
        // {
        // log.debug("The bitmap corresponding to the URL '" + url + "' is bound for the first time with an image");
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
  public String name;

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
   *          the image that will be bound with the retrieved {@link Bitmap}
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
  public abstract void get(View view, String bitmapUid, Object imageSpecs, Handler handler, BasisBitmapDownloader.Instructions instructions);

  /**
   * Provides the same feature as {@link #computeAndGetUrl(View, String, Object, Handler, BasisBitmapDownloader.Instructions)}, except that it may be
   * called from the GUI thread and blocking.
   * 
   * @param isBlocking
   *          indicates whether the call is blocking. It <code>true</code>, the call must be done from the GUI thread!
   */
  public abstract void get(boolean isBlocking, View view, String bitmapUid, Object imageSpecs, Handler handler, BasisBitmapDownloader.Instructions instructions);

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
    // log.debug("The thread '" + Thread.currentThread().getName() + "' put in cache the image with the URL '" + url + "' (size=" + cache.size()
    // + ")");
    final BasisBitmapDownloader.UsedBitmap usedBitmap = new BasisBitmapDownloader.UsedBitmap(bitmap, url);
    synchronized (cache)
    {
      final UsedBitmap previousUsedBitmap = cache.put(url, usedBitmap);
      if (previousUsedBitmap != null)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Putting twice in cache the bitmap corresponding to the image with URL '" + url + "'!");
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

  // TODO: think of doing that in another thread, because this is blocking
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
      // This cancels all the forthcoming calls, as long as the clean-up is not over
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
          log.info("The image cache '" + name + "' has been cleaned-up (" + discardedCount + " discarded and " + recycledCount + " recycled) and it now contains " + cache.size() + " item(s), and it now consumes " + memoryConsumption + " bytes");
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
