package com.smartnsoft.droid4me.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * A page view component.
 * 
 * @since 2011.01.10
 * @author Édouard Mercier
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
   * @param count
   *          the number of pages
   * @param dotImageResourceId
   *          the drawable resources which should be used to draw the dots ; it is important that this drawable has selected and un-selected stats
   * @param dotSpacing
   *          the spacing between two dots
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
