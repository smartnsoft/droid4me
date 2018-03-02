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
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * An extension of its parent {@code ViewPager} class, which offers the possibility to wrap_content.
 *
 * @author Ludovic Roland
 * @since 2017.05.19
 */
public class SmartViewPager
    extends ViewPager
{

  public SmartViewPager(Context context)
  {
    super(context);
  }

  public SmartViewPager(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    final int mode = MeasureSpec.getMode(heightMeasureSpec);

    if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST)
    {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);

      int height = 0;
      for (int index = 0; index < getChildCount(); index++)
      {
        final View child = getChildAt(index);
        child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

        final int measuredHeight = child.getMeasuredHeight();
        if (measuredHeight > height)
        {
          height = measuredHeight;
        }
      }

      heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

}
