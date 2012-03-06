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
 * 
 * @author Édouard Mercier
 * @since 2012.03.06
 */
public class SmartLinearLayout
    extends LinearLayout
{

  private OnSizeChangedListener<SmartLinearLayout> onSizeChangedListener;

  private boolean requestLayoutDisabled;

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

  public final OnSizeChangedListener<SmartLinearLayout> getOnSizeChangedListener()
  {
    return onSizeChangedListener;
  }

  public final void setOnSizeChangedListener(OnSizeChangedListener<SmartLinearLayout> onSizeChangedListener)
  {
    this.onSizeChangedListener = onSizeChangedListener;
  }

  @Override
  protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
  {
    super.onSizeChanged(newWidth, newHeight, newHeight, oldHeight);
    if (onSizeChangedListener != null)
    {
      onSizeChangedListener.onSizeChanged(this, newWidth, newHeight, newHeight, oldHeight);
    }
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

}
