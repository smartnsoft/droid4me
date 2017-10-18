package com.smartnsoft.droid4me.download.gif;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;


/**
 * @author Antoine Gerard
 * @since 2017.10.18
 */

/**
 * This class represents a Gif File.
 */
public final class Gif
{

  public static final Logger log = LoggerFactory.getInstance("Gif");

  public final List<Bitmap> bitmaps;

  public Gif(InputStream inputStream, String url)
  {
    this.bitmaps = new ArrayList<Bitmap>();
    final GifDecoder gifDecoder;
    {
      final long milliseconds = System.currentTimeMillis();
      gifDecoder = new GifDecoder();
      if (gifDecoder.read(inputStream, 8192) != GifDecoder.STATUS_OK)
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
    }
  }


  public int getHeight()
  {
    return bitmaps.size() < 1 ? 0 : bitmaps.get(0).getHeight();
  }

  public int getWidth()
  {
    return bitmaps.size() < 1 ? 0 : bitmaps.get(0).getWidth();
  }

  public int getFramesCount()
  {
    return bitmaps.size();
  }

  public Bitmap getBitmap(int index)
  {
    return bitmaps.get(index);
  }
}
