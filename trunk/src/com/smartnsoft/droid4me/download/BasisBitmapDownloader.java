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

import android.view.View;

import com.smartnsoft.droid4me.download.BasisDownloadInstructions.InputStreamDownloadInstructor;
import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;

/**
 * A first implementation of the {@link CoreBitmapDownloader} class, which is independent from the Android platform.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public class BasisBitmapDownloader<BitmapClass extends Bitmapable, ViewClass extends Viewable, HandlerClass extends Handlerable>
    extends CoreBitmapDownloader<BitmapClass, ViewClass, HandlerClass>
{

  private static int preThreadCount;

  // private final static class ImagePriorityBlockingQueue
  // extends PriorityBlockingQueue<Runnable>
  // {
  // @Override
  // public boolean offer(Runnable runnable)
  // {
  // // final int activePoolSize = BitmapDownloader.PRE_THREAD_POOL.getActiveCount();
  // // if (activePoolSize >= BitmapDownloader.PRE_THREAD_POOL.getCorePoolSize() && activePoolSize <
  // // BitmapDownloader.PRE_THREAD_POOL.getMaximumPoolSize())
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

  // private final static BitmapDownloader.ImagePriorityBlockingQueue PRE_BLOCKING_QUEUE = new BitmapDownloader.ImagePriorityBlockingQueue();

  private final static ThreadPoolExecutor PRE_THREAD_POOL = new ThreadPoolExecutor(2, 3, 5l, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>(), new ThreadFactory()
  {
    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-" + (BasisBitmapDownloader.preThreadCount < BasisBitmapDownloader.PRE_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "pre #" + BasisBitmapDownloader.preThreadCount++);
      return thread;
    }
  }, /*
      * new RejectedExecutionHandler() { public void rejectedExecution(Runnable command, ThreadPoolExecutor executor) {
      * BitmapDownloader.PRE_BLOCKING_QUEUE.add(command, true); } }
      */new ThreadPoolExecutor.AbortPolicy());

  private static int downloadThreadCount;

  @SuppressWarnings("serial")
  private final static ThreadPoolExecutor DOWNLOAD_THREAD_POOL = new ThreadPoolExecutor(2, 4, 5l, TimeUnit.SECONDS, new PriorityBlockingQueue<Runnable>()
  {
    // @Override
    // public boolean offer(Runnable runnable)
    // {
    // final int activePoolSize = BitmapDownloader.DOWNLOAD_THREAD_POOL.getActiveCount();
    // if (activePoolSize >= BitmapDownloader.DOWNLOAD_THREAD_POOL.getCorePoolSize() && activePoolSize <
    // BitmapDownloader.DOWNLOAD_THREAD_POOL.getMaximumPoolSize())
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
      thread.setName("droid4me-" + (BasisBitmapDownloader.downloadThreadCount < BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "download #" + BasisBitmapDownloader.downloadThreadCount++);
      return thread;
    }
  }, new ThreadPoolExecutor.AbortPolicy());

  private static int commandsCount;

  private abstract class BasisCommand
      implements Runnable, Comparable<BasisCommand>
  {

    private final int order = BasisBitmapDownloader.commandsCount++;

    protected final int id;

    protected final ViewClass view;

    protected final String bitmapUid;

    protected final Object imageSpecs;

    protected final HandlerClass handler;

    protected final BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions;

    protected UsedBitmap usedBitmap;

    private boolean executeEnd;

    public BasisCommand(int id, ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
        BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
    {
      this(id, view, bitmapUid, imageSpecs, handler, instructions, false);
    }

    public BasisCommand(int id, ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
        BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions, boolean executeEnd)
    {
      this.id = id;
      this.view = view;
      this.bitmapUid = bitmapUid;
      this.imageSpecs = imageSpecs;
      this.handler = handler;
      this.instructions = instructions;
      this.executeEnd = executeEnd;
    }

    protected abstract void executeStart(boolean isFromGuiThread);

    protected abstract void executeEnd();

    /**
     * Introduced in order to reduce the allocation of a {@link Runnable}, and for an optimization purpose.
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

    public int compareTo(BasisCommand other)
    {
      return other.order - order;
    }

  }

  /**
   * Indicates to the command what it should do eventually.
   * 
   * @since 2011.09.02
   */
  private static enum FinalState
  {
    /**
     * The bitmap is taken from the local resources, and the command is over.
     */
    Local,
    /**
     * The bitmap was already in the memory cache, and the command is over.
     */
    InCache,
    /**
     * The bitmap imageUid is <code>null</code>, there is no temporary bitmap, and the command is over.
     */
    NullUriNoTemporary,
    /**
     * The bitmap imageUid is <code>null</code>, there is a temporary bitmap, and the command is over.
     */
    NullUriTemporary,
    /**
     * The bitmap needs to be downloaded, and a download-command will be triggered.
     */
    NotInCache
  }

  // TODO: define a pool of Command objects, so as to minimize GC, if possible
  private class PreCommand
      extends BasisCommand
  {

    /**
     * This flag will be used when executing the {@link #run} method the second time.
     */
    private BasisBitmapDownloader.FinalState state;

    public PreCommand(int id, ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
        BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
    {
      super(id, view, bitmapUid, imageSpecs, handler, instructions);
    }

    public PreCommand(int id, ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
        BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions, boolean executeEnd)
    {
      super(id, view, bitmapUid, imageSpecs, handler, instructions, executeEnd);
    }

    @Override
    protected void executeStart(boolean isFromGuiThread)
    {
      if (IS_DEBUG_TRACE && log.isDebugEnabled())
      {
        log.debug("Starting to handle the pre-command for the bitmap with identifier '" + bitmapUid + "'");
      }
      // The command is removed from the priority stack
      if (view != null)
      {
        prioritiesPreStack.remove(view);
        dump();
      }

      // We set a local bitmap if available
      if (setLocalBitmapIfPossible(isFromGuiThread) == true)
      {
        return;
      }

      // We compute the bitmap URL and proceed
      final String url = instructions.computeUrl(bitmapUid, imageSpecs);

      // We use a bitmap from the cache if possible
      if (setBitmapFromCacheIfPossible(url, isFromGuiThread) == true)
      {
        return;
      }

      // We set a temporary bitmap, if available
      setTemporaryBitmapIfPossible(isFromGuiThread);

      if (url == null)
      {
        if (view != null)
        {
          // We need to do that in the GUI thread!
          state = state == FinalState.NotInCache ? FinalState.NullUriTemporary : FinalState.NullUriNoTemporary;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to notify the instructions regarding a bitmap with null id from the GUI thread");
              }
            }
          }
          else
          {
            run();
          }
        }
        // We do not want to go any further, since the URL is null, and the work is complete
        return;
      }

      downloadBitmap(url);
    }

    /**
     * The {@link BasisBitmapDownloader.PreCommand#view} is ensured not to be null when this method is invoked.
     */
    @Override
    protected void executeEnd()
    {
      if (IS_DEBUG_TRACE && log.isDebugEnabled())
      {
        log.debug("Running the action in state '" + state + "' for the bitmap with id '" + bitmapUid + "'");
      }
      // We only continue the process (and the temporary or local bitmap view binding) provided there is not already another command bound to be
      // processed for the same view
      Integer commandId = prioritiesStack.get(view);
      if (state != FinalState.NotInCache && (commandId == null || commandId != id))
      {
        if (log.isDebugEnabled())
        {
          log.debug("The bitmap corresponding to the id '" + bitmapUid + "' will not be bound to its view, because this bitmap has asked for another bitmap URL in the meantime");
        }
        return;
      }
      try
      {
        switch (state)
        {
        case Local:
          instructions.onBindLocalBitmap(view, bitmapUid, imageSpecs);
          instructions.onBitmapBound(true, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Set the local drawable for the bitmap with id '" + bitmapUid + "'");
          }
          break;
        case InCache:
          if (instructions.onBindBitmap(false, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs) == false)
          {
            // The binding is performed only if the instructions did not bind it
            // THINK: what can we do?
            // view.setImageBitmap(usedBitmap.getBitmap());
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(view);
          instructions.onBitmapBound(true, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Set the cached bitmap with id '" + bitmapUid + "'");
          }
          break;
        case NullUriNoTemporary:
          instructions.onBitmapBound(false, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Did not set any temporary bitmap for a null bitmap id");
          }
          break;
        case NullUriTemporary:
          instructions.onBindTemporaryBitmap(view, bitmapUid, imageSpecs);
          instructions.onBitmapBound(false, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Set the temporary bitmap for a null bitmap id");
          }
          break;
        case NotInCache:
          instructions.onBindTemporaryBitmap(view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Set the temporary bitmap with id '" + bitmapUid + "'");
          }
          break;
        }
        // We clear the priorities stack if the work is over for that command (i.e. no DownloadBitmapCommand is required)
        commandId = prioritiesStack.get(view);
        if (state != FinalState.NotInCache && commandId == id)
        {
          prioritiesStack.remove(view);
          dump();
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

    private boolean setLocalBitmapIfPossible(boolean isFromGuiThread)
    {
      if (instructions.hasLocalBitmap(bitmapUid, imageSpecs) == true)
      {
        if (view != null)
        {
          // We need to do that in the GUI thread!
          state = FinalState.Local;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that local resource for the bitmap with id '" + bitmapUid + "'");
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

    private void setTemporaryBitmapIfPossible(boolean isFromGuiThread)
    {
      if (instructions.hasTemporaryBitmap(bitmapUid, imageSpecs) == true)
      {
        if (view != null)
        {
          // We need to do that in the GUI thread!
          state = FinalState.NotInCache;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply the temporary for the bitmap with id '" + bitmapUid + "'");
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

    private boolean setBitmapFromCacheIfPossible(String url, boolean isFromGuiThread)
    {
      // There is a special case when the bitmap URL is null
      if (url == null)
      {
        // We do not want to attempt retrieving a cached bitmap corresponding to a null URL
        return false;
      }
      // We check that the bitmap is no already in the cache
      final UsedBitmap otherUsedBitmap = getUsedBitmapFromCache(url);
      if (otherUsedBitmap != null)
      {
        usedBitmap = otherUsedBitmap;
        usedBitmap.rememberAccessed();
        instructions.onBitmapReady(true, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs);
        if (view != null)
        {
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Re-using the cached bitmap for the URL '" + url + "'");
          }
          // It is possible that no bitmap exists for that URL
          // We need to do that in the GUI thread!
          state = FinalState.InCache;
          if (isFromGuiThread == false)
          {
            if (handler.post(this) == false)
            {
              if (log.isWarnEnabled())
              {
                log.warn("Failed to apply that cached bitmap with id '" + bitmapUid + "' relative to the URL '" + url + "'");
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
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("The bitmap with id '" + bitmapUid + "' relative to the URL '" + url + "' is null: no need to apply its cached bitmap");
          }
        }
        return true;
      }
      return false;
    }

    private void downloadBitmap(final String url)
    {
      // We want to remove any pending download command for the view
      if (view != null)
      {
        // But we test whether the download is still required
        final Integer commandId = prioritiesStack.get(view);
        if (commandId == null || commandId != id)
        {
          return;
        }
        // We remove a previously stacked command for the same view
        final PreCommand alreadyStackedCommand = prioritiesDownloadStack.get(view);
        if (alreadyStackedCommand != null)
        {
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("Removing an already stacked download command corresponding to the view with id '" + view.getId() + "'");
          }
          if (BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.remove(alreadyStackedCommand) == false)
          {
            if (log.isErrorEnabled())
            {
              log.error("Could not find the download command relative to the view with id '" + bitmapUid + "' when attempting to remove it!");
            }
          }
        }
      }

      // We need to download the bitmap or take it from a local persistence place
      // Hence, we stack the bitmap download command
      final DownloadBitmapCommand downloadCommand = computeDownloadBitmapCommand(id, view, url, bitmapUid, imageSpecs, handler, instructions);
      if (view != null)
      {
        prioritiesDownloadStack.put(view, downloadCommand);
        dump();
      }
      BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.execute(downloadCommand);
    }

  }

  // TODO: when a second download for the same bitmap UID occurs, do not run it
  protected class DownloadBitmapCommand
      extends PreCommand
      implements InputStreamDownloadInstructor
  {

    protected final String url;

    private boolean downloaded;

    private boolean inputStreamAsynchronous;

    public DownloadBitmapCommand(int id, ViewClass view, String url, String bitmapUid, Object imageSpecs, HandlerClass handler,
        BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
    {
      super(id, view, bitmapUid, imageSpecs, handler, instructions);
      this.url = url;
    }

    @Override
    protected void executeStart(boolean isFromGuiThread)
    {
      if (IS_DEBUG_TRACE && log.isDebugEnabled())
      {
        log.debug("Starting to handle the download command for the bitmap with identifier '" + bitmapUid + "'");
      }
      // The command is removed from the priority stack
      if (view != null)
      {
        prioritiesDownloadStack.remove(view);
        dump();
      }
      // We need to check whether the same URL has not been downloaded in the meantime
      final UsedBitmap otherUsedBitmap = getUsedBitmapFromCache(url);
      if (otherUsedBitmap == null)
      {
        // If the bitmap is not already in memory, we retrieve it
        final BitmapClass bitmap = retrieveBitmap();
        if (bitmap == null)
        {
          // This happens either when the bitmap URL is null, or when the bitmap retrieval failed
          if (inputStreamAsynchronous == false)
          {
            if (url != null && url.length() >= 1)
            {
              if (log.isWarnEnabled())
              {
                log.warn("The bitmap with id '" + bitmapUid + "' relative to the URL '" + url + "' is null");
              }
            }
          }
          // We let intentionally the 'usedBitmap' null
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

      instructions.onBitmapReady(true, view, usedBitmap == null ? null : usedBitmap.getBitmap(), bitmapUid, imageSpecs);
      bindBitmap();
    }

    /**
     * The {@link BasisBitmapDownloader.PreCommand#view} is ensured not to be null when this method is invoked.
     */
    @Override
    protected void executeEnd()
    {
      // We only bind the bitmap to the view provided there is not already another command bound to be processed
      Integer commandId = prioritiesStack.get(view);
      if (commandId == null || commandId != id)
      {
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug("The bitmap corresponding to the URL '" + url + "' will not be bound to its view, because this view has asked for another bitmap URL in the meantime");
        }
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
          dump();
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

      // Indicates whether another command has been set for the view in the meantime
      final boolean forgetTheBinding = view != null && asynchronousDownloadCommands.remove(view) == false;

      final BitmapClass bitmap = fromInputStreamToBitmap(inputStream);
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
     * @return <code>null</code>, in particular, when either if the URL is null or empty
     * @throws IOException
     *           if an IO problem occurred while retrieving the bitmap stream
     */
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
            log.warn("Could not get the provided input stream for the bitmap with id '" + bitmapUid + "' relative to the URL '" + url + "'", exception);
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
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug("Using the provided input stream corresponding to the URL '" + url + "'");
        }
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
        log.debug("The thread '" + Thread.currentThread().getName() + "' downloaded in " + (stop - start) + " ms the bitmap with id '" + bitmapUid + "' relative to the URL '" + url + "'");
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

    /**
     * @return <code>null</code>, in particular, if the underlying bitmap URL is null or empty
     */
    private final BitmapClass retrieveBitmap()
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
          log.warn("Could not access to the bitmap with id '" + bitmapUid + "  ' relative to the URL '" + url + "'");
        }
        return null;
      }
      if (inputStream == null)
      {
        if (inputStreamAsynchronous == false)
        {
          if (url != null && url.length() >= 1)
          {
            if (log.isWarnEnabled())
            {
              log.warn("The input stream used to build the bitmap with id '" + bitmapUid + "' relative to to the URL '" + url + "' is null");
            }
          }
        }
        return null;
      }
      try
      {
        return fromInputStreamToBitmap(inputStream);
      }
      finally
      {
        if (inputStream != null)
        {
          try
          {
            inputStream.close();
          }
          catch (IOException exception)
          {
            // Does not matter
          }
        }
      }
    }

    protected InputStream onInputStreamDownloaded(InputStream inputStream)
    {
      return instructions.onInputStreamDownloaded(bitmapUid, imageSpecs, url, inputStream);
    }

    protected BitmapClass fromInputStreamToBitmap(InputStream inputStream)
    {
      try
      {
        return instructions.convert(inputStream, bitmapUid, imageSpecs);
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot decode the downloaded bitmap because it exceeds the allowed memory", exception);
        }
        cleanUpCache();
        return null;
      }
    }

    private final void bindBitmap()
    {
      // We need to bind the bitmap to the view in the GUI thread!
      if (view != null)
      {
        if (handler.post(this) == false)
        {
          if (log.isWarnEnabled())
          {
            log.warn("Failed to apply the downloaded bitmap for the bitmap with id '" + bitmapUid + "' relative to the URL '" + url + "'");
          }
        }
      }
    }

  }

  /**
   * Enables to express an image specification, which indicates its size and a temporary image resource identification.
   */
  public static class SizedImageSpecs
      extends DownloadSpecs.TemporaryImageSpecs
  {

    public final int width;

    public final int height;

    public SizedImageSpecs(int imageResourceId, int width, int height)
    {
      super(imageResourceId);
      this.width = width;
      this.height = height;
    }

  }

  /**
   * A map which handles the priorities of the {@link BasisBitmapDownloader.PreCommand pre-commands}: when a new command for an {@link View} is asked
   * for, if a {@link BasisBitmapDownloader.PreCommand} is already stacked for the same view (i.e. present in {@link #preStack stacked}), the old one
   * will be discarded.
   */
  private final Map<ViewClass, PreCommand> prioritiesPreStack;

  /**
   * A map which contains all the {@link BasisBitmapDownloader.PreCommand commands} that are currently at the top of the priorities stack. When a new
   * command for an {@link View} is asked for, if a {@link BasisBitmapDownloader.PreCommand} is already stacked for the same view (i.e. present in
   * {@link BasisBitmapDownloader#preStack stacked}), the old one will be discarded.
   */
  private final Map<ViewClass, Integer> prioritiesStack;

  /**
   * A map which remembers the {@link BasisBitmapDownloader.DownloadBitmapCommand download command} which have been registered. This map allows to
   * discard some commands if a new one has been registered for the same {@link View} later on.
   */
  private final Map<ViewClass, DownloadBitmapCommand> prioritiesDownloadStack;

  private final Set<ViewClass> asynchronousDownloadCommands;

  /**
   * The counter of all commands, which is incremented on every new command, so as to identify them.
   */
  private int commandIdCount = -1;

  public BasisBitmapDownloader(String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    super(name, maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
    prioritiesStack = new Hashtable<ViewClass, Integer>();
    prioritiesPreStack = new Hashtable<ViewClass, PreCommand>();
    prioritiesDownloadStack = new Hashtable<ViewClass, DownloadBitmapCommand>();
    asynchronousDownloadCommands = new HashSet<ViewClass>();
  }

  @Override
  public final void get(ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
  {
    if (IS_DEBUG_TRACE && log.isInfoEnabled())
    {
      log.info("Starting a command for the bitmap with id '" + bitmapUid + "' and " + (view != null ? "view with id '" + view.getId() + "'" : "with no view"));
    }
    if (view != null)
    {
      // We indicate to the potential asynchronous input stream downloads that a new request is now set for the bitmap
      asynchronousDownloadCommands.remove(view);

      // We remove a previously stacked command for the same view
      final PreCommand alreadyStackedCommand = prioritiesPreStack.get(view);
      if (alreadyStackedCommand != null)
      {
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug("Removed an already stacked command corresponding to the view with id '" + view.getId() + "'");
        }
        if (BasisBitmapDownloader.PRE_THREAD_POOL.remove(alreadyStackedCommand) == false)
        {
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("The pre-command relative to view with id '" + bitmapUid + "' could not be removed, because it has already been executed!");
          }
        }
      }
    }
    final PreCommand command = new PreCommand(++commandIdCount, view, bitmapUid, imageSpecs, handler, instructions);
    if (view != null)
    {
      prioritiesStack.put(view, command.id);
      prioritiesPreStack.put(view, command);
      dump();
    }
    BasisBitmapDownloader.PRE_THREAD_POOL.execute(command);
  }

  @Override
  public final void get(boolean isBlocking, ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
  {
    if (isBlocking == false)
    {
      get(view, bitmapUid, imageSpecs, handler, instructions);
    }
    else
    {
      final PreCommand preCommand = new PreCommand(++commandIdCount, view, bitmapUid, imageSpecs, handler, instructions, true);
      if (view != null)
      {
        prioritiesStack.put(view, preCommand.id);
        prioritiesPreStack.put(view, preCommand);
        dump();
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
    BasisBitmapDownloader.PRE_THREAD_POOL.getQueue().clear();
    BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.getQueue().clear();
    asynchronousDownloadCommands.clear();
    prioritiesStack.clear();
    prioritiesPreStack.clear();
    prioritiesDownloadStack.clear();
    cache.clear();
    dump();
  }

  protected DownloadBitmapCommand computeDownloadBitmapCommand(int id, ViewClass view, String url, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
  {
    return new DownloadBitmapCommand(id, view, url, bitmapUid, imageSpecs, handler, instructions);
  }

  /**
   * Dumps the statistics about the current state of the instance.
   */
  protected void dump()
  {
    if (IS_DEBUG_TRACE && log.isDebugEnabled())
    {
      log.debug("'" + name + "' statistics: " + "prioritiesStack.size()=" + prioritiesStack.size() + " - " + "prioritiesPreStack.size()=" + prioritiesPreStack.size() + " - " + "prioritiesDownloadStack.size()=" + prioritiesDownloadStack.size() + " - " + "cache.size()=" + cache.size());
    }
  }

}
