package com.smartnsoft.droid4me.download.gif;

/**
 * @author Antoine Gerard
 * @since 2017.10.18
 * <p>
 * Inner model class housing metadata for each frame.
 * Taken From : https://gist.github.com/devunwired/4479231
 */

public final class GifFrame
{

  /**
   * Size of the frame
   */
  int ix, iy, iw, ih;

  /**
   * Control Flag.
   */
  boolean interlace;

  /**
   * Control Flag.
   */
  boolean transparency;

  /**
   * Disposal Method.
   */
  int dispose;

  /**
   * Transparency Index.
   */
  int transIndex;

  /**
   * Delay, in ms, to next frame.
   */
  int delay;

  /**
   * Index in the raw buffer where we need to start reading to decode.
   */
  int bufferFrameStart;

  /**
   * Local Color Table.
   */
  int[] localColorTable;
}
