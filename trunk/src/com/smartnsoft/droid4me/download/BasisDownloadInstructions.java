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
import android.view.View;

import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;

/**
 * Gathers in one place the download instructions contracts used by {@link BitmapDownloader}.
 * 
 * @author Édouard Mercier
 * @since 2011.07.03
 */
public class BasisDownloadInstructions
{

  /**
   * Enables the {@link CoreBitmapDownloader bitmap downloader} to know on how to handle a command.
   * 
   * @since 2009.03.04
   */
  public static interface Instructions<BitmapClass extends Bitmapable, ViewClass extends Viewable>
  {

    /**
     * Given the bitmap identifier, its size and its extra specifications, returns its URL, which will be used for downloading it.
     */
    String computeUrl(String bitmapUid, Object imageSpecs);

    /**
     * Enables to determine whether a temporary bitmap (which will be eventually a final bound bitmap if the case the provided <code>bitmapUid</code>
     * is <code>null</code>) should be bound to the underlying view.
     * 
     * <p>
     * The method will be invoked even with a <code>bitmapUid</code> set to <code>null</code>.
     * </p>
     * 
     * @return <code>true</code> if and only if a temporary bitmap should be used for the underlying view: in that case, the
     *         {@link Instructions#onBindTemporaryBitmap(View, String, Object)} will be invoked
     */
    boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs);

    /**
     * When a temporary bitmap usage has been specified, is responsible for binding the view with that temporary bitmap.
     * 
     * <p>
     * The method will be invoked even with a <code>bitmapUid</code> set to <code>null</code>.
     * </p>
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     */
    void onBindTemporaryBitmap(ViewClass view, String bitmapUid, Object imageSpecs);

    /**
     * @return <code>true</code> if and only the bitmap should be taken locally, instead of running into the downloading process: in that case, the
     *         {@link Instructions#onBindLocalBitmap(View, String, Object)} will be invoked
     */
    boolean hasLocalBitmap(String bitmapUid, Object imageSpecs);

    /**
     * When a local bitmap usage has been specified, is responsible for binding the view with that local bitmap.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     */
    void onBindLocalBitmap(ViewClass view, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked every time once the underlying view bitmap is either not <code>null</code> ready in memory (the <code>allright</code> parameter is
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
    void onBitmapReady(boolean allright, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked once the bitmap is ready when the provided {@link View} is not <code>null</code>, whether it has been downloaded from Internet or
     * retrieved from the cache.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param downloaded
     *          indicates whether the bitmap has actually been downloaded from the network or retrieved locally
     * @param bitmap
     *          will never be <code>null</code>
     * @return <code>false</code> if and only if the current instance did not perform the binding
     */
    boolean onBindBitmap(boolean downloaded, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs);

    /**
     * This is the last callback that will invoked during the binding process, provided it is still required that a binding should occur (i.e. that no
     * new binding command has not been stacked for the same view in the meantime). It is invoked once the given the bitmap identifier, its
     * specifications and either:
     * <ul>
     * <li>the underlying bitmap has actually been downloaded, and attached to its {@link View},</li>
     * <li>or the underlying bitmap has actually been bound to its {@link View} from a local resource,</li>
     * <li>or, the underlying bitmap download was failure (wrong or null bitmap URL, connectivity problem).</li>
     * </ul>
     * 
     * <p>
     * The method will be invoked even with a <code>bitmapUid</code> set to <code>null</code>.
     * </p>
     * 
     * <p>
     * It is ensured that this notification will be invoked from the GUI thread.
     * </p>
     * 
     * @param result
     *          <code>true</code> if and only if the bitmap has actually been downloaded and attached to its view
     */
    void onBitmapBound(boolean result, ViewClass view, String bitmapUid, Object imageSpecs);

    /**
     * Invoked in a non-deterministic thread, just before the bitmap gets downloaded via the provided {@link URLConnection}.
     * 
     * <p>
     * This is a place where it is possible to tweak the urlConnection.
     * </p>
     */
    void onBeforeBitmapDownloaded(String bitmapUid, Object imageSpecs, URLConnection urlConnection);

    /**
     * Invoked when a downloaded input stream need to be converted into a bitmap.
     * 
     * <p>
     * It is not ensured that this method will be invoked from the GUI thread.
     * </p>
     */
    BitmapClass convert(InputStream inputStream, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked just before the stream corresponding to the bitmap is started, provided its computed URL is not null nor empty.
     * 
     * <p>
     * This is a good place for fetching from a local cache the input stream related to the bitmap.
     * </p>
     * 
     * @param instructor
     *          if you want the input stream download to be asynchronous, call
     *          {@link CoreBitmapDownloader.InputStreamDownloadInstructor#setAsynchronous()} and return <code>null</code> ; once the input stream has
     *          been download, invoke {@link CoreBitmapDownloader.InputStreamDownloadInstructor#onDownloaded(InputStream)}.
     * @return if <code>null</code> and {@link CoreBitmapDownloader.InputStreamDownloadInstructor#setAsynchronous()} has not been invoked, the
     *         framework will download the input from Internet; otherwise that input stream will be used for populating the view
     */
    InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, BasisDownloadInstructions.InputStreamDownloadInstructor instructor)
        throws IOException;

    /**
     * Is invoked once the stream related to the image view has been downloaded.
     * 
     * <p>
     * This is a good place for storing the input stream related to the image.
     * </p>
     * 
     * @return the provided inputStream or a tweaked version of that stream
     */
    InputStream onInputStreamDownloaded(String bitmapUid, Object imageSpecs, String url, InputStream inputStream);

  }

  public static abstract class SimpleInstructions<BitmapClass extends Bitmapable, ViewClass extends Viewable>
      implements Instructions<BitmapClass, ViewClass>
  {

    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, BasisDownloadInstructions.InputStreamDownloadInstructor instructor)
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
   * {@link BasisDownloadInstructions.AbstractInstructions#onBindBitmap(boolean, View, Bitmap, String, Object)} method.
   * </p>
   */
  public static class AbstractInstructions<BitmapClass extends Bitmapable, ViewClass extends Viewable>
      extends SimpleInstructions<BitmapClass, ViewClass>
  {

    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      return bitmapUid;
    }

    public boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs)
    {
      return false;
    }

    public void onBindTemporaryBitmap(ViewClass view, String bitmapUid, Object imageSpecs)
    {
    }

    public boolean hasLocalBitmap(String bitmapUid, Object imageSpecs)
    {
      return false;
    }

    public void onBindLocalBitmap(ViewClass view, String bitmapUid, Object imageSpecs)
    {
    }

    public void onBeforeBitmapDownloaded(String bitmapUid, Object imageSpecs, URLConnection urlConnection)
    {
    }

    public BitmapClass convert(InputStream inputStream, String bitmapUid, Object imageSpecs)
    {
      return null;
    }

    public void onBitmapReady(boolean allright, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    public boolean onBindBitmap(boolean downloaded, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs)
    {
      // ((ImageView) (view)).setImageBitmap(bitmap);
      return true;
    }

    public void onBitmapBound(boolean result, ViewClass view, String bitmapUid, Object imageSpecs)
    {
    }

  }

  /**
   * When the input stream related to an bitmap needs to be downloaded asynchronously, this interface enables to indicate it, and to notify the owner
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
   * We do not want that container class to be instantiated.
   */
  protected BasisDownloadInstructions()
  {
  }

}
