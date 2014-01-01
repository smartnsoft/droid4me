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

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

import org.apache.http.NoHttpResponseException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Window;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Is responsible for intercepting an activity starting and redirect it to a prerequisite one if necessary, and for handling globally exceptions.
 * 
 * <p>
 * Everything described here which involves the {@link Activity activities}, is applicable provided the activity is a {@link Smartable}.
 * </p>
 * 
 * <p>
 * It is also a container for multiple interfaces relative to its architecture.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2009.04.28
 */
public class ActivityController
{

  /**
   * An interface which is requested when a new {@link Activity} is bound to be {@link Context#startActivity(Intent) started}.
   * 
   * <p>
   * The redirector acts as a controller over the activities starting phase: if an activity should be started before another one is really
   * {@link Activity#onResume() active}, this is the right place to handle this at runtime.
   * </p>
   * 
   * <p>
   * This component is especially useful when ones need to make sure that an {@link Activity} has actually been submitted to the end-user before
   * resuming a workflow. The common cases are the traditional application splash screen, or a signin/signup process.
   * </p>
   * 
   * @see ActivityController#registerRedirector(Redirector)
   */
  public interface Redirector
  {

    /**
     * Will be invoked by the {@link ActivityController#needsRedirection(Activity) framework}, in order to know whether an {@link Activity} should be
     * started instead of the provided one, which is supposed to have just {@link Activity#onCreate(Bundle) started}, or when the
     * {@link Activity#onNewIntent()} method is invoked. However, the method will be not been invoked when those methods are invoked due to a
     * {@link Activity#onConfigurationChanged(android.content.res.Configuration) configuration change}.
     * 
     * <p>
     * Caution: if an exception is thrown during the method execution, the application will crash!
     * </p>
     * 
     * @param activity
     *          the activity which is bound to be displayed
     * @return {@code null} if and only if nothing is to be done, i.e. no activity should be started instead. Otherwise, the given intent will be
     *         executed: in that case, the provided activity {@Activity#finish finishes}
     * @see ActivityController#needsRedirection(Activity)
     */
    Intent getRedirection(Activity activity);

  }

  /**
   * An empty interface which should be used as a marker on an {@link Activity}, which does not want to be requested by the
   * {@link ActivityController.Redirector}.
   * 
   * <p>
   * When an {@link Activity} implements this interface, the {@link ActivityController.Redirector#getRedirection(Activity)} method will not be
   * invoked.
   * </p>
   * 
   * @since 2012.04.11
   * @see ActivityController#needsRedirection(Activity)
   */
  public interface EscapeToRedirector
  {
  }

  /**
   * An interface responsible for providing Android system services, given their name. It enables to override in one place all the {@code Activity}
   * system services.
   * 
   * @since 2012.02.12
   */
  public interface SystemServiceProvider
  {

    /**
     * Returns the handle to a system-level service by name. The class of the returned object varies by the requested name.
     * 
     * @param activity
     *          the {@link Activity} asking for the service
     * @param name
     *          the name of the desired service
     * @param defaultService
     *          the provided {@code activity} default system service (retrieved by invoking the {@link Activity#getSystemService(String)} method)
     * @return the desired service, or {@code null} if no service corresponding to the provided {@code name} is available nor exists
     * @see {@link Activity#getSystemService(String)}
     */
    Object getSystemService(Activity activity, String name, Object defaultService);

  }

  /**
   * An interface which is queried during the various life cycle events of a {@link LifeCycle}.
   * 
   * <p>
   * An interceptor is the ideal place for centralizing in one place many of the {@link Activity}/{@link android.app.Fragment} entity life cycle
   * events.
   * </p>
   * 
   * @see ActivityController#registerInterceptor(Interceptor)
   */
  public interface Interceptor
  {

    /**
     * Defines all the events handled by the {@link ActivityController.Interceptor}.
     */
    public static enum InterceptorEvent
    {
      /**
       * Called during the {@link Activity#onCreate()} / {@link android.app.Fragment#onCreate()} method, before the Android built-in super method
       * {@link Activity#onCreate} method is invoked.
       * 
       * <p>
       * This is an ideal place where to {@link Window#requestFeature() request for window features}.
       * </p>
       */
      onSuperCreateBefore,
      /**
       * Called during the {@link Activity#onCreate()} / {@link android.app.Fragment#onCreate()} method, at the beginning of the method, but after the
       * parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onCreate,
      /**
       * Called during the {@link Activity#onCreate()} / {@link android.app.Fragment#onCreate()} method, at the very end of the method, after the
       * parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onCreateDone,
      /**
       * <b>Only applies to {@link Activity} entities!</b>.Called during the {@link Activity#onPostCreate()} method, at the beginning of the method,
       * but after the parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onPostCreate,
      /**
       * <b>Only applies to {@link Activity} entities!</b>.Called at the end of the {@link Activity#onContentChanged()} method execution, but after
       * the parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onContentChanged,
      /**
       * Called during the {@link Activity#onStart} / {@link android.app.Fragment#onStart()} method, at the beginning of the method, but after the
       * parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested and that the instance has not been
       * recreated due a configuration change.
       */
      onStart,
      /**
       * <b>Only applies to {@link Activity} entities!</b>.Called during the {@link Activity#onRestart()} method, after the parent's call, provided no
       * {@link ActivityController.Redirector activity redirection} is requested and that the instance has not been recreated due a configuration
       * change.
       */
      onRestart,
      /**
       * Called during the {@link Activity#onResume()} / {@link android.app.Fragment#onResume()} method, at the beginning of the method, but after the
       * parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onResume,
      /**
       * <b>Only applies to {@link Activity} entities!</b>.Called during the {@link Activity#onPostResume()} method, at the beginning of the method,
       * but after the parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onPostResume,
      /**
       * Called during the {@link Activity#onPause()} / {@link android.app.Fragment#onPause()} method, at the beginning of the method, but after the
       * parent's call, provided no {@link ActivityController.Redirector activity redirection} is requested.
       */
      onPause,
      /**
       * Called during the {@link Activity#onStop()} / {@link android.app.Fragment#onPause()} method, at the beginning of the method, before the
       * parent's call.
       */
      onStop,
      /**
       * Called just after the {@link LifeCycle#onFulfillDisplayObjects} method.
       */
      onFulfillDisplayObjectsDone,
      /**
       * Called just after the {@link LifeCycle#onSynchronizeDisplayObjects} method.
       */
      onSynchronizeDisplayObjectsDone,
      /**
       * Called during the {@link Activity#onDestroy()} / {@link android.app.Fragment#onDestroy()} method, at the very end of the method.
       */
      onDestroy
    }

    /**
     * A logger which may be used by the classes implementing this interface.
     */
    public static final Logger log = LoggerFactory.getInstance(Interceptor.class);

    /**
     * Invoked every time a new event occurs on the provided {@code activity}/{@code component}. For instance, this is an ideal for logging
     * application usage analytics.
     * 
     * <p>
     * The framework ensures that this method will be invoked from the UI thread, hence the method implementation should last a very short time!
     * <p>
     * 
     * <p>
     * Caution: if an exception is thrown during the method execution, the application will crash!
     * </p>
     * 
     * @param activity
     *          the activity on which a life cycle event occurs ; cannot be {@code null}
     * @param component
     *          the component on which the life cycle event occurs ; may be {@code null}
     * @param event
     *          the event that has just happened
     * 
     * @see ActivityController#onLifeCycleEvent()
     */
    void onLifeCycleEvent(Activity activity, Object component, ActivityController.Interceptor.InterceptorEvent event);

  }

  /**
   * Defines and splits the handling of various exceptions in a single place. This handler will be invoked once it has been
   * {@link ActivityController#registerExceptionHandler(ExceptionHandler) registered}.
   * 
   * <p>
   * The exception handler will be invoked at runtime when an exception is thrown and is not handled. You do not need to log the exception, because
   * the {@link ActivityController} already takes care of logging it, before invoking the current interface methods.
   * </p>
   * 
   * @see ActivityController#registerExceptionHandler(ExceptionHandler)
   */
  public interface ExceptionHandler
  {

    /**
     * Is invoked whenever the {@link LifeCycle#onRetrieveBusinessObjects()} throws an exception.
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @param activity
     *          the activity that issued the exception, and which is ensured not to be {@link Activity#finish() finishing} ; cannot be {@code null}
     * @param component
     *          the component that issued the exception ; may be {@code null}
     * @param exception
     *          the exception that has been thrown
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onBusinessObjectAvailableException(Activity activity, Object component, BusinessObjectUnavailableException exception);

    /**
     * Is invoked whenever an activity implementing {@link LifeCycle} throws an unexpected exception outside from the
     * {@link LifeCycle#onRetrieveBusinessObjects()} method.
     * 
     * <p>
     * This method serves as a fallback on the framework, in order to handle gracefully exceptions and prevent the application from crashing.
     * </p>
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @param activity
     *          the activity that issued the exception ; cannot be {@code null}
     * @param component
     *          the component that issued the exception ; may be {@code null}
     * @param throwable
     *          the exception that has been triggered
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onActivityException(Activity activity, Object component, Throwable throwable);

    /**
     * Is invoked whenever a handled exception is thrown with a non-{@link Activity} / {@link android.app.Fragment} {@link Context context}.
     * 
     * <p>
     * This method serves as a fallback on the framework, in order to handle gracefully exceptions and prevent the application from crashing.
     * </p>
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @param context
     *          the context that issued the exception
     * @param throwable
     *          the exception that has been triggered
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onContextException(Context context, Throwable throwable);

    /**
     * Is invoked whenever a handled exception is thrown outside from an available {@link Context context}.
     * 
     * <p>
     * This method serves as a fallback on the framework, in order to handle gracefully exceptions and prevent the application from crashing.
     * </p>
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @param throwable
     *          the exception that has been triggered
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onException(Throwable throwable);

  }

  /**
   * Responsible for analyzing issues resulting from {@link Throwable} entities.
   * 
   * @since 2013.12.23
   * 
   */
  public static abstract class IssueAnalyzer
  {

    public final static class IssueContext
    {

      private final static String SPLITTER = ";";

      public final String applicationName;

      public final String applicationVersionName;

      public final int applicationVersionCode;

      public final String deviceModel;

      public final String firmwareVersion;

      public final String buildNumber;

      public IssueContext(String string)
      {
        final String[] tokens = string.split(IssueContext.SPLITTER);
        int index = 0;
        applicationName = tokens[index++];
        applicationVersionName = tokens[index++];
        applicationVersionCode = Integer.parseInt(tokens[index++]);
        deviceModel = tokens[index++];
        firmwareVersion = tokens[index++];
        buildNumber = tokens[index++];
      }

      public IssueContext(Context context)
      {
        applicationName = context.getString(context.getApplicationInfo().labelRes);
        PackageInfo packageInfo = null;
        try
        {
          packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        }
        catch (NameNotFoundException exception)
        {
          // Cannot happen
        }
        applicationVersionName = packageInfo == null ? "" : packageInfo.versionName;
        applicationVersionCode = packageInfo == null ? -1 : packageInfo.versionCode;
        deviceModel = Build.MODEL;
        firmwareVersion = Build.VERSION.RELEASE;
        buildNumber = Build.DISPLAY;
      }

      @Override
      public String toString()
      {
        return applicationName + IssueContext.SPLITTER + applicationVersionName + IssueContext.SPLITTER + applicationVersionCode + IssueContext.SPLITTER + deviceModel + IssueContext.SPLITTER + firmwareVersion + IssueContext.SPLITTER + buildNumber;
      }

      public String toHumanString()
      {
        final StringBuilder sb = new StringBuilder();
        sb.append("applicationName = ").append(applicationName).append("\n");
        sb.append("applicationVersionName = ").append(applicationVersionName).append("\n");
        sb.append("applicationVersionCode = ").append(applicationVersionCode).append("\n");
        sb.append("deviceModel = ").append(deviceModel).append("\n");
        sb.append("firmwareVersion = ").append(firmwareVersion).append("\n");
        sb.append("buildNumber = ").append(buildNumber).append("\n");
        return sb.toString();
      }

    }

    protected final static Logger log = LoggerFactory.getInstance(IssueAnalyzer.class);

    /**
     * Attempts to find a specific exception in the provided exception by iterating over the causes, starting with the provided exception itself.
     * 
     * @param throwable
     *          the exception to be inspected
     * @param exceptionClass
     *          a list of exception classes to look after
     * @return {@code null} if and only one of the provided exception classes has not been detected ; the matching cause otherwise
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Throwable searchForCause(Throwable throwable, Class... exceptionClass)
    {
      Throwable newThrowable = throwable;
      Throwable cause = throwable;
      // We investigate over the whole causes stack
      while (cause != null)
      {
        for (Class<? extends Throwable> anExceptionClass : exceptionClass)
        {
          final Class<? extends Throwable> causeClass = cause.getClass();
          if (causeClass == anExceptionClass)
          {
            return cause;
          }
          // We scan the cause class hierarchy
          Class<?> superclass = causeClass.getSuperclass();
          while (superclass != null)
          {
            if (superclass == anExceptionClass)
            {
              return cause;
            }
            superclass = superclass.getSuperclass();
          }
        }
        // It seems that when there are no more causes, the exception itself is returned as a cause: stupid implementation!
        if (newThrowable.getCause() == newThrowable)
        {
          break;
        }
        newThrowable = cause;
        cause = newThrowable.getCause();
      }
      return null;
    }

    /**
     * @param throwable
     *          the exception to investigate
     * @return {@code true} if and only if the exception results from a connectivity issue by inspecting its causes tree
     */
    public static boolean isAConnectivityProblem(Throwable throwable)
    {
      return ActivityController.IssueAnalyzer.searchForCause(throwable, UnknownHostException.class, SocketException.class, SocketTimeoutException.class,
          InterruptedIOException.class, NoHttpResponseException.class, SSLException.class) != null;
    }

    /**
     * @param throwable
     *          the exception to investigate
     * @return {@code true} if and only if the exception results from a memory saturation issue (i.e. a {@link OutOfMemoryError} exception) by
     *         inspecting its causes tree
     */
    public static boolean isAMemoryProblem(Throwable throwable)
    {
      return ActivityController.IssueAnalyzer.searchForCause(throwable, OutOfMemoryError.class) != null;
    }

    protected final Context context;

    /**
     * @param context
     *          should be an application {@link Context}
     */
    public IssueAnalyzer(Context context)
    {
      this.context = context;
    }

    /**
     * Is responsible for analyzing the provided exception, and indicates whether it has been handled.
     * 
     * @param throwable
     *          the issue to analyze
     * @return {@code true} if and only if the issue has actually been handled by the implementation
     */
    public abstract boolean handleIssue(Throwable throwable);

  }

  private static final Logger log = LoggerFactory.getInstance(ActivityController.class);

  /**
   * When a new activity is {@link Context#startActivity(Intent) started} because of a redirection, the newly started activity will receive the
   * initial activity {@link Intent} through this {@link Parcelable} key.
   * 
   * @see #needsRedirection(Activity)
   * @see #registerInterceptor(ActivityController. Interceptor)
   */
  public static final String CALLING_INTENT = "com.smartnsoft.droid4me.callingIntent";

  /**
   * A singleton pattern is available for the moment.
   */
  private static volatile ActivityController instance;

  /**
   * The only way to access to the activity controller.
   */
  // Implements a "double-checked locking" pattern.
  public static ActivityController getInstance()
  {
    if (ActivityController.instance == null)
    {
      synchronized (ActivityController.class)
      {
        if (ActivityController.instance == null)
        {
          ActivityController.instance = new ActivityController();
        }
      }
    }
    return ActivityController.instance;
  }

  private ActivityController.Redirector redirector;

  private ActivityController.SystemServiceProvider systemServiceProvider;

  private ActivityController.Interceptor interceptor;

  private ActivityController.ExceptionHandler exceptionHandler;

  /**
   * No one else than the framework should create such an instance.
   */
  private ActivityController()
  {
  }

  /**
   * Attempts to decode from the provided {@code activity} the original {@code Intent} that was
   * 
   * @param activity
   *          the Activity whose Intent will be analyzed
   * @return an Intent that may be {@link Activity#startActivity(Intent) started} if the provided {@code activity} actually contains a reference to
   *         another {@link Activity} ; {@code null} otherwise
   * @see ActivityController#CALLING_INTENT
   * @see #needsRedirection(Activity)
   * @see #registerInterceptor(ActivityController. Interceptor)
   */
  public static Intent extractCallingIntent(Activity activity)
  {
    return activity.getIntent().getParcelableExtra(ActivityController.CALLING_INTENT);
  }

  /**
   * Remembers the system service provider that will be used by the framework, for overriding the {@link Activity#getSystemService(String)} method.
   * 
   * @param systemServiceProvider
   *          the system service provider which will be invoked at runtime, when an {@link Activity} asks for a service ; if {@code null}, the
   *          {@link Activity} default service will be used
   */
  public void registerSystemServiceProvider(ActivityController.SystemServiceProvider systemServiceProvider)
  {
    this.systemServiceProvider = systemServiceProvider;
  }

  /**
   * Remembers the activity redirector that will be used by the framework, before {@link Context#startActivity(Intent) starting} a new
   * {@link Activity}.
   * 
   * @param redirector
   *          the redirector that will be requested at runtime, when a new activity is being started; if {@code null}, no redirection mechanism will
   *          be set up
   */
  public void registerRedirector(ActivityController.Redirector redirector)
  {
    this.redirector = redirector;
  }

  /**
   * Remembers the activity interceptor that will be used by the framework, on every {@link ActivityController.Interceptor.InterceptorEvent event}
   * during the underlying {@link Activity} life cycle.
   * 
   * @param interceptor
   *          the interceptor that will be invoked at runtime, on every event; if {@code null}, no interception mechanism will be used
   */
  public void registerInterceptor(ActivityController.Interceptor interceptor)
  {
    this.interceptor = interceptor;
  }

  /**
   * Is responsible for returning a system service, just like the {@link Context#getSystemService(String)} method does.
   * 
   * @param activity
   *          the activity asking for a system service
   * @param name
   *          the name of the desired service
   * @param defaultService
   *          the {@code activity} default service
   * @return the service or {@code null} if the name does not exist
   * @see #registerSystemServiceProvider(ActivityController.SystemServiceProvider)
   */
  public Object getSystemService(Activity activity, String name, Object defaultService)
  {
    if (systemServiceProvider != null)
    {
      return systemServiceProvider.getSystemService(activity, name, defaultService);
    }
    return defaultService;
  }

  /**
   * Gives access to the currently registered {@link ActivityController.ExceptionHandler}.
   * 
   * @return the currently registered exception handler ; may be {@code null}, which is the default status
   * @see #registerExceptionHandler(ActivityController.ExceptionHandler)
   */
  public ActivityController.ExceptionHandler getExceptionHandler()
  {
    return exceptionHandler;
  }

  /**
   * Remembers the exception handler that will be used by the framework.
   * 
   * @param exceptionHandler
   *          the handler that will be invoked in case of exception; if {@code null}, no exception handler will be used
   * @see #getExceptionHandler()
   */
  public synchronized void registerExceptionHandler(ActivityController.ExceptionHandler exceptionHandler)
  {
    this.exceptionHandler = exceptionHandler;
  }

  /**
   * Is invoked by the framework every time a life cycle event occurs for the provided activity. You should not invoke that method yourself!
   * 
   * <p>
   * Note that the method is synchronized, which means that the previous call will block the next one, if no thread is spawn.
   * </p>
   * 
   * @param activity
   *          the activity which is involved with the event : cannot be {@code null}
   * @param component
   *          the component the event occurs on ; may be {code null}
   * @param event
   *          the event that has just happened for that activity
   */
  public synchronized void onLifeCycleEvent(Activity activity, Object component, ActivityController.Interceptor.InterceptorEvent event)
  {
    if (interceptor == null)
    {
      return;
    }
    interceptor.onLifeCycleEvent(activity, component, event);
  }

  /**
   * Dispatches the exception to the {@link ActivityController.ExceptionHandler}, and invokes the right method depending on its nature.
   * 
   * <p>
   * The framework is responsible for invoking that method every time an unhandled exception is thrown. If no
   * {@link ActivityController#registerExceptionHandler(ExceptionHandler) exception handler is registered}, the exception will be only logged, and the
   * method will return {@code false}.
   * </p>
   * 
   * <p>
   * Note that this method is {@code synchronized}, which prevents it from being invoking while it is already being executed, and which involves that
   * only one {@link Throwable} may be handled at the same time.
   * </p>
   * 
   * @param context
   *          the context that originated the exception ; may be {@code null}
   * @param component
   *          when not {@code null}, this will be the {@link android.app.Fragment} the exception has been thrown from
   * @param throwable
   *          the reported exception
   * @return {@code true} if the exception has been handled ; in particular, if no {@link ActivityController#getExceptionHandler() exception handled
   *         has been set}, returns {@code false}
   */
  public synchronized boolean handleException(Context context, Object component, Throwable throwable)
  {
    if (exceptionHandler == null)
    {
      if (log.isWarnEnabled())
      {
        log.warn("Detected an exception which will not be handled during the processing of the context with name '" + (context == null ? "null"
            : context.getClass().getName()) + "'", throwable);
      }
      return false;
    }
    final Activity activity;
    if (context instanceof Activity)
    {
      activity = (Activity) context;
    }
    else
    {
      activity = null;
    }
    try
    {
      if (activity != null && throwable instanceof BusinessObjectUnavailableException)
      {
        // Should only occur with a non-null activity
        final BusinessObjectUnavailableException exception = (BusinessObjectUnavailableException) throwable;
        if (log.isWarnEnabled())
        {
          log.warn(
              "Caught an exception during the retrieval of the business objects from the activity from class with name '" + activity.getClass().getName() + "'",
              exception);
        }
        // We do nothing if the activity is dying
        if (activity != null && activity.isFinishing() == true)
        {
          return true;
        }
        return exceptionHandler.onBusinessObjectAvailableException(activity, component, exception);
      }
      else
      {
        if (log.isWarnEnabled())
        {
          log.warn("Caught an exception during the processing of " + (context == null ? "a null Context"
              : "the Context from class with name '" + context.getClass().getName()) + "'", throwable);
        }
        // For this special case, we ignore the case when the activity is dying
        if (activity != null)
        {
          return exceptionHandler.onActivityException(activity, component, throwable);
        }
        else if (context != null)
        {
          return exceptionHandler.onContextException(context, throwable);
        }
        else
        {
          return exceptionHandler.onException(throwable);
        }
      }
    }
    catch (Throwable otherThrowable)
    {
      // Just to make sure that handled exceptions do not trigger un-handled exceptions on their turn ;(
      if (log.isErrorEnabled())
      {
        log.error("An error occurred while attempting to handle an exception coming from " + (context == null ? "a null Context"
            : "the Context from class with name '" + context.getClass().getName()) + "'", otherThrowable);
      }
      return false;
    }
  }

  /**
   * Indicates whether a redirection is required before letting the activity continue its life cycle. It launches the redirected {@link Activity} if a
   * redirection is need, and provide to its {@link Intent} the initial activity {@link Intent} trough the extra {@link Parcelable}
   * {@link ActivityController#CALLING_INTENT} key.
   * 
   * <p>
   * If the provided {@code activity} implements the {@link ActivityController.EscapeToRedirector} interface, the method returns {@code false}.
   * </p>
   * 
   * <p>
   * Note that this method does not need to be marked as {@code synchronized}, because it is supposed to be invoked systematically from the UI thread.
   * </p>
   * 
   * @param activity
   *          the activity which is being proved against the {@link ActivityController.Redirector}
   * @return {@code true} if and only if the given activity should be paused (or ended) and if another activity should be launched instead through the
   *         {@link Activity#startActivity(Intent)} method
   * @see ActivityController#extractCallingIntent(Activity)
   * @see ActivityController.Redirector#getRedirection(Activity)
   * @see ActivityController.EscapeToRedirector
   */
  public boolean needsRedirection(Activity activity)
  {
    if (redirector == null)
    {
      return false;
    }
    if (activity instanceof ActivityController.EscapeToRedirector)
    {
      if (log.isDebugEnabled())
      {
        log.debug("The Activity with class '" + activity.getClass().getName() + "' is escaped regarding the Redirector");
      }
      return false;
    }
    final Intent intent = redirector.getRedirection(activity);
    if (intent == null)
    {
      return false;
    }
    if (log.isDebugEnabled())
    {
      log.debug("A redirection is needed");
    }

    // We redirect to the right Activity
    {
      // We consider the parent activity in case it is embedded (like in an ActivityGroup)
      final Intent formerIntent = activity.getParent() != null ? activity.getParent().getIntent() : activity.getIntent();
      intent.putExtra(ActivityController.CALLING_INTENT, formerIntent);
      // Disables the fact that the new started activity should belong to the tasks history and from the recent tasks
      // intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
      // intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
      activity.startActivity(intent);
    }

    // We now finish the redirected Activity
    activity.finish();

    return true;
  }

}
