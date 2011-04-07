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

import java.io.InputStream;

import android.os.Handler;
import android.widget.ImageView;

/**
 * Responsible for downloading the bitmaps in dedicated threads and to bind them to Android {@link ImageView ImageViews}.
 * 
 * @author Édouard Mercier
 * @since 2010.07.09
 */
public final class AdvancedImageDownloader
    extends ImageDownloader
{

  /**
   * Enables to express an image specification with a default image resource id, in case the URL does not correspond to an image.
   */
  public static class TemporaryImageSpecs
  {

    /**
     * The Android resource image identification.
     */
    public final int imageResourceId;

    public TemporaryImageSpecs(int imageResourceId)
    {
      this.imageResourceId = imageResourceId;
    }

  }

  /**
   * Enables to indicate the identifier of the image resource that should be used when loading, and the identifier of the image resource that should
   * be used in case the underlying bitmap cannot be retrieved.
   */
  public static class TemporaryAndNoImageSpecs
  {

    /**
     * The Android resource image identification of the loading image.
     */
    public final int loadingImageResourceId;

    /**
     * The Android resource image identification of the image used when the bitmap cannot be retrieved.
     */
    public final int unavailableImageResourceId;

    public TemporaryAndNoImageSpecs(int loadingImageResourceId, int unavailableImageResourceId)
    {
      this.loadingImageResourceId = loadingImageResourceId;
      this.unavailableImageResourceId = unavailableImageResourceId;
    }

  }

  /**
   * Enables to express an image specification, with a size and a default image resource id, in case the URL does not correspond to an image.
   */
  public static class DefaultImageSpecs
      extends AdvancedImageDownloader.TemporaryImageSpecs
  {

    public final int size;

    public DefaultImageSpecs(int size, int imageId)
    {
      super(imageId);
      this.size = size;
    }

  }

  /**
   * Enables to express an image specification, which indicates the size and orientation.
   */
  public static class OrientedImageSpecs
  {

    public final int size;

    public final boolean flag;

    public OrientedImageSpecs(int size, boolean flag)
    {
      this.size = size;
      this.flag = flag;
    }

  }

  /**
   * Enables to express an image specification, which indicates its size and a temporary image resource identification.
   */
  public static class SizedImageSpecs
      extends AdvancedImageDownloader.TemporaryImageSpecs
  {

    public final int width;

    public final int height;

    public SizedImageSpecs(int imageResourceId, int width, int height)
    {
      super(imageResourceId);
      this.width = width;
      this.height = height;
    }

  }

  public static interface AdvancedInstructions
      extends BasisImageDownloader.Instructions
  {

    /**
     * Is invoked once the stream related to the image view has been downloaded.
     * 
     * <p>
     * This is a good place for storing the input stream related to the image.
     * </p>
     * 
     * @return the provided inputStream or a tweaked version of that stream
     */
    InputStream onInputStreamDownloaded(String imageUid, Object imageSpecs, String url, InputStream inputStream);

  }

  public static class AdvancedAbstractInstructions
      extends BasisImageDownloader.AbstractInstructions
      implements AdvancedImageDownloader.AdvancedInstructions
  {

    public InputStream onInputStreamDownloaded(String imageUid, Object imageSpecs, String url, InputStream inputStream)
    {
      return inputStream;
    }

  }

  private final class AdvancedDownloadBitmapCommand
      extends ImageDownloader.DownloadBitmapCommand
  {

    public AdvancedDownloadBitmapCommand(int id, ImageView image, String url, String imageUid, Object imageSpecs, Handler handler, Instructions instructions)
    {
      super(id, image, url, imageUid, imageSpecs, handler, instructions);
    }

    @Override
    protected InputStream onInputStreamDownloaded(InputStream inputStream)
    {
      return ((AdvancedImageDownloader.AdvancedInstructions) instructions).onInputStreamDownloaded(imageUid, imageSpecs, url, inputStream);
    }

  }

  protected AdvancedImageDownloader(String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    super(name, maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
  }

  @Override
  protected DownloadBitmapCommand computeDownloadBitmapCommand(int id, ImageView imageView, String url, String imageUid, Object imageSpecs, Handler handler,
      Instructions instructions)
  {
    return new AdvancedImageDownloader.AdvancedDownloadBitmapCommand(id, imageView, url, imageUid, imageSpecs, handler, instructions);
  }

}
