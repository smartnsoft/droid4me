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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;

import com.smartnsoft.droid4me.framework.LifeCycle;
import com.smartnsoft.droid4me.framework.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.framework.LifeCycle.ServiceException;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Is responsible for intercepting an activity starting and redirect it to a pre-requisite one if necessary, and for handling globally exceptions.
 * 
 * @author Édouard Mercier
 * @since 2009.04.28
 */
public final class ActivityController
{

  /**
   * An interface which is queried when a new activity is bound to be displayed.
   */
  public interface Redirector
  {

    /**
     * @param activity
     *          the activity which is bound to be displayed
     * @return <code>null</code> if nothing is to be done; otherwise, the given intent will be executed and the activity         {@Activity#finish
     *  finished}
     */
    Intent getRedirection(Activity activity);

  }

  /**
   * An interface which is queried during the various life cycle events of an activity.
   */
  public interface Interceptor
  {

    /**
     * Defines all the events handled by the {@link ActivityController.Interceptor}.
     */
    public static enum InterceptorEvent
    {
      /**
       * Called during the {@link Activity#onCreate} method, at the beginning of the method, but after the parent's call, provided no
       * {@link ActivityController.Redirector activity redirection} is requested.
       */
      onCreate,
      /**
       * Called just after the {@link AppInternals.LifeCycleInternals#onActuallyCreated} method.
       */
      onActuallyCreatedDone,
      /**
       * Called during the {@link Activity#onDestroy} method, at the very end of the method, provided no {@link ActivityController.Redirector activity
       * redirection} is requested.
       */
      onDestroy,
      /**
       * Called just after the {@link AppInternals.LifeCycleInternals#onActuallyDestroyed} method.
       */
      onActuallyDestroyedDone,
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
      onSynchronizeDisplayObjectsDone
    }

    /**
     * Invoked every time a new event occurs on the provided activity. Ideal for logging application usage analytics.
     * 
     * The method blocks the caller thread, hence the method should last a very short time!
     */
    void onLifeCycleEvent(Activity activity, ActivityController.Interceptor.InterceptorEvent event);

  }

  /**
   * Defines and splits the handling of various exceptions in a single place.
   * 
   * <p>
   * The exception handler will be invoked at runtime when an exception is thrown and is not handled.
   * </p>
   */
  public interface ExceptionHandler
  {

    /**
     * Is invoked whenever the {@link LifeCycle.ForActivity#onRetrieveBusinessObjects()} throws an exception.
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @param activity
     *          the activity that issued the exception ; cannot be <code>null</code>
     * @return <code>true</code> if the handler has actually handled the exception: this indicates to the framework that it does not need to
     *         investigate for a further exception handler anymore
     */
    boolean onBusinessObjectAvailableException(Activity activity, LifeCycle.BusinessObjectUnavailableException exception);

    /**
     * Is invoked whenever the {@link LifeCycle.ForActivity#onRetrieveBusinessObjects()} throws an exception.
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @see #onBusinessObjectAvailableException for the explanation about the return value and the <code>activity</code> parameter
     */
    boolean onServiceException(Activity activity, LifeCycle.ServiceException exception);

    /**
     * Is invoked whenever an activity implementing {@link LifeCycle.ForActivity} throws an unexpected exception.
     * 
     * <p>
     * This method serves as a fallback on the framework, in order to handle gracefully exceptions and prevent the application from crashing.
     * </p>
     * 
     * <p>
     * Warning, it is not ensured that this method will be invoked from the UI thread!
     * </p>
     * 
     * @see #onBusinessObjectAvailableException for the explanation about the return value and the <code>activity</code> parameter
     */
    boolean onOtherException(Activity activity, Throwable throwable);

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
     * @param context
     *          the context that issued the exception
     * @see #onBusinessObjectAvailableException for the explanation about the return value
     */
    boolean onContextException(Context context, Throwable throwable);

  }

  /**
   * An implementation of {@link ActivityController.ExceptionHandler} which pops-up error dialog boxes.
   */
  public static class AbstractExceptionHandler
      implements ActivityController.ExceptionHandler
  {
    private final SmartApplication.I18N i18n;

    public AbstractExceptionHandler(SmartApplication.I18N i18n)
    {
      this.i18n = i18n;
    }

    public boolean onBusinessObjectAvailableException(final Activity activity, BusinessObjectUnavailableException exception)
    {
      if (checkConnectivityProblemInCause(activity, exception) == true)
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
            public void onClick(DialogInterface dialogInterface, int i)
            {
              // We leave the activity, because we cannot go any further
              activity.finish();
            }
          }).setCancelable(false).show();
        }
      });
      return true;
    }

    public boolean onServiceException(final Activity activity, ServiceException exception)
    {
      if (checkConnectivityProblemInCause(activity, exception) == true)
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
                public void onClick(DialogInterface dialogInterface, int i)
                {
                  // We leave the activity, because we cannot go any further
                  activity.finish();
                }
              }).setCancelable(false).show();
        }
      });
      return true;
    }

    public boolean onOtherException(final Activity activity, Throwable throwable)
    {
      if (checkConnectivityProblemInCause(activity, throwable) == true)
      {
        return true;
      }
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
      return true;
    }

    public boolean onContextException(Context context, Throwable throwable)
    {
      return false;
    }

    protected final boolean checkConnectivityProblemInCause(final Activity activity, final Throwable throwable)
    {
      Throwable cause;
      Throwable newThrowable = throwable;
      // We investigate over the whole cause stack
      while ((cause = newThrowable.getCause()) != null)
      {
        if (cause instanceof UnknownHostException || cause instanceof SocketException || cause instanceof SocketTimeoutException || cause instanceof InterruptedIOException)
        {
          activity.runOnUiThread(new Runnable()
          {
            public void run()
            {
              new AlertDialog.Builder(activity).setTitle(i18n.dialogBoxErrorTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                  i18n.connectivityProblemHint).setPositiveButton(android.R.string.ok, null).setCancelable(false).setPositiveButton(android.R.string.ok,
                  new DialogInterface.OnClickListener()
                  {
                    public void onClick(DialogInterface dialog, int which)
                    {
                    }
                  }).show();
            }
          });
          return true;
        }
        newThrowable = cause;
      }
      return false;
    }

  }

  private static final Logger log = LoggerFactory.getInstance(ActivityController.class);

  public static final String CALLING_INTENT = "com.smartnsoft.droid4me.callingIntent";

  private static volatile ActivityController instance;

  /**
   * Implements a "double-checked locking" pattern.
   */
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

  private ActivityController()
  {
  }

  public ActivityController.ExceptionHandler getExceptionHandler()
  {
    return exceptionHandler;
  }

  public synchronized void registerRedirector(ActivityController.Redirector redirector)
  {
    this.redirector = redirector;
  }

  public synchronized void registerInterceptor(ActivityController.Interceptor interceptor)
  {
    this.interceptor = interceptor;
  }

  public synchronized void registerExceptionHandler(ActivityController.ExceptionHandler exceptionHandler)
  {
    this.exceptionHandler = exceptionHandler;
  }

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

  /**
   * Is invoked every time a life cycle event occurs for the provided activity.
   * 
   * Note that the method is synchronized, which means that the previous call will block the next one, if no thread is spawn.
   */
  public synchronized void onLifeCycleEvent(Activity activity, ActivityController.Interceptor.InterceptorEvent event)
  {
    if (interceptor == null)
    {
      return;
    }
    interceptor.onLifeCycleEvent(activity, event);
  }

  /**
   * Dispatches the exception to the {@link ActivityController.ExceptionHandler}, and invokes the right method depending on its nature.
   * 
   * @param context
   *          the context that originated the exception ; may be <code>null</code>
   * @param throwable
   *          the reported exception
   * @return <code>true</code> if the exception has been handled ; in particular, if no {@link ActivityController#getExceptionHandler() exception
   *         handled has been set}, returns <code>false</code>
   */
  public synchronized boolean handleException(Context context, Throwable throwable)
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
      if (throwable instanceof LifeCycle.ServiceException)
      {
        // Should only occur with a non-null activity
        final LifeCycle.ServiceException exception = (LifeCycle.ServiceException) throwable;
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
        return exceptionHandler.onServiceException(activity, exception);
      }
      else if (throwable instanceof LifeCycle.BusinessObjectUnavailableException)
      {
        // Should only occur with a non-null activity
        final LifeCycle.BusinessObjectUnavailableException exception = (LifeCycle.BusinessObjectUnavailableException) throwable;
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
        return exceptionHandler.onBusinessObjectAvailableException(activity, exception);
      }
      else
      {
        if (log.isWarnEnabled())
        {
          log.warn("Caught an exception during the processing of the context with name '" + (context == null ? "null" : context.getClass().getName()) + "'",
              throwable);
        }
        // We do nothing if the activity is dying
        if (activity != null && activity.isFinishing() == true)
        {
          return true;
        }
        if (activity != null)
        {
          return exceptionHandler.onOtherException(activity, throwable);
        }
        else
        {
          return exceptionHandler.onContextException(context, throwable);
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

}
