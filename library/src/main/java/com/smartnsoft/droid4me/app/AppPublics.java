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

package com.smartnsoft.droid4me.app;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.support.v4.content.LocalBroadcastManager;

/**
 * Gathers some interfaces and helpers for the types belonging to its Java package.
 *
 * @author Édouard Mercier
 * @since 2010.01.05
 */
public final class AppPublics
{

  /**
   * Because an Android {@link Activity}/{@link android.app.Fragment} entity can be destroyed and then recreated, following a configuration change (a
   * screen orientation change, for instance), this interface gives information about an entity life cycle.
   *
   * @author Édouard Mercier
   * @since 2010.01.05
   */
  public interface LifeCyclePublic
  {

    /**
     * Provides information about the current entity life cycle.
     *
     * @return {@code true} if and only if the entity life cycle is the first time to execute during its container life
     */
    boolean isFirstLifeCycle();

    /**
     * Provides information about the current entity life cycle.
     * <p>
     * <p>
     * It is very handy when it comes to know whether the end-user can interact with the underlying {@link Activity}/{@link android.app.Fragment}
     * entity.
     * </p>
     *
     * @return {@code true} if and only if the underlying {@link Activity}/{@link android.app.Fragment} entity life-cycle is between the
     * {@link Activity#onResume()}/{@link android.app.Fragment#onResume()} and {@link Activity#onPause()}/
     * {@link android.app.Fragment#onPause()} methods
     */
    boolean isInteracting();

    /**
     * Indicates whether the current entity is still alive.
     * <p>
     * <p>
     * It enables to know whether the underlying {@link Activity}/{@link android.app.Fragment} entity UI is still available for being handled.
     * </p>
     *
     * @return {@code true} if and only if the underlying {@link Activity}/{@link android.app.Fragment} entity {@link Activity#onDestroy()}/
     * {@link android.app.Fragment#onDestroy()} method has already been invoked
     */
    boolean isAlive();

    /**
     * Enables to know how many times the {@link LifeCycle#onSynchronizeDisplayObjects()} method has been invoked, which may be useful when you do not
     * want this method to do something that the {@link LifeCycle#onFulfillDisplayObjects()} method may have already done.
     *
     * @return the number of time the {@link LifeCycle#onSynchronizeDisplayObjects()} method has actually been invoked
     */
    int getOnSynchronizeDisplayObjectsCount();

    /**
     * Indicates whether the extending {@link Activity}/{@link android.app.Fragment} entity also implementing the {@link LifeCycle} interface is in
     * the middle of a {@link LifeCycle#refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)} call.
     * <p>
     * <p>
     * It is very handy when it comes to disable certain things, like menu entries, while an {@link Activity} is loading.
     * </p>
     *
     * @return {@code true} if and only if the {@link LifeCycle#refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)} is being executed.
     */
    boolean isRefreshingBusinessObjectsAndDisplay();

  }

  /**
   * When applied to the {@link AppPublics.BroadcastListener#getIntentFilter()} method, the implementation used will be the native
   * {@link Context#sendBroadcast(Intent)} method will be used, instead of the {@link LocalBroadcastManager#sendBroadcast(Intent)}.
   * <p>
   * <p>
   * The {@link LocalBroadcastManager} is better regarding the performance, and safer in terms of security, because it restricts the broadcast
   * consumption to the application.
   * </p>
   *
   * @see AppPublics.BroadcastListener
   * @since 2013.04.22
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @Inherited
  public @interface UseNativeBroadcast
  {

  }

  /**
   * This interface is a wrapper around a regular Android native {@link android.content.BroadcastReceiver}, by capturing the
   * {@link Context#registerReceiver(android.content.BroadcastReceiver, IntentFilter)} invocation and implementation is one place. When a
   * {@link Activity} or {@link android.app.Fragment} {@link Smartable} entity implements that interface, the framework will
   * {@link Context#registerReceiver(android.content.BroadcastReceiver, IntentFilter) register} a {@link android.content.BroadcastReceiver}, and
   * {@link Context#unregisterReceiver(android.content.BroadcastReceiver) unregister} automatically during its life cycle.
   * <p>
   * <p>
   * It indicates what kind of broadcast {@link Intent} are being listened to, and how to handle them, and enables to express both some {@link Intent}
   * filters, and their consumption at the same place.
   * </p>
   * <p>
   * <p>
   * The framework will request this interface methods during the {@link Activity#onCreate(Bundle)} or
   * {@link android.app.Fragment#onCreate(Bundle)} methods, create a corresponding {@link android.content.BroadcastReceiver} and
   * {@link Context#registerReceiver(android.content.BroadcastReceiver, IntentFilter) register it}. This created
   * {@link android.content.BroadcastReceiver} will be {@link Context#unregisterReceiver(android.content.BroadcastReceiver) unregistered} during the
   * {@link Activity#onDestroy()} or {@link Fragment#onDestroy()} method.
   * </p>
   *
   * @see Smarted#registerBroadcastListeners(BroadcastListener[])
   * @see AppPublics.BroadcastListenerProvider
   * @see AppPublics.BroadcastListenersProvider
   * @since 2010.02.04
   */
  public interface BroadcastListener
  {

    /**
     * This method will be invoked by the framework to determine what {@link IntentFilter} should be associated to the current listener.
     * <p>
     * <p>
     * The returned value of the method will be used to invoke the {@link Context#registerReceiver(android.content.BroadcastReceiver, IntentFilter)}
     * method. If this method is annotated with {@link AppPublics.UseNativeBroadcast}, the implementation will use the {@link LocalBroadcastManager}
     * implementation.
     * </p>
     *
     * @return if not {@code null}, only the {@link Intent intents} that match with this returned value, will be received by the activity
     * @see #onReceive(Intent)
     */
    IntentFilter getIntentFilter();

    /**
     * Is invoked every time an intent that matches is received by the underlying activity.
     *
     * @param intent the broadcast {@link Intent} which has been received, and which matches the declared {@link IntentFilter}
     * @see #getIntentFilter()
     */
    void onReceive(Intent intent);

  }

  /**
   * States that the Android {@link Activity} or {@link Fragment} entity which implements this interface is able to provide a single
   * {@link AppPublics.BroadcastListener}.
   * <p>
   * <p>
   * As soon as a {@link Smartable} entity implements this interface, it is able to register a wrapped {@link android.content.BroadcastReceiver}
   * instance through the concept of {@link BroadcastListener}: this is handy, because it enables to integrate an independent reusable
   * {@link BroadcastListener} at the same time, and because the framework takes care of
   * {@link Context#unregisterReceiver(android.content.BroadcastReceiver) unregistering} it when the embedding entity is destroyed.
   * </p>
   *
   * @see Smarted#registerBroadcastListeners(BroadcastListener[])
   * @see AppPublics.BroadcastListener
   * @see AppPublics.BroadcastListenersProvider
   * @since 2010.02.04
   */
  public interface BroadcastListenerProvider
  {

    /**
     * This method will be invoked by the framework for registering a {@link AppPublics.BroadcastListener}.
     *
     * @return the broadcast listener that this provider exposes ; can be {@code null}, and in that case, no {@link AppPublics.BroadcastListener} will
     * be registered
     */
    AppPublics.BroadcastListener getBroadcastListener();

  }

  /**
   * States that the Android {@link Activity} or {@link Fragment} entity which implements this interface is able to provide several
   * {@link AppPublics.BroadcastListener}.
   * <p>
   * <p>
   * As soon as a {@link Smartable} entity implements this interface, it is able to register several wrapped {@link android.content.BroadcastReceiver}
   * instances through the concept of {@link BroadcastListener}: this is handy, because it enables to aggregate several independent reusable
   * {@link BroadcastListener} at the same time, and because the framework takes care of
   * {@link Context#unregisterReceiver(android.content.BroadcastReceiver) unregistering} them when the embedding entity is destroyed.
   * </p>
   * <p>
   * <p>
   * This interface has been split into two distinct methods, {@link #getBroadcastListenersCount() one} for determining how many
   * {@link AppPublics.BroadcastListener broadcast listeners} the entity exposes, {@link #getBroadcastListener(int) one} for getting each individual
   * {@link AppPublics.BroadcastListener}. This split is mostly due to performance issues.
   * </p>
   *
   * @see Smarted#registerBroadcastListeners(BroadcastListener[])
   * @see AppPublics.BroadcastListener
   * @see AppPublics.BroadcastListenersProvider
   * @since 2010.11.07
   */
  public interface BroadcastListenersProvider
  {

    /**
     * This method will be invoked by the framework, so that it knows how many {@link AppPublics.BroadcastListener} it exposes.
     *
     * @return the number of {@link AppPublics.BroadcastListener} which are supported
     * @see #getBroadcastListener(int)
     */
    int getBroadcastListenersCount();

    /**
     * This method is bound to be invoked successfully by the framework with a {@code index} argument ranging from {@code 0} to
     * {@code  getBroadcastListenersCount() - 1}. The method implementation is responsible for returning all the {@link AppPublics.BroadcastListener}
     * that this entity is supposed to expose.
     *
     * @param index of the {@link AppPublics.BroadcastListener} to return
     * @return the {@link AppPublics.BroadcastListener} for the provided {@code index} parameter; it is not allowed to be null
     * @see #getBroadcastListenersCount()
     */
    AppPublics.BroadcastListener getBroadcastListener(int index);

  }

  /**
   * When an {@link Activity} implements that interface, it will send broadcast intents while loading and once the loading is over.
   *
   * @see AppPublics.BroadcastListener
   * @see AppPublics.BroadcastListenerProvider
   * @since 2013.04.12
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Inherited
  public @interface SendLoadingIntentAnnotation
  {

  }

  /**
   * A helper which receives multi-selection {@link Intent intents} with an action set to {@link MultiSelectionHandler#SELECTION_ACTION}, and which
   * remembers and triggers event according to the current selection.
   * <p>
   * <p>
   * The <code>BusinessObjectClass</code> "template" class must implement properly the {@link Object#equals(Object)} method, in order to ensure the
   * unicity of business objects inside the {@link MultiSelectionHandler#getSelectedObjects() selected business objects}.
   * </p>
   *
   * @since 2010.06.21
   */
  public static final class MultiSelectionHandler<BusinessObjectClass extends Serializable>
  {

    /**
     * The interface which is invoked by the {@link MultiSelectionHandler} when a new selection event occurs.
     */
    public interface OnMultiSelectionChanged<BusinessObjectClass>
    {

      /**
       * Invoked when the selection changes.
       *
       * @param previouslyAtLeastOneSelected the number of items selected before the event occurred
       * @param atLeastOneSelected           the new number of items selected
       * @param selectedObjects              the business objects that are currently selected
       */
      void onSelectionChanged(boolean previouslyAtLeastOneSelected, boolean atLeastOneSelected,
          List<BusinessObjectClass> selectedObjects);
    }

    /**
     * Used as a key in the {@link Intent#getExtras() intent bundle}, so as to indicate whether the event deals with a selection or deselection.
     */
    public final static String EXTRA_SELECTED = "selected";

    /**
     * Use this intent action when you need to indicate to the current activity that a business object has been selected or unselected.
     * <p>
     * see {@link MultiSelectionHandler#EXTRA_SELECTED} and {@link MultiSelectionHandler#EXTRA_BUSINESS_OBJECT} extra flags for indicated
     * whether the business object is selected, and what business object this is about
     *
     * @see MultiSelectionHandler#EXTRA_SELECTED
     * @see MultiSelectionHandler#EXTRA_BUSINESS_OBJECT
     */
    public static String SELECTION_ACTION = "com.smartnsoft.droid4me.action.SELECTION";

    /**
     * Keeps in memory all the selected objects list.
     */
    private final List<BusinessObjectClass> selectedObjects = new ArrayList<>();

    private int selectedCount = 0;

    /**
     * @return the number of business objects currently selected
     */
    public final int getSelectedCount()
    {
      return selectedCount;
    }

    /**
     * @return the currently selected business objects. Do not modify the returned list!
     */
    public final List<BusinessObjectClass> getSelectedObjects()
    {
      return selectedObjects;
    }

    /**
     * Clears the selected objects.
     */
    public final void clearSelection()
    {
      selectedObjects.clear();
      selectedCount = 0;
    }

    /**
     * The method to invoke when the activity receives a {@link MultiSelectionHandler#SELECTION_ACTION intent}, due to as business object
     * selection/unselection.
     * <p>
     * <p>
     * Usually invoked from the {@link android.content.BroadcastReceiver} which is listening to business objects selection events}.
     * </p>
     *
     * @param intent                  the received intent; if the action of the Intent is not the right one, no processing is done
     * @param onMultiSelectionChanged the interface that will be callbacked (in the same thread as the calling method) depending on the overall multi-selection state; is
     *                                allowed to be {@code null}
     * @return {@code true} if the intent has been handled ; {@code false} otherwise
     */
    @SuppressWarnings({ "unchecked" })
    public boolean onSelection(Intent intent,
        @SuppressWarnings("rawtypes") MultiSelectionHandler.OnMultiSelectionChanged onMultiSelectionChanged)
    {
      if (intent.getAction().equals(AppPublics.MultiSelectionHandler.SELECTION_ACTION) == false)
      {
        return false;
      }
      final boolean selected = intent.getBooleanExtra(MultiSelectionHandler.EXTRA_SELECTED, false) == true;
      final int previousSelectedCount = selectedCount;
      final BusinessObjectClass businessObject = (BusinessObjectClass) intent.getSerializableExtra(AppPublics.EXTRA_BUSINESS_OBJECT);
      setSelection(businessObject, selected);
      if (onMultiSelectionChanged != null)
      {
        onMultiSelectionChanged.onSelectionChanged(previousSelectedCount >= 1, selectedCount >= 1, selectedObjects);
      }
      return true;
    }

    /**
     * Sets the selection state for a given business object.
     * <p>
     * <p>
     * However, the call does not trigger the
     * {@link AppPublics.MultiSelectionHandler.OnMultiSelectionChanged#onSelectionChanged(boolean, boolean, List)} callback.
     * </p>
     *
     * @param businessObject the business object related to that selection event
     * @param selected       whether the business object should be considered as selected
     */
    public void setSelection(BusinessObjectClass businessObject, boolean selected)
    {
      selectedCount += (selected == true ? 1 : -1);
      if (businessObject != null)
      {
        if (selected == true)
        {
          if (selectedObjects.contains(businessObject) == false)
          {
            selectedObjects.add(businessObject);
          }
        }
        else
        {
          selectedObjects.remove(businessObject);
        }
      }
    }

  }

  /**
   * A broadcast listener which listens only to {@link AppPublics#UI_LOAD_ACTION} intents.
   * <p>
   * <p>
   * It is commonly used for {@link Activity} and {@link Fragment}, to get events when the entity is being loaded.
   * </p>
   *
   * @since 2010.02.04
   */
  public static abstract class ComponentBroadcastListener
      implements AppPublics.BroadcastListener
  {

    private static final String EXTRA_VIA_CATEGORIES = "viaCategories";

    private static final String EXTRA_ACTIVITY_ID = "activityId";

    private static final String EXTRA_COMPONENT_ID = "componentId";

    private static final boolean useCategoriesForFiltering = "".equals("");

    private final Activity activity;

    private final Object component;

    /**
     * Same as {@link ComponentBroadcastListener#ComponentBroadcastListener(Activity, Object)} with second argument set to {@code activity}.
     */
    public ComponentBroadcastListener(Activity activity)
    {
      this(activity, activity);
    }

    public ComponentBroadcastListener(Activity activity, Object component)
    {
      this.activity = activity;
      this.component = component;
    }

    public IntentFilter getIntentFilter(boolean viaClass)
    {
      final IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(getAction());
      if (viaClass == true)
      {
        if (AppPublics.ComponentBroadcastListener.useCategoriesForFiltering == true)
        {
          intentFilter.addCategory(activity.getClass().getName());
          intentFilter.addCategory(component.getClass().getName());
        }
      }
      else
      {
        if (AppPublics.ComponentBroadcastListener.useCategoriesForFiltering == true)
        {
          intentFilter.addCategory(Integer.toString(System.identityHashCode(activity)));
          intentFilter.addCategory(Integer.toString(System.identityHashCode(component)));
        }
      }
      return intentFilter;
    }

    protected abstract String getAction();

    protected final Activity getActivity()
    {
      return activity;
    }

    protected final boolean matchesIntent(Intent intent)
    {
      if (getAction().equals(intent.getAction()) == true)
      {
        if ((intent.getBooleanExtra(AppPublics.ComponentBroadcastListener.EXTRA_VIA_CATEGORIES, false) == true) || (intent.hasExtra(AppPublics.ComponentBroadcastListener.EXTRA_ACTIVITY_ID) == true && System.identityHashCode(activity) == intent.getIntExtra(AppPublics.ComponentBroadcastListener.EXTRA_ACTIVITY_ID, 0) && System.identityHashCode(component) == intent.getIntExtra(AppPublics.ComponentBroadcastListener.EXTRA_COMPONENT_ID, 0)) || (intent.hasExtra(AppPublics.EXTRA_ACTION_ACTIVITY) == true && intent.getStringExtra(AppPublics.EXTRA_ACTION_ACTIVITY).equals(activity.getClass().getName()) == true && intent.getStringExtra(AppPublics.EXTRA_ACTION_COMPONENT).equals(component.getClass().getName()) == true))
        {
          // We know that the event deals with the current (activity, component) entities pair
          return true;
        }
      }
      return false;
    }

  }

  /**
   * A broadcast listener which only watch after {@link AppPublics#UI_LOAD_ACTION} UI loading intents action.
   * <p>
   * <p>
   * It is commonly used for {@link Activity} and {@link Fragment}, to get events when the entity is being loaded.
   * </p>
   *
   * @since 2010.02.04
   */
  public static abstract class LoadingBroadcastListener
      extends AppPublics.ComponentBroadcastListener
  {

    /**
     * Triggers a loading event through a {@linkplain LocalBroadcastManager#sendBroadcast(Intent)} broadcast intent action}.
     *
     * @param context              the context which will be used to trigger the event
     * @param targetActivityClass  the class of the {@link Activity} which should receive the loading event; it is not allowed to be {@code null}
     * @param targetComponentClass the class of the component which should receive the loading event; it is not allowed to be {@code null}
     * @param isLoading            whether this deals with a loading which starts or stop
     * @param addCategory          whether the broadcast intent should contain the target category
     * @deprecated use the {@link #broadcastLoading(Context, int, int, boolean)} form, instead
     */
    public static void broadcastLoading(Context context, Class<? extends Activity> targetActivityClass,
        Class<?> targetComponentClass, boolean isLoading, boolean addCategory)
    {
      final Intent intent = new Intent(AppPublics.UI_LOAD_ACTION).putExtra(AppPublics.EXTRA_UI_LOAD_ACTION_LOADING, isLoading).putExtra(AppPublics.EXTRA_ACTION_ACTIVITY, targetActivityClass.getName()).putExtra(AppPublics.EXTRA_ACTION_COMPONENT, targetComponentClass.getName());
      if (addCategory == true)
      {
        intent.addCategory(targetActivityClass.getName());
        intent.addCategory(targetComponentClass.getName());
      }
      LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Triggers a loading event through a {@linkplain LocalBroadcastManager#sendBroadcast(Intent)} broadcast intent action}.
     *
     * @param context           the context which will be used to trigger the event
     * @param targetActivityId  the identifier of the {@link Activity} which should receive the loading event
     * @param targetComponentId the identifier of the component which should receive the loading event
     * @param isLoading         whether this deals with a loading which starts or stop
     */
    public static void broadcastLoading(Context context, int targetActivityId, int targetComponentId, boolean isLoading)
    {
      // The entities hashCode are taken, because this is safe: read the discussion at
      // http://eclipsesource.com/blogs/2012/09/04/the-3-things-you-should-know-about-hashcode/
      final Intent intent = new Intent(AppPublics.UI_LOAD_ACTION).putExtra(AppPublics.EXTRA_UI_LOAD_ACTION_LOADING, isLoading);
      if (AppPublics.ComponentBroadcastListener.useCategoriesForFiltering == false)
      {
        intent.putExtra(AppPublics.ComponentBroadcastListener.EXTRA_ACTIVITY_ID, targetActivityId).putExtra(AppPublics.ComponentBroadcastListener.EXTRA_COMPONENT_ID, targetComponentId);
      }
      else
      {
        intent.putExtra(AppPublics.ComponentBroadcastListener.EXTRA_VIA_CATEGORIES, true);
        intent.addCategory(Integer.toString(targetActivityId));
        intent.addCategory(Integer.toString(targetComponentId));
      }
      LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private int counter = 0;

    public LoadingBroadcastListener(Activity activity)
    {
      this(activity, activity);
    }

    public LoadingBroadcastListener(Activity activity, Object component)
    {
      super(activity, component);
    }

    public IntentFilter getIntentFilter()
    {
      return getIntentFilter(false);
    }

    public synchronized void onReceive(Intent intent)
    {
      if (matchesIntent(intent) == true)
      {
        // We know that the event deals with the current (activity, component) pair
        final boolean wasLoading = counter >= 1;
        // We only take into account the loading event coming from the activity itself
        final boolean isLoading = intent.getBooleanExtra(AppPublics.EXTRA_UI_LOAD_ACTION_LOADING, true);
        counter += (isLoading == true ? 1 : -1);
        final boolean isNowLoading = counter >= 1;

        // We only trigger an event provided the cumulative loading status has changed
        if (wasLoading != isNowLoading)
        {
          onLoading(isNowLoading);
        }
      }
    }

    @Override
    protected String getAction()
    {
      return AppPublics.UI_LOAD_ACTION;
    }

    /**
     * Is invoked when a new loading event (intent) occurs.
     *
     * @param isLoading is equal to {@code true} if and only if the underlying activity is not loading anymore
     */
    protected abstract void onLoading(boolean isLoading);

  }

  /**
   * A broadcast listener which only watch after {@link AppPublics#UI_LOAD_ACTION} loading intent actions.
   * <p>
   * <p>
   * It is especially useful for indicating to an entity that it should reload its content.
   * </p>
   *
   * @since 2011.08.02
   */
  public static abstract class ReloadBroadcastListener
      extends AppPublics.ComponentBroadcastListener
  {

    /**
     * Triggers a reload event through a {@linkplain LocalBroadcastManager#sendBroadcast(Intent)} broadcast intent action}.
     *
     * @param context              the context which will be used to trigger the event
     * @param targetActivityClass  the class which should receive the loading event; it is not allowed to be {@code null}
     * @param targetComponentClass an optional (may be {@code null}) class which refines the component that should receive the event
     */
    public static void broadcastReload(Context context, Class<? extends Activity> targetActivityClass,
        Class<?> targetComponentClass)
    {
      final Intent intent = new Intent(AppPublics.RELOAD_ACTION);
      if (AppPublics.ComponentBroadcastListener.useCategoriesForFiltering == true)
      {
        intent.putExtra(AppPublics.ComponentBroadcastListener.EXTRA_VIA_CATEGORIES, true);
        intent.addCategory(targetActivityClass.getName());
        if (targetComponentClass != null)
        {
          intent.addCategory(targetComponentClass.getName());
        }
      }
      else
      {
        intent.putExtra(AppPublics.EXTRA_ACTION_ACTIVITY, targetActivityClass.getName()).addCategory(targetActivityClass.getName()).putExtra(AppPublics.EXTRA_ACTION_COMPONENT, targetComponentClass == null ? null : targetComponentClass.getName());
      }
      LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Creates an {@link IntentFilter} which is able to listen for a broadcast reload events.
     *
     * @param intentFilter         an already existing intent filter, which will be enriched
     * @param targetActivityClass  the class which is supposed to receive the broadcast event
     * @param targetComponentClass an optional (may be {@code null}) class which refines the component that is supposed to receive the event
     * @return the provided intent filter, which has been enriched
     */
    public static IntentFilter addReload(IntentFilter intentFilter, Class<? extends Activity> targetActivityClass,
        Class<?> targetComponentClass)
    {
      intentFilter.addAction(AppPublics.RELOAD_ACTION);
      intentFilter.addCategory(targetActivityClass.getName());
      if (targetComponentClass != null)
      {
        intentFilter.addCategory(targetComponentClass.getName());
      }
      return intentFilter;
    }

    /**
     * Indicates whether an intent matches a reload broadcast event.
     *
     * @param intent               the intent that has been received and which is to be analyzed
     * @param targetActivityClass  the class which is supposed to receive the broadcast event
     * @param targetComponentClass an optional (may be {@code null}) class which refines the component that is supposed to receive the event
     * @return {@code true} if and only if the intent matches the expected event
     */
    public static boolean matchesReload(Intent intent, Class<? extends Activity> targetActivityClass,
        Class<?> targetComponentClass)
    {
      if (AppPublics.RELOAD_ACTION.equals(intent.getAction()) == true)
      {
        if ((intent.getBooleanExtra(AppPublics.ComponentBroadcastListener.EXTRA_VIA_CATEGORIES, false) == true) || (intent.hasExtra(AppPublics.EXTRA_ACTION_ACTIVITY) == true && intent.getStringExtra(AppPublics.EXTRA_ACTION_ACTIVITY).equals(targetActivityClass.getName()) == true && (targetComponentClass == null || intent.getStringExtra(AppPublics.EXTRA_ACTION_COMPONENT).equals(targetComponentClass) == true)))
        {
          // We know that the event deals with the current (activity, component) entities pair
          return true;
        }
      }
      return false;
    }

    public ReloadBroadcastListener(Activity activity)
    {
      this(activity, activity);
    }

    public ReloadBroadcastListener(Activity activity, Object component)
    {
      super(activity, component);
    }

    public IntentFilter getIntentFilter()
    {
      return getIntentFilter(true);
    }

    public void onReceive(Intent intent)
    {
      if (matchesIntent(intent) == true)
      {
        onReload();
      }
    }

    @Override
    protected String getAction()
    {
      return AppPublics.RELOAD_ACTION;
    }

    /**
     * The callback that will be triggered if the {@link AppPublics#RELOAD_ACTION} action is caught for the provided {@link Activity} class.
     */
    protected abstract void onReload();

  }

  /**
   * A flag name which indicates the FQN name of the activity class concerned with the action.
   */
  public final static String EXTRA_ACTION_ACTIVITY = "activity";

  /**
   * A flag name which indicates the FQN name of the component class concerned with the action.
   */
  public final static String EXTRA_ACTION_COMPONENT = "component";

  /**
   * An extra which designates a business object.
   */
  public final static String EXTRA_BUSINESS_OBJECT = "businessObject";

  /**
   * A flag name which indicates whether the loading event is starting or ending.
   */
  public final static String EXTRA_UI_LOAD_ACTION_LOADING = "loading";

  /**
   * Use this intent action when you need to indicate that something is being loaded. For indicating that the loading is over, attach the
   * {@link AppPublics#EXTRA_UI_LOAD_ACTION_LOADING} key to the {@link Intent}.
   */
  public static String UI_LOAD_ACTION = "com.smartnsoft.droid4me.action.UI_LOADING";

  /**
   * Use this intent action when you need to indicate that an activity should refresh its UI.
   */
  public static String UPDATE_ACTION = "com.smartnsoft.droid4me.action.UPDATE";

  /**
   * Use this intent action when you need to indicate that an activity should reload its business objects.
   */
  public static String RELOAD_ACTION = "com.smartnsoft.droid4me.action.RELOAD";

  /**
   * Defined in order to compute the default intent actions.
   * <p>
   * <p>
   * Is invoked by the framework, at initialization time.
   * </p>
   */
  public static void initialize(Application application)
  {
    AppPublics.UI_LOAD_ACTION = application.getPackageName() + ".action.UI_LOADING";
    AppPublics.UPDATE_ACTION = application.getPackageName() + ".action.UPDATE";
    AppPublics.RELOAD_ACTION = application.getPackageName() + ".action.RELOAD";
    AppPublics.MultiSelectionHandler.SELECTION_ACTION = application.getPackageName() + ".action.SELECTION";
  }

  /**
   * There is no reason creating an instance of that class, which is just a container.
   */
  private AppPublics()
  {
  }

}
