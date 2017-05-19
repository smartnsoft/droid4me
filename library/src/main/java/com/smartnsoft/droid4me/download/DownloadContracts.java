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
