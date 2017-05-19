package com.smartnsoft.droid4me.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * An extension of its parent {@link ViewPager} class, which offers the possibility to wrap_content.
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
        int measuredHeight = child.getMeasuredHeight();

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
