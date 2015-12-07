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
 * @author Édouard Mercier
 * @see SmartRelativeLayout
 * @see SmartFrameLayout
 * @see SmartImageView
 * @since 2012.03.06
 */
public class SmartLinearLayout
    extends LinearLayout
    implements SmartViewExtension<SmartLinearLayout>
{

  private SmartViewExtension.ViewExtensionDelegate<SmartLinearLayout> viewExtensionDelegate;

  public SmartLinearLayout(Context context)
  {
    this(context, null);
  }

  public SmartLinearLayout(Context context, AttributeSet attrs)
  {
    // Because the 3-argument constructor is only available from Android release 11!
    super(context, attrs);
    initializeViewExtensionDelegateIfNecessary();
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
  public final void setMaxWidth(int maxWidth)
  {
    viewExtensionDelegate.setMaxWidth(maxWidth);
  }

  @Override
  public final void setMaxHeight(int maxHeight)
  {
    viewExtensionDelegate.setMaxHeight(maxHeight);
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
