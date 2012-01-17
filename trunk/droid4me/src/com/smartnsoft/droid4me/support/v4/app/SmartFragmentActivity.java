package com.smartnsoft.droid4me.support.v4.app;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.app.Droid4mizer;
import com.smartnsoft.droid4me.app.Droid4mizerInterface;
import com.smartnsoft.droid4me.app.SmartableActivity;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;

/**
 * A basis classes for designing an Android compatibility library {@link android.support.v4.app.FragmentActivity} compatible with the framework.
 * 
 * <p>
 * Warning: this class is only available for applications running under Android v1.6+, i.e. release 4+, with the compatibility library!
 * </p>
 * 
 * @author Jocelyn Girard, Édouard Mercier
 * @since 2011.12.08
 */
public abstract class SmartFragmentActivity<AggregateClass>
    extends FragmentActivity
    implements Droid4mizerInterface, SmartableActivity<AggregateClass>
{

  protected static final Logger log = LoggerFactory.getInstance("SmartableActivity");

  private final Droid4mizer<AggregateClass, SmartFragmentActivity<AggregateClass>> droid4mizer = new Droid4mizer<AggregateClass, SmartFragmentActivity<AggregateClass>>(this, this, this, this, null);

  /**
   * The Intent which will be used in case the ActionBar home button is clicked.
   */
  private Intent homeIntent;

  public final Intent getHomeIntent()
  {
    return homeIntent;
  }

  public final void setHomeIntent(Intent homeIntent)
  {
    this.homeIntent = homeIntent;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      public void run()
      {
        SmartFragmentActivity.super.onCreate(savedInstanceState);
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
  public void onResume()
  {
    super.onResume();
    droid4mizer.onResume();
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    droid4mizer.onSaveInstanceState(outState);
  }

  @Override
  public void onStart()
  {
    super.onStart();
    droid4mizer.onStart();
  }

  @Override
  public void onPause()
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
  public void onStop()
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
  public void onDestroy()
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
    return droid4mizer.onCreateOptionsMenu(super.onCreateOptionsMenu(menu), menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    return droid4mizer.onPrepareOptionsMenu(super.onPrepareOptionsMenu(menu), menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId())
    {
    case android.R.id.home:
      final Intent homeIntent = getHomeIntent();
      if (homeIntent != null)
      {
        startActivity(homeIntent);
        // In case the home intent does not set the Intent.FLAG_ACTIVITY_CLEAR_TOP flag, and just returns to the previous screen
        finish();
      }
      return true;
    default:
      return droid4mizer.onOptionsItemSelected(super.onOptionsItemSelected(item), item);
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    return droid4mizer.onContextItemSelected(super.onContextItemSelected(item), item);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data)
  {
    super.onActivityResult(requestCode, resultCode, data);
    droid4mizer.onActivityResult(requestCode, resultCode, data);
  }

  /**
   * SmartableActivity implementation.
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

  public void onException(Throwable throwable, boolean fromGuiThread)
  {
    droid4mizer.onException(throwable, fromGuiThread);
  }

  public void registerBroadcastListeners(BroadcastListener[] broadcastListeners)
  {
    droid4mizer.registerBroadcastListeners(broadcastListeners);
  }

  public void onBusinessObjectsRetrieved()
  {
    droid4mizer.onBusinessObjectsRetrieved();
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

  public boolean shouldKeepOn()
  {
    return droid4mizer.shouldKeepOn();
  }

  public void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
  {
    droid4mizer.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, onOver, immediately);
  }

  /**
   * Droid4mizeInterface implementation.
   */

  public void onBeforeRetrievingDisplayObjects()
  {
    droid4mizer.onBeforeRetrievingDisplayObjects();
  }

  public Composite getCompositeActionHandler()
  {
    return droid4mizer.getCompositeActionHandler();
  }

  public CompositeHandler getCompositeActivityResultHandler()
  {
    return droid4mizer.getCompositeActivityResultHandler();
  }

  public SharedPreferences getPreferences()
  {
    return droid4mizer.getPreferences();
  }

  public void onActuallyCreated()
  {
  }

  public void onActuallyDestroyed()
  {
  }

  // --------------

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  };

}
