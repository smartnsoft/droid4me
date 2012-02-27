package com.smartnsoft.droid4me.support.v4.app;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.app.Droid4mizer;
import com.smartnsoft.droid4me.app.Droid4mizerInterface;
import com.smartnsoft.droid4me.app.Smartable;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * A basis classes for designing an Android compatibility library {@link android.support.v4.app.Fragment} compatible with the framework.
 * 
 * <p>
 * Warning: this class is only available for applications running under Android v1.6+, i.e. release 4+, with the compatibility library!
 * </p>
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @author Jocelyn Girard, Édouard Mercier
 * @since 2011.12.08
 */
public abstract class SmartFragment<AggregateClass>
    extends Fragment
    implements Droid4mizerInterface, Smartable<AggregateClass>
{

  protected static final Logger log = LoggerFactory.getInstance("Smartable");

  private Droid4mizer<AggregateClass, SmartFragment<AggregateClass>> droid4mizer;

  @Override
  public void onAttach(Activity activity)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartFragment::onAttach");
    }
    super.onAttach(activity);
    droid4mizer = new Droid4mizer<AggregateClass, SmartFragment<AggregateClass>>(getActivity(), this, this, this, this);
  }

  @Override
  public void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      public void run()
      {
        SmartFragment.super.onCreate(savedInstanceState);
      }
    }, savedInstanceState);
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
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
  {
    super.onCreateOptionsMenu(menu, menuInflater);
    droid4mizer.onCreateOptionsMenu(true, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu)
  {
    super.onPrepareOptionsMenu(menu);
    droid4mizer.onPrepareOptionsMenu(true, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    return droid4mizer.onOptionsItemSelected(super.onOptionsItemSelected(item), item);
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
  }

  /**
   * Does the same thing as the {@link #getActivity()}, except that it throws an exception if the fragment has been detached, instead of returning
   * {@code null}
   * 
   * @return a never-null activity, which is the hosting activity
   * @throws IllegalStateException
   *           if the fragment activity is currently null
   */
  public final Activity getCheckedActivity()
      throws IllegalStateException
  {
    if (getActivity() == null)
    {
      // This will generate an IllegalStateException
      getResources();
    }
    return getActivity();
  }

}
