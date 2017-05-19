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