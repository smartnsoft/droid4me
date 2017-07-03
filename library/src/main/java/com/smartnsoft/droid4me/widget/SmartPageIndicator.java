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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * A page view component.
 *
 * @author Édouard Mercier
 * @since 2011.01.10
 */
public class SmartPageIndicator
    extends LinearLayout
{

  public SmartPageIndicator(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setOrientation(LinearLayout.HORIZONTAL);
  }

  public SmartPageIndicator(Context context)
  {
    super(context);
    setOrientation(LinearLayout.HORIZONTAL);
  }

  /**
   * Indicates how many panes the pager is made of, and the way the pager dots should look like.
   *
   * @param count              the number of pages
   * @param dotImageResourceId the drawable resources which should be used to draw the dots ; it is important that this drawable has selected and un-selected stats
   * @param dotSpacing         the spacing between two dots
   */
  public void setPagesCount(int count, int dotImageResourceId, int dotSpacing)
  {
    if (count == getChildCount())
    {
      return;
    }
    removeAllViews();
    final Context context = getContext();
    for (int index = 0; index < count; index++)
    {
      final ImageView imageView = new ImageView(context);
      imageView.setSelected(false);
      imageView.setImageResource(dotImageResourceId);
      final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      layoutParams.leftMargin = dotSpacing / 2;
      layoutParams.rightMargin = dotSpacing / 2;
      addView(imageView, layoutParams);
    }
  }

  public void setSelection(int position)
  {
    for (int index = 0; index < getChildCount(); index++)
    {
      getChildAt(index).setSelected(index == position);
    }
  }

}
