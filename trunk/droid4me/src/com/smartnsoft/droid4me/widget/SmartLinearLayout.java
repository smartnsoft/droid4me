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

package com.smartnsoft.droid4me.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.LinearLayout;

/**
 * Introduced in order to get notified when a container size changes, because there is no {@code OnSizeChangedListener} in Android {@link View},
 * whereas there is a {@link View#onSizeChanged()} method.
 * 
 * <p>
 * In addition, this class enables to disable the {@link View#requestLayout()} method dynamically.
 * </p>
 * 
 * <p>
 * Use this wrapper container every time you need to be notified when a {@link View widget} sizes changes.
 * </p>
 * 
 * @see SmartRelativeLayout
 * @see SmartFrameLayout
 * @see SmartImageView
 * 
 * @author ɉdouard Mercier
 * @since 2012.03.06
 */
public class SmartLinearLayout
    extends LinearLayout
{

  /**
   * The interface which will be invoked if the widget size changes.
   */
  private OnSizeChangedListener<SmartLinearLayout> onSizeChangedListener;

  /**
   * Holds the widget current vertical/horizontal dimensions ratio;
   */
  private float ratio = 0f;

  /**
   * A flag which states whether the {@link #requestLayout()} calls should be disabled.
   */
  private boolean requestLayoutDisabled;

  /**
   * Holds the widget maximum width.
   */
  protected int maxWidth = Integer.MAX_VALUE;

  /**
   * Holds the widget maximum height.
   */
  protected int maxHeight = Integer.MAX_VALUE;

  public SmartLinearLayout(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  public SmartLinearLayout(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public SmartLinearLayout(Context context)
  {
    super(context);
  }

  /**
   * @return the vertical/horizontal ratio ; when set to {@code 0}, no ratio is applied {@link #setRatio(float)}
   */
  public float getRatio()
  {
    return ratio;
  }

  /**
   * Sets the ratio between the width and the height of the image. The default value is {@code 9 / 16}.
   * 
   * <p>
   * <ul>
   * <li>When set to a positive value, the image width is taken as a reference to force the height.</li>
   * <li>When set to a negative value, the image height is taken as a reference to force the width, and the {@code ratio} argument absolute value is
   * taken.</li>
   * </ul>
   * </p>
   * 
   * @param ratio
   *          when set to {@code 0}, no ratio is applied
   * @see #getRatio()
   */
  public void setRatio(float ratio)
  {
    this.ratio = ratio;
  }

  /**
   * Sets the widget maximum width. Defaults to {@code Integer#MAX_VALUE}. A {@link #requestLayout()} is required for the new value to take effect.
   * 
   * @param maxWidth
   *          the new widget maximum width
   */
  public void setMaxWidth(int maxWidth)
  {
    this.maxWidth = maxWidth;
  }

  /**
   * Sets the widget maximum height. Defaults to {@code Integer#MAX_VALUE}. A {@link #requestLayout()} is required for the new value to take effect.
   * 
   * @param maxHeight
   *          the new widget maximum height
   */
  public void setMaxHeight(int maxHeight)
  {
    this.maxHeight = maxHeight;
  }

  /**
   * @return the currently registered interface which listens for the widget size changes events ; is {@code null} by default
   */
  public final OnSizeChangedListener<SmartLinearLayout> getOnSizeChangedListener()
  {
    return onSizeChangedListener;
  }

  /**
   * Sets the interface that will be invoked when the widget size changes.
   * 
   * @param onSizeChangedListener
   *          may be {@code null}, and in that case, no interface will be notified
   */
  public final void setOnSizeChangedListener(OnSizeChangedListener<SmartLinearLayout> onSizeChangedListener)
  {
    this.onSizeChangedListener = onSizeChangedListener;
  }

  /**
   * The default value of the underlying flag is {@code false}.
   * 
   * @return {@code true} if and only if the {@link #requestLayout()} method execution should do nothing
   * @see #setRequestLayoutEnabled(boolean)
   */
  public boolean isRequestLayoutDisabled()
  {
    return requestLayoutDisabled;
  }

  /**
   * Indicates that the view {@link #requestLayout()} method execution should do nothing (not invoking the parent method).
   * 
   * <p>
   * This feature is especially useful used in combination with the {@link Gallery} widget, which causes flickering issues when updating the widgets
   * inside a {@link ViewGroup}.
   * </p>
   * 
   * @param requestLayoutDisabled
   *          when set to {@code true}, the {@link #requestLayout()} will not invoke its parent method, and hence will do nothing
   */
  public void setRequestLayoutDisabled(boolean requestLayoutDisabled)
  {
    this.requestLayoutDisabled = requestLayoutDisabled;
  }

  @Override
  public void requestLayout()
  {
    if (requestLayoutDisabled == false)
    {
      super.requestLayout();
    }
  }

  @Override
  protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
  {
    super.onSizeChanged(newWidth, newHeight, newHeight, oldHeight);
    if (onSizeChangedListener != null)
    {
      onSizeChangedListener.onSizeChanged(this, newWidth, newHeight, oldWidth, oldHeight);
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (ratio > 0f)
    {
      final int measuredWidth = getMeasuredWidth();
      final int newHeight = (int) ((float) getMeasuredWidth() * ratio);
      setMeasuredDimension(measuredWidth, newHeight);
    }
    else if (ratio < 0f)
    {
      final int measuredHeight = getMeasuredHeight();
      final int newWidth = (int) ((float) getMeasuredHeight() * ratio) * -1;
      setMeasuredDimension(newWidth, measuredHeight);
    }
    else
    {
      final int measuredWidth = Math.min(getMeasuredWidth(), maxWidth);
      final int measuredHeight = Math.min(getMeasuredHeight(), maxHeight);
      setMeasuredDimension(measuredWidth, measuredHeight);
    }
  }

}
