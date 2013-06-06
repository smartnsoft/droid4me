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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * An extension of its parent {@link View} class, which offers the {@link SmartViewExtension} features.
 * 
 * @see SmartRelativeLayout
 * @see SmartFrameLayout
 * @see SmartImageView
 * 
 * @author Édouard Mercier
 * @since 2012.03.06
 */
public class SmartLinearLayout
    extends LinearLayout
    implements SmartViewExtension<SmartLinearLayout>
{

  private SmartViewExtension.ViewExtensionDelegate<SmartLinearLayout> viewExtensionDelegate;

  /**
   * Holds the widget maximum width.
   */
  protected int maxWidth = Integer.MAX_VALUE;

  /**
   * Holds the widget maximum height.
   */
  protected int maxHeight = Integer.MAX_VALUE;

  public SmartLinearLayout(Context context)
  {
    this(context, null);
  }

  public SmartLinearLayout(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public SmartLinearLayout(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
    initializeViewExtensionDelegateIfNecessary();
  }

  @Override
  public final float getRatio()
  {
    return viewExtensionDelegate.getRatio();
  }

  @Override
  public final void setRatio(float ratio)
  {
    viewExtensionDelegate.setRatio(ratio);
  }

  @Override
  public final OnSizeChangedListener<SmartLinearLayout> getOnSizeChangedListener()
  {
    return viewExtensionDelegate.getOnSizeChangedListener();
  }

  @Override
  public final void setOnSizeChangedListener(OnSizeChangedListener<SmartLinearLayout> onSizeChangedListener)
  {
    viewExtensionDelegate.setOnSizeChangedListener(onSizeChangedListener);
  }

  @Override
  public final boolean isRequestLayoutDisabled()
  {
    return viewExtensionDelegate.isRequestLayoutDisabled();
  }

  @Override
  public final void setRequestLayoutDisabled(boolean requestLayoutDisabled)
  {
    viewExtensionDelegate.setRequestLayoutDisabled(requestLayoutDisabled);
  }

  @Override
  public void onSuperMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override
  public final void setSelfMeasuredDimension(int measuredWidth, int measuredHeight)
  {
    setMeasuredDimension(measuredWidth, measuredHeight);
  }

  /**
   * Sets the widget maximum width. Defaults to {@code Integer#MAX_VALUE}. A {@link #requestLayout()} is required for the new value to take effect.
   * 
   * @param maxWidth
   *          the new widget maximum width
   */
  public final void setMaxWidth(int maxWidth)
  {
    this.maxWidth = maxWidth;
  }

  /**
   * Sets the widget maximum height. Defaults to {@code Integer#MAX_VALUE}. A {@link #requestLayout()} is required for the new value to take effect.
   * 
   * @param maxHeight
   *          the new widget maximum height
   */
  public final void setMaxHeight(int maxHeight)
  {
    this.maxHeight = maxHeight;
  }

  @Override
  public void requestLayout()
  {
    initializeViewExtensionDelegateIfNecessary();
    if (viewExtensionDelegate.isRequestLayoutDisabled() == false)
    {
      super.requestLayout();
    }
  }

  @Override
  protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
  {
    super.onSizeChanged(newWidth, newHeight, newHeight, oldHeight);
    viewExtensionDelegate.onSizeChanged(this, newWidth, newHeight, oldWidth, oldHeight);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    viewExtensionDelegate.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  private void initializeViewExtensionDelegateIfNecessary()
  {
    if (viewExtensionDelegate == null)
    {
      viewExtensionDelegate = new SmartViewExtension.ViewExtensionDelegate<SmartLinearLayout>(this);
    }
  }

}
