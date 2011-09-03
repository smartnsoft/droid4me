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

/**
 * Gathers in one place some commonly used specifications used by the {@link BitmapDownloader}.
 * 
 * @author Édouard Mercier
 * @since 2011.07.03
 */
public class DownloadSpecs
{

  /**
   * Enables to express an image specification with a default image resource identifier, in case the URL does not correspond to an image.
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
      extends TemporaryImageSpecs
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
   * We do not want that container class to be instantiated.
   */
  protected DownloadSpecs()
  {
  }

}
