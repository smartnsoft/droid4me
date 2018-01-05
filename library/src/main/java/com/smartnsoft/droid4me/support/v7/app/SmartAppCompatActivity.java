// The MIT License (MIT)
//
// Copyright (c) 2017 Smart&Soft
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.smartnsoft.droid4me.support.v7.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.app.Droid4mizer;
import com.smartnsoft.droid4me.app.SmartableActivity;

/**
 * @author Jocelyn Girard
 * @since 2014.10.29
 */
public abstract class SmartAppCompatActivity<AggregateClass>
    extends AppCompatActivity
    implements SmartableActivity<AggregateClass>
{

  private final Droid4mizer<AggregateClass, SmartAppCompatActivity<AggregateClass>> droid4mizer = new Droid4mizer<>(this, this, this, null);

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    droid4mizer.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      @Override
      public void run()
      {
        SmartAppCompatActivity.super.onCreate(savedInstanceState);
      }
    }, savedInstanceState);
  }

  @Override
  protected void onStart()
  {
    super.onStart();
    droid4mizer.onStart();
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    droid4mizer.onResume();
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

  // @Override
  // public void onContentChanged()
  // {
  // super.onContentChanged();
  // droid4mizer.onContentChanged();
  // }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    return droid4mizer.onOptionsItemSelected(getParent() == null ? super.onOptionsItemSelected(item) : true, item);
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
  public void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    droid4mizer.onAttached(this);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    droid4mizer.onConfigurationChanged(newConfig);
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
  public boolean onContextItemSelected(MenuItem item)
  {
    return droid4mizer.onContextItemSelected(getParent() == null ? super.onContextItemSelected(item) : true, item);
  }

  /**
   * Smartable implementation.
   */

  @Override
  public AggregateClass getAggregate()
  {
    return droid4mizer.getAggregate();
  }

  @Override
  public void setAggregate(AggregateClass aggregate)
  {
    droid4mizer.setAggregate(aggregate);
  }

  @Override
  public Handler getHandler()
  {
    return droid4mizer.getHandler();
  }

  @Override
  public SharedPreferences getPreferences()
  {
    return droid4mizer.getPreferences();
  }

  @Override
  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    droid4mizer.onException(throwable, fromGuiThread);
  }

  /**
   * SmartableActivity implementation.
   */

  @Override
  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    droid4mizer.registerBroadcastListeners(broadcastListeners);
  }

  @Override
  public int getOnSynchronizeDisplayObjectsCount()
  {
    return droid4mizer.getOnSynchronizeDisplayObjectsCount();
  }

  @Override
  public boolean isRefreshingBusinessObjectsAndDisplay()
  {
    return droid4mizer.isRefreshingBusinessObjectsAndDisplay();
  }

  @Override
  public boolean isFirstLifeCycle()
  {
    return droid4mizer.isFirstLifeCycle();
  }

  @Override
  public final boolean isInteracting()
  {
    return droid4mizer.isInteracting();
  }

  @Override
  public final boolean isAlive()
  {
    return droid4mizer.isAlive();
  }

  @Override
  public void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
  {
    droid4mizer.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately);
  }

  /**
   * AppInternals.LifeCycleInternals implementation.
   */

  @Override
  public boolean shouldKeepOn()
  {
    return droid4mizer.shouldKeepOn();
  }

  /**
   * Own implementation.
   */

  @Override
  public void onBusinessObjectsRetrieved()
  {
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

  @Override
  protected void onPostResume()
  {
    super.onPostResume();
    droid4mizer.onPostResume();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    droid4mizer.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestart()
  {
    super.onRestart();
    droid4mizer.onRestart();
  }

  /**
   * Same as invoking {@code refreshBusinessObjectsAndDisplay(true, null, false)}.
   *
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null, false);
  }

}
