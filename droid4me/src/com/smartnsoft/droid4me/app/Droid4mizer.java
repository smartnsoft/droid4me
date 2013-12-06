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
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.app.AppInternals.StateContainer;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;

/**
 * The class that should be used when extending a legacy class to support the whole droid4me framework features.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @param <ComponentClass>
 *          the instance that will be used to determine whether {@linkplain #onRetrieveBusinessObjects() the business object should be retrieved
 *          asynchronously}, and to {@linkplain #registerBroadcastListeners(BroadcastListener[]) register broadcast listeners}
 * 
 * @author Édouard Mercier
 * @since 2011.06.14
 */
public final class Droid4mizer<AggregateClass, ComponentClass>
    implements Smartable<AggregateClass>
{

  /**
   * A flag which indicates whether the hereby {@code Droid4mizer} internal logs should be enabled. Logs will report all life-cycle events, for
   * instance. The default value is {@code false}.
   */
  public static boolean ARE_DEBUG_LOG_ENABLED = false;

  private final Activity activity;

  private final ComponentClass component;

  private final ComponentClass interceptorComponent;

  private final Smartable<AggregateClass> smartable;

  private final AppInternals.StateContainer<AggregateClass, ComponentClass> stateContainer;

  /**
   * The only way to create an instance.
   * 
   * @param activity
   *          the activity this instance relies on
   * @param smartable
   *          the component to be droid4mized
   * @param component
   *          the declared component used to determine whether {@linkplain #onRetrieveBusinessObjects() the business object should be retrieved
   *          asynchronously}, and to {@linkplain #registerBroadcastListeners(BroadcastListener[]) register broadcast listeners}
   * @param interceptorComponent
   *          the declared component used to send life-cycle events to the {@link ActivityController.Interceptor}
   */
  public Droid4mizer(Activity activity, Smartable<AggregateClass> smartable, ComponentClass component, ComponentClass interceptorComponent)
  {
    this.activity = activity;
    this.smartable = smartable;
    this.component = component;
    this.interceptorComponent = interceptorComponent;
    stateContainer = new AppInternals.StateContainer<AggregateClass, ComponentClass>(activity, component);
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED)
    {
      log.debug("Creating the droid4mizer for Activity belonging to class '" + activity.getClass().getName() + "'" + (interceptorComponent == null ? ""
          : " and with Fragment belonging to class '" + interceptorComponent.getClass().getName() + "'"));
    }
  }

  /*
   * The {@link Smarted} methods.
   */

  public AggregateClass getAggregate()
  {
    return stateContainer.getAggregate();
  }

  public void setAggregate(AggregateClass aggregate)
  {
    stateContainer.setAggregate(aggregate);
  }

  public Handler getHandler()
  {
    return stateContainer.getHandler();
  }

  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    ActivityController.getInstance().handleException(activity, interceptorComponent, throwable);
  }

  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    stateContainer.registerBroadcastListeners(broadcastListeners);
  }

  public void onBusinessObjectsRetrieved()
  {
    smartable.onBusinessObjectsRetrieved();
  }

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(true, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null, false);
  }

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(boolean, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects)
  {
    refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, null, false);
  }

  @SuppressWarnings("deprecation")
  public void refreshBusinessObjectsAndDisplay(final boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately)
  {
    if (stateContainer.isAliveAsWellAsHostingActivity() == false)
    {
      // In that case, we skip the processing
      return;
    }
    if (stateContainer.shouldDelayRefreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately) == true)
    {
      return;
    }
    if (stateContainer.isAliveAsWellAsHostingActivity() == false)
    {
      // In that case, we skip the processing
      return;
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStart();
    // We can safely retrieve the business objects
    if (component.getClass().getAnnotation(BusinessObjectsRetrievalAsynchronousPolicyAnnotation.class) != null || component instanceof LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy == true)
    {
      // We call that routine asynchronously in a background thread
      stateContainer.execute(activity, component, new Runnable()
      {
        public void run()
        {
          if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
          {
            return;
          }
          // We are handling the UI, and we need to make sure that this is done through the GUI thread
          activity.runOnUiThread(new Runnable()
          {
            public void run()
            {
              // If the hosting activity has been finished in the meantime, or the entity is not alive anymore, we should not update the UI
              if (stateContainer.isAliveAsWellAsHostingActivity() == false)
              {
                // And in that case, since we are currently in the UI thread, we are ensured that neither the entity nor the hosting Activity will be
                // destroyed in the meantime!
                return;
              }
              onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
            }
          });
        }
      });
    }
    else
    {
      // We directly run in the UI thread
      if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
      {
        return;
      }
      onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
    }
  }

  /*
   * The {@link AppPublics.LifeCyclePublic} methods.
   */

  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return stateContainer.isRefreshingBusinessObjectsAndDisplay();
  }

  public final int getOnSynchronizeDisplayObjectsCount()
  {
    return stateContainer.getOnSynchronizeDisplayObjectsCount();
  }

  public final boolean isFirstLifeCycle()
  {
    return stateContainer.isFirstLifeCycle();
  }

  public final boolean isInteracting()
  {
    return stateContainer.isInteracting();
  }

  public final boolean isAlive()
  {
    return stateContainer.isAlive();
  }

  /*
   * The {@link AppInternals.LifeCycleInternals} methods.
   */

  public boolean shouldKeepOn()
  {
    return stateContainer.shouldKeepOn();
  }

  /*
   * The {@link Activity}/{@link android.app.Fragment} methods.
   */

  public void onAttached(Activity activity)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onAttached");
    }
  }

  public void onCreate(Runnable superMethod, Bundle savedInstanceState)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreate");
    }

    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore);
    superMethod.run();
    if (isFragment() == false && ActivityController.getInstance().needsRedirection(activity) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }
    if (StateContainer.isFirstCycle(savedInstanceState) == true)
    {
      stateContainer.setFirstLifeCycle(false);
    }
    else
    {
      stateContainer.setFirstLifeCycle(true);
    }
    stateContainer.registerBroadcastListeners();

    stateContainer.initialize();
    try
    {
      onRetrieveDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.stopHandling();
      smartable.onException(throwable, true);
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onCreateDone);
  }

  public void onPostCreate(Bundle savedInstanceState)
  {
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onPostCreate);
  }

  public void onNewIntent(Intent intent)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onNewIntent");
    }

    if (isFragment() == false && ActivityController.getInstance().needsRedirection(activity) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
    }
  }

  public void onContentChanged()
  {
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onContentChanged);
  }

  public void onResume()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onResume");
    }
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onResume);
    stateContainer.onResume();
    refreshBusinessObjectsAndDisplayInternal();
  }

  public void onPostResume()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPostResume");
    }
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onPostResume);
    stateContainer.onPostResume();
  }

  public void onConfigurationChanged(Configuration newConfig)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onConfigurationChanged");
    }
  }

  public void onSaveInstanceState(Bundle outState)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onSaveInstanceState");
    }
    stateContainer.onSaveInstanceState(outState);
  }

  public void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onRestoreInstanceState");
    }
    refreshBusinessObjectsAndDisplayInternal();
  }

  public void onStart()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStart");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onStart);
    stateContainer.onStart();
  }

  public void onRestart()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onRestart");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onRestart);
    stateContainer.onRestart();
  }

  public void onPause()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPause");
    }
    if (shouldKeepOn() == false)
    {
      // We stop here if a redirection is needed or is something went wrong
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onPause);
      stateContainer.onPause();
    }
  }

  public void onStop()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStop");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onStop);
    stateContainer.onStop();
  }

  public void onDestroy()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onDestroy");
    }
    stateContainer.onDestroy();
    if (shouldKeepOn() == false)
    {
      // We stop here if a redirection is needed or is something went wrong
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onDestroy);
  }

  public void onDetached()
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onDetached");
    }
  }

  public boolean onCreateOptionsMenu(boolean superResult, Menu menu)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreateOptionsMenu");
    }
    return superResult;
  }

  public boolean onPrepareOptionsMenu(boolean superResult, Menu menu)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPrepareOptionsMenu");
    }
    return superResult;
  }

  public boolean onOptionsItemSelected(boolean superResult, MenuItem item)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onOptionsItemSelected");
    }
    return superResult;
  }

  public boolean onContextItemSelected(boolean superResult, MenuItem item)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onContextItemSelected");
    }
    return superResult;
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (Droid4mizer.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onActivityResult");
    }
  }

  /*
   * The LifeCycle interface implementation.
   */

  public void onFulfillDisplayObjects()
  {
    smartable.onFulfillDisplayObjects();
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    smartable.onRetrieveBusinessObjects();
  }

  public void onRetrieveDisplayObjects()
  {
    smartable.onRetrieveDisplayObjects();
  }

  public void onSynchronizeDisplayObjects()
  {
    smartable.onSynchronizeDisplayObjects();
  }

  public SharedPreferences getPreferences()
  {
    return stateContainer.getPreferences(activity);
  }

  /*
   * The specific methods.
   */

  /**
   * Has the same responsibility as the {@link Context#getSystemService()} method. It only applies to the {@link Activity} entity, and not to the
   * {@link Fragment} entity!
   * 
   * @param name
   *          the name of the desired service
   * @param defaultService
   *          the default service provided by the underlying {@link #activity Activity}
   * @return the service or {@code null} if the name does not exist
   * @see Context#getSystemService(name)
   */
  public Object getSystemService(String name, Object defaultService)
  {
    return ActivityController.getInstance().getSystemService(activity, name, defaultService);
  }

  /**
   * This method should not trigger any exception, otherwise we have a huge bug!
   * 
   * @return {@code true} if and only if the processing should resume
   */
  private boolean onRetrieveBusinessObjectsInternal(boolean retrieveBusinessObjects)
  {
    try
    {
      onBeforeRefreshBusinessObjectsAndDisplay();
      if (retrieveBusinessObjects == true)
      {
        if (stateContainer.isAliveAsWellAsHostingActivity() == false)
        {
          // If the entity is no more alive, we give up the process
          return false;
        }

        onRetrieveBusinessObjects();
        // We notify the entity that the business objects have actually been loaded
        if (stateContainer.isAliveAsWellAsHostingActivity() == false)
        {
          // If the entity is no more alive, we give up the process
          return false;
        }
        onBusinessObjectsRetrieved();

      }
      stateContainer.setBusinessObjectsRetrieved();
      return true;
    }
    catch (Throwable throwable)
    {
      stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
      // We check whether the issue does not come from a non-alive entity
      if (stateContainer.isAliveAsWellAsHostingActivity() == false)
      {
        // In that case, we just ignore the exception: it is very likely that the entity or the hosting Activity have turned as non-alive
        // during the "onRetrieveBusinessObjects()" method!
        return false;
      }
      // Otherwise, we report the exception
      onInternalBusinessObjectAvailableException(throwable);
      return false;
    }
  }

  private void onBeforeRefreshBusinessObjectsAndDisplay()
  {
    stateContainer.onStartLoading();
  }

  private void onFulfillAndSynchronizeDisplayObjectsInternal(Runnable onOver)
  {
    if (stateContainer.isResumedForTheFirstTime() == true)
    {
      try
      {
        onFulfillDisplayObjects();
      }
      catch (Throwable throwable)
      {
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
        smartable.onException(throwable, true);
        stateContainer.onStopLoading();
        return;
      }
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent,
          ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
    }
    try
    {
      stateContainer.onSynchronizeDisplayObjects();
      onSynchronizeDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
      smartable.onException(throwable, true);
      return;
    }
    finally
    {
      stateContainer.onStopLoading();
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent,
        ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
    stateContainer.markNotResumedForTheFirstTime();
    if (onOver != null)
    {
      try
      {
        onOver.run();
      }
      catch (Throwable throwable)
      {
        if (log.isErrorEnabled())
        {
          log.error("An exception occurred while executing the 'refreshBusinessObjectsAndDisplay()' runnable!", throwable);
        }
      }
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
  }

  private void refreshBusinessObjectsAndDisplayInternal()
  {
    smartable.refreshBusinessObjectsAndDisplay(stateContainer.isRetrieveBusinessObjects(), stateContainer.getRetrieveBusinessObjectsOver(), true);
  }

  private final void onInternalBusinessObjectAvailableException(Throwable throwable)
  {
    if (log.isErrorEnabled())
    {
      log.error("Cannot retrieve the business objects", throwable);
    }
    stateContainer.onStopLoading();
    if (stateContainer.onInternalBusinessObjectAvailableExceptionWorkAround(throwable) == true)
    {
      return;
    }
    // We need to indicate to the method that it may have been triggered from another thread than the GUI's
    smartable.onException(throwable, false);
  }

  private boolean isFragment()
  {
    return activity != smartable;
  }

}
