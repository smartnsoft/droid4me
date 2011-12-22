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

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
 * A basis class for an activity that holds some tabs.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2009.04.14
 */
public abstract class SmartTabActivity<AggregateClass>
    extends TabActivity
    implements SmartableActivity<AggregateClass>
{

  protected final static Logger log = LoggerFactory.getInstance("SmartableActivity");

  public void onBusinessObjectsRetrieved()
  {
  }

  /**
   * ------------------- Beginning of "Copied from the SmartActivity class" -------------------
   */

  private AppInternals.StateContainer<AggregateClass> stateContainer = new AppInternals.StateContainer<AggregateClass>();

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
    return stateContainer.handler;
  }

  public final AggregateClass getAggregate()
  {
    return stateContainer.aggregate;
  }

  public final void setAggregate(AggregateClass aggregate)
  {
    stateContainer.aggregate = aggregate;
  }

  public final void registerBroadcastListeners(AppPublics.BroadcastListener[] broadcastListeners)
  {
    stateContainer.registerBroadcastListeners(this, broadcastListeners);
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  }

  public final void onException(Throwable throwable, boolean fromGuiThread)
  {
    ActivityController.getInstance().handleException(this, throwable);
  }

  protected void onBeforeRetrievingDisplayObjects()
  {
  }

  /**
   * It is ensured that this method will be invoked from the GUI thread.
   */
  protected void onBeforeRefreshBusinessObjectsAndDisplay()
  {
    stateContainer.onStartLoading(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onCreate");
    }
    ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onSuperCreateBefore);
    super.onCreate(savedInstanceState);
    if (ActivityController.getInstance().needsRedirection(this) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected();
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(AppInternals.ALREADY_STARTED) == true)
    {
      stateContainer.firstLifeCycle = false;
    }
    else
    {
      stateContainer.firstLifeCycle = true;
      onActuallyCreated();
      ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onActuallyCreatedDone);
    }
    stateContainer.registerBroadcastListeners(this, this);

    stateContainer.create(getApplicationContext());
    onBeforeRetrievingDisplayObjects();
    // ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onRetrieveDisplayObjectsBefore);
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
        final ArrayList<MenuCommand<Void>> menuCommands = new ArrayList<MenuCommand<Void>>(staticMenuCommands.size());
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
      log.debug("SmartTabActivity::onNewIntent");
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
    ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onContentChanged);
  }

  @Override
  protected void onResume()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onResume");
    }
    super.onResume();
    stateContainer.doNotCallOnActivityDestroyed = false;
    if (shouldKeepOn() == false)
    {
      return;
    }
    ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onResume);
    stateContainer.onResume(this);
    businessObjectRetrievalAndResultHandlers();
  }

  private final void onInternalBusinessObjectAvailableException(Throwable throwable)
  {
    if (log.isErrorEnabled())
    {
      log.error("Cannot retrieve the business objects", throwable);
    }
    stateContainer.onStopLoading(this);
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
    if (stateContainer.shouldDelayRefreshBusinessObjectsAndDisplay(this, retrieveBusinessObjects, onOver, immediately) == true)
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
      AppInternals.execute(SmartTabActivity.this, new Runnable()
      {
        public void run()
        {
          if (onRetrieveBusinessObjectsInternal(retrieveBusinessObjects) == false)
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
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this, this);
        onInternalBusinessObjectAvailableException(throwable);
        return false;
      }
    }
    stateContainer.setBusinessObjectsRetrieved();
    return true;
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
        stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this, this);
        onException(throwable, true);
        stateContainer.onStopLoading(this);
        return;
      }
      ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
    }
    try
    {
      stateContainer.onSynchronizeDisplayObjects();
      onSynchronizeDisplayObjects();
    }
    catch (Throwable throwable)
    {
      stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this, this);
      onException(throwable, true);
      return;
    }
    finally
    {
      stateContainer.onStopLoading(this);
    }
    ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
    stateContainer.resumedForTheFirstTime = false;
    if (onOver != null)
    {
      onOver.run();
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this, this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onSaveInstanceState");
    }
    super.onSaveInstanceState(outState);
    stateContainer.doNotCallOnActivityDestroyed = true;
    outState.putBoolean(AppInternals.ALREADY_STARTED, true);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onRestoreInstanceState");
    }
    super.onRestoreInstanceState(savedInstanceState);
    businessObjectRetrievalAndResultHandlers();
  }

  @Override
  protected void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onStart");
    }
    super.onStart();
    ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onStart);
    stateContainer.onStart(this);
  }

  @Override
  protected void onPause()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onPause");
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
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onPause);
        stateContainer.onPause(this);
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
      log.debug("SmartTabActivity::onStop");
    }
    try
    {
      ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onStop);
      stateContainer.onStop(this);
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
      log.debug("SmartTabActivity::onDestroy");
    }
    try
    {
      if (shouldKeepOn() == false)
      {
        // We stop here if a redirection is needed or is something went wrong
        return;
      }
      if (stateContainer.doNotCallOnActivityDestroyed == false)
      {
        onActuallyDestroyed();
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onActuallyDestroyedDone);
      }
      else
      {
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onDestroy);
      }
      stateContainer.unregisterBroadcastListeners(this);
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
      log.debug("SmartTabActivity::onCreateOptionsMenu");
    }
    boolean result = super.onCreateOptionsMenu(menu);

    stateContainer.compositeActionHandler.onCreateOptionsMenu(this, menu);
    return result;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onPrepareOptionsMenu");
    }
    boolean result = super.onPrepareOptionsMenu(menu);

    stateContainer.compositeActionHandler.onPrepareOptionsMenu(menu);
    return result;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onOptionsItemSelected");
    }
    boolean result = super.onOptionsItemSelected(item);

    if (stateContainer.compositeActionHandler.onOptionsItemSelected(item) == true)
    {
      return true;
    }
    return result;
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartTabActivity::onContextItemSelected");
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
      log.debug("SmartTabActivity::onActivityResult");
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

  protected SharedPreferences getPreferences()
  {
    return stateContainer.getPreferences(getApplicationContext());
  }

  /**
   * ------------------- End of "Copied from the SmartActivity class" -------------------
   */

}
