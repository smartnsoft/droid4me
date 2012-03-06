package com.smartnsoft.droid4me.widget;

import android.view.View;

/**
 * A simple interface method will be invoked every time its underlying {@View view} size {@link View#onSizeChanged() changes}.
 * 
 * <p>
 * It Enables to capture the event when the widget size changes.
 * </p>
 * 
 * @param WidgetClass
 *          the widget the size change listener applies to
 * 
 * @author Édouard Mercier
 * @since 2011.04.01
 */
public interface OnSizeChangedListener<WidgetClass>
{

  /**
   * Triggered when the widget view size has changed, and in particular when it is known for the first time.
   * 
   * @param widget
   *          the view related to that event
   * @param width
   *          the new width
   * @param height
   *          the new height
   * @param oldWidth
   *          the old width
   * @param oldHeight
   *          the old height
   */
  void onSizeChanged(WidgetClass widget, int newWidth, int newHeight, int oldWidth, int oldHeight);

}