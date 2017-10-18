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

package com.smartnsoft.droid4me.widget;

import android.view.View;

/**
 * A simple interface method will be invoked every time its underlying {@link View view} size {@link View#onSizeChanged(int, int, int, int)} changes}.
 * <p>
 * <p>
 * It Enables to capture the event when the widget size changes.
 * </p>
 *
 * @param <ViewClass> the {@link View view} the size change listener applies to
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