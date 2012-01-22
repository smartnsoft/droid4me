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

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.ActivityResultHandler.CompositeHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
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
  protected final static class Rotate3dAnimation
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

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
      // We want the header to get back the focus if possible
      final boolean handled = super.dispatchKeyEvent(event);

      // unhandled key ups change focus to tab indicator for embedded activities
      // when there is nothing that will take focus from default focus searching
      if (!handled && (event.getAction() == KeyEvent.ACTION_DOWN) && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP) /*
                                                                                                                       * &&
                                                                                                                       * (frameLayout.isRootNamespace
                                                                                                                       * ())
                                                                                                                       */&& (contentView.hasFocus()) && (contentView.findFocus().focusSearch(
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

  protected static final Logger log = LoggerFactory.getInstance("SmartableActivity");

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
    super();
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

  public void onBusinessObjectsRetrieved()
  {
  }

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
      log.debug("SmartGroupActivity::onCreate");
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
  protected void onNewIntent(Intent intent)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartGroupActivity::onNewIntent");
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
      log.debug("SmartGroupActivity::onResume");
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
      AppInternals.execute(SmartGroupActivity.this, new Runnable()
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
      log.debug("SmartGroupActivity::onSaveInstanceState");
    }
    super.onSaveInstanceState(outState);
    stateContainer.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState)
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartGroupActivity::onRestoreInstanceState");
    }
    super.onRestoreInstanceState(savedInstanceState);
    businessObjectRetrievalAndResultHandlers();
  }

  @Override
  protected void onStart()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartGroupActivity::onStart");
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
      log.debug("SmartGroupActivity::onPause");
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
      log.debug("SmartGroupActivity::onStop");
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
      log.debug("SmartGroupActivity::onDestroy");
    }
    try
    {
      stateContainer.unregisterBroadcastListeners();
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
      log.debug("SmartGroupActivity::onContextItemSelected");
    }
    boolean result = super.onContextItemSelected(item);

    if (result == false && stateContainer.compositeActionHandler.onContextItemSelected(item) == true)
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
      log.debug("SmartGroupActivity::onActivityResult");
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
