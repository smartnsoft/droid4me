package com.smartnsoft.droid4me.animation;

import android.view.animation.Animation;

/**
 * An abstract {@link Animation.AnimationListener} which makes possible to override only one method.
 *
 * @author Édouard Mercier
 * @since 2013.03.26
 */
public abstract class SimpleAnimationListener
    implements Animation.AnimationListener
{

  /**
   * Does nothing.
   */
  public void onAnimationEnd(Animation animation)
  {
  }

  /**
   * Does nothing.
   */
  public void onAnimationRepeat(Animation animation)
  {
  }

  /**
   * Does nothing.
   */
  public void onAnimationStart(Animation animation)
  {
  }

}