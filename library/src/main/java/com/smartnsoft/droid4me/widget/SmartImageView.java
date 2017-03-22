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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Enables to receive an event when the {@link ImageView} size has changed, set an automatic ratio between its width and its height, and discard the
 * layout requests.
 * <p>
 * <p>
 * This is especially useful when an image width is set to a specific width policy and that the height should be set accordingly, and vice-versa.
 * </p>
 *
 * @author Édouard Mercier
 * @see SmartFrameLayout
 * @see SmartRelativeLayout
 * @see SmartLinearLayout
 * @since 2010.02.27
 */
public class SmartImageView
    extends ImageView
    implements SmartViewExtension<SmartImageView>
{

  private SmartViewExtension.ViewExtensionDelegate<SmartImageView> viewExtensionDelegate;

  public SmartImageView(Context context)
  {
    this(context, null);
  }

  public SmartImageView(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public SmartImageView(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
    initializeViewExtensionDelegateIfNecessary();
    viewExtensionDelegate.setRatio(9f / 16f);
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
    super.setMaxWidth(maxWidth);
    initializeViewExtensionDelegateIfNecessary();
    viewExtensionDelegate.setMaxWidth(maxWidth);
  }

  @Override
  public final void setMaxHeight(int maxHeight)
  {
    super.setMaxHeight(maxHeight);
    initializeViewExtensionDelegateIfNecessary();
    viewExtensionDelegate.setMaxHeight(maxHeight);
  }

  @Override
  public final OnSizeChangedListener<SmartImageView> getOnSizeChangedListener()
  {
    return viewExtensionDelegate.getOnSizeChangedListener();
  }

  @Override
  public final void setOnSizeChangedListener(OnSizeChangedListener<SmartImageView> onSizeChangedListener)
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
  public void requestLayout()
  {
    initializeViewExtensionDelegateIfNecessary();
    if (viewExtensionDelegate.isRequestLayoutDisabled() == false)
    {
      super.requestLayout();
    }
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
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
  {
    final boolean newGeneration = viewExtensionDelegate.newGeneration;
    final float ratio = viewExtensionDelegate.getRatio();
    if (newGeneration == true)
    {
      if (ratio == 0f)
      {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      }
      else
      {
        final float actualRatio = ratio > 0 ? ratio : -1f / ratio;
        final int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int finalWidth, finalHeight;
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY)
        {
          finalWidth = originalWidth;
          finalHeight = originalHeight;
        }
        else if (widthMode == MeasureSpec.EXACTLY)
        {
          finalWidth = originalWidth;
          final float idealHeight = finalWidth * actualRatio;
          if (heightMode == MeasureSpec.UNSPECIFIED)
          {
            finalHeight = (int) idealHeight;
          }
          else
          {
            finalHeight = idealHeight > originalHeight ? originalHeight : (int) idealHeight;
          }
        }
        else if (heightMode == MeasureSpec.EXACTLY)
        {
          finalHeight = originalHeight;
          final float idealWidth = finalHeight / actualRatio;
          if (widthMode == MeasureSpec.UNSPECIFIED)
          {
            finalWidth = (int) idealWidth;
          }
          else
          {
            finalWidth = idealWidth > originalWidth ? originalWidth : (int) idealWidth;
          }
        }
        else
        {
          super.onMeasure(widthMeasureSpec, heightMeasureSpec);
          return;
        }
        this.setMeasuredDimension(finalWidth, finalHeight);
      }
    }
    else
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
    }
  }

  protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
  {
    super.onSizeChanged(newWidth, newHeight, newHeight, oldHeight);
    viewExtensionDelegate.onSizeChanged(this, newWidth, newHeight, oldWidth, oldHeight);
  }

  private void initializeViewExtensionDelegateIfNecessary()
  {
    if (viewExtensionDelegate == null)
    {
      viewExtensionDelegate = new SmartViewExtension.ViewExtensionDelegate<>(this);
    }
  }

}
