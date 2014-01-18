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
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
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

  /**
   * A component which is able to display graphically the state of the {@link BitmapDownloader#getInstance(int) instances}.
   * 
   * <p>
   * It is very handy for fine-tuning the {@link CoreBitmapDownloader#highLevelMemoryWaterMarkInBytes} and the
   * {@link CoreBitmapDownloader#lowLevelMemoryWaterMarkInBytes} parameters.
   * </p>
   * 
   * @since 2012.06.08
   */
  public static class AnalyticsDisplayer
      implements CoreBitmapDownloader.AnalyticsListener
  {

    private final static class JaugeView
        extends View
    {

      private CoreBitmapDownloader.CoreAnalyticsData analyticsData;

      private float jaugeLevel;

      private float jaugeWaterLevel;

      private final Paint jaugePaint;

      private final Paint markPaint;

      private final Paint leftTextPaint;

      private final Paint rightTextPaint;

      private final Paint errorTextPaint;

      private final String label;

      public JaugeView(Context context, String label)
      {
        super(context);
        this.label = label;
        jaugePaint = new Paint();
        jaugePaint.setColor(Color.BLACK);
        markPaint = new Paint();
        markPaint.setColor(Color.GRAY);
        final float density = context.getResources().getDisplayMetrics().density;
        final int textSizeInDip = 8;
        leftTextPaint = new Paint();
        leftTextPaint.setColor(Color.WHITE);
        leftTextPaint.setTextAlign(Align.LEFT);
        leftTextPaint.setTextSize(density * textSizeInDip);
        rightTextPaint = new Paint(leftTextPaint);
        rightTextPaint.setTextAlign(Align.RIGHT);
        errorTextPaint = new Paint();
        errorTextPaint.setColor(Color.RED);
        errorTextPaint.setTextAlign(Align.RIGHT);
        errorTextPaint.setTextSize(density * textSizeInDip);
      }

      @Override
      protected void onDraw(Canvas canvas)
      {
        super.onDraw(canvas);

        final int textOffset = 10;
        canvas.drawRect(new Rect(0, getHeight() - (int) ((float) getHeight() * jaugeLevel), getWidth(), getHeight()), jaugePaint);
        final int markY = getHeight() - (int) ((float) getHeight() * jaugeWaterLevel);
        canvas.drawLine(0, markY, getWidth(), markY, markPaint);
        canvas.drawText(label, textOffset, getHeight() - textOffset, leftTextPaint);
        if (analyticsData != null)
        {
          canvas.drawText(Integer.toString(analyticsData.cleanUpsCount), textOffset, leftTextPaint.getTextSize() + textOffset, leftTextPaint);
          canvas.drawText(Integer.toString(analyticsData.bitmapsCount), getWidth() - textOffset, getHeight() - textOffset, rightTextPaint);
          canvas.drawText(Integer.toString(analyticsData.outOfMemoryOccurences), getWidth() - textOffset, errorTextPaint.getTextSize() + textOffset,
              errorTextPaint);
        }
      }

    }

    protected JaugeView[] jauges;

    /**
     * Creates on the fly a new Android {@link ViewGroup} which is able to display the data exposed by an
     * {@link CoreBitmapDownloader.AnalyticsListener} implementation.
     * 
     * <p>
     * The container widget displays as many jauges as declared through the {@link BitmapDownloader#INSTANCES_COUNT} attribute, each jauge supposed to
     * represent its underlying {@link CoreBitmapDownloader} state.
     * </p>
     * 
     * @param context
     *          the context that will host the created widget
     * @param height
     *          the height of the generated widget
     * @return an Android {@link ViewGroup}, that you may add to a widgets hierarchy
     */
    public ViewGroup getView(Context context)
    {
      jauges = new JaugeView[BitmapDownloader.INSTANCES_COUNT];
      final LinearLayout container = new LinearLayout(context);
      container.setOrientation(LinearLayout.HORIZONTAL);
      for (int index = 0; index < BitmapDownloader.INSTANCES_COUNT; index++)
      {
        final JaugeView jauge = new JaugeView(context, Integer.toString(index));
        jauges[index] = jauge;
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        layoutParams.leftMargin = layoutParams.rightMargin = 3;
        container.addView(jauge, layoutParams);
      }
      return container;
    }

    /**
     * Plugs the current {@link CoreBitmapDownloader.AnalyticsListener} to the {@link CoreBitmapDownloader#ANALYTICS_LISTENER}, so that the current
     * instance listens to all analytics events.
     */
    public void plug()
    {
      CoreBitmapDownloader.ANALYTICS_LISTENER = this;
    }

    /**
     * Unplugs the current {@link CoreBitmapDownloader.AnalyticsListener} to the {@link CoreBitmapDownloader#ANALYTICS_LISTENER}, if it is currently
     * the registered analytics listener.
     */
    public void unplug()
    {
      if (CoreBitmapDownloader.ANALYTICS_LISTENER == this)
      {
        CoreBitmapDownloader.ANALYTICS_LISTENER = null;
      }
    }

    public void onAnalytics(CoreBitmapDownloader<?, ?, ?> coreBitmapDownloader, CoreBitmapDownloader.CoreAnalyticsData analyticsData)
    {
      jauges[coreBitmapDownloader.instanceIndex].jaugeLevel = (float) coreBitmapDownloader.getMemoryConsumptionInBytes() / (float) coreBitmapDownloader.highLevelMemoryWaterMarkInBytes;
      jauges[coreBitmapDownloader.instanceIndex].jaugeWaterLevel = (float) coreBitmapDownloader.lowLevelMemoryWaterMarkInBytes / (float) coreBitmapDownloader.highLevelMemoryWaterMarkInBytes;
      jauges[coreBitmapDownloader.instanceIndex].analyticsData = analyticsData;
      jauges[coreBitmapDownloader.instanceIndex].postInvalidate();
    }

  }

  /**
   * The number of instances of {@link BitmapDownloader} that will be created. Defaults to {@code 1}.
   * 
   * <p>
   * Set that parameter before invoking the {@link BitmapDownloader#get} method, because a later change has no effect.
   * </p>
   */
  public static int INSTANCES_COUNT = 1;

  /**
   * The fully qualified name of the {@link BasisBitmapDownloader} instances implementation. Defaults to {@link BitmapDownloader}.
   */
  public static String IMPLEMENTATION_FQN = BitmapDownloader.class.getName();

  /**
   * Indicates the upper limit of memory that each cache is allowed to reach. When that limit is reached, the instance cache memory is
   * {@link #cleanUpCache() cleaned up}.
   * 
   * <p>
   * If not set, the {@link #DEFAULT_HIGH_LEVEL_MEMORY_WATER_MARK_IN_BYTES} value will be used for all instances.
   * </p>
   */
  public static long[] HIGH_LEVEL_MEMORY_WATER_MARK_IN_BYTES;

  /**
   * Indicates the default upper limit of memory that each cache is allowed to reach.
   */
  public static final long DEFAULT_HIGH_LEVEL_MEMORY_WATER_MARK_IN_BYTES = 3l * 1024l * 1024l;

  /**
   * When the cache is being {@link #cleanUpCache() cleaned-up}, it indicates the lower limit of memory that each cache is allowed to reach.
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
   * <p>
   * You may tune the actual implementation of {@link BitmapDownloader} to use by setting beforehand the {@link BitmapDownloader#IMPLEMENTATION_FQN}
   * attribute.
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
              final long highWaterMark = BitmapDownloader.HIGH_LEVEL_MEMORY_WATER_MARK_IN_BYTES == null ? BitmapDownloader.DEFAULT_HIGH_LEVEL_MEMORY_WATER_MARK_IN_BYTES
                  : BitmapDownloader.HIGH_LEVEL_MEMORY_WATER_MARK_IN_BYTES[instanceIndex];
              final long lowWaterMark = BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES == null ? BitmapDownloader.DEFAULT_LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES
                  : BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES[instanceIndex];
              final boolean references = BitmapDownloader.USE_REFERENCES == null ? false : BitmapDownloader.USE_REFERENCES[instanceIndex];
              final boolean recycle = BitmapDownloader.RECYCLE_BITMAP == null ? false : BitmapDownloader.RECYCLE_BITMAP[instanceIndex];
              final BitmapDownloader bitmapDownloader = constructor.newInstance(instanceIndex, "BitmapDownloader-" + instanceIndex, highWaterMark,
                  lowWaterMark, references, recycle);
              newInstances[instanceIndex] = bitmapDownloader;
              if (log.isInfoEnabled())
              {
                log.info("Created a BitmapDownloader instance named '" + bitmapDownloader.name + "' with a high water mark set to " + bitmapDownloader.highLevelMemoryWaterMarkInBytes + " bytes and a low water mark set to " + bitmapDownloader.lowLevelMemoryWaterMarkInBytes + " bytes");
              }
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
   * 
   * @see #clear()
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
