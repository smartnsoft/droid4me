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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;

/**
 * The class that should be used when extending a legacy class to support the whole droid4me framework features.
 * 
 * @author Édouard Mercier
 * @since 2011.06.14
 */
public final class Droid4mizer<AggregateClass>
    implements SmartableActivity<AggregateClass>, Droid4mizerInterface
{

  private static final Logger log = LoggerFactory.getInstance(Droid4mizer.class);

  private final Activity activity;

  private final SmartableActivity<AggregateClass> smartableActivity;

  private final Droid4mizerInterface droid4mizerInterface;

  private final AppInternals.StateContainer<AggregateClass> stateContainer = new AppInternals.StateContainer<AggregateClass>();

  public Droid4mizer(Activity activity, SmartableActivity<AggregateClass> smartableActivity, Droid4mizerInterface droid4mizerInterface)
  {
    this.activity = activity;
    this.smartableActivity = smartableActivity;
    this.droid4mizerInterface = droid4mizerInterface;
  }

  public AggregateClass getAggregate()
  {
    return stateContainer.aggregate;
  }

  public Handler getHandler()
  {
    return stateContainer.handler;
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  }

  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    ActivityController.getInstance().handleException(activity, throwable);
  }

  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    stateContainer.registerBroadcastListeners(activity, broadcastListeners);
  }

  public void setAggregate(AggregateClass aggregate)
  {
    stateContainer.aggregate = aggregate;
  }

  public void onBusinessObjectsRetrieved()
  {
  }

  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return stateContainer.isRefreshingBusinessObjectsAndDisplay();
  }

  public void refreshBusinessObjectsAndDisplay(final boolean retrieveBusinessObjects, final Runnable onOver)
  {
    stateContainer.onRefreshingBusinessObjectsAndDisplayStart();
    // We can safely retrieve the business objects
    if (!(activity instanceof LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy))
    {
      if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
      {
        return;
      }
      onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
    }
    else
    {
      // We invoke the method in the GUI thread
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          // We call that method asynchronously in a specific thread
          AppPublics.THREAD_POOL.execute(activity, new Runnable()
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
                  onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
                }
              });
            }
          });
        }
      });
    }
  }

  public final int getOnSynchronizeDisplayObjectsCount()
  {
    return stateContainer.getOnSynchronizeDisplayObjectsCount();
  }

  public final boolean isFirstLifeCycle()
  {
    return stateContainer.isFirstLifeCycle();
  }

  public void onActuallyCreated()
  {
  }

  public void onActuallyDestroyed()
  {
  }

  public boolean shouldKeepOn()
  {
    return stateContainer.shouldKeepOn();
  }

  /*
   * The {@link Activity} methods.
   */

  public void onCreate(Runnable superMethod, Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreate");
    }

    // TO COME
    // AppPublics.Aggregator aggregator = onRetrieveAggregator();
    // if (aggregator == null)
    // {
    // aggregator = new AppPublics.Aggregator(this);
    // }

    ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore);
    superMethod.run();
    if (ActivityController.getInstance().needsRedirection(activity) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(AppInternals.ALREADY_STARTED) == true)
    {
      stateContainer.firstLifeCycle = false;
    }
    else
    {
      stateContainer.firstLifeCycle = true;
      onActuallyCreated();
      ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onActuallyCreatedDone);
    }
    stateContainer.registerBroadcastListeners(activity);

    stateContainer.create(activity);
    droid4mizerInterface.onBeforeRetrievingDisplayObjects();
    // ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsBefore);
    // TO COME
    // aggregator.onRetrieveDisplayObjects();
    try
    {
      onRetrieveDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.stopHandling();
      onException(throwable, true);
      return;
    }
    // ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsAfter);
    // We add the static menu commands
    droid4mizerInterface.getCompositeActionHandler().add(new MenuHandler.Static()
    {
      @Override
      protected List<MenuCommand<Void>> retrieveCommands()
      {
        final List<StaticMenuCommand> staticMenuCommands = getMenuCommands();
        if (staticMenuCommands == null)
        {
          return null;
        }
        final ArrayList<MenuCommand<Void>> menuCommands = new ArrayList<MenuCommand<Void>>(staticMenuCommands.size());
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
    if (stateContainer.shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onContentChanged);
  }

  public void onResume()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onResume");
    }
    stateContainer.doNotCallOnActivityDestroyed = false;
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onResume);
    businessObjectRetrievalAndResultHandlers();
  }

  public void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onSaveInstanceState");
    }
    stateContainer.doNotCallOnActivityDestroyed = true;
    outState.putBoolean(AppInternals.ALREADY_STARTED, true);
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
    ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onStart);
    stateContainer.onStart(activity);
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
      ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onPause);
    }
  }

  public void onStop()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onStop");
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onStop);
    stateContainer.onStop(activity);
  }

  public void onDestroy()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onDestroy");
    }
    if (shouldKeepOn() == false)
    {
      // We stop here if a redirection is needed or is something went wrong
      return;
    }
    if (stateContainer.doNotCallOnActivityDestroyed == false)
    {
      onActuallyDestroyed();
      ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onActuallyDestroyedDone);
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onDestroy);
    }
    stateContainer.unregisterBroadcastListeners(activity);
  }

  public boolean onCreateOptionsMenu(boolean superResult, Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onCreateOptionsMenu");
    }
    stateContainer.compositeActionHandler.onCreateOptionsMenu(activity, menu);
    return superResult;
  }

  public boolean onPrepareOptionsMenu(boolean superResult, Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onPrepareOptionsMenu");
    }
    stateContainer.compositeActionHandler.onPrepareOptionsMenu(menu);
    return superResult;
  }

  public boolean onOptionsItemSelected(boolean superResult, MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Droid4mizer::onOptionsItemSelected");
    }
    if (stateContainer.compositeActionHandler.onOptionsItemSelected(item) == true)
    {
      return true;
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

    droid4mizerInterface.getCompositeActivityResultHandler().handle(requestCode, resultCode, data);
  }

  /*
   * The Droid4mizerInterface interface implementation.
   */

  public Composite getCompositeActionHandler()
  {
    return stateContainer.compositeActionHandler;
  }

  public void onBeforeRetrievingDisplayObjects()
  {
  }

  public CompositeHandler getCompositeActivityResultHandler()
  {
    return stateContainer.compositeActivityResultHandler;
  }

  /*
   * The LifeCycle interface implementation.
   */

  public void onFulfillDisplayObjects()
  {
    smartableActivity.onFulfillDisplayObjects();
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    smartableActivity.onRetrieveBusinessObjects();
  }

  public void onRetrieveDisplayObjects()
  {
    smartableActivity.onRetrieveDisplayObjects();
  }

  public void onSynchronizeDisplayObjects()
  {
    smartableActivity.onSynchronizeDisplayObjects();
  }

  /*
   * The specific methods.
   */

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
      }
      catch (Throwable throwable)
      {
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop();
        onInternalBusinessObjectAvailableException(throwable);
        return false;
      }
    }
    stateContainer.businessObjectsRetrieved = true;
    return true;
  }

  private void onBeforeRefreshBusinessObjectsAndDisplay()
  {
    // THINK: should we plug the feature?
  }

  private void onFulfillAndSynchronizeDisplayObjectsInternal(Runnable onOver)
  {
    if (stateContainer.resumedForTheFirstTime == true)
    {
      try
      {
        onFulfillDisplayObjects();
      }
      catch (Throwable throwable)
      {
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop();
        onException(throwable, true);
        stateContainer.onStopLoading(activity);
        return;
      }
      ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
    }
    try
    {
      stateContainer.onSynchronizeDisplayObjects();
      onSynchronizeDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.onRefreshingBusinessObjectsAndDisplayStop();
      onException(throwable, true);
      return;
    }
    finally
    {
      stateContainer.onStopLoading(activity);
    }
    ActivityController.getInstance().onLifeCycleEvent(activity, ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
    stateContainer.resumedForTheFirstTime = false;
    if (onOver != null)
    {
      onOver.run();
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStop();
  }

  private void businessObjectRetrievalAndResultHandlers()
  {
    smartableActivity.refreshBusinessObjectsAndDisplay(!stateContainer.businessObjectsRetrieved, null);
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
    stateContainer.onStopLoading(activity);
    // We need to invoke that method on the GUI thread, because that method may have been triggered from another thread
    onException(throwable, false);
  }

  private void onRegisterResultHandlers(CompositeHandler compositeActivityResultHandler)
  {
    // THINK: should we plug the feature?
  }

  public SharedPreferences getPreferences()
  {
    return stateContainer.getPreferences(activity);
  }

}
