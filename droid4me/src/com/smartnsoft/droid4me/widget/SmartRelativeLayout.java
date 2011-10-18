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
import android.widget.RelativeLayout;

/**
 * Introduced in order to get notified when a container size changes, because there is no <code>OnSizeChangedListener</code> in Android {@link View},
 * whereas there is a {@link View#onSizeChanged()} method.
 * 
 * <p>
 * Use this wrapper container every time you need to be notified when a {@link View widget} sizes changes.
 * </p>
 * 
 * @see SmartFrameLayout
 * @author Édouard Mercier
 * @since 2011.04.01
 */
public class SmartRelativeLayout
    extends RelativeLayout
{

  /**
   * The single interface method will be invoked every time its underlying {@View view} size {@link View#onSizeChanged() changes}.
   */
  public static interface OnSizeChangedListener
  {
    void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight);
  }

  private SmartRelativeLayout.OnSizeChangedListener onSizeChangedListener;

  public SmartRelativeLayout(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  public SmartRelativeLayout(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public SmartRelativeLayout(Context context)
  {
    super(context);
  }

  public final SmartRelativeLayout.OnSizeChangedListener getOnSizeChangedListener()
  {
    return onSizeChangedListener;
  }

  public final void setOnSizeChangedListener(SmartRelativeLayout.OnSizeChangedListener onSizeChangedListener)
  {
    this.onSizeChangedListener = onSizeChangedListener;
  }

  @Override
  protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight)
  {
    super.onSizeChanged(newWidth, newHeight, newHeight, oldHeight);
    if (onSizeChangedListener != null)
    {
      onSizeChangedListener.onSizeChanged(newWidth, newHeight, newHeight, oldHeight);
    }
  }

}
