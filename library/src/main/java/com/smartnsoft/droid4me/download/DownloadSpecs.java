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

    public DefaultImageSpecs(int size, int imageResourceId)
    {
      super(imageResourceId);
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
      extends TemporaryImageSpecs
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

  /**
   * We do not want that container class to be instantiated.
   */
  protected DownloadSpecs()
  {
  }

}
