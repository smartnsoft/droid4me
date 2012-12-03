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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * Enables to receive an event when the {@link ImageView} size has changed, set an automatic ratio between its width and its height, and discard the
 * layout requests.
 * 
 * <p>
 * This is especially useful when an image width is set to a specific width policy and that the height should be set accordingly, and vice-versa.
 * </p>
 * 
 * @see SmartFrameLayout
 * @see SmartRelativeLayout
 * @see SmartLinearLayout
 * 
 * @author Édouard Mercier
 * @since 2010.02.27
 */
public class SmartImageView
    extends ImageView
{

  public SmartImageView(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  public SmartImageView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public SmartImageView(Context context)
  {
    super(context);
  }

  private OnSizeChangedListener<SmartImageView> onSizeChangedListener;

  /**
   * Holds the widget current ratio;
   */
  private float ratio = 9f / 16f;

  /**
   * A flag which states whether the {@link #requestLayout()} calls should be disabled.
   */
  private boolean requestLayoutDisabled;

  public float getRatio()
  {
    return ratio;
  }

  /**
   * Sets the ratio between the height and the width of the image. The default value is {@code 9 / 16}.
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
   */
  public void setRatio(float ratio)
  {
    this.ratio = ratio;
  }

  /**
   * @return the currently registered interface which listens for the widget size changes events ; is {@code null} by default
   */
  public final OnSizeChangedListener<SmartImageView> getOnSizeChangedListener()
  {
    return onSizeChangedListener;
  }

  /**
   * Sets the interface that will be invoked when the widget size changes.
   * 
   * @param onSizeChangedListener
   *          may be {@code null}, and in that case, no interface will be notified
   */
  public final void setOnSizeChangedListener(OnSizeChangedListener<SmartImageView> onSizeChangedListener)
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
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (ratio > 0f)
    {
      final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
      if (widthSpecMode == MeasureSpec.EXACTLY || widthSpecMode == MeasureSpec.AT_MOST)
      {
        final int measuredWidth = getMeasuredWidth();
        final int newHeight = (int) ((float) getMeasuredWidth() * ratio);
        setMeasuredDimension(measuredWidth, newHeight);
      }
    }
    else if (ratio < 0f)
    {
      final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
      if (heightSpecMode == MeasureSpec.EXACTLY || heightSpecMode == MeasureSpec.AT_MOST)
      {
        final int measuredHeight = getMeasuredHeight();
        final int newWidth = (int) ((float) getMeasuredHeight() * ratio) * -1;
        setMeasuredDimension(newWidth, measuredHeight);
      }
    }
  }

  @Override
  protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
  {
    super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
    if (onSizeChangedListener != null)
    {
      onSizeChangedListener.onSizeChanged(this, newWidth, newHeight, oldWidth, oldHeight);
    }
  }

}
