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
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;

/**
 * Gathers in one place the download instructions contracts used by {@link BitmapDownloader}.
 * 
 * @author �douard Mercier
 * @since 2011.07.03
 */
public class DownloadInstructions
    extends BasisDownloadInstructions
{

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
     * @param bitmap
     *          the bitmap to be wrapped
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
      return bitmap == null ? 0 : (bitmap.getWidth() * bitmap.getHeight()) * (bitmap.getConfig() == Config.ARGB_8888 ? 4
          : (bitmap.getConfig() == Config.ALPHA_8 ? 1 : 2));
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
     * @param view
     *          the widget to be wrapped
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
     * @param handler
     *          the handler to be wrapped
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

    public abstract void onBindLocalBitmap(View view, String bitmapUid, Object imageSpecs);

    public abstract void onBindTemporaryBitmap(View view, String bitmapUid, Object imageSpecs);

    public abstract void onBitmapReady(boolean allright, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    public abstract boolean onBindBitmap(boolean downloaded, View view, Bitmap bitmap, String bitmapUid, Object imageSpecs);

    public abstract void onBitmapBound(boolean result, View view, String bitmapUid, Object imageSpecs);

    public final void onBindLocalBitmap(ViewableView view, String bitmapUid, Object imageSpecs)
    {
      onBindLocalBitmap(view != null ? view.getView() : null, bitmapUid, imageSpecs);
    }

    public final void onBindTemporaryBitmap(ViewableView view, String bitmapUid, Object imageSpecs)
    {
      onBindTemporaryBitmap(view != null ? view.getView() : null, bitmapUid, imageSpecs);
    }

    public final void onBitmapReady(boolean allright, ViewableView view, BitmapableBitmap bitmap, String bitmapUid, Object imageSpecs)
    {
      onBitmapReady(allright, view != null ? view.getView() : null, bitmap != null ? bitmap.getBitmap() : null, bitmapUid, imageSpecs);
    }

    public final boolean onBindBitmap(boolean downloaded, ViewableView view, BitmapableBitmap bitmap, String bitmapUid, Object imageSpecs)
    {
      return onBindBitmap(downloaded, view != null ? view.getView() : null, bitmap != null ? bitmap.getBitmap() : null, bitmapUid, imageSpecs);
    }

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
    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, InputStreamDownloadInstructor instructor)
        throws IOException
    {
      return null;
    }

    /**
     * The method does not use the provided {@link inputStream}, and just returns it.
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
   * 
   * <p>
   * Caution: this implementation supposes that the provided {@View view} is actually an {@ImageView} in the
   * {@link DownloadInstructions.AbstractInstructions#onBindBitmap(boolean, View, Bitmap, String, Object)} method.
   * </p>
   */
  public static class AbstractInstructions
      extends SimpleInstructions
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
     * 
     * @param inputStream
     *          the implementation should not close the input stream, because the caller will {@link InputStream#close()} it (no problem if it is
     *          closed, but this will impact the performance)
     * @return by default, the returned wrapped {@link Bitmap} will have the device density
     */
    public DownloadInstructions.BitmapableBitmap convert(InputStream inputStream, String bitmapUid, Object imageSpecs)
    {
      // final long start = System.currentTimeMillis();
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inScaled = false;
      options.inDither = false;
      options.inDensity = 0;
      final Bitmap theBitmap = BitmapFactory.decodeStream(inputStream, null, options);
      // final long stop = System.currentTimeMillis();
      // if (log.isDebugEnabled() && bitmap != null)
      // {
      // log.debug("The thread '" + Thread.currentThread().getName() + "' decoded in " + (stop - start) + " ms the bitmap with density " +
      // theBitmap.getDensity() + " relative to the URL '" + url + "'");
      // }
      return theBitmap == null ? null : new BitmapableBitmap(theBitmap);
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

    public void onOver(boolean aborted, ViewableView view, String bitmapUid, Object imageSpecs)
    {
    }

  }

  /**
   * We do not want that container class to be instantiated.
   */
  protected DownloadInstructions()
  {
  }

}
