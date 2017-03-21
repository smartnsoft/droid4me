/*
 * (C) Copyright 2009-2013 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.widget;

import android.view.View;

/**
 * A simple interface method will be invoked every time its underlying {@View view} size {@link View#onSizeChanged(int, int, int, int)} changes}.
 * <p/>
 * <p>
 * It Enables to capture the event when the widget size changes.
 * </p>
 *
 * @param <ViewClass> the {@link View} the size change listener applies to
 * @author Édouard Mercier
 * @since 2011.04.01
 */
public interface OnSizeChangedListener<ViewClass extends View>
{

  /**
   * Triggered when the widget view size has changed, and in particular when it is known for the first time.
   *
   * @param widget    the view related to that event
   * @param newWidth  the new width
   * @param newHeight the new height
   * @param oldWidth  the old width
   * @param oldHeight the old height
   */
  void onSizeChanged(ViewClass widget, int newWidth, int newHeight, int oldWidth, int oldHeight);

}