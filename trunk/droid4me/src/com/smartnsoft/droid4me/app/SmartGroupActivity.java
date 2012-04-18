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

import android.app.ActivityGroup;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuHandler.Composite;
import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * A basis class for an activity that contains other activities.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2010.02.24
 */
public abstract class SmartGroupActivity<AggregateClass>
    extends ActivityGroup
    implements SmartableActivity<AggregateClass>/*
                                                 * ,ViewTreeObserver . OnTouchModeChangeListener , OnFocusChangeListener
                                                 */
{
  /**
   * This is taken from the Android API Demos source code!
   * 
   * An animation that rotates the view on the Y axis between two specified angles. This animation also adds a translation on the Z axis (depth) to
   * improve the effect.
   */
  public final static class Rotate3dAnimation
      extends Animation
  {

    private final float mFromDegrees;

    private final float mToDegrees;

    private final float mCenterX;

    private final float mCenterY;

    private final float mDepthZ;

    private final boolean mReverse;

    public Camera mCamera;

    /**
     * Creates a new 3D rotation on the Y axis. The rotation is defined by its start angle and its end angle. Both angles are in degrees. The rotation
     * is performed around a center point on the 2D space, defined by a pair of X and Y coordinates, called centerX and centerY. When the animation
     * starts, a translation on the Z axis (depth) is performed. The length of the translation can be specified, as well as whether the translation
     * should be reversed in time.
     * 
     * @param fromDegrees
     *          the start angle of the 3D rotation
     * @param toDegrees
     *          the end angle of the 3D rotation
     * @param centerX
     *          the X center of the 3D rotation
     * @param centerY
     *          the Y center of the 3D rotation
     * @param depthZ
     *          the Z zoom effect factor
     * @param reverse
     *          true if the translation should be reversed, false otherwise
     */
    public Rotate3dAnimation(float fromDegrees, float toDegrees, float centerX, float centerY, float depthZ, boolean reverse)
    {
      mFromDegrees = fromDegrees;
      mToDegrees = toDegrees;
      mCenterX = centerX;
      mCenterY = centerY;
      mDepthZ = depthZ;
      mReverse = reverse;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight)
    {
      super.initialize(width, height, parentWidth, parentHeight);
      mCamera = new Camera();
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation transformation)
    {
      final float fromDegrees = mFromDegrees;
      float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);

      final float centerX = mCenterX;
      final float centerY = mCenterY;
      final Camera camera = mCamera;

      final Matrix matrix = transformation.getMatrix();

      camera.save();

      if (mReverse == false)
      {
        camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
      }
      else
      {
        camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - interpolatedTime));
      }
      camera.rotateY(degrees + (mReverse == true ? 180 : 0));
      camera.getMatrix(matrix);
      camera.restore();

      matrix.preTranslate(-centerX, -centerY);
      matrix.postTranslate(centerX, centerY);
    }

  }

  private final class GroupLayout
      extends LinearLayout
  {

    private GroupLayout(Context context)
    {
      super(context);
    }

    // We want the header to get back the focus if possible
    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
      // This is a work-around against the following exception, as discussed at
      // https://groups.google.com/forum/?fromgroups#!topic/android-developers/dOrPnXoy-NM
      //
      // java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
      // 04-18 11:59:03.575: ERROR/AndroidRuntime(6414): at android.app.FragmentManagerImpl.checkStateLoss(FragmentManager.java:1213)
      // 04-18 11:59:03.575: ERROR/AndroidRuntime(6414): at android.app.FragmentManagerImpl.popBackStackImmediate(FragmentManager.java:442)
      // 04-18 11:59:03.575: ERROR/AndroidRuntime(6414): at android.app.Activity.onBackPressed(Activity.java:2121)
      // 04-18 11:59:03.575: ERROR/AndroidRuntime(6414): at android.app.Activity.onKeyUp(Activity.java:2099)
      final boolean actuallyDispathKeyEvent = event.getAction() != KeyEvent.ACTION_UP || event.getKeyCode() != KeyEvent.KEYCODE_BACK;
      final boolean handled = actuallyDispathKeyEvent == true ? super.dispatchKeyEvent(event) : false;

      // unhandled key ups change focus to tab indicator for embedded activities
      // when there is nothing that will take focus from default focus searching
      if (actuallyDispathKeyEvent == true && !handled && (event.getAction() == KeyEvent.ACTION_DOWN) && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) && (contentView.hasFocus()) && (contentView.findFocus().focusSearch(
          View.FOCUS_UP) == null))
      {
        if (headerView != null)
        {
          headerView.requestFocus();
        }
        playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
        return true;
      }
      return handled;
    }

  }

  private final List<String> activitiesIds = new ArrayList<String>();

  private boolean hasActivityAlreadyStarted;

  private Window windowDisplayed;

  private Window windowDisplaying;

  private String previousActivityId;

  private String currentActivityId;

  private SmartGroupActivity<AggregateClass>.GroupLayout wrapperView;

  private View headerView;

  private View footerView;

  private FrameLayout contentView;

  public SmartGroupActivity()
  {
  }

  public SmartGroupActivity(boolean singleActivityMode)
  {
    super(singleActivityMode);
  }

  /**
   * Gives access to the {@link ActivityGroup} content view, which contains a {@link #getHeaderView() header} and a {@link #getFooterView() footer}
   * view.
   * 
   * @return the root view of the activity
   */
  protected final ViewGroup getWrapperView()
  {
    return wrapperView;
  }

  /**
   * Is responsible for providing the header view of the {@link ActivityGroup}, if any.
   * 
   * <p>
   * This method will be invoked once during its content view creation.
   * </p>
   * 
   * @return the top view ; can be {@code null}, which is the default implementation
   */
  protected View getHeaderView()
  {
    return null;
  }

  /**
   * Is responsible for providing the footer view of the {@link ActivityGroup}, if any.
   * 
   * <p>
   * This method will be invoked once during its content view creation.
   * </p>
   * 
   * @return the bottom view ; can be {@code null}, which is the default implementation
   */
  protected View getFooterView()
  {
    return null;
  }

  protected abstract Intent getSubIntent(String activityId);

  protected final void addSubActivity(String activityId)
  {
    activitiesIds.add(activityId);
  }

  protected final String getPreviousActivityId()
  {
    return previousActivityId;
  }

  protected final String getCurrentActivityId()
  {
    return currentActivityId;
  }

  public List<String> getActivitiesIds()
  {
    return activitiesIds;
  }

  protected final ViewGroup getContentView()
  {
    return contentView;
  }

  protected final boolean hasActivityAlreadyStarted()
  {
    return hasActivityAlreadyStarted;
  }

  protected void onActivityStarted(String activityId)
  {
    switchWindow();
    hasActivityAlreadyStarted = true;
  }

  protected final void switchToPreviousActivity()
  {
    if (previousActivityId == null)
    {
      throw new IllegalStateException("Cannot switch to a null previous activity!");
    }
    switchToActivity(previousActivityId);
  }

  protected final void switchToActivity(String activityId)
  {
    windowDisplaying = startActivityWithId(activityId, getSubIntent(activityId));
    onActivityStarted(activityId);
  }

  // Synchronized to avoid concurrency
  protected synchronized final void switchWindow()
  {
    // Does nothing if the same window is displayed again
    if (windowDisplaying != windowDisplayed)
    {
      contentView.addView(windowDisplaying.getDecorView(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

      if (windowDisplayed != null)
      {
        contentView.removeView(windowDisplayed.getDecorView());
      }
      windowDisplayed = windowDisplaying;
    }
  }

  protected final View startActivity(String activityId, Intent intent)
  {
    return startActivityWithId(activityId, intent).getDecorView();
  }

  // @Override
  // public void onResume()
  // {
  // super.onResume();
  //
  // if (windowDisplaying == null && activitiesIds.size() >= 1)
  // {
  // currentActivityId = activitiesIds.get(0);
  // switchToActivity(currentActivityId);
  // }
  // }

  private Window startActivityWithId(String activityId, Intent intent)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Starting the child activity with id '" + activityId + "'");
    }
    final Window window = getLocalActivityManager().startActivity(activityId, intent);
    previousActivityId = currentActivityId;
    // This is essential on embedded activities for now so they can get focus if none of their children have it. They need focus to be able to
    // display menu items.
    final View decorView = window.getDecorView();
    if (decorView != null)
    {
      decorView.setVisibility(View.VISIBLE);
      decorView.setFocusableInTouchMode(true);
      ((ViewGroup) decorView).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
      decorView.requestFocus();
    }
    currentActivityId = activityId;
    if (headerView != null && headerView.hasFocus() == false)
    {
      contentView.requestFocus();
    }
    return window;
  }

  // public void onTouchModeChanged(boolean isInTouchMode)
  // {
  // if (isInTouchMode == false)
  // {
  // // leaving touch mode.. if nothing has focus, let's give it to
  // // the indicator of the current tab
  // if (frameLayout != null && (!frameLayout.hasFocus() || frameLayout.isFocused()))
  // {
  // frameLayout.requestFocus();
  // }
  // }
  // }

  // public void onFocusChange(View view, boolean hasFocus)
  // {
  // log.debug("onFocusChange(" + view + ", " + hasFocus + ")");
  // }

  public void onRetrieveDisplayObjects()
  {
    wrapperView = new GroupLayout(this);
    wrapperView.setOrientation(LinearLayout.VERTICAL);

    contentView = new FrameLayout(this);

    headerView = getHeaderView();
    if (headerView != null)
    {
      wrapperView.addView(headerView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
      // headerView.setOnFocusChangeListener(this);
    }

    // We add the content view, and provide a layout weight, so that it takes the maximum space
    wrapperView.addView(contentView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1f));

    footerView = getFooterView();
    if (footerView != null)
    {
      wrapperView.addView(footerView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
    }

    // contentView.setOnFocusChangeListener(this);
    // wrapperView.setOnFocusChangeListener(this);
    setContentView(wrapperView);
  }

  protected static final Logger log = LoggerFactory.getInstance("Smartable");

  private final Droid4mizer<AggregateClass, SmartGroupActivity<AggregateClass>> droid4mizer = new Droid4mizer<AggregateClass, SmartGroupActivity<AggregateClass>>(this, this, this, null);

  @Override
  protected void onCreate(final Bundle savedInstanceState)
  {
    droid4mizer.onCreate(new Runnable()
    {
      public void run()
      {
        SmartGroupActivity.super.onCreate(savedInstanceState);
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
