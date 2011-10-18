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
 * Gathers in one place all contracts related to the {@link ImageDownloader}.
 * 
 * @author �douard Mercier
 * @since 2011.09.03
 */
public final class DownloadContracts
{

  /**
   * We want no one to instantiate that class, which is a container.
   */
  private DownloadContracts()
  {
  }

  public static interface Bitmapable
  {

    int getSizeInBytes();

    void recycle();

  }

  public static interface Viewable
  {

    Object getTag();

    void setTag(Object tag);

    int getId();

  }

  public static interface Handlerable
  {

    boolean post(Runnable runnnable);

  }

}
