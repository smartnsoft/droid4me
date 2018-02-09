package com.smartnsoft.droid4me.download.gif;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Bitmap;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;


/**
 * @author Antoine Gerard
 * @since 2017.10.18
 */

/**
 * This class represents a Gif File and its display in an ImageView
 */
@RequiresApi(api = VERSION_CODES.HONEYCOMB_MR1)
public final class Gif
    implements OnAttachStateChangeListener
{

  public static final Logger log = LoggerFactory.getInstance("Gif");

  private final List<Bitmap> bitmaps;

  private int duration;

  private ImageView imageView;

  /**
   * We are using a value animator for animating
   * the gif in the ImageView
   */
  private ValueAnimator valueAnimator;


  public Gif(InputStream inputStream, String url)
  {
    this.bitmaps = new ArrayList<>();
    decodeGif(inputStream, url);
  }

  @Override
  public void onViewAttachedToWindow(View view)
  {

  }

  /**
   * We end the gif animation once the ImageView that plays the
   * gif is detached from the current window.
   * This will prevent multiple display of the same gif in a recycler view
   * It will also free the memory preventing memory leak and OOM
   *
   * @param view
   */
  @Override
  public void onViewDetachedFromWindow(View view)
  {
    this.imageView.removeOnAttachStateChangeListener(this);
    end();
  }

  @RequiresApi(api = VERSION_CODES.HONEYCOMB_MR1)
  public void startAnimation(@NonNull final ImageView imageView)
  {
    this.imageView = imageView;
    this.imageView.addOnAttachStateChangeListener(this);
    animate();
  }

  @RequiresApi(android.os.Build.VERSION_CODES.HONEYCOMB_MR1)
  public void endAnimation()
  {
    end();
  }


  public int getHeight()
  {
    return bitmaps.isEmpty() ? 0 : bitmaps.get(0).getHeight();
  }

  public int getWidth()
  {
    return bitmaps.isEmpty() ? 0 : bitmaps.get(0).getWidth();
  }

  public int getFramesCount()
  {
    return bitmaps.size();
  }

  public Bitmap getBitmap(int index)
  {
    return bitmaps.get(index);
  }

  public List<Bitmap> getBitmaps()
  {
    return bitmaps;
  }

  public int getDuration()
  {
    return duration;
  }

  private void end()
  {
    if (valueAnimator != null && valueAnimator.isRunning())
    {
      valueAnimator.cancel();
      valueAnimator.end();
      bitmaps.clear();
    }
  }

  private void animate()
  {
    if (bitmaps.isEmpty() == false)
    {
      final int[] table = new int[bitmaps.size()];
      for (int i = 0; i < table.length; i++)
      {
        table[i] = i;
      }

      valueAnimator = ValueAnimator.ofInt(table);
      valueAnimator.setInterpolator(new LinearInterpolator());
      valueAnimator.setDuration(getDuration());
      valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
      valueAnimator.addUpdateListener(new AnimatorUpdateListener()
      {
        @Override
        public void onAnimationUpdate(ValueAnimator animation)
        {
          if (valueAnimator.isRunning())
          {
            imageView.setImageBitmap(bitmaps.get((Integer) animation.getAnimatedValue()));
          }
        }
      });
      valueAnimator.start();
    }
  }


  private void decodeGif(final InputStream inputStream, final String url)
  {
    final GifDecoder gifDecoder;
    {
      long milliseconds = 0L;
      if (log.isInfoEnabled())
      {
        milliseconds = System.currentTimeMillis();
      }
      gifDecoder = new GifDecoder();

      if (gifDecoder.read(inputStream, 2 * 8192) != GifDecoder.STATUS_OK)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot decode the animated GIF image");
        }
        return;
      }

      if (log.isInfoEnabled())
      {
        log.info("Parsed the animated GIF with URL '" + url + "' in " + (System.currentTimeMillis() - milliseconds) + " ms");
      }
    }

    final int framesCount = gifDecoder.getFrameCount();
    {
      final long milliseconds = System.currentTimeMillis();
      while (bitmaps.size() < framesCount)
      {
        gifDecoder.advance();
        final Bitmap nextBitmap = gifDecoder.getNextFrame();
        // We need to deep copy the bitmap
        duration += gifDecoder.getDelay(gifDecoder.getCurrentFrameIndex());
        final Bitmap bitmap = nextBitmap.copy(nextBitmap.getConfig(), true);
        if (bitmap == null)
        {
          break;
        }
        bitmaps.add(bitmap);
      }
      if (log.isInfoEnabled())
      {
        log.info("Prepared the individual images belonging to the animated GIF with URL '" + url + "' in " + (System.currentTimeMillis() - milliseconds) + " ms");
      }

      gifDecoder.clear();
    }
  }
}
