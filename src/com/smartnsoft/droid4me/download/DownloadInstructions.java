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
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import com.smartnsoft.droid4me.download.BasisDownloadInstructions.InputStreamDownloadInstructor;
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
{

  public final static class BitmapableBitmap
      implements Bitmapable
  {

    private final Bitmap bitmap;

    public BitmapableBitmap(Bitmap bitmap)
    {
      this.bitmap = bitmap;
    }

    public Bitmap getBitmap()
    {
      return bitmap;
    }

    public int getSizeInBytes()
    {
      return bitmap == null ? 0 : bitmap.getWidth() * bitmap.getHeight() * 4;
    }

    public void recycle()
    {
      if (bitmap != null)
      {
        bitmap.recycle();
      }
    }

  }

  public final static class ViewableView
      implements Viewable
  {

    private final View view;

    public ViewableView(View view)
    {
      this.view = view;
    }

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

  }

  public final static class HandlerableHander
      implements Handlerable
  {

    private final Handler handler;

    public HandlerableHander(Handler handler)
    {
      this.handler = handler;
    }

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

  public static abstract class SimpleInstructions
      extends Instructions
  {

    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, InputStreamDownloadInstructor instructor)
        throws IOException
    {
      return null;
    }

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

    public void onBeforeBitmapDownloaded(String bitmapUid, Object imageSpecs, URLConnection urlConnection)
    {
    }

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

  }

  /**
   * We do not want that container class to be instantiated.
   */
  protected DownloadInstructions()
  {
  }

}
