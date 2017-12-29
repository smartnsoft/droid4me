package com.smartnsoft.droid4me.download.gif;

import java.util.HashMap;
import java.util.HashSet;

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

@RequiresApi(api = VERSION_CODES.HONEYCOMB)
public final class GifEngine
{
  private Gif gif;

  private static volatile HashMap<Integer, GifEngine> map = new HashMap<>();

  private ImageView imageView;

  private ValueAnimator valueAnimator;

  public GifEngine(final ImageView imageView, final Gif gif)
  {
    this.imageView = imageView;
    this.gif = gif;

    final int key = imageView.hashCode();
    final GifEngine gifEngine = map.get(key);

    if (gifEngine != null)
    {
      gifEngine.recycle();
    }
    map.put(key, this);
  }

  public void recycle()
  {
    if (valueAnimator != null && valueAnimator.isRunning())
    {
      valueAnimator.end();
    }
  }

  public void end()
  {
    if (valueAnimator != null && valueAnimator.isRunning())
    {
      valueAnimator.end();
    }
  }

  public void animate()
  {
    if (gif != null && gif.bitmaps.isEmpty() == false)
    {
      int[] table = new int[gif.bitmaps.size()];
      for (int i = 0; i < table.length; i++)
      {
        table[i] = i;
      }

      valueAnimator = ValueAnimator.ofInt(table);
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
