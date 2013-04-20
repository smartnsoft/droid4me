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
import java.util.concurrent.Future;
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
   * Defines several contracts for each {@link Activity activity}/{@link android.app.Fragment} entity of the framework.
   * 
   * @since 2010.01.05
   */
  interface LifeCycleInternals
  {

    /**
     * Indicates whether nothing went wrong during the implementing entity, or if a {@link #isBeingRedirected() redirection} is being processed.
     * 
     * <p>
     * Thanks to the {@link ActivityController.Redirector redirection controller}, it is possible to re-route any {@link Smartable} {@link Activity}
     * when it starts, if some other {@code Activity} needs to be executed beforehand.
     * </p>
     * 
     * @return {@code true} if and only if the implementing {@link Activity}/@{link android.app.Fragment} entity should resume its execution
     */
    boolean shouldKeepOn();

    MenuHandler.Composite getCompositeActionHandler();

    CompositeHandler getCompositeActivityResultHandler();

  }

  /**
   * There for gathering all instance variables, and in order to make copy and paste smarter.
   * 
   * @param <AggregateClass>
   *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
   * @param <ComponentClass>
   *          the instance the container has been created for
   * 
   * @since 2009.02.16
   */
  final static class StateContainer<AggregateClass, ComponentClass>
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

    /**
     * The activity this container has been created for in case of an {@link Activity}, or the hosting {@link Activity} in case of a
     * {@link android.app.Fragment}.
     */
    private final Activity activity;

    /**
     * The component this container has been created for.
     */
    private final ComponentClass component;

    private boolean resumedForTheFirstTime = true;

    MenuHandler.Composite compositeActionHandler;

    CompositeHandler compositeActivityResultHandler;

    private Handler handler;

    private AggregateClass aggregate;

    private SharedPreferences preferences;

    boolean actionResultsRetrieved;

    private boolean businessObjectsRetrieved;

    private boolean doNotCallOnActivityDestroyed;

    private boolean firstLifeCycle = true;

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
     * A flag which states whether the underlying {@link Activity}/{@link android.app.Fragment} life-cycle is between the {@link Activity#onResume()}/
     * {@link android.app.Fragment#onResume()} and {@link Activity#onPause()}/{@link android.app.Fragment#onPause()}.
     */
    private boolean isInteracting;

    /**
     * A flag which states whether the underlying {@link Activity}/{@link android.app.Fragment} has not already invoked the
     * {@link Activity#onDestroy()}/{@link android.app.Fragment#onDestroy()} method.
     */
    private boolean isAlive = true;

    private AppInternals.StateContainer.RefreshBusinessObjectsAndDisplay refreshBusinessObjectsAndDisplayNextTime;

    private AppInternals.StateContainer.RefreshBusinessObjectsAndDisplay refreshBusinessObjectsAndDisplayPending;

    /**
     * Contains all the commands which have been registered.
     */
    private List<Future<?>> futures = new ArrayList<Future<?>>();

    /**
     * Should only be created by classes in the same package.
     * 
     * @param activity
     *          the activity this container has been created for
     * @param component
     *          the component this container has been created for
     */
    StateContainer(Activity activity, ComponentClass component)
    {
      this.activity = activity;
      this.component = component;
    }

    AggregateClass getAggregate()
    {
      return aggregate;
    }

    void setAggregate(AggregateClass aggregate)
    {
      this.aggregate = aggregate;
    }

    Handler getHandler()
    {
      return handler;
    }

    boolean isFirstLifeCycle()
    {
      return firstLifeCycle;
    }

    void setFirstLifeCycle(boolean firstLifeCycle)
    {
      this.firstLifeCycle = firstLifeCycle;
    }

    boolean isResumedForTheFirstTime()
    {
      return resumedForTheFirstTime;
    }

    void markNotResumedForTheFirstTime()
    {
      resumedForTheFirstTime = false;
    }

    /**
     * @see #isInteracting
     */
    final boolean isInteracting()
    {
      return isInteracting;
    }

    /**
     * @see #isAlive
     */
    final boolean isAlive()
    {
      return isAlive;
    }

    boolean isDoNotCallOnActivityDestroyed()
    {
      return doNotCallOnActivityDestroyed;
    }

    synchronized SharedPreferences getPreferences(Context applicationContext)
    {
      if (preferences == null)
      {
        preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
      }
      return preferences;
    }

    /**
     * Should be always invoked from the UI thread.
     */
    void initialize()
    {
      compositeActionHandler = new MenuHandler.Composite();
      compositeActionHandler.add(createStaticActionHandler());
      compositeActivityResultHandler = new ActivityResultHandler.CompositeHandler();
      handler = new Handler();
    }

    private MenuHandler.Static createStaticActionHandler()
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
     * If the {@link #component} implements either the {@link AppPublics.BroadcastListener} or {@link AppPublics.BroadcastListenerProvider} or
     * {@link AppPublics.BroadcastListenersProvider} interface, it will register for listening to some broadcast intents.
     */
    void registerBroadcastListeners()
    {
      if (component instanceof AppPublics.BroadcastListenersProvider)
      {
        final AppPublics.BroadcastListenersProvider broadcastListenersProvider = (AppPublics.BroadcastListenersProvider) component;
        final int count = broadcastListenersProvider.getBroadcastListenersCount();
        if (log.isDebugEnabled())
        {
          log.debug("Found out that the entity supports " + count + " intent broadcast listeners");
        }
        final int startIndex = enrichBroadcastListeners(count);
        for (int index = 0; index < count; index++)
        {
          registerBroadcastListeners(startIndex + index, broadcastListenersProvider.getBroadcastListener(index));
        }
      }
      else if (component instanceof AppPublics.BroadcastListenerProvider)
      {
        if (log.isDebugEnabled())
        {
          log.debug("Found out that the entity supports a single intent broadcast listener");
        }
        final AppPublics.BroadcastListener broadcastListener = ((AppPublics.BroadcastListenerProvider) component).getBroadcastListener();
        if (broadcastListener != null)
        {
          registerBroadcastListeners(enrichBroadcastListeners(1), broadcastListener);
        }
      }
      else if (component instanceof AppPublics.BroadcastListener)
      {
        if (log.isDebugEnabled())
        {
          log.debug("Found out that the entity implements a single intent broadcast listener");
        }
        final AppPublics.BroadcastListener broadcastListener = (AppPublics.BroadcastListener) component;
        registerBroadcastListeners(enrichBroadcastListeners(1), broadcastListener);
      }
    }

    void registerBroadcastListeners(AppPublics.BroadcastListener[] broadcastListeners)
    {
      final int startIndex = enrichBroadcastListeners(broadcastListeners.length);
      for (int index = 0; index < broadcastListeners.length; index++)
      {
        registerBroadcastListeners(index + startIndex, broadcastListeners[index]);
      }
    }

    private void registerBroadcastListeners(int index, final AppPublics.BroadcastListener broadcastListener)
    {
      if (broadcastListener == null)
      {
        throw new NullPointerException("Cannot register a null 'broadcastListener'!");
      }
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

    private int enrichBroadcastListeners(int count)
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
    void unregisterBroadcastListeners()
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
    @SuppressWarnings("deprecation")
    void onStartLoading()
    {
      if (component.getClass().getAnnotation(AppPublics.SendLoadingIntentAnnotation.class) != null || component instanceof AppPublics.SendLoadingIntent)
      {
        // We indicate the activity which is loading, in order to filter the loading events
        AppPublics.LoadingBroadcastListener.broadcastLoading(activity, System.identityHashCode(activity), System.identityHashCode(component), true);
      }
    }

    /**
     * Should be invoked just after the {@link SmartActivity.onSynchronizeDisplayObjects} method: is responsible for triggering a loading broadcast
     * intent if required, in order to indicate that the activity has stopped loading.
     */
    @SuppressWarnings("deprecation")
    void onStopLoading()
    {
      if (component.getClass().getAnnotation(AppPublics.SendLoadingIntentAnnotation.class) != null || component instanceof AppPublics.SendLoadingIntent)
      {
        // We indicate the activity which is loading, in order to filter the loading events
        AppPublics.LoadingBroadcastListener.broadcastLoading(activity, System.identityHashCode(activity), System.identityHashCode(component), false);
      }
    }

    /**
     * Invoked when the provided activity enters the {@link Activity#onStart()} method. We check whether to invoke the {@link ServiceLifeCycle}
     * methods.
     */
    void onStart()
    {
      if (activity instanceof ServiceLifeCycle)
      {
        final ServiceLifeCycle forServices = (ServiceLifeCycle) activity;
        if (areServicesAsynchronous() == false)
        {
          internalPrepareServices(forServices);
        }
        else
        {
          AppInternals.THREAD_POOL.submit(new Runnable()
          {
            public void run()
            {
              internalPrepareServices(forServices);
            }
          });
        }
      }
    }

    /**
     * Invoked when the provided activity enters the {@link Activity#onResume()} method.
     */
    void onResume()
    {
      isInteracting = true;
      doNotCallOnActivityDestroyed = false;
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

    synchronized void onRefreshingBusinessObjectsAndDisplayStop(LifeCycle lifeCycleActivity)
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
    void onPause()
    {
      isInteracting = false;
    }

    /**
     * Invoked when the underlying activity/fragment enters the {@link Activity#onStop()} method. We check whether to invoke the
     * {@link ServiceLifeCycle} methods.
     */
    void onStop()
    {
      if (activity instanceof ServiceLifeCycle)
      {
        final ServiceLifeCycle forServices = (ServiceLifeCycle) activity;
        if (areServicesAsynchronous() == false)
        {
          internalDisposeServices(forServices);
        }
        else
        {
          AppInternals.THREAD_POOL.submit(new Runnable()
          {
            public void run()
            {
              internalDisposeServices(forServices);
            }
          });
        }
      }
    }

    /**
     * Invoked when the underlying activity/fragment enters the {@link Activity#onDestro()} method.
     * 
     * <p>
     * The method marks the entity as {@link #isAlive no more alive}, unregisters the previously registered {@link AppPublics.BroadcastListener
     * broadcast listeners}, and eventually interrupts all pending {@link #futures}.
     * </p>
     */
    void onDestroy()
    {
      isAlive = false;
      unregisterBroadcastListeners();

      // We cancel all the commands which are still running, or which have not yet been started
      for (Future<?> future : futures)
      {
        if (future.isDone() == false)
        {
          final boolean result = future.cancel(true);
          if (log.isDebugEnabled())
          {
            log.debug("Aborted " + (result == true ? "successfuly" : "unsucessfuly") + " a command which has not already been executed, or which is still being executing");
          }
        }
      }
    }

    void onSaveInstanceState(Bundle outState)
    {
      doNotCallOnActivityDestroyed = true;
      outState.putBoolean(AppInternals.ALREADY_STARTED, true);
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

    synchronized boolean shouldDelayRefreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
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

    private boolean areServicesAsynchronous()
    {
      return activity instanceof ServiceLifeCycle.ForServicesAsynchronousPolicy;
    }

    private void internalPrepareServices(ServiceLifeCycle forServices)
    {
      try
      {
        forServices.prepareServices();
      }
      catch (ServiceLifeCycle.ServiceException exception)
      {
        onInternalServiceException(forServices, exception);
      }
    }

    private void internalDisposeServices(ServiceLifeCycle forServices)
    {
      try
      {
        forServices.disposeServices();
      }
      catch (ServiceLifeCycle.ServiceException exception)
      {
        onInternalServiceException(forServices, exception);
      }
    }

    private void onInternalServiceException(ServiceLifeCycle forServices, ServiceLifeCycle.ServiceException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot access properly to the services", exception);
      }
      // TODO: handle another way the component
      if (ActivityController.getInstance().handleException(activity, component, exception) == false)
      {
      }
    }

    /**
     * Is responsible for executing the given runnable in background via the {@link AppInternals#THREAD_POOL internal threads pool}.
     * 
     * <p>
     * The command will be remembered, so as to be able to abort/cancel/remove it when the underlying activity/fragment is destroyed.
     * </p>
     * 
     * @param activity
     *          the activity which is responsible for running the command
     * @param component
     *          the component which is responsible for running the command ; may be {@code null}
     * @param runnable
     *          the command to execute
     */
    void execute(Activity activity, Object component, Runnable runnable)
    {
      final Future<?> future = AppInternals.THREAD_POOL.submit(runnable);
      futures.add(future);
    }

    /**
     * This hook is there to handle the special case of the Fragment, which raises an IllegalStateException when the "getString()"/"getResources()"
     * method is invoked, whereas it is already detached. Regarding this discussion, see
     * http://groups.google.com/group/android-developers/browse_frm/thread/ae9c7890201c9b0b
     */
    boolean onInternalBusinessObjectAvailableExceptionWorkAround(Throwable throwable)
    {
      if (component != null && throwable instanceof IllegalStateException)
      {
        final IllegalStateException illegalStateException = (IllegalStateException) throwable;
        final String ILLEGAL_STATE_EXCEPTION_FRAGMENT_MESSAGE_SUFFIX = "not attached to Activity";
        if (illegalStateException.getMessage() != null && illegalStateException.getMessage().endsWith(ILLEGAL_STATE_EXCEPTION_FRAGMENT_MESSAGE_SUFFIX) == true)
        {
          return true;
        }
      }
      return false;
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

  private AppInternals()
  {
  }

}
