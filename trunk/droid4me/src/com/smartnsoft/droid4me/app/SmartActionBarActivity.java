package com.smartnsoft.droid4me.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.app.Droid4mizer;
import com.smartnsoft.droid4me.app.SmartableActivity;

/**
 * @author Jocelyn Girard
 * @since 2014.10.29
 */
public abstract class SmartActionBarActivity<AggregateClass>
    extends ActionBarActivity
    implements SmartableActivity<AggregateClass>
{
  private final Droid4mizer<AggregateClass, SmartActionBarActivity<AggregateClass>> droid4mizer = new Droid4mizer<AggregateClass, SmartActionBarActivity<AggregateClass>>(this, this, this, null);

  @Override
  public LayoutInflater getLayoutInflater()
  {
    return (LayoutInflater) droid4mizer.getSystemService(Context.LAYOUT_INFLATER_SERVICE, super.getLayoutInflater());
  }

  @Override
  public Object getSystemService(String name)
  {
    return droid4mizer.getSystemService(name, super.getSystemService(name));
  }

  @Override
  public void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    droid4mizer.onAttached(this);
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      public void run()
      {
        SmartActionBarActivity.super.onCreate(savedInstanceState);
      }
    }, savedInstanceState);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState)
  {
    super.onPostCreate(savedInstanceState);
    droid4mizer.onPostCreate(savedInstanceState);
  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    droid4mizer.onNewIntent(intent);
  }

  // @Override
  // public void onContentChanged()
  // {
  // super.onContentChanged();
  // droid4mizer.onContentChanged();
  // }

  @Override
  protected void onResume()
  {
    super.onResume();
    droid4mizer.onResume();
  }

  @Override
  protected void onPostResume()
  {
    super.onPostResume();
    droid4mizer.onPostResume();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    droid4mizer.onConfigurationChanged(newConfig);
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
  protected void onRestart()
  {
    super.onRestart();
    droid4mizer.onRestart();
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
  public void onDetachedFromWindow()
  {
    try
    {
      droid4mizer.onDetached();
    }
    finally
    {
      super.onDetachedFromWindow();
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

  public void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
  {
    droid4mizer.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately);
  }

  /**
   * AppInternals.LifeCycleInternals implementation.
   */

  public boolean shouldKeepOn()
  {
    return droid4mizer.shouldKeepOn();
  }

  /**
   * Own implementation.
   */

  public void onBusinessObjectsRetrieved()
  {
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
}