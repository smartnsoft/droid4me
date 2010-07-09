/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import java.util.List;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListenerProvider;
import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.LifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

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

    Handler getHandler();

    List<StaticMenuCommand> getMenuCommands();

    /**
     * Can be invoked when an exception is thrown, so that the {@link ActivityController.ExceptionHandler} handles it.
     * 
     * @param fromGuiThread
     *          indicates whether the call is done from the GUI thread
     */
    void onException(Throwable throwable, boolean fromGuiThread);

  }

  /**
   * There for gathering all instance variables, and in order to make copy and paste smarter.
   * 
   * @since 2009.02.16
   */
  final static class StateContainer
  {

    private static final Logger log = LoggerFactory.getInstance(StateContainer.class);

    boolean resumedForTheFirstTime = true;

    MenuHandler.Composite compositeActionHandler;

    CompositeHandler compositeActivityResultHandler;

    Handler handler;

    private SharedPreferences preferences;

    boolean actionResultsRetrieved;

    boolean businessObjectsRetrieved;

    boolean doNotCallOnActivityDestroyed;

    boolean firstLifeCycle = true;

    public boolean beingRedirected;

    private BroadcastReceiver broadcastReceiver;

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
     * If the activity implements the {@link AppPublics.BroadcastListener} interface, it will register for listening to some broadcast intents.
     */
    void registerBroadcastListener(Activity activity)
    {
      final BroadcastListener broadcastListener;
      if (activity instanceof BroadcastListener)
      {
        broadcastListener = (BroadcastListener) activity;
      }
      else if (activity instanceof BroadcastListenerProvider)
      {
        broadcastListener = ((BroadcastListenerProvider) activity).getBroadcastListener();
      }
      else
      {
        return;
      }
      if (log.isDebugEnabled())
      {
        log.debug("Registering for listening to intent broacasts");
      }
      broadcastReceiver = new BroadcastReceiver()
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
      activity.registerReceiver(broadcastReceiver, intentFilter == null ? new IntentFilter() : intentFilter);
    }

    /**
     * If the activity implements the {@link AppPublics.BroadcastListener} interface, this will make it stop listening to broadcasted intents.
     */
    void unregisterBroadcastListener(Activity activity)
    {
      if (broadcastReceiver != null)
      {
        activity.unregisterReceiver(broadcastReceiver);
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
        activity.sendBroadcast(new Intent(AppPublics.UI_LOAD_ACTION).putExtra(AppPublics.UI_LOAD_ACTION_ACTIVITY, activity.getClass().getName()));
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
        activity.sendBroadcast(new Intent(AppPublics.UI_LOAD_ACTION).putExtra(AppPublics.UI_LOAD_ACTION_LOADING, false).putExtra(
            AppPublics.UI_LOAD_ACTION_ACTIVITY, activity.getClass().getName()));
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
