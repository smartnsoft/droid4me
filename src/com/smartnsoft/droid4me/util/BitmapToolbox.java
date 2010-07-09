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

package com.smartnsoft.droid4me.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.Shader.TileMode;

/**
 * A toolbox of {@link Bitmap} functions, that handle bitmap transformations.
 * 
 * @author Édouard Mercier
 * @since 2010.02.27
 */
public final class BitmapToolbox
{

  /**
   * @return the bitmap itself, but turned into a gray-scale
   */
  // Taken from http://stackoverflow.com/questions/1793338/drawable-grayscale
  public static Bitmap turnToGrayScale(Bitmap bitmap)
  {
    final Canvas canvas = new Canvas(bitmap);
    final Paint paint = new Paint();
    final ColorMatrix colorMatrix = new ColorMatrix();
    colorMatrix.setSaturation(0);
    final ColorMatrixColorFilter colorFilter = new ColorMatrixColorFilter(colorMatrix);
    paint.setColorFilter(colorFilter);
    canvas.drawBitmap(bitmap, 0, 0, paint);
    return bitmap;
  }

  /**
   * Merges a little bitmap to a larger one.
   * 
   * @param bigBitmap
   *          this object is {@link Bitmap#recycle() recycled} once the method has returned
   * @param littleBitmap
   *          this object is {@link Bitmap#recycle() recycled} once the method has returned
   * @param left
   *          the left of the top-left corner position on the big bitmap where to place the little bitmap
   * @param left
   *          the top of the top-left corner position on the big bitmap where to place the little bitmap
   * 
   * @return a bitmap which is a merge of the two bitmaps
   */
  public static Bitmap mergeBitmaps(Bitmap bigBitmap, Bitmap littleBitmap, int left, int top)
  {
    final int width = bigBitmap.getWidth();
    final int height = bigBitmap.getHeight();
    final Matrix matrix = new Matrix();
    matrix.preScale(1, -1);
    final Bitmap bitmap = Bitmap.createBitmap(bigBitmap, 0, 0, width, height, matrix, false);
    final Canvas canvas = new Canvas(bitmap);
    canvas.drawBitmap(bigBitmap, 0, 0, null);
    canvas.drawBitmap(littleBitmap, left, top, null);
    bigBitmap.recycle();
    littleBitmap.recycle();
    return bitmap;
  }

  /**
   * Computes a new bitmap with a reflect at the bottom.
   * 
   * <p>
   * Warning, the original bitmap is recycled and cannot be used after this call!
   * </p>
   * 
   * @param bitmap
   *          the bitmap used for the reflection
   * @param reflectionRatio
   *          the percentage of the input bitmap height (between 0 and 1) which indicates the height of the reflect
   * @param reflectionGap
   *          the number of pixels that should separate the bitmap and its reflect
   * @return a new bitmap, which contains the reflect
   */
  public static Bitmap computeReflectedBitmap(Bitmap bitmap, float reflectionRatio, int reflectionGap)
  {
    final int width = bitmap.getWidth();
    final int height = bitmap.getHeight();

    // This will not scale but will flip on the Y axis
    final Matrix matrix = new Matrix();
    matrix.preScale(1, -1);

    // Create a Bitmap with the flip matrix applied to it.
    // We only want the bottom half of the image
    final Bitmap reflectionBitmap = Bitmap.createBitmap(bitmap, 0, height - (int) (height * reflectionRatio), width, (int) (height * reflectionRatio), matrix,
        false);

    // Create a new bitmap with same width but taller to fit reflection
    final Bitmap bitmapWithReflection = Bitmap.createBitmap(width, height + (int) (height * reflectionRatio), Config.ARGB_8888);

    // Create a new Canvas with the bitmap that's big enough for
    // the image plus gap plus reflection
    final Canvas canvas = new Canvas(bitmapWithReflection);
    // Draw in the original image
    canvas.drawBitmap(bitmap, 0, 0, null);
    // Draw in the gap
    final Paint firstPaint = new Paint();
    canvas.drawRect(0, height, width, height + reflectionGap, firstPaint);
    // Draw in the reflection
    canvas.drawBitmap(reflectionBitmap, 0, height + reflectionGap, null);

    // Create a shader that is a linear gradient that covers the reflection
    final Paint paint = new Paint();
    final LinearGradient shader = new LinearGradient(0, bitmap.getHeight(), 0, bitmapWithReflection.getHeight() + reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
    // Set the paint to use this shader (linear gradient)
    paint.setShader(shader);
    // Set the Transfer mode to be porter duff and destination in
    paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
    // Draw a rectangle using the paint with our linear gradient
    canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

    // We free the original bitmap image
    bitmap.recycle();

    return bitmapWithReflection;
  }
}
