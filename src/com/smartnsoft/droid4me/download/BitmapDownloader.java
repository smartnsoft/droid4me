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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;

/**
 * A first implementation of the {@link BasisBitmapDownloader} class.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public class BitmapDownloader
    extends BasisBitmapDownloader
{

  private static int preThreadCount;

  // private final static class ImagePriorityBlockingQueue
  // extends PriorityBlockingQueue<Runnable>
  // {
  // @Override
  // public boolean offer(Runnable runnable)
  // {
  // // final int activePoolSize = ImageDownloader.PRE_THREAD_POOL.getActiveCount();
  // // if (activePoolSize >= ImageDownloader.PRE_THREAD_POOL.getCorePoolSize() && activePoolSize <
  // // ImageDownloader.PRE_THREAD_POOL.getMaximumPoolSize())
  // // {
  // // return false;
  // // }
  // // return super.offer(runnable);
  // return false;
  // }
  //
  // public void add(Runnable runnable, boolean forceAddition)
  // {
  // super.offer(runnable);
  // }
  // };

  // private final static ImageDownloader.ImagePriorityBlockingQueue PRE_BLOCKING_QUEUE = new ImageDownloader.ImagePriorityBlockingQueue();

  private final static ThreadPoolExecutor PRE_THREAD_POOL = new ThreadPoolExecutor(2, 3, 5l, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(), new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-" + (BitmapDownloader.preThreadCount < BitmapDownloader.PRE_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "pre #" + BitmapDownloader.preThreadCount++);
      return thread;
    }
  }, /*
      * new RejectedExecutionHandler() { public void rejectedExecution(Runnable command, ThreadPoolExecutor executor) {
      * ImageDownloader.PRE_BLOCKING_QUEUE.add(command, true); } }
      */new ThreadPoolExecutor.AbortPolicy());

  private static int downloadThreadCount;

  @SuppressWarnings("serial")
  private final static ThreadPoolExecutor DOWNLOAD_THREAD_POOL = new ThreadPoolExecutor(2, 4, 5l, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>()
  {
    // @Override
    // public boolean offer(Runnable runnable)
    // {
    // final int activePoolSize = ImageDownloader.DOWNLOAD_THREAD_POOL.getActiveCount();
    // if (activePoolSize >= ImageDownloader.DOWNLOAD_THREAD_POOL.getCorePoolSize() && activePoolSize <
    // ImageDownloader.DOWNLOAD_THREAD_POOL.getMaximumPoolSize())
    // {
    // return false;
    // }
    // return super.offer(runnable);
    // }
  }, new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-" + (BitmapDownloader.downloadThreadCount < BitmapDownloader.DOWNLOAD_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "download #" + BitmapDownloader.downloadThreadCount++);
      return thread;
    }
  }, new ThreadPoolExecutor.AbortPolicy());

  private static int commandsCount;

  // TODO: define a pool of Command objects, so as to minimize GC, if possible
  private class PreCommand
      implements Runnable, Comparable<PreCommand>
  {

    private final int order = BitmapDownloader.commandsCount++;

    protected final int id;

    protected final View view;

    protected final String bitmapUid;

    protected final Object imageSpecs;

    protected final Handler handler;

    protected final BasisBitmapDownloader.Instructions instructions;

    private boolean executeEnd;

    private int state;

    protected BasisBitmapDownloader.UsedBitmap usedBitmap;

    public PreCommand(int id, View view, String bitmapUid, Object imageSpecs, Handler handler, BasisBitmapDownloader.Instructions instructions)
    {
      this.id = id;
      this.view = view;
      this.bitmapUid = bitmapUid;
      this.imageSpecs = imageSpecs;
      this.handler = handler;
      this.instructions = instructions;
    }

    public PreCommand(int id, View view, String bitmapUid, Object imageSpecs, Handler handler, BasisBitmapDownloader.Instructions instructions,
        boolean executeEnd)
    {
      this(id, view, bitmapUid, imageSpecs, handler, instructions);
      this.executeEnd = executeEnd;
    }

    /**
     * Introduced in order to reduce the allocation of a {@link Runnable}, and for an optimization purpose.
     * 
     * <p>
     * The {@link BitmapDownloader.PreCommand#view} is ensured not to be null when this method is invoked.
     * </p>
     */
    public final void run()
    {
      try
      {
        if (executeEnd == false)
        {
          executeEnd = true;
          executeStart(false);
        }
        else
        {
          executeEnd();
        }
      }
      catch (Throwable throwable)
      {
        if (log.isErrorEnabled())
        {
          log.error("An unhandled exception has been raised during the processing of a command", throwable);
        }
      }
    }

    protected void executeEnd()
    {
      // if (log.isDebugEnabled())
      // {
      // log.debug("Running the action in state '" + state + "'");
      // }
      // We only bind the temporary image provided there is not already another command bound to be processed
      Integer commandId = prioritiesStack.get(view);
      if (state != 0 && (commandId == null || commandId != id))
      {
        if (log.isDebugEnabled())
        {
          log.debug("The bitmap corresponding to the id '" + bitmapUid + "' will not be bound to its image, because this image has asked for another bitmap URL in the meantime");
        }
        return;
      }
      try
      {
        switch (state)
        {
        case 0:
          instructions.onBindLocalBitmap(view, bitmapUid, imageSpecs);
          instructions.onBitmapBound(true, view, bitmapUid, imageSpecs);
          // if (log.isDebugEnabled())
          // {
          // log.debug("Set the local drawable for the image with id '" + bitmapUid + "'");
          // }
          break;
        case 1:
          instructions.onBindTemporaryBitmap(view, bitmapUid, imageSpecs);
          // if (log.isDebugEnabled())
          // {
          // log.debug("Set the temporary drawable for the image with id '" + bitmapUid + "'");
          // }
          break;
        case 2:
          if (instructions.onBindBitmap(false, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs) == false)
          {
            // The binding is performed only if the instructions did not bind it
            // THINK: what can we do?
            // view.setImageBitmap(usedBitmap.getBitmap());
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(view);
          instructions.onBitmapBound(true, view, bitmapUid, imageSpecs);
          // if (log.isDebugEnabled())
          // {
          // log.debug("Set the cached bitmap for the image with id '" + bitmapUid + "'");
          // }
          break;
        }
        // We clear the priorities stack if the work is over for that command (i.e. no DownloadBitmapCommand is required)
        commandId = prioritiesStack.get(view);
        if (state != 1 && commandId == id)
        {
          prioritiesStack.remove(view);
        }
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Process exceeding available memory", exception);
        }
        cleanUpCache();
      }
    }

    public void executeStart(boolean isFromGuiThread)
    {
      // if (log.isDebugEnabled())
      // {
      // log.debug("Starting to handle the drawable for the image with identifier '" + bitmapUid + "'");
      // }
      // The command is removed from the priority stack
      if (view != null)
      {
        prioritiesPreStack.remove(view);
      }

      // We set a local image if available
      if (setLocalImageIfPossible(isFromGuiThread) == true)
      {
        return;
      }

      // We compute the image URL and proceed to its download if necessary
      final String url = instructions.computeUrl(bitmapUid, imageSpecs);

      // We use an image from the cache if possible
      if (setImageFromCacheIfPossible(url, isFromGuiThread) == true)
      {
        return;
      }

      // We set a temporary image if available
      setTemporaryImageIfPossible(isFromGuiThread);

      // And we stack the image download command
      if (view != null)
      {
        // But we test whether the download is still required
        final Integer commandId = prioritiesStack.get(view);
        if (commandId == null || commandId != id)
        {
          return;
        }
        // We remove a previously stacked command for the same image
        final BitmapDownloader.PreCommand alreadyStackedCommand = prioritiesDownloadStack.get(view);
        if (alreadyStackedCommand != null)
        {
          if (log.isDebugEnabled())
          {
            log.debug("Removed an already stacked download command corresponding to the image with id " + view.getId());
          }
          if (BitmapDownloader.DOWNLOAD_THREAD_POOL.remove(alreadyStackedCommand) == false)
          {
            if (log.isErrorEnabled())
            {
              log.error("Could not find the download command relative to the image with id '" + bitmapUid + "' to remove it!");
            }
          }
        }
      }
      final BitmapDownloader.DownloadBitmapCommand downloadCommand = computeDownloadBitmapCommand(id, view, url, bitmapUid, imageSpecs, handler,
          instructions);
      if (view != null)
      {
        prioritiesDownloadStack.put(view, downloadCommand);
      }
      BitmapDownloader.DOWNLOAD_THREAD_POOL.execute(downloadCommand);
    }

    private boolean setLocalImageIfPossible(boolean isFromGuiThread)
    {
      if (instructions.hasLocalBitmap(bitmapUid, imageSpecs) == true)
      {
        if (view != null)
        {
          // We need to do that in the GUI thread!
          state = 0;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that local resource for the image with id '" + bitmapUid + "' from the GUI thread");
              }
            }
          }
          else
          {
            run();
          }
          return true;
        }
      }
      return false;
    }

    private void setTemporaryImageIfPossible(boolean isFromGuiThread)
    {
      if (instructions.hasTemporaryBitmap(bitmapUid, imageSpecs) == true)
      {
        if (view != null)
        {
          // We need to do that in the GUI thread!
          state = 1;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that temporary local resource for the image with id '" + bitmapUid + "' from the GUI thread");
              }
            }
          }
          else
          {
            run();
          }
        }
      }
    }

    private boolean setImageFromCacheIfPossible(String url, boolean isFromGuiThread)
    {
      // We check that the image is no already in the cache
      final BasisBitmapDownloader.UsedBitmap otherUsedBitmap = getUsedBitmapFromCache(url);
      if (otherUsedBitmap != null)
      {
        usedBitmap = otherUsedBitmap;
        usedBitmap.rememberAccessed();
        instructions.onBitmapReady(true, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs);
        if (view != null)
        {
          // if (log.isDebugEnabled())
          // {
          // log.debug("Re-using the cached bitmap for the URL '" + url + "'");
          // }
          // It is possible that no bitmap exists for that URL
          // We need to do that in the GUI thread!
          state = 2;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that cached bitmap for the URL '" + url + "' from the GUI thread");
              }
            }
          }
          else
          {
            run();
          }
        }
        else
        {
          // if (log.isDebugEnabled())
          // {
          // log.debug("The image corresponding to the URL '" + url + "' is null: no need to apply its cached bitmap");
          // }
        }
        return true;
      }
      return false;
    }

    public int compareTo(PreCommand other)
    {
      return other.order - order;
    }

  }

  // TODO: when a second download for the same image UID occurs, do not run it
  protected class DownloadBitmapCommand
      extends BitmapDownloader.PreCommand
      implements BasisBitmapDownloader.InputStreamDownloadInstructor
  {

    protected final String url;

    private boolean downloaded;

    private boolean inputStreamAsynchronous;

    public DownloadBitmapCommand(int id, View view, String url, String bitmapUid, Object imageSpecs, Handler handler,
        BasisBitmapDownloader.Instructions instructions)
    {
      super(id, view, bitmapUid, imageSpecs, handler, instructions);
      this.url = url;
    }

    public final void setAsynchronous()
    {
      inputStreamAsynchronous = true;
    }

    public final void onDownloaded(InputStream inputStream)
    {
      try
      {
        inputStream = onInputStreamDownloaded(inputStream);
        downloaded = true;
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Process exceeding available memory", exception);
        }
        cleanUpCache();
        return;
      }

      // Indicates whether another command has been set for the image in the meantime
      final boolean forgetTheBinding = view != null && asynchronousDownloadCommands.remove(view) == false;

      final Bitmap bitmap = convertInputStream(inputStream);
      if (forgetTheBinding == false && bitmap == null)
      {
        instructions.onBitmapReady(false, view, null, bitmapUid, imageSpecs);
        return;
      }
      usedBitmap = putInCache(url, bitmap);
      if (forgetTheBinding == false)
      {
        instructions.onBitmapReady(true, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs);
        bindBitmap();
      }
    }

    /**
     * The {@link BitmapDownloader.PreCommand#view} is ensured not to be null when this method is invoked.
     */
    @Override
    public final void executeEnd()
    {
      // We only bind the image provided there is not already another command bound to be processed
      Integer commandId = prioritiesStack.get(view);
      if (commandId == null || commandId != id)
      {
        // if (log.isDebugEnabled())
        // {
        // log.debug("The bitmap corresponding to the URL '" + url +
        // "' will not be bound to its image, because this image has asked for another bitmap URL in the meantime");
        // }
        return;
      }
      try
      {
        if (usedBitmap != null)
        {
          if (instructions.onBindBitmap(downloaded, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs) == false)
          {
            // The binding is performed only if the instructions did not bind it
            // THINK: what can we do?
            // view.setImageBitmap(usedBitmap.getBitmap());
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(view);
        }
        instructions.onBitmapBound(usedBitmap != null, view, bitmapUid, imageSpecs);
        // We clear the priorities stack if the work is over for that command (i.e. no DownloadBitmapCommand is required)
        commandId = prioritiesStack.get(view);
        if (commandId != null && commandId == id)
        {
          prioritiesStack.remove(view);
        }
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Process exceeding available memory", exception);
        }
        cleanUpCache();
      }
    }

    @Override
    public final void executeStart(boolean isFromGuiThread)
    {
      // The command is removed from the priority stack
      if (view != null)
      {
        prioritiesDownloadStack.remove(view);
      }
      // We need to check whether the same URL has not been downloaded in the meantime
      final BasisBitmapDownloader.UsedBitmap otherUsedBitmap = getUsedBitmapFromCache(url);
      if (otherUsedBitmap == null)
      {
        // If the bitmap is not already in memory, we load it in memory
        final Bitmap bitmap = retrieveBitmap();
        if (bitmap == null)
        {
          if (inputStreamAsynchronous == false)
          {
            if (url != null && url.length() >= 1 && log.isWarnEnabled())
            {
              log.warn("The bitmap relative to the URL '" + url + "' is null");
            }
          }
          // We let intentionally the 'usedBitmap' null
          instructions.onBitmapReady(false, view, null, bitmapUid, imageSpecs);
          // We cannot do anything further in that case
          return;
        }
        else
        {
          usedBitmap = putInCache(url, bitmap);
        }
      }
      else
      {
        // Otherwise, we reuse it
        usedBitmap = otherUsedBitmap;
        usedBitmap.rememberAccessed();
      }

      if (usedBitmap != null)
      {
        instructions.onBitmapReady(true, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs);
      }
      bindBitmap();
    }

    private final InputStream fetchInputStream()
        throws IOException
    {
      InputStream inputStream = null;
      // We do not event attempt to request for the input stream if the bitmap URI is null or empty
      if (url != null && url.length() >= 1)
      {
        try
        {
          inputStream = instructions.getInputStream(bitmapUid, imageSpecs, url, this);
        }
        catch (IOException exception)
        {
          if (log.isWarnEnabled())
          {
            log.warn("Could not get the provided input stream corresponding to the URL '" + url + "'", exception);
          }
          // In that case, we consider that the input stream custom download was a failure
          throw exception;
        }
      }
      else
      {
        return null;
      }

      if (inputStream != null)
      {
        // if (log.isDebugEnabled())
        // {
        // log.debug("Using the provided input stream corresponding to the URL '" + url + "'");
        // }
        return inputStream;
      }

      // We determine whether the input stream has turned asynchronous
      if (inputStreamAsynchronous == true)
      {
        asynchronousDownloadCommands.add(view);
        return null;
      }

      final long start = System.currentTimeMillis();
      final URL aURL = new URL(url);
      final URLConnection connection = aURL.openConnection();
      instructions.onBeforeBitmapDownloaded(bitmapUid, imageSpecs, connection);
      // if (log.isDebugEnabled())
      // {
      // log.debug("Setting to the URL connection belonging to class '" + connection.getClass().getName() + "' a 'User-Agent' header property");
      // }
      connection.connect();
      final long stop = System.currentTimeMillis();
      inputStream = connection.getInputStream();
      if (log.isDebugEnabled())
      {
        log.debug("The thread '" + Thread.currentThread().getName() + "' downloaded in " + (stop - start) + " ms the image relative to the URL '" + url + "'");
      }
      try
      {
        inputStream = onInputStreamDownloaded(inputStream);
        downloaded = true;
        return inputStream;
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Process exceeding available memory", exception);
        }
        cleanUpCache();
        return null;
      }
    }

    private final Bitmap retrieveBitmap()
    {
      final InputStream inputStream;
      try
      {
        inputStream = fetchInputStream();
      }
      catch (IOException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Could not access to the image URL '" + url + "'");
        }
        return null;
      }
      if (inputStream == null)
      {
        if (inputStreamAsynchronous == false)
        {
          if (url != null && url.length() >= 1 && log.isWarnEnabled())
          {
            log.warn("The input stream used to build the bitmap corresponding to the URL '" + url + "' is null");
          }
        }
        return null;
      }
      try
      {
        return convertInputStream(inputStream);
      }
      finally
      {
        if (inputStream != null)
        {
          try
          {
            inputStream.close();
          }
          catch (IOException e)
          {
            // Does not matter
          }
        }
      }
    }

    protected InputStream onInputStreamDownloaded(InputStream inputStream)
    {
      return inputStream;
    }

    private final Bitmap convertInputStream(InputStream inputStream)
    {
      final Bitmap theBitmap;
      try
      {
        // final long start = System.currentTimeMillis();
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDither = false;
        options.inDensity = 0;
        theBitmap = BitmapFactory.decodeStream(inputStream, null, options);
        // final long stop = System.currentTimeMillis();
        // if (log.isDebugEnabled() && bitmap != null)
        // {
        // log.debug("The thread '" + Thread.currentThread().getName() + "' decoded in " + (stop - start) + " ms the bitmap with density " +
        // theBitmap.getDensity() + " relative to the URL '" + url + "'");
        // }
        return theBitmap;
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot decode the downloaded image because it exceeds the allowed memory", exception);
        }
        cleanUpCache();
        return null;
      }
    }

    private final void bindBitmap()
    {
      // We need to bind the bitmap to the image in the GUI thread!
      if (view != null)
      {
        if (handler.post(this) == false)
        {
          if (log.isWarnEnabled())
          {
            log.warn("Failed to apply the downloaded bitmap for the image with id '" + bitmapUid + "' and URL '" + url + "' from the GUI thread");
          }
        }
      }
    }

  }

  public static int INSTANCES_COUNT = 1;

  /**
   * The fully qualified name of the {@link BitmapDownloader} instances implementation.
   */
  public static String IMPLEMENTATION_FQN = BitmapDownloader.class.getName();

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
            final Constructor<? extends BitmapDownloader> constructor = implementationClass.getDeclaredConstructor(String.class, long.class, long.class,
                boolean.class, boolean.class);
            for (int index = 0; index < BitmapDownloader.INSTANCES_COUNT; index++)
            {
              newInstances[index] = constructor.newInstance("ImageDownloader-" + index, BitmapDownloader.MAX_MEMORY_IN_BYTES[index],
                  BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES[index], BitmapDownloader.USE_REFERENCES[index], BitmapDownloader.RECYCLE_BITMAP[index]);
            }
            // We only assign the instances class variable here, once all instances have actually been created
            BitmapDownloader.instances = newInstances;
          }
          catch (Exception exception)
          {
            if (log.isFatalEnabled())
            {
              log.fatal("Cannot instantiate properly the ImageDownloader instances", exception);
            }
          }
        }
      }
    }
    return BitmapDownloader.instances[position];
  }

  /**
   * A map which handles the priorities of the {@link BitmapDownloader.PreCommand pre-commands}: when a new command for an {@link View} is asked for,
   * if a {@link BitmapDownloader.PreCommand} is already stacked for the same image (i.e. present in {@link #preStack stacked}), the old one will be
   * discarded.
   */
  private final Map<View, BitmapDownloader.PreCommand> prioritiesPreStack;

  /**
   * A map which contains all the {@link BitmapDownloader.PreCommand commands} that are currently at the top of the priorities stack. When a new
   * command for an {@link View} is asked for, if a {@link BitmapDownloader.PreCommand} is already stacked for the same image (i.e. present in
   * {@link BitmapDownloader#preStack stacked}), the old one will be discarded.
   */
  private final Map<View, Integer> prioritiesStack;

  private final Map<View, BitmapDownloader.DownloadBitmapCommand> prioritiesDownloadStack;

  private final Set<View> asynchronousDownloadCommands = new HashSet<View>();

  /**
   * The counter of all commands, which is incremented on every new command, so as to identify them.
   */
  private int commandIdCount = -1;

  protected BitmapDownloader(String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    super(name, maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
    prioritiesStack = new Hashtable<View, Integer>();
    prioritiesPreStack = new Hashtable<View, BitmapDownloader.PreCommand>();
    prioritiesDownloadStack = new Hashtable<View, BitmapDownloader.DownloadBitmapCommand>();
  }

  @Override
  public final void get(View view, String bitmapUid, Object imageSpecs, Handler handler, BasisBitmapDownloader.Instructions instructions)
  {
    // if (log.isDebugEnabled())
    // {
    // log.debug("Asking to handle the bitmap related to the image with id '" + bitmapUid + "'");
    // }
    // try
    // {
    if (view != null)
    {
      // We indicate to the potential asynchronous input stream downloads that a new request is now set for the image
      asynchronousDownloadCommands.remove(view);

      // We remove a previously stacked command for the same image
      final BitmapDownloader.PreCommand alreadyStackedCommand = prioritiesPreStack.get(view);
      if (alreadyStackedCommand != null)
      {
        if (log.isDebugEnabled())
        {
          log.debug("Removed an already stacked command corresponding to the image with id '" + view.getId() + "'");
        }
        if (BitmapDownloader.PRE_THREAD_POOL.remove(alreadyStackedCommand) == false)
        {
          if (log.isErrorEnabled())
          {
            log.error("Could not find the pre-command relative to the image with id '" + bitmapUid + "' to remove it!");
          }
        }
      }
    }
    final BitmapDownloader.PreCommand command = new BitmapDownloader.PreCommand(++commandIdCount, view, bitmapUid, imageSpecs, handler, instructions);
    if (view != null)
    {
      prioritiesStack.put(view, command.id);
      prioritiesPreStack.put(view, command);
    }
    BitmapDownloader.PRE_THREAD_POOL.execute(command);
  }

  @Override
  public final void get(boolean isBlocking, View view, String bitmapUid, Object imageSpecs, Handler handler, BasisBitmapDownloader.Instructions instructions)
  {
    if (isBlocking == false)
    {
      get(view, bitmapUid, imageSpecs, handler, instructions);
    }
    else
    {
      final BitmapDownloader.PreCommand preCommand = new BitmapDownloader.PreCommand(++commandIdCount, view, bitmapUid, imageSpecs, handler, instructions, true);
      if (view != null)
      {
        prioritiesStack.put(view, preCommand.id);
        prioritiesPreStack.put(view, preCommand);
      }
      preCommand.executeStart(true);
    }
  }

  public synchronized void empty()
  {
    if (log.isInfoEnabled())
    {
      log.info("Clearing the cache '" + name + "'");
    }
    BitmapDownloader.PRE_THREAD_POOL.getQueue().clear();
    BitmapDownloader.DOWNLOAD_THREAD_POOL.getQueue().clear();
    asynchronousDownloadCommands.clear();
    prioritiesStack.clear();
    prioritiesPreStack.clear();
    prioritiesDownloadStack.clear();
    cache.clear();
  }

  protected DownloadBitmapCommand computeDownloadBitmapCommand(int id, View view, String url, String bitmapUid, Object imageSpecs, Handler handler,
      BasisBitmapDownloader.Instructions instructions)
  {
    return new BitmapDownloader.DownloadBitmapCommand(id, view, url, bitmapUid, imageSpecs, handler, instructions);
  }

}
