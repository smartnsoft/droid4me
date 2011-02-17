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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.widget.ImageView;

/**
 * A first implementation of the {@link BasisImageDownloader} class.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public class ImageDownloader
    extends BasisImageDownloader
{

  /**
   * Introduced so as to be able to catch the exceptions thrown during the execution of the thread.
   * 
   * @since 2010.09.10
   */
  public final static class ImageThreadPoolExecutor
      extends ThreadPoolExecutor
  {

    private ImageThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue,
        ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler)
    {
      super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler);
    }

    /**
     * Makes sure that exceptions will not kill the thread.
     */
    @Override
    public void execute(Runnable command)
    {
      try
      {
        super.execute(command);
      }
      catch (Throwable throwable)
      {
        if (log.isErrorEnabled())
        {
          log.error("An unhandled exception has been raised during the processing of a command", throwable);
        }
      }
    }

  }

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

  private final static ImageDownloader.ImageThreadPoolExecutor PRE_THREAD_POOL = new ImageDownloader.ImageThreadPoolExecutor(2, 3, 5l, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(), new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me.ext-" + (ImageDownloader.preThreadCount < ImageDownloader.PRE_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "pre #" + ImageDownloader.preThreadCount++);
      return thread;
    }
  }, /*
      * new RejectedExecutionHandler() { public void rejectedExecution(Runnable command, ThreadPoolExecutor executor) {
      * ImageDownloader.PRE_BLOCKING_QUEUE.add(command, true); } }
      */new ThreadPoolExecutor.AbortPolicy());

  private static int downloadThreadCount;

  @SuppressWarnings("serial")
  private final static ImageDownloader.ImageThreadPoolExecutor DOWNLOAD_THREAD_POOL = new ImageDownloader.ImageThreadPoolExecutor(2, 4, 5l, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>()
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
      thread.setName("droid4me.ext-" + (ImageDownloader.downloadThreadCount < ImageDownloader.DOWNLOAD_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "download #" + ImageDownloader.downloadThreadCount++);
      return thread;
    }
  }, new ThreadPoolExecutor.AbortPolicy());

  private static int commandsCount;

  // TODO: define a pool of Command objects, so as to minimize GC, if possible
  private class PreCommand
      implements Runnable, Comparable<PreCommand>
  {

    private final int order = ImageDownloader.commandsCount++;

    protected final int id;

    protected final ImageView imageView;

    protected final String imageUid;

    protected final Object imageSpecs;

    protected final Handler handler;

    protected final BasisImageDownloader.Instructions instructions;

    private boolean executeEnd;

    private int state;

    protected BasisImageDownloader.UsedBitmap usedBitmap;

    public PreCommand(int id, ImageView imageView, String imageUid, Object imageSpecs, Handler handler, BasisImageDownloader.Instructions instructions)
    {
      this.id = id;
      this.imageView = imageView;
      this.imageUid = imageUid;
      this.imageSpecs = imageSpecs;
      this.handler = handler;
      this.instructions = instructions;
    }

    public PreCommand(int id, ImageView imageView, String imageUid, Object imageSpecs, Handler handler, BasisImageDownloader.Instructions instructions,
        boolean executeEnd)
    {
      this(id, imageView, imageUid, imageSpecs, handler, instructions);
      this.executeEnd = executeEnd;
    }

    /**
     * Introduced in order to reduce the allocation of a {@link Runnable}, and for an optimization purpose.
     * 
     * <p>
     * The {@link ImageDownloader.PreCommand#imageView} is ensured not to be null when this method is invoked.
     * </p>
     */
    public final void run()
    {
      if (executeEnd == false)
      {
        // if (log.isDebugEnabled())
        // {
        // log.debug("Executing a command start phase in the thread '" + Thread.currentThread().getName() + "' for image with id '" + imageUid + "'");
        // }
        executeEnd = true;
        executeStart(false);
      }
      else
      {
        executeEnd();
      }
    }

    protected void executeEnd()
    {
      // if (log.isDebugEnabled())
      // {
      // log.debug("Running the action in state '" + state + "'");
      // }
      // We only bind the temporary image provided there is not already another command bound to be processed
      Integer commandId = prioritiesStack.get(imageView);
      if (state != 0 && (commandId == null || commandId != id))
      {
        if (log.isDebugEnabled())
        {
          log.debug("The bitmap corresponding to the id '" + imageUid + "' will not be bound to its image, because this image has asked for another bitmap URL in the meantime");
        }
        return;
      }
      try
      {
        switch (state)
        {
        case 0:
          instructions.onBindLocalImage(imageView, imageUid, imageSpecs);
          instructions.onImageBound(true, imageView, imageUid, imageSpecs);
          // if (log.isDebugEnabled())
          // {
          // log.debug("Set the local drawable for the image with id '" + imageUid + "'");
          // }
          break;
        case 1:
          instructions.onBindTemporaryImage(imageView, imageUid, imageSpecs);
          // if (log.isDebugEnabled())
          // {
          // log.debug("Set the temporary drawable for the image with id '" + imageUid + "'");
          // }
          break;
        case 2:
          if (instructions.onBindImage(false, imageView, usedBitmap.getBitmap(), imageUid, imageSpecs) == false)
          {
            imageView.setImageBitmap(usedBitmap.getBitmap());
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(imageView);
          instructions.onImageBound(true, imageView, imageUid, imageSpecs);
          // if (log.isDebugEnabled())
          // {
          // log.debug("Set the cached bitmap for the image with id '" + imageUid + "'");
          // }
          break;
        }
        // We clear the priorities stack if the work is over for that command (i.e. no DownloadBitmapCommand is required)
        commandId = prioritiesStack.get(imageView);
        if (state != 1 && commandId == id)
        {
          prioritiesStack.remove(imageView);
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
      // log.debug("Starting to handle the drawable for the image with identifier '" + imageUid + "'");
      // }
      // The command is removed from the priority stack
      if (imageView != null)
      {
        prioritiesPreStack.remove(imageView);
      }

      // We set a local image if available
      if (setLocalImageIfPossible(isFromGuiThread) == true)
      {
        return;
      }

      // We compute the image URL and proceed to its download if necessary
      final String url = instructions.computeUrl(imageUid, imageSpecs);

      // We use an image from the cache if possible
      if (setImageFromCacheIfPossible(url, isFromGuiThread) == true)
      {
        return;
      }

      // We set a temporary image if available
      setTemporaryImageIfPossible(isFromGuiThread);

      // And we stack the image download command
      if (imageView != null)
      {
        // But we test whether the download is still required
        final Integer commandId = prioritiesStack.get(imageView);
        if (commandId == null || commandId != id)
        {
          return;
        }
        // We remove a previously stacked command for the same image
        final ImageDownloader.PreCommand alreadyStackedCommand = prioritiesDownloadStack.get(imageView);
        if (alreadyStackedCommand != null)
        {
          if (log.isDebugEnabled())
          {
            log.debug("Removed an already stacked download command corresponding to the image with id " + imageView.getId());
          }
          if (ImageDownloader.DOWNLOAD_THREAD_POOL.remove(alreadyStackedCommand) == false)
          {
            if (log.isErrorEnabled())
            {
              log.error("Could not find the download command relative to the image with id '" + imageUid + "' to remove it!");
            }
          }
        }
      }
      final ImageDownloader.DownloadBitmapCommand downloadCommand = computeDownloadBitmapCommand(id, imageView, url, imageUid, imageSpecs, handler,
          instructions);
      if (imageView != null)
      {
        prioritiesDownloadStack.put(imageView, downloadCommand);
      }
      ImageDownloader.DOWNLOAD_THREAD_POOL.execute(downloadCommand);
    }

    private boolean setLocalImageIfPossible(boolean isFromGuiThread)
    {
      if (instructions.hasLocalImage(imageUid, imageSpecs) == true)
      {
        if (imageView != null)
        {
          // We need to do that in the GUI thread!
          state = 0;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that local resource for the image with id '" + imageUid + "' from the GUI thread");
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
      if (instructions.hasTemporaryImage(imageUid, imageSpecs) == true)
      {
        if (imageView != null)
        {
          // We need to do that in the GUI thread!
          state = 1;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that temporary local resource for the image with id '" + imageUid + "' from the GUI thread");
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
      final BasisImageDownloader.UsedBitmap otherUsedBitmap = getUsedBitmapFromCache(url);
      if (otherUsedBitmap != null)
      {
        usedBitmap = otherUsedBitmap;
        usedBitmap.rememberAccessed();
        instructions.onImageReady(true, imageView, usedBitmap.getBitmap(), imageUid, imageSpecs);
        if (imageView != null)
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
      extends ImageDownloader.PreCommand
      implements BasisImageDownloader.InputStreamDownloadInstructor
  {

    protected final String url;

    private boolean downloaded;

    private boolean inputStreamAsynchronous;

    public DownloadBitmapCommand(int id, ImageView imageView, String url, String imageUid, Object imageSpecs, Handler handler,
        BasisImageDownloader.Instructions instructions)
    {
      super(id, imageView, imageUid, imageSpecs, handler, instructions);
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
      final boolean forgetTheBinding = imageView != null && asynchronousDownloadCommands.remove(imageView) == false;

      final Bitmap bitmap = convertInputStream(inputStream);
      if (forgetTheBinding == false && bitmap == null)
      {
        instructions.onImageReady(false, imageView, null, imageUid, imageSpecs);
        return;
      }
      usedBitmap = putInCache(url, bitmap);
      if (forgetTheBinding == false)
      {
        instructions.onImageReady(true, imageView, usedBitmap.getBitmap(), imageUid, imageSpecs);
        bindBitmap();
      }
    }

    /**
     * The {@link ImageDownloader.PreCommand#imageView} is ensured not to be null when this method is invoked.
     */
    @Override
    public final void executeEnd()
    {
      // We only bind the image provided there is not already another command bound to be processed
      Integer commandId = prioritiesStack.get(imageView);
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
          if (instructions.onBindImage(downloaded, imageView, usedBitmap.getBitmap(), imageUid, imageSpecs) == false)
          {
            // The binding is performed only if the instructions did not bind it
            imageView.setImageBitmap(usedBitmap.getBitmap());
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(imageView);
        }
        instructions.onImageBound(usedBitmap != null, imageView, imageUid, imageSpecs);
        // We clear the priorities stack if the work is over for that command (i.e. no DownloadBitmapCommand is required)
        commandId = prioritiesStack.get(imageView);
        if (commandId != null && commandId == id)
        {
          prioritiesStack.remove(imageView);
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
      if (imageView != null)
      {
        prioritiesDownloadStack.remove(imageView);
      }
      // We need to check whether the same URL has not been downloaded in the meantime
      final BasisImageDownloader.UsedBitmap otherUsedBitmap = getUsedBitmapFromCache(url);
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
          instructions.onImageReady(false, imageView, null, imageUid, imageSpecs);
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
        instructions.onImageReady(true, imageView, usedBitmap.getBitmap(), imageUid, imageSpecs);
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
          inputStream = instructions.getInputStream(imageUid, imageSpecs, url, this);
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
        asynchronousDownloadCommands.add(imageView);
        return null;
      }

      final long start = System.currentTimeMillis();
      final URL aURL = new URL(url);
      final URLConnection connection = aURL.openConnection();
      instructions.onBeforeImageDownloaded(imageUid, imageSpecs, connection);
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
      if (imageView != null)
      {
        if (handler.post(this) == false)
        {
          if (log.isWarnEnabled())
          {
            log.warn("Failed to apply the downloaded bitmap for the image with id '" + imageUid + "' and URL '" + url + "' from the GUI thread");
          }
        }
      }
    }

  }

  public static int INSTANCES_COUNT = 1;

  /**
   * The fully qualified name of the {@link ImageDownloader} instances implementation.
   */
  public static String IMPLEMENTATION_FQN = ImageDownloader.class.getName();

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

  private static volatile ImageDownloader[] instances;

  /**
   * Implements a "double-checked locking" pattern.
   */
  public static ImageDownloader getInstance()
  {
    return ImageDownloader.getInstance(0);
  }

  // We accept the "out-of-order writes" case
  @SuppressWarnings("unchecked")
  public static ImageDownloader getInstance(int position)
  {
    if (ImageDownloader.instances == null)
    {
      synchronized (ImageDownloader.class)
      {
        if (ImageDownloader.instances == null)
        {
          try
          {
            final Class<? extends ImageDownloader> implementationClass = (Class<? extends ImageDownloader>) Class.forName(ImageDownloader.IMPLEMENTATION_FQN);
            final ImageDownloader[] newInstances = (ImageDownloader[]) Array.newInstance(implementationClass, ImageDownloader.INSTANCES_COUNT);
            final Constructor<? extends ImageDownloader> constructor = implementationClass.getDeclaredConstructor(long.class, long.class, boolean.class,
                boolean.class);
            for (int index = 0; index < ImageDownloader.INSTANCES_COUNT; index++)
            {
              newInstances[index] = constructor.newInstance(ImageDownloader.MAX_MEMORY_IN_BYTES[index],
                  ImageDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES[index], ImageDownloader.USE_REFERENCES[index], ImageDownloader.RECYCLE_BITMAP[index]);
            }
            // We only assign the instances class variable here, once all instances have actually been created
            ImageDownloader.instances = newInstances;
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
    return ImageDownloader.instances[position];
  }

  /**
   * A map which handles the priorities of the {@link ImageDownloader.PreCommand pre-commands}: when a new command for an {@link ImageView} is asked
   * for, if a {@link ImageDownloader.PreCommand} is already stacked for the same image (i.e. present in {@link #preStack stacked}), the old one will
   * be discarded.
   */
  private final Map<ImageView, ImageDownloader.PreCommand> prioritiesPreStack;

  /**
   * A map which contains all the {@link ImageDownloader.PreCommand commands} that are currently at the top of the priorities stack. When a new
   * command for an {@link ImageView} is asked for, if a {@link ImageDownloader.PreCommand} is already stacked for the same image (i.e. present in
   * {@link ImageDownloader#preStack stacked}), the old one will be discarded.
   */
  private final Map<ImageView, Integer> prioritiesStack;

  private final Map<ImageView, ImageDownloader.DownloadBitmapCommand> prioritiesDownloadStack;

  private final Set<ImageView> asynchronousDownloadCommands = new HashSet<ImageView>();

  /**
   * The counter of all commands, which is incremented on every new command, so as to identify them.
   */
  private int commandIdCount = -1;

  protected ImageDownloader(long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    super(maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
    prioritiesStack = new Hashtable<ImageView, Integer>();
    prioritiesPreStack = new Hashtable<ImageView, ImageDownloader.PreCommand>();
    prioritiesDownloadStack = new Hashtable<ImageView, ImageDownloader.DownloadBitmapCommand>();
  }

  @Override
  public final void get(ImageView imageView, String imageUid, Object imageSpecs, Handler handler, BasisImageDownloader.Instructions instructions)
  {
    // if (log.isDebugEnabled())
    // {
    // log.debug("Asking to handle the bitmap related to the image with id '" + imageUid + "'");
    // }
    // try
    // {
    if (imageView != null)
    {
      // We indicate to the potential asynchronous input stream downloads that a new request is now set for the image
      asynchronousDownloadCommands.remove(imageView);

      // We remove a previously stacked command for the same image
      final ImageDownloader.PreCommand alreadyStackedCommand = prioritiesPreStack.get(imageView);
      if (alreadyStackedCommand != null)
      {
        if (log.isDebugEnabled())
        {
          log.debug("Removed an already stacked command corresponding to the image with id '" + imageView.getId() + "'");
        }
        if (ImageDownloader.PRE_THREAD_POOL.remove(alreadyStackedCommand) == false)
        {
          if (log.isErrorEnabled())
          {
            log.error("Could not find the pre-command relative to the image with id '" + imageUid + "' to remove it!");
          }
        }
      }
    }
    final ImageDownloader.PreCommand command = new ImageDownloader.PreCommand(++commandIdCount, imageView, imageUid, imageSpecs, handler, instructions);
    if (imageView != null)
    {
      prioritiesStack.put(imageView, command.id);
      prioritiesPreStack.put(imageView, command);
    }
    ImageDownloader.PRE_THREAD_POOL.execute(command);
  }

  @Override
  public final void get(boolean isBlocking, ImageView imageView, String imageUid, Object imageSpecs, Handler handler,
      BasisImageDownloader.Instructions instructions)
  {
    if (isBlocking == false)
    {
      get(imageView, imageUid, imageSpecs, handler, instructions);
    }
    else
    {
      final ImageDownloader.PreCommand preCommand = new ImageDownloader.PreCommand(++commandIdCount, imageView, imageUid, imageSpecs, handler, instructions, true);
      if (imageView != null)
      {
        prioritiesStack.put(imageView, preCommand.id);
        prioritiesPreStack.put(imageView, preCommand);
      }
      preCommand.executeStart(true);
    }
  }

  /**
   * @deprecated Use the {@link #get(ImageView, String, Object, Handler, BasisImageDownloader.Instructions)} method instead.
   */
  @Deprecated
  public void computeAndGetUrl(ImageView imageView, String imageUid, Object imageSpecs, Handler handler, BasisImageDownloader.Instructions instructions)
  {
    get(imageView, imageUid, imageSpecs, handler, instructions);
  }

  /**
   * @deprecated Use the {@link #get(boolean, ImageView, String, Object, Handler, BasisImageDownloader.Instructions)} method instead.
   */
  @Deprecated
  public void computeAndGetUrl(boolean isBlocking, ImageView imageView, String imageUid, Object imageSpecs, Handler handler,
      BasisImageDownloader.Instructions instructions)
  {
    get(isBlocking, imageView, imageUid, imageSpecs, handler, instructions);
  }

  public synchronized void empty()
  {
    if (log.isInfoEnabled())
    {
      log.info("Clearing the cache");
    }
    ImageDownloader.PRE_THREAD_POOL.getQueue().clear();
    ImageDownloader.DOWNLOAD_THREAD_POOL.getQueue().clear();
    asynchronousDownloadCommands.clear();
    prioritiesStack.clear();
    prioritiesPreStack.clear();
    prioritiesDownloadStack.clear();
    cache.clear();
  }

  protected DownloadBitmapCommand computeDownloadBitmapCommand(int id, ImageView imageView, String url, String imageUid, Object imageSpecs, Handler handler,
      BasisImageDownloader.Instructions instructions)
  {
    return new ImageDownloader.DownloadBitmapCommand(id, imageView, url, imageUid, imageSpecs, handler, instructions);
  }

}
