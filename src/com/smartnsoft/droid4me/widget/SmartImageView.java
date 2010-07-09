/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 * Enables to receive an event when the {@link ImageView} size has changed and to set an automatic ratio between its width and its height.
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

  /**
   * Enables to capture the event when the image view size changes.
   */
  public static interface OnSizeChangedListener
  {

    /**
     * Triggered when an image view size has changed, and in particular when it is known for the first time.
     * 
     * @param imageView
     *          the image view related to that event
     * @param width
     *          the new width
     * @param height
     *          the new height
     * @param oldWidth
     *          the old width
     * @param oldHeight
     *          the old height
     */
    void onSizeChanged(SmartImageView imageView, int width, int height, int oldWidth, int oldHeight);

  }

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

  private SmartImageView.OnSizeChangedListener onSizeChangedListener;

  private float ratio = 9f / 16f;

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
   * Indicates the interface to trigger when the image view size has changed.
   */
  public void setOnSizeChangedListener(SmartImageView.OnSizeChangedListener onSizeChangedListener)
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
  protected void onSizeChanged(int w, int h, int oldw, int oldh)
  {
    super.onSizeChanged(w, h, oldw, oldh);
    if (onSizeChangedListener != null)
    {
      onSizeChangedListener.onSizeChanged(this, w, h, oldw, oldh);
    }
  }

}
