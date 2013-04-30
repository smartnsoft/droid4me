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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

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

  private final Activity activity;

  private final ComponentClass component;

  private final ComponentClass interceptorComponent;

  private final Smartable<AggregateClass> smartable;

  private final AppInternals.StateContainer<AggregateClass, ComponentClass> stateContainer;

  /**
   * The {@link Activity} {@link Intent} which will be used in case the {@link android.app.ActionBar} "Home" button is clicked.
   */
  private Intent homeIntent;

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

  public List<StaticMenuCommand> getMenuCommands()
  {
    return smartable.getMenuCommands();
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
    if (stateContainer.shouldDelayRefreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately) == true)
    {
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
          // If the hosting activity has been finished in the meantime, or the entity is not alive anymore, we should not update the UI
          if (activity.isFinishing() == true || smartable.isAlive() == false)
          {
            return;
          }
          // We are handling the UI, and we need to make sure that this is done through the GUI thread
          activity.runOnUiThread(new Runnable()
          {
            public void run()
            {
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

  public Composite getCompositeActionHandler()
  {
    return stateContainer.compositeActionHandler;
  }

  public CompositeHandler getCompositeActivityResultHandler()
  {
    return stateContainer.compositeActivityResultHandler;
  }

  /*
   * The {@link Activity}/{@link Fragment} methods.
   */

  public void onCreate(Runnable superMethod, Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreate");
    }

    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore);
    superMethod.run();
    if (ActivityController.getInstance().needsRedirection(activity) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(AppInternals.ALREADY_STARTED) == true)
    {
      stateContainer.setFirstLifeCycle(false);
    }
    else
    {
      stateContainer.setFirstLifeCycle(true);
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onActuallyCreatedDone);
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
    // We add the static menu commands
    smartable.getCompositeActionHandler().add(new MenuHandler.Static()
    {
      @Override
      protected List<MenuCommand<Void>> retrieveCommands()
      {
        final List<StaticMenuCommand> staticMenuCommands = getMenuCommands();
        if (staticMenuCommands == null)
        {
          return null;
        }
        final List<MenuCommand<Void>> menuCommands = new ArrayList<MenuCommand<Void>>(staticMenuCommands.size());
        for (StaticMenuCommand staticMenuCommand : staticMenuCommands)
        {
          menuCommands.add(staticMenuCommand);
        }
        return menuCommands;
      }
    });
  }

  public void onNewIntent(Intent intent)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onNewIntent");
    }

    if (ActivityController.getInstance().needsRedirection(activity) == true)
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
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onResume");
    }
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onResume);
    stateContainer.onResume();
    businessObjectRetrievalAndResultHandlers();
  }

  public void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onSaveInstanceState");
    }
    stateContainer.onSaveInstanceState(outState);
  }

  public void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onRestoreInstanceState");
    }
    businessObjectRetrievalAndResultHandlers();
  }

  public void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStart");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onStart);
    stateContainer.onStart();
  }

  public void onPause()
  {
    if (log.isDebugEnabled())
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
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStop");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onStop);
    stateContainer.onStop();
  }

  public void onDestroy()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onDestroy");
    }
    stateContainer.onDestroy();
    if (shouldKeepOn() == false)
    {
      // We stop here if a redirection is needed or is something went wrong
      return;
    }
    if (stateContainer.isDoNotCallOnActivityDestroyed() == false)
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onActuallyDestroyedDone);
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, interceptorComponent, ActivityController.Interceptor.InterceptorEvent.onDestroy);
    }
  }

  public boolean onCreateOptionsMenu(boolean superResult, Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreateOptionsMenu");
    }
    if (stateContainer.compositeActionHandler != null)
    {
      stateContainer.compositeActionHandler.onCreateOptionsMenu(activity, menu);
    }
    else
    {
      if (log.isErrorEnabled())
      {
        log.error("onCreateOptionsMenu() being called whereas the 'stateContainer.compositeActionHandler' has not yet been initialized!");
      }
    }
    return superResult;
  }

  public boolean onPrepareOptionsMenu(boolean superResult, Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPrepareOptionsMenu");
    }
    if (stateContainer.compositeActionHandler != null)
    {
      stateContainer.compositeActionHandler.onPrepareOptionsMenu(menu);
    }
    else
    {
      if (log.isErrorEnabled())
      {
        log.error("onPrepareOptionsMenu() being called whereas the 'stateContainer.compositeActionHandler' has not yet been initialized!");
      }
    }
    return superResult;
  }

  public boolean onOptionsItemSelected(boolean superResult, MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onOptionsItemSelected");
    }
    // This is the special case of the ActionBar "Home" button
    final int homeMenuResourceId = 0x0102002c;// android.R.id.home;
    if (item.getItemId() == homeMenuResourceId)
    {
      final Intent homeIntent = getHomeIntent();
      if (homeIntent != null)
      {
        try
        {
          activity.startActivity(homeIntent);
        }
        catch (Throwable throwable)
        {
          if (log.isErrorEnabled())
          {
            log.error("Could not start the home Intent " + homeIntent, throwable);
          }
        }
      }
      // In case the home intent does not set the Intent.FLAG_ACTIVITY_CLEAR_TOP flag, and just returns to the previous screen
      activity.finish();
      return true;
    }
    if (stateContainer.compositeActionHandler != null)
    {
      if (stateContainer.compositeActionHandler.onOptionsItemSelected(item) == true)
      {
        return true;
      }
    }
    else
    {
      if (log.isErrorEnabled())
      {
        log.error("onOptionsItemSelected() being called whereas the 'stateContainer.compositeActionHandler' has not yet been initialized!");
      }
    }
    return superResult;
  }

  public boolean onContextItemSelected(boolean superResult, MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onContextItemSelected");
    }
    if (stateContainer.compositeActionHandler.onContextItemSelected(item) == true)
    {
      return true;
    }
    return superResult;
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onActivityResult");
    }

    // // BUG: this seems to be a bug in Android, because this method is invoked before the "onResume()"
    // try
    // {
    // businessObjectRetrievalAndResultHandlers();
    // }
    // catch (Throwable throwable)
    // {
    // handleUnhandledException(throwable);
    // return;
    // }

    smartable.getCompositeActivityResultHandler().handle(requestCode, resultCode, data);
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
   * Indicates the {@link Activity} {@link Intent} that will be launched when hitting the {@link android.app.ActionBar} "Home" button.
   * 
   * <p>
   * If the {@link #setHomeIntent(Intent)} method has not been invoked, the returned value will be {@code null}, which is the default.
   * </p>
   * 
   * @return an {@link Activity} {@code Intent} ; may be {@code null}
   * @see #setHomeIntent(Intent)
   */
  private Intent getHomeIntent()
  {
    return homeIntent;
  }

  /**
   * Indicates the {@link Activity} {@link Intent} to be launched when the {@link android.app.ActionBar} "Home" button is hit.
   * 
   * <p>
   * Caution: this method is only effective from Android v3+!
   * </p>
   * 
   * @param intent
   *          a valid {@code Intent}
   * @see #getHomeIntent()
   * @see #onOptionsItemSelected(MenuItem)
   */
  public void setHomeIntent(Intent intent)
  {
    this.homeIntent = intent;
  }

  /**
   * This method should not trigger any exception!
   */
  private boolean onRetrieveBusinessObjectsInternal(boolean retrieveBusinessObjects)
  {
    onBeforeRefreshBusinessObjectsAndDisplay();
    if (retrieveBusinessObjects == true)
    {
      try
      {
        onRetrieveBusinessObjects();
        // We notify the entity that the business objects have actually been loaded
        if (isAlive() == false)
        {
          // If the entity is no more alive, we give up the process
          return false;
        }
        onBusinessObjectsRetrieved();
      }
      catch (Throwable throwable)
      {
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
        onInternalBusinessObjectAvailableException(throwable);
        return false;
      }
    }
    stateContainer.setBusinessObjectsRetrieved();
    return true;
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

  private void businessObjectRetrievalAndResultHandlers()
  {
    smartable.refreshBusinessObjectsAndDisplay(stateContainer.isRetrieveBusinessObjects(), stateContainer.getRetrieveBusinessObjectsOver(), true);
    if (stateContainer.actionResultsRetrieved == false)
    {
      onRegisterResultHandlers(stateContainer.compositeActivityResultHandler);
      stateContainer.actionResultsRetrieved = true;
    }
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

  private void onRegisterResultHandlers(CompositeHandler compositeActivityResultHandler)
  {
    // THINK: should we plug the feature?
  }

}
