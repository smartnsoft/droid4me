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

package com.smartnsoft.droid4me.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.ServiceLifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;

/**
 * Gathers some internal interfaces and helpers for the types belonging to its Java package.
 * 
 * @author Édouard Mercier
 * @since 2010.01.05
 */
final class AppInternals
{

  /**
   * Defines several contracts for all {@link Activity activities} of the framework.
   * 
   * @since 2010.01.05
   */
  interface LifeCycleInternals
  {

    /**
     * Indicates whether nothing went wrong during the implementing {@link Activity activity}, or if a {@link #isBeingRedirected() redirection} is
     * being processed.
     * 
     * <p>
     * Thanks to the {@link ActivityController.Redirector redirection controller}, it is possible to re-route any {@link SmartableActivity activity}
     * when it starts, if some other activity needs to be executed beforehand.
     * </p>
     * 
     * @return {@code true} if and only if the implementing {@link Activity} should resume its execution
     */
    boolean shouldKeepOn();

    /**
     * This callback method will be invoked when the activity is being created, during the {@link Activity#onCreate(Bundle)} method execution, but not
     * when it is recreated due to a configuration change.
     */
    void onActuallyCreated();

    /**
     * This callback method will be invoked when the activity is being destroyed, during the {@link Activity#onDestroy()} method execution, but not
     * when it is destroyed due to a configuration change.
     */
    void onActuallyDestroyed();

  }

  /**
   * There for gathering all instance variables, and in order to make copy and paste smarter.
   * 
   * @since 2009.02.16
   */
  final static class StateContainer<AggregateClass>
  {

    private final static class RefreshBusinessObjectsAndDisplay
    {

      private final boolean retrieveBusinessObjects;

      private final Runnable onOver;

      private RefreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver)
      {
        this.retrieveBusinessObjects = retrieveBusinessObjects;
        this.onOver = onOver;
      }

      private void refreshBusinessObjectsAndDisplay(LifeCycle lifeCycleActivity)
      {
        lifeCycleActivity.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, true);
      }

    }

    private static final Logger log = LoggerFactory.getInstance(StateContainer.class);

    boolean resumedForTheFirstTime = true;

    MenuHandler.Composite compositeActionHandler;

    CompositeHandler compositeActivityResultHandler;

    Handler handler;

    AggregateClass aggregate;

    private SharedPreferences preferences;

    boolean actionResultsRetrieved;

    private boolean businessObjectsRetrieved;

    boolean doNotCallOnActivityDestroyed;

    boolean firstLifeCycle = true;

    /**
     * A flag which indicates whether the underlying {@link LifeCycle} is executing a
     * {@link LifeCycle#refreshBusinessObjectsAndDisplay(boolean, Runnable)} call.
     */
    private boolean refreshingBusinessObjectsAndDisplay;

    private boolean beingRedirected;

    private BroadcastReceiver[] broadcastReceivers;

    private int onSynchronizeDisplayObjectsCount = -1;

    private boolean stopHandling;

    /**
     * A flag which states whether the underlying {@link Activity} life-cycle is between the {@link Activity#onResume()} and
     * {@link Activity#onPause()}.
     */
    private boolean isInteracting;

    private AppInternals.StateContainer.RefreshBusinessObjectsAndDisplay refreshBusinessObjectsAndDisplayNextTime;

    private AppInternals.StateContainer.RefreshBusinessObjectsAndDisplay refreshBusinessObjectsAndDisplayPending;

    boolean isFirstLifeCycle()
    {
      return firstLifeCycle;
    }

    synchronized SharedPreferences getPreferences(Context applicationContext)
    {
      if (preferences == null)
      {
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
      }
      return preferences;
    }

    void create(Context context)
    {
      compositeActionHandler = new MenuHandler.Composite();
      compositeActionHandler.add(createStaticActionHandler(context));
      compositeActivityResultHandler = new ActivityResultHandler.CompositeHandler();
      handler = new Handler();
    }

    MenuHandler.Static createStaticActionHandler(final Context context)
    {
      final MenuHandler.Static staticActionHandler = new MenuHandler.Static()
      {
        @Override
        protected List<MenuCommand<Void>> retrieveCommands()
        {
          final List<MenuCommand<Void>> commands = new ArrayList<MenuCommand<Void>>();
          return commands;
        }
      };
      return staticActionHandler;
    }

    /**
     * If the activity implements either the {@link AppPublics.BroadcastListener} or {@link AppPublics.BroadcastListenerProvider} or
     * {@link AppPublics.BroadcastListenersProvider} interface, it will register for listening to some broadcast intents.
     */
    void registerBroadcastListeners(Activity activity)
    {
      final AppPublics.BroadcastListener broadcastListener;
      if (activity instanceof AppPublics.BroadcastListenerProvider)
      {
        broadcastListener = ((AppPublics.BroadcastListenerProvider) activity).getBroadcastListener();
        if (broadcastListener != null)
        {
          registerBroadcastListeners(activity, enrichBroadCastListeners(1), broadcastListener);
        }
      }
      else if (activity instanceof AppPublics.BroadcastListener)
      {
        broadcastListener = (AppPublics.BroadcastListener) activity;
        if (broadcastListener != null)
        {
          registerBroadcastListeners(activity, enrichBroadCastListeners(1), broadcastListener);
        }
      }
      else if (activity instanceof AppPublics.BroadcastListenersProvider)
      {
        final AppPublics.BroadcastListenersProvider broadcastListenersProvider = (AppPublics.BroadcastListenersProvider) activity;
        final int count = broadcastListenersProvider.getBroadcastListenersCount();
        if (log.isDebugEnabled())
        {
          log.debug("Found out that the activity supports " + count + " intent broadcast listeners");
        }
        final int startIndex = enrichBroadCastListeners(count);
        for (int index = 0; index < count; index++)
        {
          registerBroadcastListeners(activity, startIndex + index, broadcastListenersProvider.getBroadcastListener(index));
        }
      }
    }

    void registerBroadcastListeners(Activity activity, AppPublics.BroadcastListener[] broadcastListeners)
    {
      final int startIndex = enrichBroadCastListeners(broadcastListeners.length);
      for (int index = 0; index < broadcastListeners.length; index++)
      {
        registerBroadcastListeners(activity, index + startIndex, broadcastListeners[index]);
      }
    }

    private void registerBroadcastListeners(Activity activity, int index, final AppPublics.BroadcastListener broadcastListener)
    {
      if (index == 0 && log.isDebugEnabled())
      {
        log.debug("Registering for listening to intent broadcasts");
      }
      broadcastReceivers[index] = new BroadcastReceiver()
      {
        public void onReceive(Context context, Intent intent)
        {
          try
          {
            broadcastListener.onReceive(intent);
          }
          catch (Throwable throwable)
          {
            if (log.isErrorEnabled())
            {
              log.error("An exception occurred while handling a broadcast intent!", throwable);
            }
          }
        }
      };
      IntentFilter intentFilter = null;
      try
      {
        intentFilter = broadcastListener.getIntentFilter();
      }
      catch (Throwable throwable)
      {
        if (log.isErrorEnabled())
        {
          log.error("An exception occurred while computing the intent filter!", throwable);
        }
      }
      activity.registerReceiver(broadcastReceivers[index], intentFilter == null ? new IntentFilter() : intentFilter);
    }

    private int enrichBroadCastListeners(int count)
    {
      final int newIndex;
      if (broadcastReceivers == null)
      {
        newIndex = 0;
        broadcastReceivers = new BroadcastReceiver[count];
      }
      else
      {
        newIndex = broadcastReceivers.length;
        final BroadcastReceiver[] newBroadcastReceivers = new BroadcastReceiver[count + broadcastReceivers.length];
        // Only available from Android v9, a.k.a. GingerBread
        // broadcastReceivers = Arrays.copyOf(broadcastReceivers, count + broadcastReceivers.length);
        for (int index = 0; index < broadcastReceivers.length; index++)
        {
          newBroadcastReceivers[index] = broadcastReceivers[index];
        }
        broadcastReceivers = newBroadcastReceivers;
      }
      return newIndex;
    }

    /**
     * If the activity implements the {@link AppPublics.BroadcastListener} interface, this will make it stop listening to broadcasted intents.
     */
    void unregisterBroadcastListeners(Activity activity)
    {
      if (broadcastReceivers != null)
      {
        for (int index = broadcastReceivers.length - 1; index >= 0; index--)
        {
          if (broadcastReceivers[index] != null)
          {
            activity.unregisterReceiver(broadcastReceivers[index]);
          }
        }
        if (log.isDebugEnabled())
        {
          log.debug("Stopped listening to intent broadcasts");
        }
      }
    }

    /**
     * Should be invoked during the {@link SmartActivity.onBeforeRefreshBusinessObjectsAndDisplay} method: is responsible for triggering a loading
     * broadcast intent if required, in order to indicate that the activity is loading.
     */
    void onStartLoading(Activity activity)
    {
      if (activity instanceof AppPublics.SendLoadingIntent)
      {
        // We indicate the activity which is loading, in order to filter the loading events
        AppPublics.LoadingBroadcastListener.broadcastLoading(activity, activity.getClass(), true, false);
      }
    }

    /**
     * Should be invoked just after the {@link SmartActivity.onSynchronizeDisplayObjects} method: is responsible for triggering a loading broadcast
     * intent if required, in order to indicate that the activity has stopped loading.
     */
    void onStopLoading(Activity activity)
    {
      if (activity instanceof AppPublics.SendLoadingIntent)
      {
        // We indicate the activity which is loading, in order to filter the loading events
        AppPublics.LoadingBroadcastListener.broadcastLoading(activity, activity.getClass(), false, false);
      }
    }

    /**
     * Invoked when the provided activity enters the {@link Activity#onStart()} method. We check whether to invoke the {@link ServiceLifeCycle}
     * methods.
     */
    void onStart(final Activity activity)
    {
      if (activity instanceof ServiceLifeCycle)
      {
        final ServiceLifeCycle forServices = (ServiceLifeCycle) activity;
        if (areServicesAsynchronous(activity) == false)
        {
          internalPrepareServices(activity, forServices);
        }
        else
        {
          AppInternals.THREAD_POOL.submit(new Runnable()
          {
            public void run()
            {
              internalPrepareServices(activity, forServices);
            }
          });
        }
      }
    }

    /**
     * Invoked when the provided activity enters the {@link Activity#onResume()} method.
     */
    void onResume(Activity activity)
    {
      isInteracting = true;
    }

    void setBusinessObjectsRetrieved()
    {
      businessObjectsRetrieved = true;
    }

    boolean isRetrieveBusinessObjects()
    {
      return businessObjectsRetrieved == false || (refreshBusinessObjectsAndDisplayNextTime != null && refreshBusinessObjectsAndDisplayNextTime.retrieveBusinessObjects == true);
    }

    Runnable getRetrieveBusinessObjectsOver()
    {
      return refreshBusinessObjectsAndDisplayNextTime == null ? null : refreshBusinessObjectsAndDisplayNextTime.onOver;
    }

    void onRefreshingBusinessObjectsAndDisplayStart()
    {
      refreshingBusinessObjectsAndDisplay = true;
    }

    synchronized void onRefreshingBusinessObjectsAndDisplayStop(LifeCycle lifeCycleActivity, Activity activity)
    {
      refreshingBusinessObjectsAndDisplay = false;
      if (activity.isFinishing() == true)
      {
        return;
      }
      if (refreshBusinessObjectsAndDisplayPending != null)
      {
        if (log.isDebugEnabled())
        {
          log.debug("The stacked refresh of the business objects and display is stacked can now be executed");
        }
        refreshBusinessObjectsAndDisplayPending.refreshBusinessObjectsAndDisplay(lifeCycleActivity);
        refreshBusinessObjectsAndDisplayPending = null;
      }
    }

    boolean isRefreshingBusinessObjectsAndDisplay()
    {
      return refreshingBusinessObjectsAndDisplay;
    }

    /**
     * Invoked when the provided activity enters the {@link Activity#onPause()} method.
     */
    void onPause(Activity activity)
    {
      isInteracting = false;
    }

    /**
     * Invoked when the provided activity enters the {@link Activity#onStop()} method. We check whether to invoke the {@link ServiceLifeCycle}
     * methods.
     */
    void onStop(final Activity activity)
    {
      if (activity instanceof ServiceLifeCycle)
      {
        final ServiceLifeCycle forServices = (ServiceLifeCycle) activity;
        if (areServicesAsynchronous(activity) == false)
        {
          internalDisposeServices(activity, forServices);
        }
        else
        {
          AppInternals.THREAD_POOL.submit(new Runnable()
          {
            public void run()
            {
              internalDisposeServices(activity, forServices);
            }
          });
        }
      }
    }

    void onSynchronizeDisplayObjects()
    {
      onSynchronizeDisplayObjectsCount++;
    }

    int getOnSynchronizeDisplayObjectsCount()
    {
      return onSynchronizeDisplayObjectsCount + 1;
    }

    void beingRedirected()
    {
      beingRedirected = true;
    }

    /**
     * Should be invoked as soon as the activity has stopped working and that no more handling should be done.
     */
    void stopHandling()
    {
      stopHandling = true;
    }

    boolean shouldKeepOn()
    {
      return stopHandling == false && beingRedirected == false;
    }

    synchronized boolean shouldDelayRefreshBusinessObjectsAndDisplay(Activity activity, boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
    {
      // If the Activity is finishing, we give up
      if (activity.isFinishing() == true)
      {
        return true;
      }
      // We test whether the Activity is active (its life-cycle state is between 'onResume()' and 'onPause()'
      if (isInteracting == false && immediately == false)
      {
        refreshBusinessObjectsAndDisplayNextTime = new AppInternals.StateContainer.RefreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver);
        if (log.isDebugEnabled())
        {
          log.debug("The refresh of the business objects and display is delayed because the Activity is not interacting");
        }
        return true;
      }
      // We test whether the Activity is already being refreshed
      if (refreshingBusinessObjectsAndDisplay == true)
      {
        // In that case, we need to wait for the refresh action to be over
        if (log.isDebugEnabled())
        {
          log.debug("The refresh of the business objects and display is stacked because it is already refreshing");
        }
        refreshBusinessObjectsAndDisplayPending = new AppInternals.StateContainer.RefreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver);
        return true;
      }
      refreshBusinessObjectsAndDisplayNextTime = null;
      return false;
    }

    private boolean areServicesAsynchronous(Activity activity)
    {
      return activity instanceof ServiceLifeCycle.ForServicesAsynchronousPolicy;
    }

    private void internalPrepareServices(Activity activity, ServiceLifeCycle forServices)
    {
      try
      {
        forServices.prepareServices();
      }
      catch (ServiceLifeCycle.ServiceException exception)
      {
        onInternalServiceException(activity, forServices, exception);
      }
    }

    private void internalDisposeServices(Activity activity, ServiceLifeCycle forServices)
    {
      try
      {
        forServices.disposeServices();
      }
      catch (ServiceLifeCycle.ServiceException exception)
      {
        onInternalServiceException(activity, forServices, exception);
      }
    }

    private void onInternalServiceException(Activity activity, ServiceLifeCycle forServices, ServiceLifeCycle.ServiceException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot access properly to the services", exception);
      }
      if (ActivityController.getInstance().handleException(activity, exception) == false)
      {
        // onServiceException(exception);
      }
    }

  }

  /**
   * An internal key, which enables to determine whether an activity has already been started.
   */
  final static String ALREADY_STARTED = "com.smartnsoft.droid4me.alreadyStarted";

  /**
   * This threads pool is used internally, in order to prevent from new thread creation, for an optimization purpose.
   * 
   * <ul>
   * <li>This pool can contain an unlimited number of threads;</li>
   * <li>exceptions thrown by the {@link Runnable} are handled by the
   * {@link ActivityController#registerExceptionHandler(ActivityController.ExceptionHandler) exception handler}.</li>
   * </ul>
   * 
   * <p>
   * This pool will be used by the framework, instead of creating new threads.
   * </p>
   * 
   * @see SmartCommands#LOW_PRIORITY_THREAD_POOL
   */
  final static SmartCommands.SmartThreadPoolExecutor THREAD_POOL = new SmartCommands.SmartThreadPoolExecutor(0, Integer.MAX_VALUE, 60l, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory()
  {

    private final AtomicInteger threadsCount = new AtomicInteger(1);

    public Thread newThread(Runnable runnable)
    {
      final Thread thread = new Thread(runnable);
      thread.setName("droid4me-pool-thread #" + threadsCount.getAndIncrement());
      return thread;
    }
  });

  static void execute(Activity activity, Runnable runnable)
  {
    AppInternals.THREAD_POOL.execute(activity, runnable);
  }

  private AppInternals()
  {
  }

}
