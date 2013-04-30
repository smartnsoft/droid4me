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

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * The basis class for all activities available in the framework.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2008.04.11
 */
// TODO: think of using the onRetainNonConfigurationInstance/getLastNonConfigurationInstance() when the screen orientation changes.
public abstract class SmartActivity<AggregateClass>
    extends Activity
    implements SmartableActivity<AggregateClass>
{

  private final Droid4mizer<AggregateClass, SmartActivity<AggregateClass>> droid4mizer = new Droid4mizer<AggregateClass, SmartActivity<AggregateClass>>(this, this, this, null);

  @Override
  public Object getSystemService(String name)
  {
    return droid4mizer.getSystemService(name, super.getSystemService(name));
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      public void run()
      {
        SmartActivity.super.onCreate(savedInstanceState);
      }
    }, savedInstanceState);
  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    droid4mizer.onNewIntent(intent);
  }

  @Override
  public void onContentChanged()
  {
    super.onContentChanged();
    droid4mizer.onContentChanged();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    droid4mizer.onResume();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    droid4mizer.onSaveInstanceState(outState);
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    droid4mizer.onStart();
  }

  @Override
  protected void onPause()
  {
    try
    {
      droid4mizer.onPause();
    }
    finally
    {
      super.onPause();
    }
  }

  @Override
  protected void onStop()
  {
    try
    {
      droid4mizer.onStop();
    }
    finally
    {
      super.onStop();
    }
  }

  @Override
  protected void onDestroy()
  {
    try
    {
      droid4mizer.onDestroy();
    }
    finally
    {
      super.onDestroy();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu)
  {
    // Taken from http://www.londatiga.net/it/android-coding-tips-how-to-create-options-menu-on-child-activity-inside-an-activitygroup/
    return droid4mizer.onCreateOptionsMenu(getParent() == null ? super.onCreateOptionsMenu(menu) : true, menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    return droid4mizer.onPrepareOptionsMenu(getParent() == null ? super.onPrepareOptionsMenu(menu) : true, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    return droid4mizer.onOptionsItemSelected(getParent() == null ? super.onOptionsItemSelected(item) : true, item);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    return droid4mizer.onContextItemSelected(getParent() == null ? super.onContextItemSelected(item) : true, item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    droid4mizer.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * SmartableActivity implementation.
   */

  public final void setHomeIntent(Intent intent)
  {
    droid4mizer.setHomeIntent(intent);
  }

  /**
   * Smartable implementation.
   */

  public AggregateClass getAggregate()
  {
    return droid4mizer.getAggregate();
  }

  public void setAggregate(AggregateClass aggregate)
  {
    droid4mizer.setAggregate(aggregate);
  }

  public Handler getHandler()
  {
    return droid4mizer.getHandler();
  }

  public SharedPreferences getPreferences()
  {
    return droid4mizer.getPreferences();
  }

  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    droid4mizer.onException(throwable, fromGuiThread);
  }

  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    droid4mizer.registerBroadcastListeners(broadcastListeners);
  }

  public int getOnSynchronizeDisplayObjectsCount()
  {
    return droid4mizer.getOnSynchronizeDisplayObjectsCount();
  }

  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return droid4mizer.isRefreshingBusinessObjectsAndDisplay();
  }

  public boolean isFirstLifeCycle()
  {
    return droid4mizer.isFirstLifeCycle();
  }

  public final boolean isInteracting()
  {
    return droid4mizer.isInteracting();
  }

  public final boolean isAlive()
  {
    return droid4mizer.isAlive();
  }

  public final void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately)
  {
    refreshBusinessObjectsAndDisplayInternal(retrieveBusinessObjects, onOver, immediately, false);
  }

  /**
   * AppInternals.LifeCycleInternals implementation.
   */

  public boolean shouldKeepOn()
  {
    return droid4mizer.shouldKeepOn();
  }

  public Composite getCompositeActionHandler()
  {
    return droid4mizer.getCompositeActionHandler();
  }

  public CompositeHandler getCompositeActivityResultHandler()
  {
    return droid4mizer.getCompositeActivityResultHandler();
  }

  /**
   * Own implementation.
   */

  public void onBusinessObjectsRetrieved()
  {
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  };

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(true, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null, false);
  }

  void refreshBusinessObjectsAndDisplayInternal(final boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately,
      final boolean businessObjectCountAndSortingUnchanged)
  {
    droid4mizer.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately);
  }

}
