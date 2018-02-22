package com.smartnsoft.droid4me.download.gif;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Antoine Gerard
 * @since 2017.10.18
 * A header object containing the number of frames in an animated GIF image as well as basic
 * metadata like width and height that can be used to decode each individual frame of the GIF. Can
 * be shared by one or more {@link GifDecoder}s to play the same animated GIF in multiple views.
 * <p>
 * Source : https://github.com/bumptech/glide/blob/master/third_party/gif_decoder/src/main/java/com/bumptech/glide/gifdecoder/GifHeader.java
 */
public final class GifHeader
{

  int[] gct = null;

  int status = GifDecoder.STATUS_OK;

  int frameCount = 0;

  GifFrame currentFrame;

  List<GifFrame> frames = new ArrayList<>();

  // Logical screen size.
  // Full image width.
  int width;

  // Full image height.
  int height;

  // 1 : global color table flag.
  boolean gctFlag;

  // 2-4 : color resolution.
  // 5 : gct sort flag.
  // 6-8 : gct size.
  int gctSize;

  // Background color index.
  int bgIndex;

  // Pixel aspect ratio.
  int pixelAspect;

  //TODO: this is set both during reading the header and while decoding frames...
  int bgColor;

  int loopCount = 0;

  public int getHeight()
  {
    return height;
  }

  public int getWidth()
  {
    return width;
  }

  public int getNumFrames()
  {
    return frameCount;
  }

  /**
   * Global status code of GIF data parsing.
   */
  public int getStatus()
  {
    return status;
  }
}
