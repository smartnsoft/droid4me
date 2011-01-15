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

package com.smartnsoft.droid4me.graphics.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * A very basic bitmap drawable which displays the image just like it is. It is translucent.
 * 
 * Inspired from the Shelves application at
 * http://shelves.googlecode.com/svn/trunk/Shelves/src/org/curiouscreature/android/shelves/drawable/FastBitmapDrawable.java.
 * 
 * @author Édouard Mercier
 * @since 2010.02.24
 * 
 */
public final class BasicBitmapDrawable
    extends Drawable
{

  private final Bitmap bitmap;

  public BasicBitmapDrawable(Bitmap bitamp)
  {
    bitmap = bitamp;
  }

  public Bitmap getBitmap()
  {
    return bitmap;
  }

  @Override
  public void draw(Canvas canvas)
  {
    canvas.drawBitmap(bitmap, 0.0f, 0.0f, null);
  }

  @Override
  public int getOpacity()
  {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public void setAlpha(int alpha)
  {
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter)
  {
  }

  @Override
  public int getIntrinsicWidth()
  {
    return bitmap.getWidth();
  }

  @Override
  public int getIntrinsicHeight()
  {
    return bitmap.getHeight();
  }

  @Override
  public int getMinimumWidth()
  {
    return bitmap.getWidth();
  }

  @Override
  public int getMinimumHeight()
  {
    return bitmap.getHeight();
  }

}
