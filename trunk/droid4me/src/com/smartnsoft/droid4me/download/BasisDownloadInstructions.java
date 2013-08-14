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
     * The method will be invoked, so as to known whether the bitmap could not be extracted locally, i.e. from the application .apk, or a static local
     * resource.
     * 
     * <p>
     * Do not use that placeholder for introducing any persistent strategy, but use the
     * {@link #getInputStream(String, Object, String, BasisDownloadInstructions.InputStreamDownloadInstructor)} and
     * {@link #onInputStreamDownloaded(String, Object, String, InputStream)} methods instead, for that purpose.
     * </p>
     * 
     * @param view
     *          the View the command should be executed against: cannot be {@code null}
     * @return {@code true} if and only the bitmap should be taken locally, instead of running into the downloading process: in that case, the
     *         {@link Instructions#onBindLocalBitmap(View, String, Object)} will be invoked
     * @see #onBindLocalBitmap(Viewable, String, Object)
     */
    BitmapClass hasLocalBitmap(ViewClass view, String bitmapUid, Object imageSpecs);

    /**
     * When a local bitmap usage has been specified, is responsible for binding the view with that local bitmap.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param bitmap
     *          the bitmap which has previously returned from the {@link #hasTemporaryBitmap(String, Object)} method, if not {@code null}
     * @see #hasLocalBitmap(String, Object)
     */
    void onBindLocalBitmap(ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs);

    /**
     * Given the bitmap identifier, its extra specifications, returns its URL, which will be used for downloading it.
     * 
     * <p>
     * This method will be invoked from the GUI thread, if and only if the
     * {@link CoreBitmapDownloader#get(boolean, boolean, Viewable, String, Object, com.smartnsoft.droid4me.download.DownloadContracts.Handlerable, Instructions)}
     * method has been invoked with a {@code isPreBlocking} argument set to {@code true}. However, it is ensured that this method will be only be
     * invoked at most once per
     * {@link CoreBitmapDownloader#get(boolean, boolean, Viewable, String, Object, com.smartnsoft.droid4me.download.DownloadContracts.Handlerable, Instructions)}
     * method invocation (i.e. for a command).
     * </p>
     * 
     * <p>
     * For performance reasons, the method does not manage {@link RuntimeException}: this is the reason why the implementation should make sure that
     * no such error occurs ; in particular, pay attention to the {@link NullPointerException}
     * </p>
     * 
     * @return the URL that will be used to investigate on whether the bitmap is already in cache memory, persisted, and to download it from the
     *         network. If the method returns {@code null}, it will be considered that no bitmap is available for the {@code imageUid}
     */
    String computeUrl(String bitmapUid, Object imageSpecs);

    /**
     * Enables to determine whether a temporary bitmap (which will be eventually a final bound bitmap if the case the provided <code>bitmapUid</code>
     * is {@code null}) should be bound to the underlying view.
     * 
     * <p>
     * The method will be invoked even with a <code>bitmapUid</code> set to {@code null}, but not if the
     * {@link CoreBitmapDownloader#get(Viewable, String, Object, com.smartnsoft.droid4me.download.DownloadContracts.Handlerable, Instructions)} is
     * invoked with a first argument set to {@code null}. Note that the method is granted to be invoked from the UI thread, unless the
     * {@link CoreBitmapDownloader#get(boolean, boolean, Viewable, String, Object, com.smartnsoft.droid4me.download.DownloadContracts.Handlerable, Instructions)}
     * is invoked with a first argument set to {@code true}.
     * </p>
     * 
     * @param view
     *          the View the command should be executed against: cannot be {@code null}
     * @return a non-{@code null} bitmap if and only if a temporary bitmap should be used for the underlying view: in that case, the
     *         {@link Instructions#onBindTemporaryBitmap(View, String, Object)} will be invoked with this provided returned valued
     * @see #onBindTemporaryBitmap(Viewable, Bitmapable, String, Object)
     */
    BitmapClass hasTemporaryBitmap(ViewClass view, String bitmapUid, Object imageSpecs);

    /**
     * When a temporary bitmap usage has been specified, is responsible for binding the view with that temporary bitmap.
     * 
     * <p>
     * The method will be invoked even with a <code>bitmapUid</code> set to {@code null}.
     * </p>
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param bitmap
     *          the bitmap which has previously returned from the {@link #hasTemporaryBitmap(String, Object)} method, if not {@code null}
     * @see #hasTemporaryBitmap(String, Object)
     */
    void onBindTemporaryBitmap(ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked just before {@link #downloadInputStream(String, Object, String) the stream download} corresponding to the bitmap is started,
     * provided its computed URL is not null nor empty.
     * 
     * <p>
     * This is a good place for fetching from a local cache the input stream related to the bitmap.
     * </p>
     * 
     * @param instructor
     *          if you want the input stream download to be asynchronous, call
     *          {@link CoreBitmapDownloader.InputStreamDownloadInstructor#setAsynchronous()} and return {@code null} ; once the input stream has been
     *          download, invoke {@link CoreBitmapDownloader.InputStreamDownloadInstructor#onDownloaded(InputStream)}.
     * @return if {@code null} and {@link CoreBitmapDownloader.InputStreamDownloadInstructor#setAsynchronous()} has not been invoked, the framework
     *         will download the input from Internet; otherwise that input stream will be used for populating the view
     */
    InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, BasisDownloadInstructions.InputStreamDownloadInstructor instructor)
        throws IOException;

    /**
     * Should download from the network the input stream corresponding to the bitmap.
     * 
     * <p>
     * The callback is invoked when the {@link #getInputStream(String, Object, String, InputStreamDownloadInstructor)} method could not return a valid
     * input stream. This callback will not be invoked if the {@link CoreBitmapDownloader#isConnected()} returns {@code false}.
     * </p>
     * 
     * @return the input stream corresponding to the bitmap ; the returned should not be {@code null} in case of failure, but the method should throw
     *         a {@link IOException} exception instead
     * @throws IOException
     *           if a network problem occurred while downloading the bitmap
     * @see #onInputStreamDownloaded(String, Object, String, InputStream)
     */
    InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
        throws IOException;

    /**
     * Is invoked once the stream related to the image view has been downloaded.
     * 
     * <p>
     * This is a good place for storing the input stream related to the image.
     * </p>
     * 
     * @return the provided inputStream or a tweaked version of that stream
     * @see #downloadInputStream(String, Object, String)
     */
    InputStream onInputStreamDownloaded(String bitmapUid, Object imageSpecs, String url, InputStream inputStream);

    /**
     * Invoked when a downloaded input stream need to be converted into a bitmap.
     * 
     * <p>
     * It is not ensured that this method will be invoked from the GUI thread.
     * </p>
     * 
     * @param the
     *          input stream that should be converted. It is ensured that this method will not be invoked with a {@code null} input stream
     */
    BitmapClass convert(InputStream inputStream, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked every time, once the underlying view bitmap is either not {@code null} ready in memory (the <code>allright</code> parameter is set
     * to {@code true}), or when it occurs that the bitmap could not be downloaded or is not well formed (the <code>allright</code> parameter is set
     * to {@code false}).
     * 
     * <p>
     * It is NOT ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param allright
     *          indicates whether the bitmap is actually available and well formed
     * @param view
     *          will be {@code null} if, and only if the provided {@link View} is {@code null}
     * @param bitmap
     *          cannot be {@code null} if and only if <code>allright</code> is {@code true}
     */
    void onBitmapReady(boolean allright, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs);

    /**
     * Is invoked once the bitmap is ready when the provided {@link View} is not {@code null}, whether it has been downloaded from Internet or
     * retrieved from the cache.
     * 
     * <p>
     * It is ensured that this method will be run from the UI thread.
     * </p>
     * 
     * @param downloaded
     *          indicates whether the bitmap has actually been downloaded from the network or retrieved locally
     * @param bitmap
     *          will never be {@code null}
     * @return {@code false} if and only if the current instance did not perform the binding
     */
    boolean onBindBitmap(boolean downloaded, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs);

    /**
     * This is the last callback that will invoked during the binding process, provided it is still required that a binding should occur (i.e. that no
     * new binding command has not been stacked for the same view in the meantime). It is invoked once the given the bitmap identifier, its
     * specifications and either:
     * <ul>
     * <li>the underlying bitmap has actually been downloaded, and attached to its {@link View},</li>
     * <li>or the underlying bitmap has actually been bound to its {@link View} from a local resource,</li>
     * <li>or, the underlying bitmap download was a failure (wrong or null bitmap URL, connectivity problem).</li>
     * </ul>
     * 
     * <p>
     * The method will be invoked even with a <code>bitmapUid</code> set to {@code null}.
     * </p>
     * 
     * <p>
     * It is ensured that this notification will be invoked from the GUI thread.
     * </p>
     * 
     * @param result
     *          {@code true} if and only if the bitmap has actually been downloaded and attached to its view
     */
    void onBitmapBound(boolean result, ViewClass view, String bitmapUid, Object imageSpecs);

    /**
     * This method is invoked systematically once the command is over, either when it has successfully completed, or when it has been aborted.
     * 
     * <p>
     * Do not make any assumption regarding the thread invoking that method: it may be the GUI thread or a background taken from a worker thread!
     * </p>
     * 
     * <p>
     * This callback is especially useful the unitary tests.
     * </p>
     * 
     * @param aborted
     *          {@code true} when and only when the command has been aborted, because another command working on the same {@code view} has also been
     *          stacked in the meantime
     */
    void onOver(boolean aborted, ViewClass view, String bitmapUid, Object imageSpecs);

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

    @Override
    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      return bitmapUid;
    }

    @Override
    public BitmapClass hasTemporaryBitmap(ViewClass view, String bitmapUid, Object imageSpecs)
    {
      return null;
    }

    @Override
    public void onBindTemporaryBitmap(ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public BitmapClass hasLocalBitmap(ViewClass view, String bitmapUid, Object imageSpecs)
    {
      return null;
    }

    @Override
    public void onBindLocalBitmap(ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
        throws IOException
    {
      throw new IOException("Not implemented!");
    }

    @Override
    public BitmapClass convert(InputStream inputStream, String bitmapUid, Object imageSpecs)
    {
      return null;
    }

    @Override
    public void onBitmapReady(boolean allright, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public boolean onBindBitmap(boolean downloaded, ViewClass view, BitmapClass bitmap, String bitmapUid, Object imageSpecs)
    {
      return false;
    }

    @Override
    public void onBitmapBound(boolean result, ViewClass view, String bitmapUid, Object imageSpecs)
    {
    }

    @Override
    public void onOver(boolean aborted, ViewClass view, String bitmapUid, Object imageSpecs)
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
     *          should not be {@code null}
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
