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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.view.View;

import com.smartnsoft.droid4me.download.BasisDownloadInstructions.InputStreamDownloadInstructor;
import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;

/**
 * An implementation of the {@link CoreBitmapDownloader} class, which is independent from the Android platform.
 * 
 * @author Édouard Mercier
 * @since 2009.02.19
 */
public class BasisBitmapDownloader<BitmapClass extends Bitmapable, ViewClass extends Viewable, HandlerClass extends Handlerable>
    extends CoreBitmapDownloader<BitmapClass, ViewClass, HandlerClass>
{

  /**
   * The default number of authorized threads available in the "pre" threads pool.
   * 
   * @see #setPreThreadPoolSize(int)
   */
  public final static int PRE_THREAD_POOL_DEFAULT_SIZE = 3;

  /**
   * Enables to tune how many threads at most will be available in the "pre" threads pool.
   * 
   * @param poolSize
   *          the maximum of threads will created for handling incoming commands ; defaults to {@link #PRE_THREAD_POOL_DEFAULT_SIZE}
   */
  public static void setPreThreadPoolSize(int poolSize)
  {
    BasisBitmapDownloader.PRE_THREAD_POOL.setCorePoolSize(poolSize);
  }

  private final static ThreadPoolExecutor PRE_THREAD_POOL = new ThreadPoolExecutor(BasisBitmapDownloader.PRE_THREAD_POOL_DEFAULT_SIZE, BasisBitmapDownloader.PRE_THREAD_POOL_DEFAULT_SIZE, 5l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory()
  {
    private final AtomicInteger threadsCount = new AtomicInteger(0);

    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-" + (threadsCount.get() < BasisBitmapDownloader.PRE_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "pre #" + threadsCount.getAndIncrement());
      return thread;
    }
  }, new ThreadPoolExecutor.AbortPolicy());

  /**
   * The default number of authorized threads available in the "download" threads pool.
   * 
   * @see #setDownloadThreadPoolSize(int)
   */
  public final static int DOWNLOAD_THREAD_POOL_DEFAULT_SIZE = 4;

  /**
   * Enables to tune how many threads at most will be available in the "download" threads pool.
   * 
   * @param poolSize
   *          the maximum of threads will created for handling incoming commands ; defaults to {@link #DOWNLOAD_THREAD_POOL_DEFAULT_SIZE}
   */
  public static void setDownloadThreadPoolSize(int poolSize)
  {
    BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.setCorePoolSize(poolSize);
  }

  private final static ThreadPoolExecutor DOWNLOAD_THREAD_POOL = new ThreadPoolExecutor(BasisBitmapDownloader.DOWNLOAD_THREAD_POOL_DEFAULT_SIZE, BasisBitmapDownloader.DOWNLOAD_THREAD_POOL_DEFAULT_SIZE, 5l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory()
  {

    private final AtomicInteger threadsCount = new AtomicInteger(0);

    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-" + (threadsCount.get() < BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.getCorePoolSize() ? "core-" : "") + "download #" + threadsCount.getAndIncrement());
      return thread;
    }
  }, new ThreadPoolExecutor.AbortPolicy());

  private static int commandsCount = 0;

  private abstract class BasisCommand
      implements Runnable, Comparable<BasisCommand>
  {

    private final int ordinal = BasisBitmapDownloader.commandsCount++;

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

    /**
     * This method will always be executed in the UI thread.
     */
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
          log.error(logCommandId() + "An unhandled exception has been raised during the processing of a command", throwable);
        }
      }
    }

    public final int compareTo(BasisCommand other)
    {
      if (ordinal > other.ordinal)
      {
        return -1;
      }
      else if (ordinal < other.ordinal)
      {
        return 1;
      }
      return 0;
    }

    public final String logCommandId()
    {
      return logCommandIdPrefix() + "C(" + id + ") ";
    }

    protected abstract String logCommandIdPrefix();

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
     * The bitmap imageUid is {@code null}, there is no temporary bitmap, and the command is over.
     */
    NullUriNoTemporary,
    /**
     * The bitmap imageUid is {@code null}, there is a temporary bitmap, and the command is over.
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
        log.debug(logCommandId() + "Starting to handle in thread '" + Thread.currentThread().getName() + "' the pre-command for the bitmap with uid '" + bitmapUid + "'" + (view != null ? " corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
            : "") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
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
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug(logCommandId() + "Some local availability has been detected for the bitmap with uid '" + bitmapUid + "'" + (view != null ? " corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
              : "") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
        }
        return;
      }

      // We compute the bitmap URL and proceed
      final String url = instructions.computeUrl(bitmapUid, imageSpecs);

      // We use a bitmap from the cache if possible
      if (setBitmapFromCacheIfPossible(url, isFromGuiThread) == true)
      {
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug(logCommandId() + "Some caching availability has been detected for the bitmap with uid '" + bitmapUid + "'" + (view != null ? " corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
              : "") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
        }
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
                log.warn(logCommandId() + "Failed to notify the instructions regarding a bitmap with null id from the GUI thread");
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
        log.debug(logCommandId() + "Running the action in state '" + state + "' for the bitmap with uid '" + bitmapUid + "'");
      }
      // We only continue the process (and the temporary or local bitmap view binding) provided there is not already another command bound to be
      // processed for the same view
      final Integer commandId = prioritiesStack.get(view);
      if (state != FinalState.NotInCache && (commandId == null || commandId.intValue() != id))
      {
        instructions.onOver(true, view, bitmapUid, imageSpecs);
        if (log.isDebugEnabled())
        {
          log.debug(logCommandId() + "The bitmap corresponding to the uid '" + bitmapUid + "' will not be bound to its view, because this bitmap has asked for another bitmap URL in the meantime");
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
          instructions.onOver(false, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Set the local drawable for the bitmap with uid '" + bitmapUid + "'");
          }
          break;
        case InCache:
          if (instructions.onBindBitmap(false, view, usedBitmap.getBitmap(), bitmapUid, imageSpecs) == false)
          {
            // The binding is performed only if the instructions did not bind it
            // THINK: what can we do?
            // view.setImageBitmap(usedBitmap.getBitmap());
          }
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Binded the bitmap with uid '" + bitmapUid + "' to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")") + (imageSpecs == null ? ""
                : (" and with specs '" + imageSpecs.toString() + "'")));
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(view);
          instructions.onBitmapBound(true, view, bitmapUid, imageSpecs);
          instructions.onOver(false, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Used the cached bitmap with uid '" + bitmapUid + "'");
          }
          break;
        case NullUriNoTemporary:
          instructions.onBitmapBound(false, view, bitmapUid, imageSpecs);
          instructions.onOver(false, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Did not set any temporary bitmap for a null bitmap id");
          }
          break;
        case NullUriTemporary:
          instructions.onBindTemporaryBitmap(view, bitmapUid, imageSpecs);
          instructions.onBitmapBound(false, view, bitmapUid, imageSpecs);
          instructions.onOver(false, view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Set the temporary bitmap for a null bitmap URL");
          }
          break;
        case NotInCache:
          instructions.onBindTemporaryBitmap(view, bitmapUid, imageSpecs);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Set the temporary bitmap with uid '" + bitmapUid + "'");
          }
          break;
        }
        // We clear the priorities stack if the work is over for that command (i.e. no DownloadBitmapCommand is required)
        if (state != FinalState.NotInCache)
        {
          prioritiesStack.remove(view);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Removed from the priority stack the view" + (view != null ? " " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
                : ""));
          }
          dump();
        }
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn(logCommandId() + "Process exceeding available memory", exception);
        }
        cleanUpCache();
      }
    }

    /**
     * @return {@code true} if and only if a local bitmap should be used
     */
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
                log.warn(logCommandId() + "Failed to apply that local resource for the bitmap with uid '" + bitmapUid + "'");
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
                log.warn(logCommandId() + "Failed to apply the temporary for the bitmap with uid '" + bitmapUid + "'");
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
      // We check that the bitmap is not already in the cache
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
            log.debug(logCommandId() + "Re-using the cached bitmap for the URL '" + url + "'");
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
                log.warn(logCommandId() + "Failed to apply that cached bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "'");
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
            log.debug(logCommandId() + "The bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "' is null: no need to apply its cached bitmap");
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
        if (commandId == null || commandId.intValue() != id)
        {
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Because another command has been stacked for the same view in the meantime, aborting the command corresponding to the view with uid '" + bitmapUid + "'" + (view != null ? " corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
                : "") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
          }
          instructions.onOver(true, view, bitmapUid, imageSpecs);
          return;
        }
        // We remove a previously stacked command for the same view
        final DownloadBitmapCommand alreadyStackedCommand = prioritiesDownloadStack.get(view);
        if (alreadyStackedCommand != null)
        {
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Removing an already stacked download command corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")") + (imageSpecs == null ? ""
                : (" and with specs '" + imageSpecs.toString() + "'")));
          }
          if (BasisBitmapDownloader.DOWNLOAD_THREAD_POOL.remove(alreadyStackedCommand) == false)
          {
            // This may happen if the download command has already started
          }
          else
          {
            instructions.onOver(true, alreadyStackedCommand.view, alreadyStackedCommand.bitmapUid, alreadyStackedCommand.imageSpecs);
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

    @Override
    protected String logCommandIdPrefix()
    {
      return "P";
    }

  }

  protected class DownloadBitmapCommand
      extends PreCommand
      implements InputStreamDownloadInstructor
  {

    protected final String url;

    private boolean downloaded;

    private boolean inputStreamAsynchronous;

    /**
     * When not negative, the timestamp corresponding to the time when the underlying bitmap started to be downloaded.
     */
    private long downloadStartTimestamp = -1;

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
        log.debug(logCommandId() + "Starting to handle in thread '" + Thread.currentThread().getName() + "' the download command for the bitmap with uid '" + bitmapUid + "'" + (view != null ? " corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
            : "") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
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
        // But, before that, we check whether the same bitmap would not be currently downloading
        final BitmapClass bitmap;
        final DownloadingBitmap downloadingBitmap;
        synchronized (inProgressDownloads)
        {
          downloadingBitmap = inProgressDownloads.get(url);
        }
        if (downloadingBitmap != null)
        {
          downloadingBitmap.referencesCount++;
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Waiting for the bitmap corresponding to the URL '" + url + "' to be downloaded" + (view != null ? " regarding the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
                : "") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
          }
          synchronized (downloadingBitmap)
          {
            bitmap = downloadingBitmap.bitmap;
            downloadingBitmap.referencesCount--;
            if (downloadingBitmap.referencesCount <= 0)
            {
              inProgressDownloads.remove(url);
            }
          }
        }
        else
        {
          final DownloadingBitmap newDownloadingBitmap;
          synchronized (inProgressDownloads)
          {
            newDownloadingBitmap = new DownloadingBitmap();
            inProgressDownloads.put(url, newDownloadingBitmap);
          }
          synchronized (newDownloadingBitmap)
          {
            BitmapClass retrievedBitmap = null;
            try
            {
              retrievedBitmap = retrieveBitmap();
            }
            catch (Throwable throwable)
            {
              // We want to make sure that the process resumes if a problem occurred during the retrieval of the bitmap
              if (log.isWarnEnabled())
              {
                log.warn(
                    logCommandId() + "An unattended problem occured while retrieving the bitmap with uid '" + bitmapUid + "' corresponding to the URL '" + url + "'",
                    throwable);
              }
            }
            bitmap = retrievedBitmap;
            // A minor optimization
            if (newDownloadingBitmap.referencesCount <= 0)
            {
              inProgressDownloads.remove(url);
            }
            else
            {
              newDownloadingBitmap.bitmap = bitmap;
            }
            if (inputStreamAsynchronous == true)
            {
              return;
            }
          }
        }

        if (bitmap == null)
        {
          // This happens either when the bitmap URL is null, or when the bitmap retrieval failed
          if (inputStreamAsynchronous == false)
          {
            if (url != null && url.length() >= 1)
            {
              if (log.isWarnEnabled())
              {
                log.warn(logCommandId() + "The bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "' is null");
              }
            }
          }
          // We let intentionally the 'usedBitmap' null
        }
        else
        {
          // TODO: invoke the 'putInCache()' only once when the same URL is asked for multiple times
          usedBitmap = putInCache(url, bitmap);
        }
      }
      else
      {
        // Otherwise, we reuse it
        usedBitmap = otherUsedBitmap;
        usedBitmap.rememberAccessed();
      }

      instructions.onBitmapReady(usedBitmap != null && usedBitmap.getBitmap() != null, view, usedBitmap == null ? null : usedBitmap.getBitmap(), bitmapUid,
          imageSpecs);
      bindBitmap();
    }

    /**
     * The {@link BasisBitmapDownloader.PreCommand#view} is ensured not to be null when this method is invoked.
     */
    @Override
    protected void executeEnd()
    {
      // We only bind the bitmap to the view provided there is not already another command bound to be processed
      final Integer commandId = prioritiesStack.get(view);
      if (commandId == null || commandId.intValue() != id)
      {
        if (IS_DEBUG_TRACE && log.isDebugEnabled())
        {
          log.debug(logCommandId() + "The bitmap corresponding to the URL '" + url + "' will not be bound to its view" + (view != null ? " " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
              : "") + ", because its related view has been requested again in the meantime");
        }
        instructions.onOver(true, view, bitmapUid, imageSpecs);
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
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Binded the bitmap with uid '" + bitmapUid + "' to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")") + (imageSpecs == null ? ""
                : (" and with specs '" + imageSpecs.toString() + "'")));
          }
          usedBitmap.forgetBitmap();
          usedBitmap.rememberBinding(view);
        }
        instructions.onBitmapBound(usedBitmap != null, view, bitmapUid, imageSpecs);
        instructions.onOver(false, view, bitmapUid, imageSpecs);
        // We clear the priorities stack because the work is over for that command
        {
          prioritiesStack.remove(view);
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug(logCommandId() + "Removed from the priority stack the view" + (view != null ? " " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")")
                : ""));
          }
          dump();
        }
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn(logCommandId() + "Process exceeding available memory", exception);
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
          log.warn(logCommandId() + "Process exceeding available memory", exception);
        }
        cleanUpCache();
        return;
      }

      // We first check for the input stream
      if (inputStream == null)
      {
        instructions.onBitmapReady(false, view, null, bitmapUid, imageSpecs);
        return;
      }

      // We attempt to convert the input stream into a bitmap
      final BitmapClass bitmap = fromInputStreamToBitmap(inputStream);
      if (bitmap == null)
      {
        instructions.onBitmapReady(false, view, null, bitmapUid, imageSpecs);
        return;
      }

      // We put in cache the bitmap
      usedBitmap = putInCache(url, bitmap);

      instructions.onBitmapReady(true, view, bitmap, bitmapUid, imageSpecs);

      // Indicates whether another command has been set for the view in the meantime
      if (view != null && asynchronousDownloadCommands.remove(view) == true)
      {
        bindBitmap();
      }
    }

    /**
     * Is responsible for getting the bitmap, either from the local persisted cache, and if not available from Internet.
     * 
     * @return {@code null}, in particular, if the underlying bitmap URL is null or empty
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
          log.warn(logCommandId() + "Could not access to the bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "'");
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
              log.warn(logCommandId() + "The input stream used to build the bitmap with uid '" + bitmapUid + "' relative to to the URL '" + url + "' is null");
            }
          }
        }
        return null;
      }
      try
      {
        final BitmapClass bitmap = fromInputStreamToBitmap(inputStream);
        if (downloadStartTimestamp >= 0)
        {
          final long stop = System.currentTimeMillis();
          if (log.isDebugEnabled())
          {
            log.debug(logCommandId() + "The thread '" + Thread.currentThread().getName() + "' downloaded and transformed in " + (stop - downloadStartTimestamp) + " ms the bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "'");
          }
        }
        return bitmap;
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

    /**
     * This method assumes that the provided <code>inputStream</code> is not {@code null}.
     * 
     * @param inputStream
     *          an input stream corresponding to a bitmap
     * @return {@code null} if the input stream could not be properly converted ; a valid bitmap otherwise
     */
    protected BitmapClass fromInputStreamToBitmap(InputStream inputStream)
    {
      try
      {
        final BitmapClass bitmap = instructions.convert(inputStream, bitmapUid, imageSpecs);
        if (bitmap == null)
        {
          if (log.isWarnEnabled())
          {
            log.warn(logCommandId() + "Could not turn properly into a valid bitmap the input stream corresponding to the bitmap with uid '" + bitmapUid + "', related to the URL '" + url + "'");
          }
        }
        return bitmap;
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn(logCommandId() + "Cannot decode the downloaded bitmap because it exceeds the allowed memory", exception);
        }
        cleanUpCache();
        return null;
      }
    }

    /**
     * @return {@code null}, in particular, when either if the URL is null or empty
     * @throws IOException
     *           if an IO problem occurred while retrieving the bitmap stream
     */
    private final InputStream fetchInputStream()
        throws IOException
    {
      {
        final InputStream inputStream;
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
              log.warn(
                  logCommandId() + "Could not get the provided input stream for the bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "'",
                  exception);
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
            log.debug(logCommandId() + "Using the provided input stream corresponding to the URL '" + url + "'");
          }
          return inputStream;
        }
      }

      // We determine whether the input stream has turned asynchronous
      if (inputStreamAsynchronous == true)
      {
        asynchronousDownloadCommands.add(view);
        return null;
      }

      try
      {
        return downloadInputStream();
      }
      catch (OutOfMemoryError exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn(logCommandId() + "Process exceeding available memory", exception);
        }
        cleanUpCache();
        return null;
      }
    }

    /**
     * Is responsible for actually downloading the input stream corresponding to the bitmap.
     * 
     * @return the input stream corresponding to the bitmap ; should not return {@code null} but throw an exception instead when the input stream
     *         cannot be downloaded in case of download attempt error, and {@code null} if no Internet connectivity is available
     * @throws IOException
     *           if a problem occurred during the download
     */
    private InputStream downloadInputStream()
        throws IOException
    {
      downloadStartTimestamp = System.currentTimeMillis();
      if (isConnected() == false)
      {
        return null;
      }
      final InputStream inputStream = instructions.downloadInputStream(bitmapUid, imageSpecs, url);
      final InputStream downloadedInputStream = onInputStreamDownloaded(inputStream);
      downloaded = true;
      return downloadedInputStream;
    }

    /**
     * The method will do nothing if the {@link #view} is {@code null}.
     */
    private final void bindBitmap()
    {
      // We need to bind the bitmap to the view in the GUI thread!
      if (view != null)
      {
        if (handler.post(this) == false)
        {
          if (log.isWarnEnabled())
          {
            log.warn(logCommandId() + "Failed to apply the downloaded bitmap for the bitmap with uid '" + bitmapUid + "' relative to the URL '" + url + "'");
          }
        }
      }
    }

    @Override
    protected String logCommandIdPrefix()
    {
      return "D";
    }

  }

  private final class DownloadingBitmap
  {

    private BitmapClass bitmap;

    private int referencesCount;

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

  /**
   * Contains the URL of the bitmap being currently downloaded. The key is the URL, the value is a {@link DownloadingBitmap} used for the
   * synchronization.
   */
  private Map<String, DownloadingBitmap> inProgressDownloads;

  private final Set<ViewClass> asynchronousDownloadCommands;

  /**
   * The counter of all commands, which is incremented on every new command, so as to identify them.
   */
  private int commandIdCount = -1;

  public BasisBitmapDownloader(String name, long maxMemoryInBytes, long lowLevelMemoryWaterMarkInBytes, boolean useReferences, boolean recycleMap)
  {
    super(name, maxMemoryInBytes, lowLevelMemoryWaterMarkInBytes, useReferences, recycleMap);
    prioritiesStack = new Hashtable<ViewClass, Integer>();// Collections.synchronizedMap(new IdentityHashMap<ViewClass, Integer>());
    prioritiesPreStack = new Hashtable<ViewClass, PreCommand>();// Collections.synchronizedMap(new IdentityHashMap<ViewClass, PreCommand>());
    prioritiesDownloadStack = new Hashtable<ViewClass, DownloadBitmapCommand>();// Collections.synchronizedMap(new IdentityHashMap<ViewClass,
    // DownloadBitmapCommand>());
    inProgressDownloads = new Hashtable<String, DownloadingBitmap>();
    asynchronousDownloadCommands = new HashSet<ViewClass>();
  }

  @Override
  public final void get(ViewClass view, String bitmapUid, Object imageSpecs, HandlerClass handler,
      BasisDownloadInstructions.Instructions<BitmapClass, ViewClass> instructions)
  {
    if (IS_DEBUG_TRACE && log.isInfoEnabled())
    {
      log.info("Starting asynchronously a command for the bitmap with uid '" + bitmapUid + "' and " + (view != null ? ("view (id='" + view.getId() + "',hash=" + view.hashCode() + ")")
          : "with no view") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
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
          log.debug("Removed an already stacked command corresponding to the view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")") + (alreadyStackedCommand.imageSpecs == null ? ""
              : (" and with specs '" + alreadyStackedCommand.imageSpecs.toString() + "'")));
        }
        if (BasisBitmapDownloader.PRE_THREAD_POOL.remove(alreadyStackedCommand) == false)
        {
          if (IS_DEBUG_TRACE && log.isDebugEnabled())
          {
            log.debug("The pre-command relative to view " + ("(id='" + view.getId() + "',hash=" + view.hashCode() + ")") + (alreadyStackedCommand.imageSpecs == null ? ""
                : (" and with specs '" + alreadyStackedCommand.imageSpecs.toString() + "'")) + " could not be removed, because its execution has already started!");
          }
        }
        else
        {
          // In that case, the command is aborted over
          instructions.onOver(true, alreadyStackedCommand.view, alreadyStackedCommand.bitmapUid, alreadyStackedCommand.imageSpecs);
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
    if (IS_DEBUG_TRACE && log.isInfoEnabled())
    {
      log.info("Pre-command with id " + command.id + " registered regarding the bitmap with uid '" + bitmapUid + "' and " + (view != null ? ("view (id='" + view.getId() + "',hash=" + view.hashCode() + ")")
          : "with no view") + (imageSpecs == null ? "" : (" and with specs '" + imageSpecs.toString() + "'")));
    }
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

  public synchronized void clear()
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
    inProgressDownloads.clear();
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
    if (IS_DUMP_TRACE && IS_DEBUG_TRACE && log.isDebugEnabled())
    {
      log.debug("'" + name + "' statistics: " + "prioritiesStack.size()=" + prioritiesStack.size() + " - " + "prioritiesPreStack.size()=" + prioritiesPreStack.size() + " - " + "prioritiesDownloadStack.size()=" + prioritiesDownloadStack.size() + " - " + "inProgressDownloads.size()=" + inProgressDownloads.size() + " - " + "cache.size()=" + cache.size());
    }
  }

}
