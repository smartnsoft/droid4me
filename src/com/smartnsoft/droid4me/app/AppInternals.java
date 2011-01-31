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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.LifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuHandler;

/**
 * Gathers some internal interfaces and helpers for the types belonging to its Java package.
 * 
 * @author Édouard Mercier
 * @since 2010.01.05
 */
abstract class AppInternals
{

  /**
   * Defines several contracts for all {@link Activity activities} of the framework.
   * 
   * @since 2010.01.05
   */
  interface LifeCycleInternals
  {

    boolean isBeingRedirected();

    void onActuallyCreated();

    void onActuallyDestroyed();

  }

  /**
   * There for gathering all instance variables, and in order to make copy and paste smarter.
   * 
   * @since 2009.02.16
   */
  final static class StateContainer<AggregateClass>
  {

    private static final Logger log = LoggerFactory.getInstance(StateContainer.class);

    boolean resumedForTheFirstTime = true;

    MenuHandler.Composite compositeActionHandler;

    CompositeHandler compositeActivityResultHandler;

    Handler handler;

    AggregateClass aggregate;

    private SharedPreferences preferences;

    boolean actionResultsRetrieved;

    boolean businessObjectsRetrieved;

    boolean doNotCallOnActivityDestroyed;

    boolean firstLifeCycle = true;

    public boolean beingRedirected;

    private BroadcastReceiver[] broadcastReceivers;

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
      compositeActionHandler.add(SmartActivity.createStaticActionHandler(context));
      compositeActivityResultHandler = new ActivityResultHandler.CompositeHandler();
      handler = new Handler();
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
        registerBroadcastListeners(activity, enrichBroadCastListeners(1), broadcastListener);
      }
      else if (activity instanceof AppPublics.BroadcastListener)
      {
        broadcastListener = (AppPublics.BroadcastListener) activity;
        registerBroadcastListeners(activity, enrichBroadCastListeners(1), broadcastListener);
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
     * Invoked when the provided activity enters the {@link Activity#onStart()} method. We check whether to invoke the {@link LifeCycle.ForServices}
     * methods.
     */
    void onStart(final Activity activity)
    {
      if (activity instanceof LifeCycle.ForServices)
      {
        final LifeCycle.ForServices forServices = (LifeCycle.ForServices) activity;
        if (areServicesAsynchronous(activity) == false)
        {
          internalPrepareServices(activity, forServices);
        }
        else
        {
          AppPublics.THREAD_POOL.submit(new Runnable()
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
     * Invoked when the provided activity enters the {@link Activity#onStop()} method. We check whether to invoke the {@link LifeCycle.ForServices}
     * methods.
     */
    public void onStop(final Activity activity)
    {
      if (activity instanceof LifeCycle.ForServices)
      {
        final LifeCycle.ForServices forServices = (LifeCycle.ForServices) activity;
        if (areServicesAsynchronous(activity) == false)
        {
          internalDisposeServices(activity, forServices);
        }
        else
        {
          AppPublics.THREAD_POOL.submit(new Runnable()
          {
            public void run()
            {
              internalDisposeServices(activity, forServices);
            }
          });
        }
      }
    }

    private boolean areServicesAsynchronous(Activity activity)
    {
      return activity instanceof LifeCycle.ForServicesAsynchronousPolicy;
    }

    private void internalPrepareServices(Activity activity, LifeCycle.ForServices forServices)
    {
      try
      {
        forServices.prepareServices();
      }
      catch (LifeCycle.ServiceException exception)
      {
        onInternalServiceException(activity, forServices, exception);
      }
    }

    private void internalDisposeServices(Activity activity, LifeCycle.ForServices forServices)
    {
      try
      {
        forServices.disposeServices();
      }
      catch (LifeCycle.ServiceException exception)
      {
        onInternalServiceException(activity, forServices, exception);
      }
    }

    private void onInternalServiceException(Activity activity, LifeCycle.ForServices forServices, LifeCycle.ServiceException exception)
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

}
