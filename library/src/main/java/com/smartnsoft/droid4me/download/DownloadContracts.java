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
 * Gathers in one place all contracts related to the {@link BitmapDownloader}.
 *
 * @author Édouard Mercier
 * @since 2011.09.03
 */
public final class DownloadContracts
{

  /**
   * An abstraction of a {@link android.graphics.Bitmap} which makes possible the writing of unitary tests.
   * <p>
   * <p>
   * Any implementing class is supposed to wrap a bitmap, i.e. the bytes that enable to create it.
   * </p>
   */
  public interface Bitmapable
  {

    /**
     * @return the underlying bitmap size expressed in bytes
     */
    int getSizeInBytes();

    /**
     * Should release the memory hold by the underlying bitmap, when reclaimed.
     */
    void recycle();

  }

  /**
   * An abstraction of a {@link android.view.View} which makes possible the writing of unitary tests.
   * <p>
   * <p>
   * Any implementing class is supposed to wrap a view, i.e. a graphical widget.
   * </p>
   */
  public interface Viewable
  {

    /**
     * @return a tag object associated to the underlying view.
     */
    Object getTag();

    /**
     * Sets the tag object associated to the underlying view.
     *
     * @param tag
     */
    void setTag(Object tag);

    /**
     * The identifier returned here is just use for logging purposes.
     *
     * @return the unique identifier of the underlying view: two physical different views should not have the same identifier
     */
    int getId();

  }

  /**
   * An abstraction of a {@link android.os.Handler} which makes possible the writing of unitary tests.
   * <p>
   * <p>
   * Any implementing class is supposed run a command in the User Interface thread.
   * </p>
   */
  public interface Handlerable
  {

    /**
     * Is supposed to run the given command in the UI thread.
     *
     * @param runnnable the command that should be executed
     * @return {@code true} if and only if the execution in the UI thread has actually successfuly started
     */
    boolean post(Runnable runnnable);

  }

  /**
   * We want no one to instantiate that class, which is a container.
   */
  private DownloadContracts()
  {
  }

}
