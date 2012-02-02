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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ZoomControls;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * A basis class for an activity that holds a map.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author �douard Mercier
 * @since 2009.02.16
 */
public abstract class SmartMapActivity<AggregateClass>
    extends MapActivity
    implements SmartableActivity<AggregateClass>
{

  protected final static Logger log = LoggerFactory.getInstance("SmartableActivity");

  private final static float MILLION_AS_FLOAT = 1E6f;

  private MapView mapView;

  private LinearLayout headerView;

  private RelativeLayout mapWrapperLayout;

  private LinearLayout footerView;

  private LinearLayout innerHeaderView;

  private LinearLayout innerFooterView;

  private RelativeLayout wrapperLayout;

  protected abstract String getGoogleMapsApiKey();

  protected final MapView getMapView()
  {
    return mapView;
  }

  public void onRetrieveDisplayObjects()
  {
    wrapperLayout = new RelativeLayout(this);
    {
      headerView = new LinearLayout(this);
      headerView.setId(1230);
      final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
      wrapperLayout.addView(headerView, layoutParams);
    }
    {
      footerView = new LinearLayout(this);
      footerView.setId(headerView.getId() + 1);
      footerView.setGravity(Gravity.BOTTOM);
      final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
      wrapperLayout.addView(footerView, layoutParams);
    }
    {
      mapWrapperLayout = new RelativeLayout(this);
      mapView = internalCreateMapView();
      getMapWrapperLayout().addView(mapView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
      // We need to set an identifier to the map view, because we want to locate other views relatively to it
      mapView.setId(footerView.getId() + 1);
      {
        innerHeaderView = new LinearLayout(this);
        innerHeaderView.setOrientation(LinearLayout.VERTICAL);
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
        layoutParams.addRule(RelativeLayout.ALIGN_TOP, mapView.getId());
        getMapWrapperLayout().addView(innerHeaderView, layoutParams);
      }
      {
        innerFooterView = new LinearLayout(this);
        innerFooterView.setOrientation(LinearLayout.VERTICAL);
        innerFooterView.setId(mapView.getId() + 1);
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, mapView.getId());
        getMapWrapperLayout().addView(innerFooterView, layoutParams);
      }
      {
        final LinearLayout zoomLayout = new LinearLayout(this);
        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ABOVE, innerFooterView.getId());
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        getMapWrapperLayout().addView(zoomLayout, layoutParams);
        {
          @SuppressWarnings("deprecation")
          final ZoomControls zoomView = (ZoomControls) mapView.getZoomControls();
          zoomView.setGravity(Gravity.CENTER_HORIZONTAL);
          zoomView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
          mapView.displayZoomControls(true);
          zoomLayout.addView(zoomView);
        }
      }
      final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
      layoutParams.addRule(RelativeLayout.BELOW, headerView.getId());
      layoutParams.addRule(RelativeLayout.ABOVE, footerView.getId());
      wrapperLayout.addView(getMapWrapperLayout(), layoutParams);
    }
    setContentView(wrapperLayout);
  }

  public void onBusinessObjectsRetrieved()
  {
  }

  @Override
  protected boolean isRouteDisplayed()
  {
    return false;
  }

  /**
   * @return the top view of the activity, which contains the {@link #getHeaderView() header view}, the {@link #getMapWrapperLayout() map view wrapper
   *         layout} and the {@link #getFooterView() footer view}
   */
  protected final RelativeLayout getWrapperLayout()
  {
    return wrapperLayout;
  }

  /**
   * @return the layout which contains the map activity header view
   */
  protected final LinearLayout getHeaderView()
  {
    return headerView;
  }

  /**
   * @return the layout which contains the {@link #getInnerHeaderView() overlay header view}, the {@link MapView} and the
   *         {@link #getInnerFooterView() overlay footer view}
   */
  protected final RelativeLayout getMapWrapperLayout()
  {
    return mapWrapperLayout;
  }

  /**
   * @return the layout which contains the map activity footer view
   */
  protected final LinearLayout getFooterView()
  {
    return footerView;
  }

  /**
   * @return the layout which is displayed in overlay mode at the top of the map activity
   */
  protected final LinearLayout getInnerHeaderView()
  {
    return innerHeaderView;
  }

  /**
   * @return the layout which is displayed in overlay mode at the bottom of the map activity
   */
  protected final LinearLayout getInnerFooterView()
  {
    return innerFooterView;
  }

  // Many explanations are taken from http://androidguys.com/?p=1413 and at
  // http://groups.google.com/group/android-developers/browse_thread/thread/db3eee0abe7aafe4/16a44c09741d9883?lnk=raot
  private MapView internalCreateMapView()
  {
    final MapView theMapView = createMapView(getGoogleMapsApiKey());
    // Without these two settings, it is not possible to click, zoom in/out, and navigate on the map
    theMapView.setEnabled(true);
    theMapView.setClickable(true);
    // mapView.setReticleDrawMode(ReticleDrawMode.DRAW_RETICLE_OVER);
    return theMapView;
  }

  /**
   * You can override this method, in order to tune the handled {@link MapView}.
   */
  protected MapView createMapView(String apiKey)
  {
    return new MapView(this, apiKey);
  }

  private final static double EQUATOR_IN_PIXELS_WHEN_ZOOM_SET_TO_1 = 256.0d;

  // The information distance was taken from http://answers.yahoo.com/question/index?qid=20060626170225AA0RThe
  private final static double EQUATOR_DISTANCE_IN_METERS = 40075016.686d;

  private static double adjustWithLatitude(int latitudeE6, double pixelsCount)
  {
    return pixelsCount * Math.cos(((((float) latitudeE6) / MILLION_AS_FLOAT) / 90f) * (Math.PI / 2d));
  }

  public static final int convertMetersIntoPixels(int zoomLevel, int latitudeE6, float radiusInMeters)
  {
    // BUG: in the Android documentation, the equator zoom is 0: see
    // http://groups.google.com/group/android-developers/browse_thread/thread/24fc4fd4d6aa67cd/6a2567fb26f207a4?#6a2567fb26f207a4
    final double equatorPixels = EQUATOR_IN_PIXELS_WHEN_ZOOM_SET_TO_1 * Math.pow(2d, zoomLevel);
    final double pixelPerMeter = equatorPixels / EQUATOR_DISTANCE_IN_METERS;
    final double pixelsCount = pixelPerMeter * radiusInMeters;
    final double pixelsCountByTakingAccountLatitude = SmartMapActivity.adjustWithLatitude(latitudeE6, pixelsCount);
    // log.debug("The conversion of " + radiusInMeters + " m. in pixels with a zoom level set to " + zoomLevel + " gives a result of " +
    // pixelsCountByTakingAccountLatitude);
    return (int) (pixelsCountByTakingAccountLatitude);
  }

  public static final int convertLatitudeDistanceInMetersIntoZoom(int latitudeE6, float radiusInMeters)
  {
    int zoomLevel = 1;
    double screenWidthInMeters = EQUATOR_DISTANCE_IN_METERS;
    final double radiusInMetersTakingAccountLatitude = SmartMapActivity.adjustWithLatitude(latitudeE6, radiusInMeters);
    // We apply a dichotomy
    while ((screenWidthInMeters > radiusInMetersTakingAccountLatitude) && zoomLevel <= (21 + 1))
    {
      screenWidthInMeters = screenWidthInMeters / 2d;
      zoomLevel++;
    }
    return zoomLevel - 1 - 2;
  }

  public static final double convertZoomIntoMeters(int zoomLevel, int pixelSpan)
  {
    return (((double) pixelSpan / 256d) * EQUATOR_DISTANCE_IN_METERS) / (Math.pow(2, zoomLevel - 1));
  }

  // public static final int convertLatitudeDistanceInMetersIntoSpan(int latitudeE6, float latitudeDistanceInMeters)
  // {
  // int zoomLevel = 1;
  // double screenWidthInMeters = EQUATOR_DISTANCE_IN_METERS;
  // final double radiusInMetersTakingAccountLatitude = SmartMapActivity.adjustWithLatitude(latitudeE6, latitudeDistanceInMeters);
  // // We apply a dichotomy
  // while ((screenWidthInMeters > radiusInMetersTakingAccountLatitude) && zoomLevel <= (21 + 1))
  // {
  // screenWidthInMeters = screenWidthInMeters / 2d;
  // zoomLevel++;
  // }
  // return zoomLevel - 1 - 2;
  // }

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

  protected void onBeforeRetrievingDisplayObjects()
  {
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
      log.debug("SmartMapActivity::onCreate");
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
    onBeforeRetrievingDisplayObjects();
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
  public void onNewIntent(Intent intent)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartMapActivity::onNewIntent");
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
      log.debug("SmartMapActivity::onResume");
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
      onOver.run();
    }
    stateContainer.onRefreshingBusinessObjectsAndDisplayStop(this);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartMapActivity::onSaveInstanceState");
    }
    super.onSaveInstanceState(outState);
    stateContainer.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartMapActivity::onRestoreInstanceState");
    }
    super.onRestoreInstanceState(savedInstanceState);
    businessObjectRetrievalAndResultHandlers();
  }

  @Override
  protected void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartMapActivity::onStart");
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
      log.debug("SmartMapActivity::onPause");
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
      log.debug("SmartMapActivity::onStop");
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
      log.debug("SmartMapActivity::onDestroy");
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
      log.debug("SmartMapActivity::onContextItemSelected");
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
      log.debug("SmartMapActivity::onActivityResult");
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