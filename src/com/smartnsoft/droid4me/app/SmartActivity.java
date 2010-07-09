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

import java.util.ArrayList;
import java.util.List;

import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.Commands;
import com.smartnsoft.droid4me.framework.Events;
import com.smartnsoft.droid4me.framework.LifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.framework.Events.OnCompletion;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

/**
 * The basis class for all activities available in the framework.
 * 
 * @author Édouard Mercier
 * @since 2008.04.11
 */
// TODO: think of using the onRetainNonConfigurationInstance/getLastNonConfigurationInstance() when the screen orientation changes.
public abstract class SmartActivity
    extends Activity
    implements LifeCycle.ForActivity, AppPublics.LifeCyclePublic, AppInternals.LifeCycleInternals
{

  protected static final Logger log = LoggerFactory.getInstance(SmartActivity.class);

  final static String ALREADY_STARTED = "com.smartnsoft.droid4me.alreadyStarted";

  public static String TRACE_PREFIX_FILE_PATH = "/sdcard/trace";

  public static boolean TRACING_ON = false;

  public static int traceIndex = 1;

  static boolean isTracing;

  public void onBusinessObjectsRetrieved()
  {
  }

  private AppInternals.StateContainer stateContainer = new AppInternals.StateContainer();

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

  public final boolean isBeingRedirected()
  {
    return stateContainer.beingRedirected;
  }

  public final Handler getHandler()
  {
    return stateContainer.handler;
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  }

  public final void onException(final Throwable throwable, boolean fromGuiThread)
  {
    // Eventually, we do not ensure that the handling of the exception will be performed in a specific thread
    // if (fromGuiThread == true)
    {
      ActivityController.getInstance().handleException(this, throwable);
    }
    // else
    // {
    // runOnUiThread(new Runnable()
    // {
    // public void run()
    // {
    // ActivityController.getInstance().handleException(SmartActivity.this, throwable);
    // }
    // });
    // }
  }

  private final void onInternalCreate(Bundle savedInstanceState)
  {
    stateContainer.create(getApplicationContext());
  }

  private final void onInternalDestroy()
  {
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
      log.debug("SmartActivity::onCreate");
    }
    super.onCreate(savedInstanceState);
    if (ActivityController.getInstance().needsRedirection(this) == true)
    {
      // We stop here if a redirection is needed
      stateContainer.beingRedirected = true;
      return;
    }
    else
    {
      ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onCreate);
    }

    if (savedInstanceState != null && savedInstanceState.containsKey(SmartActivity.ALREADY_STARTED) == true)
    {
      stateContainer.firstLifeCycle = false;
    }
    else
    {
      stateContainer.firstLifeCycle = true;
      onActuallyCreated();
      ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onActuallyCreatedDone);
    }
    stateContainer.registerBroadcastListener(this);

    onInternalCreate(savedInstanceState);
    onBeforeRetrievingDisplayObjects();
    onRetrieveDisplayObjects();
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

  static MenuHandler.Static createStaticActionHandler(final Context context)
  {
    final MenuHandler.Static staticActionHandler = new MenuHandler.Static()
    {
      @Override
      protected List<MenuCommand<Void>> retrieveCommands()
      {
        final List<MenuCommand<Void>> actions = new ArrayList<MenuCommand<Void>>();
        if (SmartActivity.TRACING_ON == true)
        {
          actions.add(new MenuCommand<Void>("Start Tracing", ' ', ' ', android.R.drawable.ic_menu_more, new Commands.StaticExecutable()
          {
            public void run()
            {
              Toast.makeText(context, "Starting the traceview recording for series " + SmartActivity.traceIndex, Toast.LENGTH_SHORT).show();
              Debug.startMethodTracing(TRACE_PREFIX_FILE_PATH + SmartActivity.traceIndex);
              SmartActivity.isTracing = true;
            }

            public boolean isEnabled()
            {
              return !SmartActivity.isTracing;
            }
          }));
          actions.add(new MenuCommand<Void>("Stop tracing", ' ', ' ', android.R.drawable.ic_menu_more, new Commands.StaticExecutable()
          {
            public void run()
            {
              Debug.stopMethodTracing();
              Toast.makeText(context, "Traceview recording for series " + SmartActivity.traceIndex + " over", Toast.LENGTH_SHORT).show();
              SmartActivity.isTracing = false;
              SmartActivity.traceIndex++;
            }

            public boolean isEnabled()
            {
              return SmartActivity.isTracing;
            }
          }));
        }
        return actions;
      }
    };
    return staticActionHandler;
  }

  @Override
  protected void onResume()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onResume");
    }
    super.onResume();
    stateContainer.doNotCallOnActivityDestroyed = false;
    if (isBeingRedirected() == true)
    {
      return;
    }
    try
    {
      businessObjectRetrievalAndResultHandlers();
    }
    catch (Throwable throwable)
    {
      onInternalHandleOtherException(throwable);
      return;
    }
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

  private void onInternalHandleOtherException(Throwable throwable)
  {
    if (log.isErrorEnabled())
    {
      log.error("Unhandled problem", throwable);
    }
    onException(throwable, true);
  }

  private void businessObjectRetrievalAndResultHandlers()
  {
    if (stateContainer.businessObjectsRetrieved == false)
    {
      refreshBusinessObjectsAndDisplay(!stateContainer.businessObjectsRetrieved);
    }
    if (stateContainer.actionResultsRetrieved == false)
    {
      onRegisterResultHandlers(stateContainer.compositeActivityResultHandler);
      stateContainer.actionResultsRetrieved = true;
    }
  }

  /**
   * Must be invoked only from the GUI thread, when the {@link SmartActivity} is synchronous!
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, OnCompletion)
   */
  public final void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null);
  }

  /**
   * Must be invoked only from the GUI thread, when the {@link SmartActivity} is synchronous!
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, OnCompletion)
   */
  public final void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects)
  {
    refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, null);
  }

  /**
   * Must be invoked only from the GUI thread, when the {@link SmartActivity} is synchronous!
   */
  public final void refreshBusinessObjectsAndDisplay(final boolean retrieveBusinessObjects, final Events.OnCompletion onRefreshBusinessObjectsAndDisplay)
  {
    // We can safely retrieve the business objects
    if (!(this instanceof LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy))
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
          onInternalBusinessObjectAvailableException(throwable);
          return;
        }
      }
      stateContainer.businessObjectsRetrieved = true;
      if (stateContainer.resumedForTheFirstTime == true)
      {
        onFulfillDisplayObjects();
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
      }
      onSynchronizeDisplayObjects();
      stateContainer.onStopLoading(this);
      ActivityController.getInstance().onLifeCycleEvent(SmartActivity.this, ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
      stateContainer.resumedForTheFirstTime = false;
      if (onRefreshBusinessObjectsAndDisplay != null)
      {
        onRefreshBusinessObjectsAndDisplay.done();
      }
    }
    else
    {
      // We invoke the method in the GUI thread
      runOnUiThread(new Runnable()
      {
        public void run()
        {
          onBeforeRefreshBusinessObjectsAndDisplay();

          // We call that method asynchronously in a specific thread
          AppPublics.THREAD_POOL.execute(SmartActivity.this, new Runnable()
          {
            public void run()
            {
              if (retrieveBusinessObjects == true)
              {
                try
                {
                  onRetrieveBusinessObjects();
                }
                catch (final Throwable throwable)
                {
                  onInternalBusinessObjectAvailableException(throwable);
                  return;
                }
              }
              stateContainer.businessObjectsRetrieved = true;
              // We are handling the UI, and we need to make sure that this is done through the GUI thread
              runOnUiThread(new Runnable()
              {
                public void run()
                {
                  if (stateContainer.resumedForTheFirstTime == true)
                  {
                    onFulfillDisplayObjects();
                    ActivityController.getInstance().onLifeCycleEvent(SmartActivity.this,
                        ActivityController.Interceptor.InterceptorEvent.onFulfillDisplayObjectsDone);
                  }
                  onSynchronizeDisplayObjects();
                  stateContainer.onStopLoading(SmartActivity.this);
                  ActivityController.getInstance().onLifeCycleEvent(SmartActivity.this,
                      ActivityController.Interceptor.InterceptorEvent.onSynchronizeDisplayObjectsDone);
                  stateContainer.resumedForTheFirstTime = false;
                  if (onRefreshBusinessObjectsAndDisplay != null)
                  {
                    onRefreshBusinessObjectsAndDisplay.done();
                  }
                }
              });
            }
          });
        }
      });
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onSaveInstanceState");
    }
    super.onSaveInstanceState(outState);
    stateContainer.doNotCallOnActivityDestroyed = true;
    outState.putBoolean(SmartActivity.ALREADY_STARTED, true);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onRestoreInstanceState");
    }
    super.onRestoreInstanceState(savedInstanceState);
    try
    {
      businessObjectRetrievalAndResultHandlers();
    }
    catch (Throwable throwable)
    {
      onInternalHandleOtherException(throwable);
      return;
    }
  }

  @Override
  protected void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartActivity::onStart");
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
      log.debug("SmartActivity::onPause");
    }
    try
    {
      if (isBeingRedirected() == true)
      {
        // We stop here if a redirection is needed
        return;
      }
      else
      {
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onPause);
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
      log.debug("SmartActivity::onStop");
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
      log.debug("SmartActivity::onDestroy");
    }
    try
    {
      if (isBeingRedirected() == true)
      {
        // We stop here if a redirection is needed
        return;
      }
      onInternalDestroy();
      if (stateContainer.doNotCallOnActivityDestroyed == false)
      {
        onActuallyDestroyed();
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onActuallyDestroyedDone);
      }
      else
      {
        ActivityController.getInstance().onLifeCycleEvent(this, ActivityController.Interceptor.InterceptorEvent.onDestroy);
      }
      stateContainer.unregisterBroadcastListener(this);
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

    stateContainer.compositeActionHandler.onCreateOptionsMenu(this, menu);
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

    stateContainer.compositeActionHandler.onPrepareOptionsMenu(menu);
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
      log.debug("SmartActivity::onContextItemSelected");
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
      log.debug("SmartActivity::onActivityResult");
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

}
