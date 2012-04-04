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
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * A basis class for an activity that holds a map.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2009.02.16
 */
public abstract class SmartMapActivity<AggregateClass>
    extends MapActivity
    implements SmartableActivity<AggregateClass>
{

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

  protected final static Logger log = LoggerFactory.getInstance("Smartable");

  private final Droid4mizer<AggregateClass, SmartMapActivity<AggregateClass>> droid4mizer = new Droid4mizer<AggregateClass, SmartMapActivity<AggregateClass>>(this, this, this, null);

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      public void run()
      {
        SmartMapActivity.super.onCreate(savedInstanceState);
      }
    }, savedInstanceState);
  }

  @Override
  public void onNewIntent(Intent intent)
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
    return droid4mizer.onOptionsItemSelected(super.onOptionsItemSelected(item), item);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item)
  {
    return droid4mizer.onContextItemSelected(super.onContextItemSelected(item), item);
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

  public final void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately)
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

  /**
   * Same as invoking {@link #refreshBusinessObjectsAndDisplay(true, null, false)}.
   * 
   * @see #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)
   */
  public final void refreshBusinessObjectsAndDisplay()
  {
    refreshBusinessObjectsAndDisplay(true, null, false);
  }

  public void onSynchronizeDisplayObjects()
  {
  }

  public List<StaticMenuCommand> getMenuCommands()
  {
    return null;
  };

}
