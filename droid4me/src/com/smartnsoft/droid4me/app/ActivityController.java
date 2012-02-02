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

import java.io.File;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Parcelable;
import android.view.Window;
import android.widget.Toast;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.ServiceLifeCycle.ServiceException;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Is responsible for intercepting an activity starting and redirect it to a prerequisite one if necessary, and for handling globally exceptions.
 * 
 * <p>
 * Everything described here which involves the {@link Activity activities}, is applicable provided the activity is a {@link SmartableActivity}.
 * </p>
 * 
 * <p>
 * It is also a container for multiple interfaces relative to its architecture.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2009.04.28
 */
public final class ActivityController
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
     * started instead of the provided one, which is supposed to have just {@link Activity#onCreate(Bundle) started}.
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
   * An interface which is queried during the various life cycle events of an {@link LifeCycle activity}.
   * 
   * <p>
   * An interceptor is the ideal place for centralizing in one place many of the {@link Activity} life cycle events.
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
       * Called during the {@link Activity#onCreate} method, before the Android built-in super method {@link Activity#onCreate} method is invoked.
       * 
       * <p>
       * This is an ideal place where to {@link Window#requestFeature(int) request for window features}.
       * </p>
       */
      onSuperCreateBefore,
      /**
       * Called during the {@link Activity#onCreate} method, at the beginning of the method, but after the parent's call, provided no
       * {@link ActivityController.Redirector activity redirection} is requested.
       */
      onCreate,
      /**
       * Called at the end of the {@link Activity#onContentChanged} method execution, but after the parent's call, provided no
       * {@link ActivityController.Redirector activity redirection} is requested.
       */
      onContentChanged,
      // /**
      // * Called during the {@link Activity#onCreate} method, just before the {@link LifeCycle.ForActivity#onRetrieveDisplayObjects()} method has
      // been
      // * invoked, provided no {@link ActivityController.Redirector activity redirection} is requested.
      // */
      // onRetrieveDisplayObjectsBefore,
      // /**
      // * Called during the {@link Activity#onCreate} method, just after the {@link LifeCycle.ForActivity#onRetrieveDisplayObjects()} method has been
      // * invoked, provided no {@link ActivityController.Redirector activity redirection} is requested.
      // */
      // onRetrieveDisplayObjectsAfter,
      /**
       * Called just after the {@link AppInternals.LifeCycleInternals#onActuallyCreated} method.
       */
      onActuallyCreatedDone,
      /**
       * Called during the {@link Activity#onStart} method, at the beginning of the method, but after the parent's call.
       */
      onStart,
      /**
       * Called during the {@link Activity#onResume} method, at the beginning of the method, but after the parent's call, provided no
       * {@link ActivityController.Redirector activity redirection} is requested.
       */
      onResume,
      /**
       * Called during the {@link Activity#onPause} method, at the beginning of the method, but after the parent's call, provided no
       * {@link ActivityController.Redirector activity redirection} is requested.
       */
      onPause,
      /**
       * Called during the {@link Activity#onStop} method, at the beginning of the method, before the parent's call.
       */
      onStop,
      /**
       * Called just after the {@link LifeCycle.ForActivity#onFulfillDisplayObjects} method.
       */
      onFulfillDisplayObjectsDone,
      /**
       * Called just after the {@link LifeCycle.ForActivity#onSynchronizeDisplayObjects} method.
       */
      onSynchronizeDisplayObjectsDone,
      /**
       * Called during the {@link Activity#onDestroy} method, at the very end of the method, provided no {@link ActivityController.Redirector activity
       * redirection} is requested.
       */
      onDestroy,
      /**
       * Called just after the {@link AppInternals.LifeCycleInternals#onActuallyDestroyed} method.
       */
      onActuallyDestroyedDone
    }

    /**
     * Invoked every time a new event occurs on the provided activity. Ideal for logging application usage analytics.
     * 
     * <p>
     * The method blocks the caller thread, which is sometimes the UI thread, hence the method should last a very short time!
     * <p>
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
     *          the activity that issued the exception ; cannot be {@code null}
     * @param component
     *          the component that issued the exception ; may be {@code null}
     * @param exception
     *          the exception that has been triggered
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onBusinessObjectAvailableException(Activity activity, Object component, BusinessObjectUnavailableException exception);

    /**
     * Is invoked whenever the {@link LifeCycle#onRetrieveBusinessObjects()} throws an exception.
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @param activity
     *          the activity that issued the exception ; cannot be {@code null}
     * @param component
     *          the component that issued the exception ; may be {@code null}
     * @param exception
     *          the exception that has been triggered
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onServiceException(Activity activity, Object component, ServiceException exception);

    /**
     * Is invoked whenever an activity implementing {@link LifeCycle} throws an unexpected exception, or when an exception is thrown during a
     * {@link AppPublics.GuardedCommand} execution.
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
     *          the throwable that has been triggered
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onOtherException(Activity activity, Object component, Throwable throwable);

    /**
     * Is invoked whenever a handled exception is thrown with a non-activity context.
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
     *          the throwable that has been triggered
     * @param component
     *          the component that issued the exception ; may be {@code null}
     * @param context
     *          the context that issued the exception
     * @return {@code true} if the handler has actually handled the exception: this indicates to the framework that it does not need to investigate
     *         for a further exception handler anymore
     */
    boolean onContextException(Context context, Object component, Throwable throwable);

  }

  /**
   * A simple implementation of {@link ActivityController.ExceptionHandler} which pops-up error dialog boxes and toasts.
   * 
   * <p>
   * The labels of the dialogs and the toasts are i18ned though the provided {@link SmartApplication.I18N} provided instance.
   * </p>
   */
  public static class AbstractExceptionHandler
      implements ActivityController.ExceptionHandler
  {

    /**
     * Defines how the framework should behave when an Internet connectivity problem has been detected.
     */
    public static enum ConnectivityUIExperience
    {
      /**
       * Should open a {@link android.app.Dialog dialog box} with a "retry"/"ok" button.
       */
      DialogRetry,
      /**
       * Should open a {@link android.app.Dialog dialog box} with a single "ok" button.
       */
      Dialog,
      /**
       * Should issue an Android {@link Toast.LENGTH_SHORT short} {@link android.widget.Toast}.
       */
      Toast
    }

    private final SmartApplication.I18N i18n;

    /**
     * @param throwable
     *          the exception to investigate
     * @return {@code true} if and only if the exception results from a connectivity issue by inspecting its causes tree
     */
    public static boolean isAConnectivityProblem(Throwable throwable)
    {
      return ActivityController.AbstractExceptionHandler.searchForCause(throwable, UnknownHostException.class, SocketException.class,
          SocketTimeoutException.class, InterruptedIOException.class) != null;
    }

    /**
     * @param throwable
     *          the exception to investigate
     * @return {@code true} if and only if the exception results from a memory saturation issue (i.e. a {@link OutOfMemoryError} exception) by
     *         inspecting its causes tree
     */
    public static boolean isAMemoryProblem(Throwable throwable)
    {
      return ActivityController.AbstractExceptionHandler.searchForCause(throwable, OutOfMemoryError.class) != null;
    }

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
     * @param i18n
     *          will be used at runtime, so as to display i18ned labels in the UI
     */
    public AbstractExceptionHandler(SmartApplication.I18N i18n)
    {
      this.i18n = i18n;
    }

    public boolean onBusinessObjectAvailableException(final Activity activity, Object component, BusinessObjectUnavailableException exception)
    {
      if (handleCommonCauses(activity, component, exception, ConnectivityUIExperience.DialogRetry) == true)
      {
        return true;
      }
      else if (handleOtherCauses(activity, exception) == true)
      {
        return true;
      }
      // We make sure that the dialog is popped from the UI thread
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          new AlertDialog.Builder(activity).setTitle(i18n.dialogBoxErrorTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
              i18n.businessObjectAvailabilityProblemHint).setPositiveButton(android.R.string.ok, new OnClickListener()
          {
            public void onClick(DialogInterface dialogInterface, int which)
            {
              // We leave the activity, because we cannot go any further
              activity.finish();
            }
          }).setCancelable(false).show();
        }
      });
      return true;
    }

    public boolean onServiceException(final Activity activity, Object component, ServiceException exception)
    {
      if (handleCommonCauses(activity, component, exception, ConnectivityUIExperience.Dialog) == true)
      {
        return true;
      }
      else if (handleOtherCauses(activity, exception) == true)
      {
        return true;
      }
      // We make sure that the dialog is popped from the UI thread
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          new AlertDialog.Builder(activity).setTitle(i18n.dialogBoxErrorTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(i18n.serviceProblemHint).setPositiveButton(
              android.R.string.ok, new OnClickListener()
              {
                public void onClick(DialogInterface dialogInterface, int which)
                {
                  // We leave the activity, because we cannot go any further
                  activity.finish();
                }
              }).setCancelable(false).show();
        }
      });
      return true;
    }

    public boolean onOtherException(final Activity activity, Object component, Throwable throwable)
    {
      if (handleCommonCauses(activity, component, throwable, ConnectivityUIExperience.Toast) == true)
      {
        return true;
      }
      else if (handleOtherCauses(activity, throwable) == true)
      {
        return true;
      }
      onOtherExceptionFallback(activity, component, throwable);
      return true;
    }

    /**
     * This method will be invoked by the {@link #onOtherException()} method, as a fallback if the provided throwable has not been handled neither by
     * the {@link #handleCommonCauses()} nor the {@link #handleOtherCauses()} methods.
     * 
     * <p>
     * A dialog box which reports the problem will be popped up.
     * </p>
     * 
     * @param activity
     *          the activity from which the throwable has been thrown ; cannot be {@code null}
     * @param component
     *          the component responsible for having thrown the exception
     * @param throwable
     *          the throwable that has been triggered
     */
    protected void onOtherExceptionFallback(final Activity activity, Object component, Throwable throwable)
    {
      // We make sure that the dialog is popped from the UI thread
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          new AlertDialog.Builder(activity).setTitle(i18n.dialogBoxErrorTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(i18n.otherProblemHint).setPositiveButton(
              android.R.string.ok, new OnClickListener()
              {
                public void onClick(DialogInterface dialogInterface, int i)
                {
                  // We leave the activity, because we cannot go any further
                  activity.finish();
                }
              }).setCancelable(false).show();
        }
      });
    }

    /**
     * @return {@code false} in the current implementation
     */
    public boolean onContextException(Context context, Object component, Throwable throwable)
    {
      return false;
    }

    /**
     * A place holder for handling in a centralized way all kinds of exceptions.
     * 
     * <p>
     * When deriving from the {@link ActivityController.AbstractExceptionHandler} class, this method should be overridden, so as to handle all
     * application specific exceptions.
     * </p>
     * 
     * @param activity
     *          the activity which has triggered the exception
     * @param throwable
     *          the throwable to analyze
     * @return {@code true} if and only if the throwable has been handled; the current implementation returns {@code false}
     */
    protected boolean handleOtherCauses(Activity activity, Throwable throwable)
    {
      return false;
    }

    /**
     * Checks for an Internet connectivity issue or a memory saturation issue inside the provided throwable root causes.
     * 
     * <p>
     * This method is especially useful when overriding the {@link ActivityController.AbstractExceptionHandler}, in order to let the framework hunt
     * for common troubles.
     * </p>
     * 
     * @param activity
     *          the activity which has triggered the exception
     * @param component
     *          the component which has triggered the exception ; may be {@code null}
     * @param throwable
     *          the throwable to analyze
     * @param connectivityUIExperience
     *          indicates what end-user experience to deliver if the problem is an Internet connectivity issue
     * @return {@code true} if and only if the throwable has been handled
     * @see #handleConnectivityProblemInCause(Activity, Throwable, ConnectivityUIExperience)
     * @see #handleMemoryProblemInCause(Activity, Throwable)
     */
    protected final boolean handleCommonCauses(final Activity activity, Object component, Throwable throwable, ConnectivityUIExperience connectivityUIExperience)
    {
      if (handleConnectivityProblemInCause(activity, component, throwable, connectivityUIExperience) == true)
      {
        return true;
      }
      else if (handleMemoryProblemInCause(activity, throwable) == true)
      {
        return true;
      }
      return false;
    }

    /**
     * Attempts to find an Internet connection issue in the provided exception by iterating over the causes, and display a dialog box if any.
     * 
     * @param activity
     *          in case a connection issue is discovered, it will be used to pop up a dialog box. When the dialog box "OK" button is hit, it will be
     *          simply dismissed
     * @param component
     *          the component which has triggered the exception ; may be {@code null}
     * @param throwable
     *          the exception to be inspected
     * @param connectivityUIExperience
     *          indicates the end-user experience to provide if a connectivity problem has been detected
     * @return {@code true} if and only a connection issue has been detected
     */
    protected final boolean handleConnectivityProblemInCause(final Activity activity, Object component, Throwable throwable,
        final ActivityController.AbstractExceptionHandler.ConnectivityUIExperience connectivityUIExperience)
    {
      if (ActivityController.AbstractExceptionHandler.isAConnectivityProblem(throwable) == true)
      {
        final LifeCycle lifeCycle;
        if (component instanceof LifeCycle)
        {
          lifeCycle = (LifeCycle) component;
        }
        else if (activity instanceof LifeCycle)
        {
          lifeCycle = (LifeCycle) activity;
        }
        else
        {
          lifeCycle = null;
        }
        activity.runOnUiThread(new Runnable()
        {
          public void run()
          {
            if (lifeCycle == null || connectivityUIExperience == ConnectivityUIExperience.Toast)
            {
              // Either the activity/fragment is not droi4mized, or the end-user experience should be a toast
              Toast.makeText(activity, i18n.connectivityProblemHint, Toast.LENGTH_SHORT).show();
            }
            else
            {
              final boolean retry = connectivityUIExperience == ConnectivityUIExperience.DialogRetry && activity instanceof LifeCycle;
              final Builder builder = new AlertDialog.Builder(activity).setTitle(i18n.dialogBoxErrorTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                  retry == true ? i18n.connectivityProblemRetryHint : i18n.connectivityProblemHint).setPositiveButton(android.R.string.ok, null);
              if (retry == true)
              {
                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                {
                  public void onClick(DialogInterface dialog, int which)
                  {
                    lifeCycle.refreshBusinessObjectsAndDisplay(true, null, false);
                    dialog.dismiss();
                  }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                {
                  public void onClick(DialogInterface dialog, int which)
                  {
                    dialog.cancel();
                    activity.finish();
                  }
                }).setOnCancelListener(new DialogInterface.OnCancelListener()
                {
                  public void onCancel(DialogInterface dialog)
                  {
                    activity.finish();
                  }
                }).show();
              }
              else
              {
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                  public void onClick(DialogInterface dialog, int which)
                  {
                  }
                }).show();
              }
            }
          }
        });
        return true;
      }
      return false;
    }

    /**
     * Attempts to find a memory saturation issue in the provided exception by iterating over the causes, and display a dialog box if any.
     * 
     * <p>
     * In the exception root cause is a memory saturation issue, a @{link .hprof} dump file will be generated into the directory {@link
     * Environment.getExternalStorageDirectory()}, and the exact name of this file will be traced.
     * </p>
     * 
     * @param activity
     *          the activity responsible for triggering the exception
     * @param throwable
     *          the exception to be inspected
     * @return {@code true} if and only a memory saturation issue has been detected
     */
    protected final boolean handleMemoryProblemInCause(Activity activity, Throwable throwable)
    {
      if (ActivityController.AbstractExceptionHandler.isAMemoryProblem(throwable) == true)
      {
        // We first run a garbage collection, in the hope to free some memory ;(
        System.gc();
        final File file = new File(Environment.getExternalStorageDirectory(), activity.getApplication().getPackageName() + "-outofmemory-" + System.currentTimeMillis() + ".hprof");
        if (log.isErrorEnabled())
        {
          log.error("A memory saturation issue has been detected: dumping the memory usage to file '" + file.getAbsolutePath() + "'", throwable);
        }
        try
        {
          Debug.dumpHprofData(file.getAbsolutePath());
        }
        catch (Throwable innerThrowable)
        {
          if (log.isErrorEnabled())
          {
            log.error("A problem occurred while attempting to dump the memory usage to file '" + file.getAbsolutePath() + "'", innerThrowable);
          }
        }
        return true;
      }
      return false;
    }

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
    if (instance == null)
    {
      synchronized (ActivityController.class)
      {
        if (instance == null)
        {
          instance = new ActivityController();
        }
      }
    }
    return instance;
  }

  private ActivityController.Redirector redirector;

  private ActivityController.Interceptor interceptor;

  private ActivityController.ExceptionHandler exceptionHandler;

  /**
   * No one else than the framework should create such an instance.
   */
  private ActivityController()
  {
  }

  public ActivityController.ExceptionHandler getExceptionHandler()
  {
    return exceptionHandler;
  }

  /**
   * Remembers the activity redirector that will be used by the framework, before {@link Context#startActivity(Intent) starting} a new
   * {@link Activity}.
   * 
   * @param redirector
   *          the redirector that will be requested at runtime, when a new activity is being started; if {@code null}, then, redirection mechanism
   *          will be set up
   */
  public synchronized void registerRedirector(ActivityController.Redirector redirector)
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
  public synchronized void registerInterceptor(ActivityController.Interceptor interceptor)
  {
    this.interceptor = interceptor;
  }

  /**
   * Remembers the exception handler that will be used by the framework.
   * 
   * @param exceptionHandler
   *          the handler that will be invoked in case of exception; if {@code null}, no exception handler will be used
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
   * @param context
   *          the context that originated the exception ; may be {@code null}
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
        if (log.isWarnEnabled())
        {
          log.warn("Caught an exception during the processing of the context with name '" + (context == null ? "null" : context.getClass().getName()) + "'",
              throwable);
        }
      }
      return false;
    }
    final Activity activity;
    if (context != null && context instanceof Activity)
    {
      activity = (Activity) context;
    }
    else
    {
      activity = null;
    }
    try
    {
      if (throwable instanceof ServiceException)
      {
        // Should only occur with a non-null activity
        final ServiceException exception = (ServiceException) throwable;
        if (log.isWarnEnabled())
        {
          log.warn("Caught an exception during the processing of the services from the activity with name '" + (context == null ? "null"
              : activity.getClass().getName()) + "'", exception);
        }
        // We do nothing if the activity is dying
        if (activity != null && activity.isFinishing() == true)
        {
          return true;
        }
        return exceptionHandler.onServiceException(activity, component, exception);
      }
      else if (throwable instanceof BusinessObjectUnavailableException)
      {
        // Should only occur with a non-null activity
        final BusinessObjectUnavailableException exception = (BusinessObjectUnavailableException) throwable;
        if (log.isWarnEnabled())
        {
          log.warn("Caught an exception during the retrieval of the business objects from the activity with name '" + (activity == null ? "null"
              : activity.getClass().getName()) + "'", exception);
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
          log.warn("Caught an exception during the processing of the context with name '" + (context == null ? "null" : context.getClass().getName()) + "'",
              throwable);
        }
        // For this special case, we ignore the case when the activity is dying
        if (activity != null)
        {
          return exceptionHandler.onOtherException(activity, component, throwable);
        }
        else
        {
          return exceptionHandler.onContextException(context, component, throwable);
        }
      }
    }
    catch (Throwable otherThrowable)
    {
      // Just to make sure that handled exceptions do not trigger unhandled exceptions on their turn;)
      if (log.isErrorEnabled())
      {
        log.error("An error occurred while handling an exception coming from the context with name '" + (context == null ? "null"
            : context.getClass().getName()) + "'", otherThrowable);
      }
      return false;
    }
  }

  /**
   * Indicates whether a redirection is required before letting the activity continue its life cycle. Also launches the redirected {@link Activity} is
   * a redirection is need, and provide to its {@link Intent} the initial activity {@link Intent} trough the extra {@link Parcelable}
   * {@link ActivityController#CALLING_INTENT} key.
   * 
   * @param activity
   *          the activity which is being proved against the {@link ActivityController.Redirector}
   * @return {@code true} if and only if the given activity should be paused (or ended) and if another activity should be launched instead
   */
  public synchronized boolean needsRedirection(Activity activity)
  {
    if (redirector == null)
    {
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
    activity.finish();
    // We consider the parent activity in case it is embedded (like in a TabActivity)
    intent.putExtra(ActivityController.CALLING_INTENT, (activity.getParent() != null ? activity.getParent().getIntent() : activity.getIntent()));
    // Disables the fact that the new started activity should belong to the tasks history and from the recent tasks
    // intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    // intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    activity.startActivity(intent);
    return true;
  }

}
