/*
 * (C) Copyright 2009-2015 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
 *
 * The code hereby is the full property of Smart&Soft, SIREN 444 622 690.
 * 34, boulevard des Italiens - 75009 - Paris - France
 * contact@smartnsoft.com - 00 33 6 79 60 05 49
 *
 * You are not allowed to use the source code or the resulting binary code, nor to modify the source code, without prior permission of the owner.
 *
 * This library is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * Contributors:
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import com.smartnsoft.droid4me.app.ActivityController.Interceptor;
import com.smartnsoft.droid4me.app.AppPublics.BroadcastListener;
import com.smartnsoft.droid4me.app.AppPublics.UseNativeBroadcast;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.support.v4.content.LocalBroadcastManager;

/**
 * A basis class responsible for listening to connectivity events.
 * <p/>
 * <p>
 * <b>Caution: this component requires the Android {@code android.permission.ACCESS_NETWORK_STATE} permission!</b>.
 * </p>
 * <p/>
 * <p>
 * This component will issue a {@link Intent broadcast Intent}, every time the hosting application Internet connectivity status changes with the
 * {@link ConnectivityListener#CONNECTIVITY_CHANGED_ACTION} action, and an extra {@link ConnectivityListener#EXTRA_HAS_CONNECTIVITY boolean flag},
 * which states the current application connectivity status.
 * </p>
 * <p>
 * This component should be created during the {@link com.smartnsoft.droid4me.app.SmartApplication#onCreateCustom()} method, and it should be enrolled
 * to all the hosting application {@link Activity activities}, during the {@link com.smartnsoft.droid4me.app.SmartApplication#getInterceptor()} when
 * receiving the {@link ActivityController.Interceptor.InterceptorEvent#onCreate} and {@link ActivityController.Interceptor.InterceptorEvent#onResume}
 * events.
 * </p>
 *
 * @author Édouard Mercier
 * @since 2012.04.04
 */
public abstract class ConnectivityListener
    implements Interceptor
{

  protected final static Logger log = LoggerFactory.getInstance(ConnectivityListener.class);

  /**
   * The action that will used to notify via a broadcast {@link Intent} when the hosting application Internet connectivity changes.
   */
  public static final String CONNECTIVITY_CHANGED_ACTION = "connectivityChangedAction";

  /**
   * A broadcast {@link Intent} boolean flag which indicates the hosting application Internet connectivity status.
   */
  public static final String EXTRA_HAS_CONNECTIVITY = "hasConnectivity";

  private final Context context;

  private boolean hasConnectivity = true;

  private NetworkCallback networkCallback;

  private int activitiesCount;

  private Map<String, Boolean> networkStatus = new HashMap<String, Boolean>();

  /**
   * The constructor will issue an exception if the hosting application does not declare the {@code android.permission.ACCESS_NETWORK_STATE}
   * permission.
   *
   * @param context the application context
   */
  public ConnectivityListener(Context context)
  {
    this.context = context;
    activitiesCount = 0;
    // We immediately extract the connectivity status
    final NetworkInfo activeNetworkInfo = getActiveNetworkInfo();
    if (activeNetworkInfo == null || activeNetworkInfo.isConnected() == false)
    {
      if (log.isInfoEnabled())
      {
        log.info("The Internet connection is off");
      }
      hasConnectivity = false;
      notifyServices(hasConnectivity);
    }
  }

  /**
   * @return whether the device has Internet connectivity
   */
  public boolean hasConnectivity()
  {
    return hasConnectivity;
  }

  /**
   * @return the currently active network info; may be {@code null}
   */
  public NetworkInfo getActiveNetworkInfo()
  {
    return getConnectivityManager().getActiveNetworkInfo();
  }


  /**
   * This method should be invoked during the {@link Activity#onResume()} or {android.app.Fragment#onResume()} methods.
   * <p/>
   * <p>
   * If the provided {@code activity} parameter does not implement that interface, or if it has a {@link Activity#getParent()} method, or if the
   * {@code component} second parameter is {@code null} the method will do nothing.
   * </p>
   * <p/>
   * <p>
   * This method will invoke the {@link #updateActivity(Smarted)} method from the calling thread.
   * </p>
   *
   * @param activity  an {@link Activity} which is supposed to implement the {@link Smarted} interface
   * @param component when not-null, the {@link android.app.Fragment} in which the {@link android.app.Fragment#onResume()} method is invoked
   */
  public void updateActivityOnCreate(Activity activity, Object component)
  {
    // We transmit the connectivity status
    if (component == null && activity.getParent() == null && activity instanceof Smarted<?>)
    {
      final Smarted<?> smartedActivity = (Smarted<?>) activity;
      updateActivity(smartedActivity);
    }
  }

  /**
   * This method is invoked when the component has detected an Internet connectivity change. It is invoked a first time when the connectivity status
   * is known.
   * <p/>
   * <p>
   * It is a place-holder for notify all components depending on the Internet connectivity about the new status.
   * </p>
   * <p/>
   * <p>
   * Note that this method will be invoked from the UI thread.
   * </p>
   *
   * @param hasConnectivity the new Internet connectivity status
   */
  protected abstract void notifyServices(boolean hasConnectivity);

  /**
   * This method is invoked systematically during the {@link Activity#onResume()} method, provided the
   * {@link #registerBroadcastListenerOnCreate(Activity, Object)} method has been invoked.
   * <p/>
   * <p>
   * It is a place-holder for updating the {@link Activity} Internet connectivity new status.
   * </p>
   * <p/>
   * <p>
   * Note that this method will be invoked from the UI thread.
   * </p>
   *
   * @param smartedActivity the {@link Smarted} {@link Activity} that should be updated graphically
   */
  protected abstract void updateActivity(final Smarted<?> smartedActivity);


  /**
   * This method should be invoked during the {@link ActivityController.Interceptor#onLifeCycleEvent(Activity, Object, InterceptorEvent)} method, and
   * it will handle everything.
   */
  @Override
  public void onLifeCycleEvent(Activity activity, Object component, InterceptorEvent event)
  {
    if (event == ActivityController.Interceptor.InterceptorEvent.onCreate)
    {
      // We listen to the network connection potential issues: we do not want child activities to also register for the connectivity change events
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
      {
        registerBroadcastListenerOnCreate(activity, component);
      }
      else
      {
        registerBroadcastListenerOnCreateLollipopAndAbove(activity, component);
      }
      // We transmit the connectivity status
      updateActivityOnCreate(activity, component);
    }
    else if (event == InterceptorEvent.onDestroy)
    {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      {
        unregisterBroadcastListenerLollipopAndAbove(activity, component);
      }
    }
  }

  /**
   * @return the Android {@link ConnectivityManager} which may be requested to get the network connexion status, for instance
   */
  protected final ConnectivityManager getConnectivityManager()
  {
    return ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
  }

  /**
   * This method should be invoked during the {@code Activity.onCreate()} or {@code Fragment.onCreate()} methods, so that it listens to the
   * Internet connectivity status, and so that it is notified when this status changes.
   * <p/>
   * <p>
   * If the provided {@code activity} parameter does not implement that interface, or if it has a {@link Activity#getParent()} method, or if the
   * {@code component} second parameter is {@code null} the method will do nothing.
   * </p>
   * <p/>
   * <p>
   * This method will {@link Smarted#registerBroadcastListeners(BroadcastListener[]) register} the current instance.
   * </p>
   *
   * @param activity  an {@link Activity} which is supposed to implement the {@link Smarted} interface
   * @param component when not-null, the {@link android.app.Fragment} in which the {@link android.app.Fragment#onCreate(Bundle)} method is invoked
   */
  private void registerBroadcastListenerOnCreate(final Activity activity, final Object component)
  {
    // We listen to the network connection potential issues: we do not want child activities to also register for the connectivity change events
    if (component == null && activity.getParent() == null && activity instanceof Smarted<?>)
    {
      final Smarted<?> smartedActivity = (Smarted<?>) activity;
      final BroadcastListener broadcastListener = new BroadcastListener()
      {

        @UseNativeBroadcast
        @Override
        public IntentFilter getIntentFilter()
        {
          return new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        }

        @Override
        public void onReceive(Intent intent)
        {
          // There is only one registered Intent action, hence, we do not need any filtering test
          final boolean previousConnectivity = hasConnectivity;
          hasConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) == false;
          handleConnectivityChange(activity, component, previousConnectivity, true);
        }
      };
      smartedActivity.registerBroadcastListeners(new AppPublics.BroadcastListener[] { broadcastListener });
    }
  }


  @TargetApi(VERSION_CODES.LOLLIPOP)
  private void registerBroadcastListenerOnCreateLollipopAndAbove(final Activity activity, final Object component)
  {
    // We listen to the network connection potential issues: we do not want child Activities to also register for the connectivity change events
    if (component == null && activity.getParent() == null && activity instanceof Smarted<?>)
    {
      activitiesCount++;

      // No need to synchronize this scope, because the method is invoked from the UI thread
      if (networkCallback == null)
      {
        final NetworkRequest.Builder builder = new NetworkRequest.Builder();
        networkCallback = new ConnectivityManager.NetworkCallback()
        {
          @Override
          public void onAvailable(Network network)
          {
            networkStatus.put(network.toString(), true);
            onNetworkChangedLollipopAndAbove(activity, component, networkStatus.containsValue(true));
          }

          @Override
          public void onLost(Network network)
          {
            networkStatus.remove(network.toString());
            onNetworkChangedLollipopAndAbove(activity, component, networkStatus.containsValue(true));
          }
        };

        getConnectivityManager().registerNetworkCallback(builder.build(), networkCallback);
        if (log.isDebugEnabled())
        {
          log.debug("Registered the Lollipop network callback");
        }
      }
    }
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  private void unregisterBroadcastListenerLollipopAndAbove(Activity activity, Object component)
  {
    // We listen to the network connection potential issues: we do not want child activities to also register for the connectivity change events
    if (component == null && activity.getParent() == null && activity instanceof Smarted<?>)
    {
      activitiesCount--;

      if (activitiesCount <= 0)
      {
        if (networkCallback != null)
        {
          getConnectivityManager().unregisterNetworkCallback(networkCallback);
          if (log.isDebugEnabled())
          {
            log.debug("Unregisters the Lollipop network callback");
          }
          networkCallback = null;
        }
      }
    }
  }


  private void onNetworkChangedLollipopAndAbove(Activity activity, Object component, boolean hasConnectivity)
  {
    final boolean previousConnectivity = this.hasConnectivity;
    this.hasConnectivity = hasConnectivity;
    handleConnectivityChange(activity, component, previousConnectivity, false);
  }

  private void handleConnectivityChange(Activity activity, Object component, boolean previousConnectivity,
      boolean invokeUpdateActivity)
  {
    if (previousConnectivity != hasConnectivity)
    {
      // With this filter, only one broadcast listener will handle the event
      if (log.isInfoEnabled())
      {
        log.info("Received an Internet connectivity change event: the connection is now " + (hasConnectivity == false ? "off" : "on"));
      }
      // We notify the application regarding this connectivity change event
      LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ConnectivityListener.CONNECTIVITY_CHANGED_ACTION).putExtra(ConnectivityListener.EXTRA_HAS_CONNECTIVITY, hasConnectivity));

      notifyServices(hasConnectivity);
      if (invokeUpdateActivity == true)
      {
        updateActivityOnCreate(activity, component);
      }
    }
  }

}
