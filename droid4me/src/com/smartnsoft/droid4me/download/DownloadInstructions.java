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
 *     Smart&Soft - initial API and implementation
 */
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
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;

/**
 * Gathers in one place the download instructions contracts used by {@link BitmapDownloader}.
 *
 * @author Édouard Mercier
 * @since 2011.07.03
 */
public class DownloadInstructions
    extends BasisDownloadInstructions
{

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

    /**
     * @return the wrapped bitmap
     */
    public Bitmap getBitmap()
    {
      return bitmap;
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

    /**
     * @return the wrapped Android view
     */
    public View getView()
    {
      return view;
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

    /**
     * @return the wrapped Android handler
     */
    public Handler getHandler()
    {
      return handler;
    }

    public boolean post(Runnable runnnable)
    {
      return handler.post(runnnable);
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

    protected abstract Bitmap hasLocalBitmap(View view, String bitmapUid, Object imageSpecs);

    protected abstract void onBindLocalBitmap(View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    protected abstract Bitmap hasTemporaryBitmap(View view, String bitmapUid, Object imageSpecs);

    protected abstract void onBindTemporaryBitmap(View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    protected abstract void onBitmapReady(boolean allright, View view, Bitmap bitmap, String bitmapUid,
        Object imageSpecs);

    protected abstract boolean onBindBitmap(boolean downloaded, View view, Bitmap bitmap, String bitmapUid,
        Object imageSpecs);

    protected abstract void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs);

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

  /**
   * An implementation of the {@link Instructions}, which returns the <code>bitmapUid</code> as an URL, and which does not present any temporary nor
   * local bitmap.
   * <p/>
   * <p>
   * Caution: this implementation supposes that the provided {@View view} is actually an {@ImageView} in the
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
     * <p/>
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
     * <p/>
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
