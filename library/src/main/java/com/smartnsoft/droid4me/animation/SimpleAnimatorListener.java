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

package com.smartnsoft.droid4me.animation;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.os.Build.VERSION_CODES;

/**
 * An abstract {@link Animator.AnimatorListener} which makes possible to override only one method.
 * <p>
 * <p>
 * Warning: this class is only available for applications running under Android v3+, i.e. release 11+!
 * </p>
 *
 * @author Ludovic Roland
 * @since 2017.05.19
 */
@TargetApi(VERSION_CODES.HONEYCOMB)
public abstract class SimpleAnimatorListener
    implements Animator.AnimatorListener
{

  /**
   * Does nothing.
   */
  @Override
  public void onAnimationStart(Animator animation)
  {

  }

  /**
   * Does nothing.
   */
  @Override
  public void onAnimationEnd(Animator animation)
  {

  }

  /**
   * Does nothing.
   */
  @Override
  public void onAnimationCancel(Animator animation)
  {

  }

  /**
   * Does nothing.
   */
  @Override
  public void onAnimationRepeat(Animator animation)
  {

  }

}