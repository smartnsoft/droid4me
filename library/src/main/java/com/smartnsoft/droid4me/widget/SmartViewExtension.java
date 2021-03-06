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

package com.smartnsoft.droid4me.widget;

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

/**
 * An interface introduced in order to get notified when a container size changes, because there is no {@code OnSizeChangedListener} in Android
 * {@link View}, whereas there is a {@link View#onSizeChanged(int, int, int, int)} method.
 * <p>
 * <p>
 * In addition, this interface for disabling the {@link View#requestLayout()} method dynamically.
 * </p>
 *
 * @param <ViewClass> the {@link View} class the extension is applied on
 * @author Édouard Mercier
 * @since 2013.06.06
 */
public interface SmartViewExtension<ViewClass extends View>
{

  final class ViewExtensionDelegate<ViewClass extends View>
      implements SmartViewExtension<ViewClass>
  {

    public boolean newGeneration = true;

    private final SmartViewExtension<ViewClass> smartViewExtension;

    /**
     * The interface which will be invoked if the widget size changes.
     */
    private OnSizeChangedListener<ViewClass> onSizeChangedListener;

    /**
     * Holds the widget current horizontal/vertical dimensions ratio;
     */
    private float ratio = 0f;

    /**
     * Holds the widget maximum width.
     */
    private int maxWidth = Integer.MAX_VALUE;

    /**
     * Holds the widget maximum height.
     */
    private int maxHeight = Integer.MAX_VALUE;

    /**
     * A flag which states whether the {@link View#requestLayout()} calls should be disabled.
     */
    private boolean requestLayoutDisabled;

    public ViewExtensionDelegate(SmartViewExtension<ViewClass> smartViewExtension)
    {
      this.smartViewExtension = smartViewExtension;
    }

    @Override
    public float getRatio()
    {
      return ratio;
    }

    @Override
    public void setRatio(float ratio)
    {
      this.ratio = ratio;
    }

    public void setMaxWidth(int maxWidth)
    {
      this.maxWidth = maxWidth;
    }

    public void setMaxHeight(int maxHeight)
    {
      this.maxHeight = maxHeight;
    }

    @Override
    public OnSizeChangedListener<ViewClass> getOnSizeChangedListener()
    {
      return onSizeChangedListener;
    }

    @Override
    public void setOnSizeChangedListener(OnSizeChangedListener<ViewClass> onSizeChangedListener)
    {
      this.onSizeChangedListener = onSizeChangedListener;
    }

    @Override
    public boolean isRequestLayoutDisabled()
    {
      return requestLayoutDisabled;
    }

    @Override
    public void setRequestLayoutDisabled(boolean requestLayoutDisabled)
    {
      this.requestLayoutDisabled = requestLayoutDisabled;
    }

    @Override
    public void onSuperMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
      if (newGeneration == true)
      {
        if (ratio == 0f)
        {
          if (maxWidth == Integer.MAX_VALUE && maxHeight == Integer.MAX_VALUE)
          {
            smartViewExtension.onSuperMeasure(widthMeasureSpec, heightMeasureSpec);
          }
          else
          {
            // The view has a maximum width or a maximum height
            final int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
            final int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
            final int originalWidthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int originalHeightMode = MeasureSpec.getMode(heightMeasureSpec);
            final int finalWidth, finalHeight;
            final int finalWidthMode, finalHeightMode;
            if (originalWidthMode == MeasureSpec.EXACTLY && originalHeightMode == MeasureSpec.EXACTLY)
            {
              finalWidth = originalWidth;
              finalHeight = originalHeight;
              finalWidthMode = MeasureSpec.EXACTLY;
              finalHeightMode = MeasureSpec.EXACTLY;
            }
            else
            {
              if (maxWidth != Integer.MAX_VALUE)
              {
                if (originalWidthMode == MeasureSpec.EXACTLY)
                {
                  finalWidth = originalWidth;
                  finalWidthMode = MeasureSpec.EXACTLY;
                }
                else
                {
                  finalWidth = Math.min(originalWidth, maxWidth);
                  finalWidthMode = MeasureSpec.AT_MOST;
                }
              }
              else
              {
                finalWidth = originalWidth;
                finalWidthMode = originalWidthMode;
              }

              if (maxHeight != Integer.MAX_VALUE)
              {
                if (originalHeightMode == MeasureSpec.EXACTLY)
                {
                  finalHeight = originalHeight;
                  finalHeightMode = MeasureSpec.EXACTLY;
                }
                else
                {
                  finalHeight = Math.min(originalHeight, maxHeight);
                  finalHeightMode = originalHeightMode;
                }
              }
              else
              {
                finalHeight = originalHeight;
                finalHeightMode = originalHeightMode;
              }
            }
            smartViewExtension.onSuperMeasure(MeasureSpec.makeMeasureSpec(finalWidth, finalWidthMode), MeasureSpec.makeMeasureSpec(finalHeight, finalHeightMode));
          }
        }
        else
        {
          final float actualRatio = ratio > 0 ? ratio : -1f / ratio;
          final int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
          final int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
          final int originalWidthMode = MeasureSpec.getMode(widthMeasureSpec);
          final int originalHeightMode = MeasureSpec.getMode(heightMeasureSpec);
          final int finalWidth, finalHeight;
          if (originalWidthMode == MeasureSpec.EXACTLY && originalHeightMode == MeasureSpec.EXACTLY)
          {
            finalWidth = originalWidth;
            finalHeight = originalHeight;
          }
          else if (originalWidthMode == MeasureSpec.EXACTLY)
          {
            finalWidth = originalWidth;
            final float idealHeight = finalWidth / actualRatio;
            if (originalHeightMode == MeasureSpec.UNSPECIFIED)
            {
              finalHeight = (int) idealHeight;
            }
            else
            {
              finalHeight = idealHeight > originalHeight ? originalHeight : (int) idealHeight;
            }
          }
          else if (originalHeightMode == MeasureSpec.EXACTLY)
          {
            finalHeight = originalHeight;
            final float idealWidth = finalHeight * actualRatio;
            if (originalWidthMode == MeasureSpec.UNSPECIFIED)
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
            smartViewExtension.onSuperMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
          }
          // This is a way to notify the children about the dimensions
          smartViewExtension.onSuperMeasure(MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
          smartViewExtension.setSelfMeasuredDimension(finalWidth, finalHeight);
        }
      }
      else
      {
        // Taken from http://stackoverflow.com/questions/7058507/fixed-aspect-ratio-view
        if (ratio == 0f)
        {
          smartViewExtension.onSuperMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        else
        {
          final float actualRatio = ratio > 0 ? ratio : -1f / ratio;
          final int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
          final int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
          final int calculatedHeight = (int) ((float) originalWidth / actualRatio);
          final int finalWidth, finalHeight;
          if (originalHeight > 0 && calculatedHeight > originalHeight)
          {
            finalWidth = (int) ((float) originalHeight * actualRatio);
            finalHeight = originalHeight;
          }
          else
          {
            finalWidth = originalWidth;
            finalHeight = calculatedHeight;
          }
          smartViewExtension.onSuperMeasure(MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
        }
      }
    }

    @Override
    public void setSelfMeasuredDimension(int measuredWidth, int measuredHeight)
    {
      smartViewExtension.setSelfMeasuredDimension(measuredWidth, measuredHeight);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
      onSuperMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void onSizeChanged(ViewClass view, int newWidth, int newHeight, int oldWidth, int oldHeight)
    {
      if (onSizeChangedListener != null)
      {
        onSizeChangedListener.onSizeChanged(view, newWidth, newHeight, oldWidth, oldHeight);
      }
    }

  }

  /**
   * @return the horizontal/vertical ratio ; when set to {@code 0}, no ratio is applied {@link #setRatio(float)}
   * @see #setRatio(float)
   */
  float getRatio();

  /**
   * Sets the ratio between the width and the height of the image.
   * More formally, the value of the ratio is equal to the width divided by the height. The default value is {@code 16 / 9}.
   * <p>
   * <ul>
   * <li>When set to a positive value, the image height is taken as a reference to force the width.</li>
   * <li>When set to a negative value, the image width is taken as a reference to force the height, and the {@code ratio} argument absolute value is
   * taken.</li>
   * </ul>
   *
   * @param ratio when set to {@code 0}, no ratio is applied
   * @see #getRatio()
   */
  void setRatio(float ratio);

  /**
   * Sets the widget maximum width. Defaults to {@code Integer#MAX_VALUE}. A {@link View#requestLayout()} is required for the new value to take effect.
   *
   * @param maxWidth the new widget maximum width
   * @see #setMaxHeight(int)
   */
  void setMaxWidth(int maxWidth);

  /**
   * Sets the widget maximum height. Defaults to {@code Integer#MAX_VALUE}. A {@link View#requestLayout()} is required for the new value to take effect.
   *
   * @param maxHeight the new widget maximum height
   * @see #setMaxWidth(int)
   */
  void setMaxHeight(int maxHeight);

  /**
   * @return the currently registered interface which listens for the widget size changes events ; is {@code null} by default
   */
  OnSizeChangedListener<ViewClass> getOnSizeChangedListener();

  /**
   * Sets the interface that will be invoked when the widget size changes.
   *
   * @param onSizeChangedListener may be {@code null}, and in that case, no interface will be notified
   */
  void setOnSizeChangedListener(OnSizeChangedListener<ViewClass> onSizeChangedListener);

  /**
   * The default value of the underlying flag is {@code false}.
   *
   * @return {@code true} if and only if the {@link View#requestLayout()} method execution should do nothing
   * @see #setRequestLayoutDisabled(boolean)
   */
  boolean isRequestLayoutDisabled();

  /**
   * Indicates that the view {@link View#requestLayout()} method execution should do nothing (not invoking the parent method).
   * <p>
   * <p>
   * This feature is especially useful used in combination with the Gallery widget, which causes flickering issues when updating the widgets
   * inside a {@link ViewGroup}.
   * </p>
   *
   * @param requestLayoutDisabled when set to {@code true}, the {@link View#requestLayout()} will not invoke its parent method, and hence will do nothing
   */
  void setRequestLayoutDisabled(boolean requestLayoutDisabled);

  /**
   * Is responsible for invoking the underlying {@link View#onMeasure(int, int)} {@code super} method.
   */
  void onSuperMeasure(int widthMeasureSpec, int heightMeasureSpec);

  /**
   * Is responsible for invoking the underlying {@link View#setMeasuredDimension(int, int)} method.
   */
  void setSelfMeasuredDimension(int measuredWidth, int measuredHeight);

}
