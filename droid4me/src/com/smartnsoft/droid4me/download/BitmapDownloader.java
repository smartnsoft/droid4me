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

import android.os.Handler;
import android.view.View;

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
    extends
    BasisBitmapDownloader<BitmapableBitmap, ViewableView, HandlerableHander>
{

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
  public static String IMPLEMENTATION_FQN = BasisBitmapDownloader.class.getName();

  /**
   * Indicates the upper limit of memory that each cache is allowed to reach.
   */
  public static long[] MAX_MEMORY_IN_BYTES;

  /**
   * When the cache is being cleaned-up, indicates the lower limit of memory that each cache is allowed to reach.
   */
  public static long[] LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES;

  /**
   * Indicates whether the instances should let the Java garbage collector handle bitmap soft references.
   */
  public static boolean[] USE_REFERENCES;

  public static boolean[] RECYCLE_BITMAP;

  private static volatile BitmapDownloader[] instances;

  /**
   * Implements a "double-checked locking" pattern.
   */
  public static BitmapDownloader getInstance()
  {
    return BitmapDownloader.getInstance(0);
  }

  // We accept the "out-of-order writes" case
  @SuppressWarnings("unchecked")
  public static BitmapDownloader getInstance(int position)
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
            final Constructor<? extends BitmapDownloader> constructor = implementationClass.getDeclaredConstructor(String.class, long.class, long.class, boolean.class, boolean.class);
            for (int index = 0; index < BitmapDownloader.INSTANCES_COUNT; index++)
            {
              newInstances[index] = constructor.newInstance("BitmapDownloader-" + index, BitmapDownloader.MAX_MEMORY_IN_BYTES[index], BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES[index], BitmapDownloader.USE_REFERENCES[index], BitmapDownloader.RECYCLE_BITMAP[index]);
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
    return BitmapDownloader.instances[position];
  }

  protected BitmapDownloader(String name, long maxMemoryInBytes,
      long lowLevelMemoryWaterMarkInBytes, boolean useReferences,
      boolean recycleMap)
  {
    super(name, maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
  }

  public final void get(View view, String bitmapUid, Object imageSpecs,
      Handler handler, DownloadInstructions.Instructions instructions)
  {
    get(view != null ? new ViewableView(view) : null, bitmapUid, imageSpecs, handler != null ? new HandlerableHander(handler)
        : null, instructions);
  }

  public final void get(boolean isBlocking, View view, String bitmapUid,
      Object imageSpecs, Handler handler,
      DownloadInstructions.Instructions instructions)
  {
    get(isBlocking, view != null ? new ViewableView(view) : null, bitmapUid, imageSpecs, handler != null ? new HandlerableHander(handler)
        : null, instructions);
  }

}
