/*
 * (C) Copyright 2009-2011 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.download;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.smartnsoft.droid4me.download.DownloadInstructions.BitmapableBitmap;
import com.smartnsoft.droid4me.download.DownloadInstructions.HandlerableHander;
import com.smartnsoft.droid4me.download.DownloadInstructions.ViewableView;

/**
 * A first implementation of the {@link CoreBitmapDownloader} class.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
// TODO: introduce a pool of BitmapableBitmap, ViewableView and HandlerableHandler, so as to minimize GC
public class BitmapDownloader
    extends BasisBitmapDownloader<BitmapableBitmap, ViewableView, HandlerableHander>
{

  public static class AnalyticsDisplayer
      implements CoreBitmapDownloader.AnalyticsListener
  {

    private final static class JaugeView
        extends View
    {

      private final Paint jaugePaint;

      private final Paint markPaint;

      private final Paint textPaint;

      private final Paint errorTextPaint;

      private final String label;

      private float jaugeLevel;

      private float jaugeWaterLevel;

      private CoreBitmapDownloader.AnalyticsData analyticsData;

      public JaugeView(Context context, String label)
      {
        super(context);
        this.label = label;
        jaugePaint = new Paint();
        jaugePaint.setColor(Color.CYAN);
        markPaint = new Paint();
        markPaint.setColor(Color.GRAY);
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        errorTextPaint = new Paint();
        errorTextPaint.setColor(Color.RED);
      }

      @Override
      protected void onDraw(Canvas canvas)
      {
        super.onDraw(canvas);

        canvas.drawRect(new Rect(0, getHeight() - (int) ((float) getHeight() * jaugeLevel), getWidth(), getHeight()), jaugePaint);
        final int markY = getHeight() - (int) ((float) getHeight() * jaugeWaterLevel);
        canvas.drawLine(0, markY, getWidth(), markY, markPaint);
        canvas.drawText(label, 0, getHeight(), textPaint);
        if (analyticsData != null)
        {
          final int textOffset = 20;
          canvas.drawText(Integer.toString(analyticsData.cleanUpsCount), 0, textOffset, textPaint);
          canvas.drawText(Integer.toString(analyticsData.bitmapsCount), getWidth() - textOffset, getHeight(), textPaint);
          canvas.drawText(Integer.toString(analyticsData.outOfMemoryOccurences), getWidth() - textOffset, textOffset, errorTextPaint);
        }
      }

    }

    public static int HEIGHT = 150;

    protected JaugeView[] jauges;

    public ViewGroup getView(Context context)
    {
      jauges = new JaugeView[BitmapDownloader.INSTANCES_COUNT];
      final LinearLayout container = new LinearLayout(context);
      container.setOrientation(LinearLayout.HORIZONTAL);
      for (int index = 0; index < BitmapDownloader.INSTANCES_COUNT; index++)
      {
        final JaugeView jauge = new JaugeView(context, Integer.toString(index));
        jauges[index] = jauge;
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, AnalyticsDisplayer.HEIGHT, 1f);
        layoutParams.leftMargin = layoutParams.rightMargin = 3;
        container.addView(jauge, layoutParams);
      }
      return container;
    }

    public void plug()
    {
      CoreBitmapDownloader.ANALYTICS_LISTENER = this;
    }

    public void onAnalytics(CoreBitmapDownloader<?, ?, ?> coreBitmapDownloader, CoreBitmapDownloader.AnalyticsData analyticsData)
    {
      jauges[coreBitmapDownloader.instanceIndex].jaugeLevel = (float) coreBitmapDownloader.getMemoryConsumptionInBytes() / (float) coreBitmapDownloader.highLevelMemoryWaterMarkInBytes;
      jauges[coreBitmapDownloader.instanceIndex].jaugeWaterLevel = (float) coreBitmapDownloader.lowLevelMemoryWaterMarkInBytes / (float) coreBitmapDownloader.highLevelMemoryWaterMarkInBytes;
      jauges[coreBitmapDownloader.instanceIndex].analyticsData = analyticsData;
      jauges[coreBitmapDownloader.instanceIndex].postInvalidate();
    }

  }

  /**
   * The number of instances of {@link BitmapDownloader} that will be created.
   * 
   * <p>
   * Set that parameter before invoking the {@link BitmapDownloader#get} method, because a later change has no effect.
   * </p>
   */
  public static int INSTANCES_COUNT = 1;

  /**
   * The fully qualified name of the {@link BasisBitmapDownloader} instances implementation.
   */
  public static String IMPLEMENTATION_FQN = BitmapDownloader.class.getName();

  /**
   * Indicates the upper limit of memory that each cache is allowed to reach.
   * 
   * <p>
   * If not set, the {@link #DEFAULT_MAX_MEMORY_IN_BYTES} value will be used for all instances.
   * </p>
   */
  public static long[] MAX_MEMORY_IN_BYTES;

  /**
   * Indicates the default upper limit of memory that each cache is allowed to reach.
   */
  public static final long DEFAULT_MAX_MEMORY_IN_BYTES = 3l * 1024l * 1024l;

  /**
   * When the cache is being cleaned-up, indicates the lower limit of memory that each cache is allowed to reach.
   * 
   * <p>
   * If not set, the {@link #DEFAULT_LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES} value will be used for all instances.
   * </p>
   */
  public static long[] LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES;

  /**
   * Indicates the default lower limit of memory that each cache is allowed to reach.
   */
  public static final long DEFAULT_LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES = 1l * 1024l * 1024l;

  /**
   * Indicates whether the instances should let the Java garbage collector handle bitmap soft references.
   */
  public static boolean[] USE_REFERENCES;

  /**
   * Experimental, should be set to {@code false} for the moment.
   */
  public static boolean[] RECYCLE_BITMAP;

  private static volatile BitmapDownloader[] instances;

  /**
   * Equivalent to calling {@link #getInstance(int)} with a parameter set to {@code 0}.
   */
  public static BitmapDownloader getInstance()
  {
    return BitmapDownloader.getInstance(0);
  }

  /**
   * Gives access to a "BitmapDownloader" instance.
   * 
   * <p>
   * If no instance had been created at the time of the call, the instances will be created on the fly, which respects the lazy loading pattern.
   * </p>
   * 
   * @param index
   *          the instance to be returned
   * @return a valid bitmap downloader instance, ready to use
   * @throws ArrayIndexOutOfBoundsException
   *           in the case the required instance is greater or equal than {@link BitmapDownloader#INSTANCES_COUNT}
   */
  // We accept the "out-of-order writes" case
  @SuppressWarnings("unchecked")
  public static BitmapDownloader getInstance(int index)
  {
    if (BitmapDownloader.instances == null)
    {
      synchronized (BitmapDownloader.class)
      {
        if (BitmapDownloader.instances == null)
        {
          try
          {
            final Class<? extends BitmapDownloader> implementationClass = (Class<? extends BitmapDownloader>) Class.forName(BitmapDownloader.IMPLEMENTATION_FQN);
            final BitmapDownloader[] newInstances = (BitmapDownloader[]) Array.newInstance(implementationClass, BitmapDownloader.INSTANCES_COUNT);
            final Constructor<? extends BitmapDownloader> constructor = implementationClass.getDeclaredConstructor(int.class, String.class, long.class,
                long.class, boolean.class, boolean.class);
            for (int instanceIndex = 0; instanceIndex < BitmapDownloader.INSTANCES_COUNT; instanceIndex++)
            {
              final long highWaterMark = BitmapDownloader.MAX_MEMORY_IN_BYTES == null ? BitmapDownloader.DEFAULT_MAX_MEMORY_IN_BYTES
                  : BitmapDownloader.MAX_MEMORY_IN_BYTES[instanceIndex];
              final long lowWaterMark = BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES == null ? BitmapDownloader.DEFAULT_LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES
                  : BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES[instanceIndex];
              final boolean references = BitmapDownloader.USE_REFERENCES == null ? false : BitmapDownloader.USE_REFERENCES[instanceIndex];
              final boolean recycle = BitmapDownloader.RECYCLE_BITMAP == null ? false : BitmapDownloader.RECYCLE_BITMAP[instanceIndex];
              newInstances[instanceIndex] = constructor.newInstance(instanceIndex, "BitmapDownloader-" + instanceIndex, highWaterMark, lowWaterMark,
                  references, recycle);
            }
            // We only assign the instances class variable here, once all instances have actually been created
            BitmapDownloader.instances = newInstances;
          }
          catch (Exception exception)
          {
            if (log.isFatalEnabled())
            {
              log.fatal("Cannot instantiate properly the BitmapDownloader instances", exception);
            }
          }
        }
      }
    }
    return BitmapDownloader.instances[index];
  }

  protected BitmapDownloader(int instanceIndex, String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences,
      boolean recycleMap)
  {
    super(instanceIndex, name, maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
  }

  public final void get(View view, String bitmapUid, Object imageSpecs, Handler handler, DownloadInstructions.Instructions instructions)
  {
    get(view != null ? new ViewableView(view) : null, bitmapUid, imageSpecs, handler != null ? new HandlerableHander(handler) : null, instructions);
  }

  public final void get(boolean isPreBlocking, boolean isDownloadBlocking, View view, String bitmapUid, Object imageSpecs, Handler handler,
      DownloadInstructions.Instructions instructions)
  {
    get(isPreBlocking, isDownloadBlocking, view != null ? new ViewableView(view) : null, bitmapUid, imageSpecs,
        handler != null ? new HandlerableHander(handler) : null, instructions);
  }

  /**
   * Totally clears all instances memory caches.
   */
  public static synchronized void clearAll()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Clearing all BitmapDownloader instances");
    }
    for (int index = 0; index < BitmapDownloader.INSTANCES_COUNT; index++)
    {
      BitmapDownloader.getInstance(index).clear();
    }
  }

}
