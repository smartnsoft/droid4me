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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.Gallery;
import android.widget.ImageView;

/**
 * Enables to receive an event when the {@link ImageView} size has changed, set an automatic ratio between its width and its height, and discard the
 * layout requests.
 * 
 * <p>
 * This is especially useful when an image width is set to a specific width policy and that the height should be set accordingly.
 * </p>
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

  private float ratio = 9f / 16f;

  private boolean fixedSized;

  public float getRatio()
  {
    return ratio;
  }

  /**
   * Sets the ratio between the height and the width of the image. The default value is <code>9 / 16</code>.
   * 
   * @param ratio
   *          when set to <code>-1</code>, no ratio is applied
   */
  public void setRatio(float ratio)
  {
    this.ratio = ratio;
  }

  /**
   * The default value of the underlying flag is {@code false}.
   * 
   * @return {@code true} if and only if the {@link #requestLayout()} should do nothing
   * @see #setFixedSized(boolean)
   */
  public boolean isFixedSized()
  {
    return fixedSized;
  }

  /**
   * Indicates that the underlying {@code ImageView} {@link Drawable} size never changes, and hence causing the {@link #requestLayout()} to do nothing
   * (not invoking the parent method).
   * 
   * <p>
   * This feature is especially useful used in combination with the {@link Gallery} widget, which causes flickering issues when updating the widgets
   * inside a {@link ViewGroup}.
   * </p>
   * 
   * @param fixedSized
   *          when set to {@code true}, the {@link #requestLayout()} will not invoke its parent method, and hence will do nothing
   */
  public void setFixedSized(boolean fixedSized)
  {
    this.fixedSized = fixedSized;
  }

  public final OnSizeChangedListener<SmartImageView> getOnSizeChangedListener()
  {
    return onSizeChangedListener;
  }

  /**
   * Indicates the interface to trigger when the image view size has changed.
   */
  public void setOnSizeChangedListener(OnSizeChangedListener<SmartImageView> onSizeChangedListener)
  {
    this.onSizeChangedListener = onSizeChangedListener;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if (ratio != -1f)
    {
      setMeasuredDimension(getMeasuredWidth(), (int) (getMeasuredWidth() * ratio));
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

  @Override
  public void requestLayout()
  {
    if (fixedSized == false)
    {
      super.requestLayout();
    }
  }

}
