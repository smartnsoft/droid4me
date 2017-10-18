package com.smartnsoft.droid4me.download.gif;

/**
 * @author Antoine Gerard
 * @since 2017.10.18
 */

/**
 * Inner model class housing metadata for each frame.
 */
public final class GifFrame
{

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
  int[] lct;
}
