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

package com.smartnsoft.droid4me.support.v7.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.smartnsoft.droid4me.widget.OnSizeChangedListener;
import com.smartnsoft.droid4me.widget.SmartFrameLayout;
import com.smartnsoft.droid4me.widget.SmartLinearLayout;
import com.smartnsoft.droid4me.widget.SmartRelativeLayout;
import com.smartnsoft.droid4me.widget.SmartViewExtension;

import static android.support.v7.appcompat.R.styleable.AppCompatImageView;

/**
 * Enables to receive an event when the {@link ImageView} size has changed, set an automatic ratio between its width and its height, and discard the
 * layout requests.
 * <p>
 * <p>
 * This is especially useful when an image width is set to a specific width policy and that the height should be set accordingly, and vice-versa.
 * </p>
 *
 * @author Ludovic Roland
 * @see SmartFrameLayout
 * @see SmartRelativeLayout
 * @see SmartLinearLayout
 * @since 2010.02.27
 */
public class SmartAppCompatImageView
    extends AppCompatImageView
    implements SmartViewExtension<SmartAppCompatImageView>
{

  private ViewExtensionDelegate<SmartAppCompatImageView> viewExtensionDelegate;

  public SmartAppCompatImageView(Context context)
  {
    this(context, null);
  }

  public SmartAppCompatImageView(Context context, AttributeSet attrs)
  {
    this(context, attrs, 0);
  }

  public SmartAppCompatImageView(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
    initializeViewExtensionDelegateIfNecessary();
    viewExtensionDelegate.setRatio(16f / 9f);
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
  public final OnSizeChangedListener<SmartAppCompatImageView> getOnSizeChangedListener()
  {
    return viewExtensionDelegate.getOnSizeChangedListener();
  }

  @Override
  public final void setOnSizeChangedListener(OnSizeChangedListener<SmartAppCompatImageView> onSizeChangedListener)
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
          final float idealHeight = finalWidth / actualRatio;
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
          final float idealWidth = finalHeight * actualRatio;
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
      if (ratio < 0f)
      {
        final int measuredWidth = getMeasuredWidth();
        final int newHeight = (int) ((float) getMeasuredWidth() * ratio);
        setMeasuredDimension(measuredWidth, newHeight);
      }
      else if (ratio > 0f)
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
      viewExtensionDelegate = new ViewExtensionDelegate<>(this);
    }
  }

}
