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

package com.smartnsoft.droid4me.download;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;
import com.smartnsoft.droid4me.download.gif.Gif;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Gathers in one place the download instructions contracts used by {@link BitmapDownloader}.
 *
 * @author Édouard Mercier
 * @since 2011.07.03
 */
public class DownloadInstructions
    extends BasisDownloadInstructions
{

  private static final Logger log = LoggerFactory.getInstance(DownloadInstructions.class);

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface BitmapConfigAnnotation
  {

    Config bitmapConfig() default Config.ARGB_8888;

  }

  /**
   * An implementation of the {@link BitmapableBitmap} interface specific to the Android platform.
   */
  public final static class BitmapableBitmap
      implements Bitmapable
  {

    private final Bitmap bitmap;

    /**
     * Creates a wrapper around the provided actual Android bitmap.
     *
     * @param bitmap the bitmap to be wrapped
     */
    public BitmapableBitmap(Bitmap bitmap)
    {
      this.bitmap = bitmap;
    }

    public int getSizeInBytes()
    {
      if (bitmap == null)
      {
        return 0;
      }
      // Taken from DevBytes, "Making Apps Beautiful - Part 4" Youtube episode
      if (Build.VERSION.SDK_INT >= 12)
      {
        return bitmap.getByteCount();
      }
      else
      {
        return bitmap.getRowBytes() * bitmap.getHeight();
      }
    }

    public void recycle()
    {
      if (bitmap != null)
      {
        bitmap.recycle();
      }
    }

    /**
     * @return the wrapped bitmap
     */
    public Bitmap getBitmap()
    {
      return bitmap;
    }

  }

  public final static class BitmapableGif
      implements Bitmapable
  {

    private final Gif gif;

    private final Bitmap bitmap;

    public BitmapableGif(Gif gif)
    {
      this.gif = gif;
      bitmap = gif.getBitmap(0);
    }

    @Override
    public int getSizeInBytes()
    {
      if (gif == null)
      {
        return 0;
      }
      else
      {
        return gif.getHeight() * gif.getWidth() * gif.getFramesCount();
      }
    }

    @Override
    public void recycle()
    {
      if (gif != null && gif.getBitmaps().isEmpty() == false)
      {
        for (Bitmap bitmap : gif.getBitmaps())
        {
          bitmap.recycle();
        }
      }
    }

    public void endAnimation()
    {
      if (gif != null)
      {
        gif.endAnimation();
      }
    }

    public Gif getGif()
    {
      return gif;
    }
  }

  /**
   * An implementation of the {@link Viewable} interface specific to the Android platform.
   */
  public final static class ViewableView
      implements Viewable
  {

    private final View view;

    /**
     * Creates a wrapper around the provided actual Android view.
     *
     * @param view the widget to be wrapped
     */
    public ViewableView(View view)
    {
      this.view = view;
    }

    public int getId()
    {
      return view.getId();
    }

    public Object getTag()
    {
      return view.getTag();
    }

    public void setTag(Object tag)
    {
      view.setTag(tag);
    }

    /**
     * It is very important that this wrapper class just returns the {@link View#hashCode()} method, because it will be used as a {@link Map} key.
     */
    @Override
    public int hashCode()
    {
      return view.hashCode();
    }

    /**
     * It is very important that this wrapper class just returns the {@link View#equals(Object)} method, because it will be used as a {@link Map} key.
     */
    @Override
    public boolean equals(Object object)
    {
      if (this == object)
      {
        return true;
      }
      if (object == null)
      {
        return false;
      }
      if (getClass() != object.getClass())
      {
        return false;
      }
      final ViewableView other = (ViewableView) object;
      return view == other.view;
    }

    /**
     * @return the wrapped Android view
     */
    public View getView()
    {
      return view;
    }

  }

  /**
   * An implementation of the {@link Handlerable} interface specific to the Android platform.
   */
  public final static class HandlerableHander
      implements Handlerable
  {

    private final Handler handler;

    /**
     * Creates a wrapper around the provided actual Android handler.
     *
     * @param handler the handler to be wrapped
     */
    public HandlerableHander(Handler handler)
    {
      this.handler = handler;
    }

    public boolean post(Runnable runnnable)
    {
      return handler.post(runnnable);
    }

    /**
     * @return the wrapped Android handler
     */
    public Handler getHandler()
    {
      return handler;
    }

  }

  /**
   * Enables the {@link CoreBitmapDownloader bitmap downloader} to know on how to handle a command.
   *
   * @since 2009.03.04
   */
  public static abstract class Instructions
      implements BasisDownloadInstructions.Instructions<DownloadInstructions.BitmapableBitmap, DownloadInstructions.ViewableView>
  {

    @Override
    public final BitmapableBitmap hasLocalBitmap(ViewableView view, String bitmapUid, Object imageSpecs)
    {
      final Bitmap bitmap = hasLocalBitmap(view.getView(), bitmapUid, imageSpecs);
      return bitmap == null ? null : new BitmapableBitmap(bitmap);
    }

    @Override
    public final void onBindLocalBitmap(ViewableView view, BitmapableBitmap bitmap, String bitmapUid, Object imageSpecs)
    {
      onBindLocalBitmap(view != null ? view.getView() : null, bitmap.getBitmap(), bitmapUid, imageSpecs);
    }

    @Override
    public final BitmapableBitmap hasTemporaryBitmap(ViewableView view, String bitmapUid, Object imageSpecs)
    {
      final Bitmap bitmap = hasTemporaryBitmap(view.getView(), bitmapUid, imageSpecs);
      return bitmap == null ? null : new BitmapableBitmap(bitmap);
    }

    @Override
    public final void onBindTemporaryBitmap(ViewableView view, BitmapableBitmap bitmap, String bitmapUid,
        Object imageSpecs)
    {
      onBindTemporaryBitmap(view != null ? view.getView() : null, bitmap.getBitmap(), bitmapUid, imageSpecs);
    }

    @Override
    public final void onBitmapReady(boolean allright, ViewableView view, BitmapableBitmap bitmap, String bitmapUid,
        Object imageSpecs)
    {
      onBitmapReady(allright, view != null ? view.getView() : null, bitmap != null ? bitmap.getBitmap() : null, bitmapUid, imageSpecs);
    }

    @Override
    public final boolean onBindBitmap(boolean downloaded, ViewableView view, BitmapableBitmap bitmap, String bitmapUid,
        Object imageSpecs)
    {
      return onBindBitmap(downloaded, view != null ? view.getView() : null, bitmap != null ? bitmap.getBitmap() : null, bitmapUid, imageSpecs);
    }

    @Override
    public final void onBitmapBound(boolean result, ViewableView view, String bitmapUid, Object imageSpecs)
    {
      onBitmapBound(result, view != null ? view.getView() : null, bitmapUid, imageSpecs);
    }

    protected abstract Bitmap hasLocalBitmap(View view, String bitmapUid, Object imageSpecs);

    protected abstract void onBindLocalBitmap(View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    protected abstract Bitmap hasTemporaryBitmap(View view, String bitmapUid, Object imageSpecs);

    protected abstract void onBindTemporaryBitmap(View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    protected abstract void onBitmapReady(boolean allright, View view, Bitmap bitmap, String bitmapUid,
        Object imageSpecs);

    protected abstract boolean onBindBitmap(boolean downloaded, View view, Bitmap bitmap, String bitmapUid,
        Object imageSpecs);

    protected abstract void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs);

  }

  /**
   * A very simple implementation of the {@link Instructions}, which by-passes the persistence option.
   */
  public static abstract class SimpleInstructions
      extends Instructions
  {

    /**
     * @return {@code null}, which means that the underlying {@link BitmapableBitmap} cannot be retrieved from the persistence
     */
    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url,
        InputStreamDownloadInstructor instructor)
        throws IOException
    {
      return null;
    }

    /**
     * The method does not use the provided {@link InputStream inputStream}, and just returns it.
     *
     * @return the provided {@link InputStream}, which means that the underlying {@link BitmapableBitmap} is not persisted
     */
    public InputStream onInputStreamDownloaded(String bitmapUid, Object imageSpecs, String url, InputStream inputStream)
    {
      return inputStream;
    }

  }

  public static abstract class GifAbstractInstructions
      implements BasisDownloadInstructions.Instructions<DownloadInstructions.BitmapableGif, DownloadInstructions.ViewableView>
  {

    @Override
    public BitmapableGif hasLocalBitmap(ViewableView view, String bitmapUid, Object imageSpecs)
    {
      final Bitmap bitmap = hasLocalBitmap(view.getView(), bitmapUid, imageSpecs);
      return bitmap == null ? null : new BitmapableGif(null);
    }

    @Override
    public void onBindLocalBitmap(ViewableView view, BitmapableGif bitmap, String bitmapUid, Object imageSpecs)
    {
      ((ImageView) view.getView()).setImageBitmap(bitmap.bitmap);
    }

    @Override
    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      return bitmapUid;
    }

    @Override
    public void onBindTemporaryBitmap(ViewableView view, BitmapableGif bitmap, String bitmapUid, Object imageSpecs)
    {
      onBindTemporaryBitmap(view != null ? view.getView() : null, bitmap.gif, bitmapUid, imageSpecs);
    }

    @Override
    public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
        throws IOException
    {
      return new URL(url).openStream();
    }

    @Override
    public BitmapableGif convert(InputStream inputStream, String bitmapUid, Object imageSpecs, String url)
    {
      return null;
    }

    @Override
    public void onBitmapReady(boolean allright, ViewableView view, BitmapableGif bitmap, String bitmapUid,
        Object imageSpecs)
    {
      onGifReady(allright, view != null ? view.getView() : null, bitmap != null ? bitmap.gif : null, bitmapUid, imageSpecs);
    }

    @Override
    public boolean onBindBitmap(boolean downloaded, ViewableView view, BitmapableGif bitmap, String bitmapUid,
        Object imageSpecs)
    {
      return onBindGif(downloaded, view != null ? view.getView() : null, bitmap != null ? bitmap.gif : null, bitmapUid, imageSpecs);
    }

    @Override
    public void onBitmapBound(boolean result, ViewableView view, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public void onOver(boolean aborted, ViewableView view, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public InputStream getInputStream(String imageUid, Object imageSpecs, String url,
        BasisDownloadInstructions.InputStreamDownloadInstructor downloadInstructor)
        throws IOException
    {
      return null;
    }

    @Override
    public InputStream onInputStreamDownloaded(String imageUid, Object imageSpecs, String url, InputStream inputStream)
    {
      return inputStream;
    }

    protected abstract Bitmap hasLocalBitmap(View view, String bitmapUid, Object imageSpecs);

    protected abstract void onBindLocalGif(View view, Gif gif, String bitmapUid, Object imageSpecs);

    protected abstract Bitmap hasTemporaryBitmap(View view, String bitmapUid, Object imageSpecs);

    protected abstract void onBindTemporaryBitmap(View view, Gif bitmap, String bitmapUid, Object imageSpecs);

    protected abstract void onGifReady(boolean allright, View view, Gif bitmap, String bitmapUid,
        Object imageSpecs);

    protected abstract boolean onBindGif(boolean downloaded, View view, Gif bitmap, String bitmapUid,
        Object imageSpecs);

    protected abstract void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs);

  }

  public static class GifInstructions
      extends GifAbstractInstructions
  {

    @Override
    public BitmapableGif hasTemporaryBitmap(ViewableView view, String bitmapUid, Object imageSpecs)
    {
      final Bitmap bitmap = hasLocalBitmap(view.getView(), bitmapUid, imageSpecs);
      return bitmap == null ? null : new BitmapableGif(null);
    }

    @Override
    public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
        throws IOException
    {
      final URL aURL = new URL(url);
      final URLConnection connection = aURL.openConnection();
      connection.connect();
      final InputStream inputStream = connection.getInputStream();
      return inputStream;
    }

    @Override
    public BitmapableGif convert(InputStream inputStream, String bitmapUid, Object imageSpecs, String url)
    {
      long start = 0L;
      boolean isDebug = CoreBitmapDownloader.IS_DEBUG_TRACE && CoreBitmapDownloader.log.isDebugEnabled();
      if (isDebug)
      {
        start = System.currentTimeMillis();
      }
      final Gif theGif = convertInputStreamToGif(inputStream, url);
      if (isDebug && theGif != null)
      {
        final long stop = System.currentTimeMillis();
        CoreBitmapDownloader.log.debug("The thread '" + Thread.currentThread().getName() + "' decoded in " + (stop - start) + " relative to the URL '" + url + "'");
      }
      return theGif == null ? null : new BitmapableGif(theGif);
    }

    @Override
    protected Bitmap hasLocalBitmap(View view, String bitmapUid, Object imageSpecs)
    {
      if (imageSpecs instanceof DownloadSpecs.TemporaryImageSpecs)
      {
        final DownloadSpecs.TemporaryImageSpecs temporaryImageSpecs = (DownloadSpecs.TemporaryImageSpecs) imageSpecs;
        return temporaryImageSpecs.imageResourceId != -1 ? BitmapFactory.decodeResource(view.getContext().getResources(), temporaryImageSpecs.imageResourceId) : null;
      }
      return null;
    }

    @Override
    protected void onBindLocalGif(View view, Gif gif, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    protected Bitmap hasTemporaryBitmap(View view, String bitmapUid, Object imageSpecs)
    {
      if (imageSpecs instanceof DownloadSpecs.TemporaryImageSpecs)
      {
        final DownloadSpecs.TemporaryImageSpecs temporaryImageSpecs = (DownloadSpecs.TemporaryImageSpecs) imageSpecs;
        return temporaryImageSpecs.imageResourceId != -1 ? BitmapFactory.decodeResource(view.getContext().getResources(), temporaryImageSpecs.imageResourceId) : null;
      }
      else
      {
        return null;
      }
    }

    @Override
    protected void onBindTemporaryBitmap(View view, Gif bitmap, String bitmapUid, Object imageSpecs)
    {
      if (view instanceof ImageView)
      {
        ((ImageView) view).setImageBitmap(bitmap.getBitmap(0));
      }
    }

    @Override
    protected void onGifReady(boolean allright, View view, Gif bitmap, String bitmapUid, Object imageSpecs)
    {

    }

    @Override
    protected boolean onBindGif(boolean downloaded, View view, Gif gif, String bitmapUid, Object imageSpecs)
    {
      if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1)
      {
        gif.startAnimation((ImageView) view);
      }
      else
      {
        if (log.isInfoEnabled() == true)
        {
          log.info("Gif animation is only available from API 11");
        }
      }
      return true;
    }

    @Override
    protected void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs)
    {

    }

    protected Gif convertInputStreamToGif(InputStream inputStream, String url)
    {
      if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB_MR1)
      {
        return new Gif(inputStream, url);
      }
      else
      {
        if (log.isWarnEnabled() == true)
        {
          log.warn("Gif support is available from API 12", new UnsupportedOperationException("Gif support is available from API 12"));
        }
        return null;
      }
    }
  }


  /**
   * An implementation of the {@link Instructions}, which returns the <code>bitmapUid</code> as an URL, and which does not present any temporary nor
   * local bitmap.
   * <p>
   * <p>
   * Caution: this implementation supposes that the provided {@link View view} is actually an {@link ImageView} in the
   * {@link DownloadInstructions.AbstractInstructions#onBindBitmap(boolean, View, Bitmap, String, Object)} method.
   * </p>
   */
  public static class AbstractInstructions
      extends SimpleInstructions
  {

    @Override
    public Bitmap hasLocalBitmap(View view, String bitmapUid, Object imageSpecs)
    {
      return null;
    }

    @Override
    public void onBindLocalBitmap(View view, Bitmap bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      return bitmapUid;
    }

    @Override
    public Bitmap hasTemporaryBitmap(View view, String bitmapUid, Object imageSpecs)
    {
      return null;
    }

    @Override
    public void onBindTemporaryBitmap(View view, Bitmap bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
        throws IOException
    {
      final URL aURL = new URL(url);
      final URLConnection connection = aURL.openConnection();
      connection.connect();
      final InputStream inputStream = connection.getInputStream();
      return inputStream;
    }

    /**
     * Is responsible for turning the provided input stream into a bitmap representation.
     * <p>
     * <p>
     * The method measures the time taken for the conversion, and logs that duration. The technical part of the method is delegated to the
     * {@link #convertInputStreamToBitmap(InputStream, String, Object, String)} method.
     * </p>
     *
     * @param inputStream the implementation should not close the input stream, because the caller will {@link InputStream#close()} it (no problem if it is
     *                    closed, but this will impact the performance)
     * @return by default, the returned wrapped {@link Bitmap} will have the device density
     * @see #convert(InputStream, String, Object, String)
     */
    @Override
    public DownloadInstructions.BitmapableBitmap convert(InputStream inputStream, String bitmapUid, Object imageSpecs,
        String url)
    {
      final long start = System.currentTimeMillis();
      final Bitmap theBitmap = convertInputStreamToBitmap(inputStream, bitmapUid, imageSpecs, url);
      if (theBitmap != null)
      {
        if (CoreBitmapDownloader.IS_DEBUG_TRACE && CoreBitmapDownloader.log.isDebugEnabled())
        {
          final long stop = System.currentTimeMillis();
          CoreBitmapDownloader.log.debug("The thread '" + Thread.currentThread().getName() + "' decoded in " + (stop - start) + " ms the bitmap with density " + theBitmap.getDensity() + " relative to the URL '" + url + "'");
        }
      }
      return theBitmap == null ? null : new BitmapableBitmap(theBitmap);
    }

    @Override
    public void onBitmapReady(boolean allright, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public boolean onBindBitmap(boolean downloaded, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs)
    {
      ((ImageView) (view)).setImageBitmap(bitmap);
      return true;
    }

    @Override
    public void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public void onOver(boolean aborted, ViewableView view, String bitmapUid, Object imageSpecs)
    {
    }

    /**
     * Actually converts the given {@link InputStream} into an Android {@link Bitmap}.
     * <p>
     * <p>
     * The hereby implementation does not perform any scaling.
     * </p>
     *
     * @param inputStream the representation of the {@link Bitmap} to be decoded
     * @return the decoded {@link Bitmap} if the conversion could be performed properly ; {@code null} otherwise
     * @see #convert(InputStream, String, Object, String)
     */
    protected Bitmap convertInputStreamToBitmap(InputStream inputStream, String bitmapUid, Object imageSpecs,
        String url)
    {
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inScaled = false;
      options.inDither = false;
      options.inDensity = 0;

      if (getClass().isAnnotationPresent(BitmapConfigAnnotation.class) == true)
      {
        options.inPreferredConfig = getClass().getAnnotation(BitmapConfigAnnotation.class).bitmapConfig();

        if (getClass().getAnnotation(BitmapConfigAnnotation.class).bitmapConfig() == Config.RGB_565)
        {
          options.inDither = true;
        }
      }

      return BitmapFactory.decodeStream(inputStream, null, options);
    }

  }

  /**
   * We do not want that container class to be instantiated.
   */
  protected DownloadInstructions()
  {
  }

}
