package com.smartnsoft.droid4me.download.gif;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/**
 * This class is used to display and
 * animated a GIF inside every View that are
 * based on ImageView.
 * See {@link GifDecoder} for the logic.
 *
 * @author Antoine Gerard
 * @since 2017.10.18
 */

public final class GifEngine
{

  private Gif gif;

  private ImageView imageView;

  public GifEngine(final ImageView imageView, final Gif gif)
  {
    this.imageView = imageView;
    this.gif = gif;
  }

  @RequiresApi(api = VERSION_CODES.HONEYCOMB)
  public void animate()
  {
    if (gif != null && gif.bitmaps.isEmpty() == false)
    {
      int[] table = new int[gif.bitmaps.size()];
      for (int i = 0; i < table.length; i++)
      {
        table[i] = i;
      }

      final ValueAnimator valueAnimator = ValueAnimator.ofInt(table);
      valueAnimator.setInterpolator(new LinearInterpolator());
      valueAnimator.setDuration(gif.getDuration());
      valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
      valueAnimator.addUpdateListener(new AnimatorUpdateListener()
      {
        @Override
        public void onAnimationUpdate(ValueAnimator animation)
        {
          imageView.setImageBitmap(gif.getBitmap((Integer) animation.getAnimatedValue()));
        }
      });
      valueAnimator.start();
    }
  }
}
