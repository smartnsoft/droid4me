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

package com.smartnsoft.droid4me.graphics.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * A very basic bitmap drawable which displays the image just like it is. It is translucent.
 * <p>
 * Inspired from the Shelves application at
 * http://shelves.googlecode.com/svn/trunk/Shelves/src/org/curiouscreature/android/shelves/drawable/FastBitmapDrawable.java.
 *
 * @author Édouard Mercier
 * @since 2010.02.24
 */
public final class BasicBitmapDrawable
    extends Drawable
{

  private final Bitmap bitmap;

  public BasicBitmapDrawable(Bitmap bitamp)
  {
    bitmap = bitamp;
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

  public Bitmap getBitmap()
  {
    return bitmap;
  }

}
