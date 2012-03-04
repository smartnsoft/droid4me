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
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * Makes the Android {@link PreferenceActivity} compatible with the framework.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2010.09.23
 */
public abstract class SmartPreferenceActivity<AggregateClass>
    extends PreferenceActivity
    implements Droid4mizerInterface, Smartable<AggregateClass>
{

  protected static final Logger log = LoggerFactory.getInstance("Smartable");

  /**
   * Does nothing, but we can overload it in derived classes.
   */
  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
  }

  /**
   * Does nothing, but we can overload it in derived classes.
   */
  public void onFulfillDisplayObjects()
  {
  }

  public void onBusinessObjectsRetrieved()
  {
  }

  /**
   * ------------------- Beginning of "Copied from the SmartActivity class" -------------------
   */

  private final AppInternals.StateContainer<AggregateClass, Activity> stateContainer = new AppInternals.StateContainer<AggregateClass, Activity>(this, this);

  public void onActuallyCreated()
  {
  }

  public void onActuallyDestroyed()
  {
  }

  public final boolean isFirstLifeCycle()
  {
    return stateContainer.isFirstLifeCycle();
  }

  public final boolean isInteracting()
  {
    return stateContainer.isInteracting();
  }

  public final int getOnSynchronizeDisplayObjectsCount()
  {
    return stateContainer.getOnSynchronizeDisplayObjectsCount();
  }

  public final boolean shouldKeepOn()
  {
    return stateContainer.shouldKeepOn();
  }

  public final Handler getHandler()
  {
    return stateContainer.getHandler();
  }

  public SharedPreferences getPreferences()
  {
    return stateContainer.getPreferences(getApplicationContext());
  }

  public final AggregateClass getAggregate()
  {
    return stateContainer.getAggregate();
  }

  public final void setAggregate(AggregateClass aggregate)
  {
    stateContainer.setAggregate(aggregate);
  }

  public final void registerBroadcastListeners(AppPublics.BroadcastListener[] broadcastListeners)
  {
    stateContainer.registerBroadcastListeners(broadcastListeners);
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  }

  public final void onException(Throwable throwable, boolean fromGuiThread)
  {
    ActivityController.getInstance().handleException(this, null, throwable);
  }

  /**
   * It is ensured that this method will be invoked from the GUI thread.
   */
  protected void onBeforeRefreshBusinessObjectsAndDisplay()
  {
    stateContainer.onStartLoading();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onCreate");
    }
    ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore);
    super.onCreate(savedInstanceState);
    if (ActivityController.getInstance().needsRedirection(this) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(AppInternals.ALREADY_STARTED) == true)
    {
      stateContainer.setFirstLifeCycle(false);
    }
    else
    {
      stateContainer.setFirstLifeCycle(true);
      onActuallyCreated();
      ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onActuallyCreatedDone);
    }
    stateContainer.registerBroadcastListeners();

    stateContainer.initialize();
    // ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsBefore);
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
    // ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsAfter);
    // We add the static menu commands
    getCompositeActionHandler().add(new MenuHandler.Static()
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

  @Override
  protected void onNewIntent(Intent intent)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onNewIntent");
    }
    super.onNewIntent(intent);

    if (ActivityController.getInstance().needsRedirection(this) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
    }
  }

  @Override
  public void onContentChanged()
  {
    super.onContentChanged();
    if (stateContainer.shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onContentChanged);
  }

  @Override
  protected void onResume()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onResume");
    }
    super.onResume();
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onResume);
    stateContainer.onResume();
    businessObjectRetrievalAndResultHandlers();
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
    // We need to invoke that method on the GUI thread, because that method may have been triggered from another thread
    onException(throwable, false);
  }

  private void businessObjectRetrievalAndResultHandlers()
  {
    refreshBusinessObjectsAndDisplay(stateContainer.isRetrieveBusinessObjects(), stateContainer.getRetrieveBusinessObjectsOver(), true);
    if (stateContainer.actionResultsRetrieved == false)
    {
      onRegisterResultHandlers(stateContainer.compositeActivityResultHandler);
      stateContainer.actionResultsRetrieved = true;
    }
  }

  protected final void onBeforeRetrievingBusinessObjectsOver(boolean retrieveBusinessObjects)
  {
    refreshBusinessObjectsAndDisplay(retrieveBusinessObjects);
  }

  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return stateContainer.isRefreshingBusinessObjectsAndDisplay();
  }

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(true, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay()
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

  public final void refreshBusinessObjectsAndDisplay(final boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately)
  {
    refreshBusinessObjectsAndDisplayInternal(retrieveBusinessObjects, onOver, immediately, false);
  }

  void refreshBusinessObjectsAndDisplayInternal(final boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately,
      final boolean businessObjectCountAndSortingUnchanged)
  {
    if (stateContainer.shouldDelayRefreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately) == true)
    {
      return;
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStart();
    // We can safely retrieve the business objects
    if (!(this instanceof LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy))
    {
      if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
      {
        return;
      }
      onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
    }
    else
    {
      // We call that routine asynchronously in a background thread
      stateContainer.execute(this, null, new Runnable()
      {
        public void run()
        {
          if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
          {
            return;
          }
          // If the activity has been finished in the meantime, no need to update the UI
          if (isFinishing() == true)
          {
            return;
          }
          // We are handling the UI, and we need to make sure that this is done through the GUI thread
          runOnUiThread(new Runnable()
          {
            public void run()
            {
              onFulfillAndSynchronizeDisplayObjectsInternal(onOver);
            }
          });
        }
      });
    }
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
        onException(throwable, true);
        stateContainer.onStopLoading();
        return;
      }
      ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
    }
    try
    {
      stateContainer.onSynchronizeDisplayObjects();
      onSynchronizeDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
      onException(throwable, true);
      return;
    }
    finally
    {
      stateContainer.onStopLoading();
    }
    ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
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

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onSaveInstanceState");
    }
    super.onSaveInstanceState(outState);
    stateContainer.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onRestoreInstanceState");
    }
    super.onRestoreInstanceState(savedInstanceState);
    businessObjectRetrievalAndResultHandlers();
  }

  @Override
  protected void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onStart");
    }
    super.onStart();
    ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onStart);
    stateContainer.onStart();
  }

  @Override
  protected void onPause()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onPause");
    }
    try
    {
      if (shouldKeepOn() == false)
      {
        // We stop here if a redirection is needed or is something went wrong
        return;
      }
      else
      {
        ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onPause);
        stateContainer.onPause();
      }
    }
    finally
    {
      super.onPause();
    }
  }

  @Override
  protected void onStop()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onStop");
    }
    try
    {
      ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onStop);
      stateContainer.onStop();
    }
    finally
    {
      super.onStop();
    }
  }

  @Override
  protected void onDestroy()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onDestroy");
    }
    try
    {
      stateContainer.onDestroy();
      if (shouldKeepOn() == false)
      {
        // We stop here if a redirection is needed or is something went wrong
        return;
      }
      if (stateContainer.isDoNotCallOnActivityDestroyed() == false)
      {
        onActuallyDestroyed();
        ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onActuallyDestroyedDone);
      }
      else
      {
        ActivityController.getInstance().onLifeCycleEvent(this, null, ActivityController.Interceptor.InterceptorEvent.onDestroy);
      }
    }
    finally
    {
      super.onDestroy();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onCreateOptionsMenu");
    }
    boolean result = super.onCreateOptionsMenu(menu);

    if (stateContainer.compositeActionHandler != null)
    {
      stateContainer.compositeActionHandler.onCreateOptionsMenu(this, menu);
    }
    else
    {
      if (log.isErrorEnabled())
      {
        log.error("onCreateOptionsMenu() being called whereas the 'stateContainer.compositeActionHandler' has not yet been initialized!");
      }
    }
    return result;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onPrepareOptionsMenu");
    }
    boolean result = super.onPrepareOptionsMenu(menu);

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
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onOptionsItemSelected");
    }
    boolean result = super.onOptionsItemSelected(item);

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
    return result;
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onContextItemSelected");
    }
    boolean result = super.onContextItemSelected(item);

    if (stateContainer.compositeActionHandler.onContextItemSelected(item) == true)
    {
      return true;
    }
    return result;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartPreferenceActivity::onActivityResult");
    }
    super.onActivityResult(requestCode, resultCode, data);

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

    getCompositeActivityResultHandler().handle(requestCode, resultCode, data);
  }

  protected void onRegisterResultHandlers(ActivityResultHandler.Handler resultHandler)
  {
  }

  /**
   * Does nothing, but we can overload it in derived classes.
   */
  public void onSynchronizeDisplayObjects()
  {
  }

  public MenuHandler.Composite getCompositeActionHandler()
  {
    return stateContainer.compositeActionHandler;
  }

  public CompositeHandler getCompositeActivityResultHandler()
  {
    return stateContainer.compositeActivityResultHandler;
  }

  /**
   * ------------------- End of "Copied from the SmartActivity class" -------------------
   */

}
