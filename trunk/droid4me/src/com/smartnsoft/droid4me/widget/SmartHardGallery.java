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
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.Gallery;

/**
 * A {@link Gallery gallery} with a hard-sliding behavior.
 * 
 * <p>
 * This gallery prevents from swiping multiple gallery panes at a time.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2011.10.20
 */
public class SmartHardGallery
    extends Gallery
{

  /**
   * The minimum velocity value which forbids the sliding movement to swipe multiple gallery panes at the same time.
   */
  public static int VELOCITY_THRESHOLD = 400;

  public SmartHardGallery(Context context, AttributeSet attrs, int defStyle)
  {
    super(context, attrs, defStyle);
  }

  public SmartHardGallery(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public SmartHardGallery(Context context)
  {
    super(context);
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
  {
    if (Math.abs(velocityX) < VELOCITY_THRESHOLD)
    {
      return true;
    }
    if (e2.getX() > e1.getX())
    {
      onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
    }
    else
    {
      onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
    }
    return true;
  }

}
