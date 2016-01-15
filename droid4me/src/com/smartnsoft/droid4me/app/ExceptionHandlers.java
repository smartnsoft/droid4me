/*
 * (C) Copyright 2009-2013 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Environment;
import android.widget.Toast;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.app.ActivityController.IssueAnalyzer;
import com.smartnsoft.droid4me.app.SmartApplication.I18N;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.util.SendLogsTask;

/**
 * A wrapper class which holds {@link ActivityController.ExceptionHandler} implementations.
 *
 * @author Édouard Mercier
 * @since 2014.01.01
 */
public final class ExceptionHandlers
{

  /**
   * A simple implementation of {@link ExceptionHandlers.DefaultExceptionHandler} which pops-up error dialog boxes and toasts.
   * <p/>
   * <p>
   * The labels of the dialogs and the toasts are i18ned though the provided {@link SmartApplication.I18N} provided instance.
   * </p>
   *
   * @since 2009.07.09
   */
  public static class AbstractExceptionHandler
      implements ActivityController.ExceptionHandler
  {

    protected final static Logger log = LoggerFactory.getInstance("ExceptionHandler");

    /**
     * Defines how the framework should behave when an Internet connectivity problem has been detected.
     */
    public enum ConnectivityUIExperience
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
       * Should issue an Android {@link Toast#LENGTH_SHORT short} {@link android.widget.Toast}.
       */
      Toast
    }

    protected final SmartApplication.I18N i18n;

    protected final IssueAnalyzer issueAnalyzer;

    /**
     * @param i18n          will be used at runtime, so as to display i18ned labels in the UI
     * @param issueAnalyzer will be used when it comes to analyze an issue represented by an {@link Throwable}
     */
    public AbstractExceptionHandler(SmartApplication.I18N i18n, ActivityController.IssueAnalyzer issueAnalyzer)
    {
      this.i18n = i18n;
      this.issueAnalyzer = issueAnalyzer;
    }

    @Override
    public final boolean onBusinessObjectAvailableException(final Activity activity, Object component,
        BusinessObjectUnavailableException exception)
    {
      if (handleCommonCauses(activity, component, exception, ConnectivityUIExperience.DialogRetry) == true)
      {
        return true;
      }
      else if (handleOtherCauses(activity, component, exception) == true)
      {
        return true;
      }
      return onBusinessObjectAvailableExceptionFallback(activity, component, exception);
    }

    @Override
    public final boolean onActivityException(final Activity activity, Object component, Throwable throwable)
    {
      if (handleCommonCauses(activity, component, throwable, ConnectivityUIExperience.Toast) == true)
      {
        return true;
      }
      else if (handleOtherCauses(activity, component, throwable) == true)
      {
        return true;
      }
      return onActivityExceptionFallback(activity, component, throwable);
    }

    /**
     * {@inheritDoc}
     *
     * @see #onContextExceptionFallback(boolean, Context, Throwable)
     */
    @Override
    public final boolean onContextException(boolean isRecoverable, Context context, Throwable throwable)
    {
      if (handleCommonCauses(null, null, throwable, null) == true)
      {
        return true;
      }
      return onContextExceptionFallback(isRecoverable, context, throwable);
    }

    /**
     * {@inheritDoc}
     *
     * @see #onExceptionFallback(boolean, Throwable)
     */
    @Override
    public final boolean onException(boolean isRecoverable, Throwable throwable)
    {
      if (handleCommonCauses(null, null, throwable, null) == true)
      {
        return true;
      }
      return onExceptionFallback(isRecoverable, throwable);
    }

    /**
     * This method will be invoked by the {@link #onBusinessObjectAvailableException(Activity, Object, BusinessObjectUnavailableException)} method, as a fallback if the provided exception has not been
     * handled neither by the {@link #handleCommonCauses(Activity, Object, Throwable, ConnectivityUIExperience)} nor the {@link #handleOtherCauses(Activity, Object, Throwable)} methods.
     * <p/>
     * <p>
     * A dialog box which reports the problem will be popped up by the current implementation: when the end-user acknowledges the issue reported by
     * the dialog box, the {@link Activity#finish() Activity will be finished}.
     * </p>
     *
     * @param activity  the activity from which the exception has been thrown ; cannot be {@code null}
     * @param component the component responsible for having thrown the exception
     * @param exception the exception that has been thrown
     * @return {@code true} if and only if the exception has been handled
     * @see #onBusinessObjectAvailableException(Activity, Object, BusinessObjectUnavailableException)
     */
    protected boolean onBusinessObjectAvailableExceptionFallback(final Activity activity, Object component,
        BusinessObjectUnavailableException exception)
    {
      showDialog(activity, i18n.dialogBoxErrorTitle, i18n.businessObjectAvailabilityProblemHint, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface dialog, int which)
        {
          dialog.dismiss();
          // We leave the activity, because we cannot go any further
          activity.finish();
        }
      }, null, null, null);
      if (issueAnalyzer != null && issueAnalyzer.handleIssue(exception) == true)
      {
        return true;
      }
      return true;
    }

    /**
     * This method will be invoked by the {@link #onActivityException(Activity, Object, Throwable)} method, as a fallback if the provided exception has not been handled neither
     * by the {@link #handleCommonCauses(Activity, Object, Throwable, ConnectivityUIExperience)} nor the {@link #handleOtherCauses(Activity, Object, Throwable)} methods.
     * <p/>
     * <p>
     * A dialog box which reports the problem will be popped up by the current implementation: when the end-user acknowledges the issue reported by
     * the dialog box, the {@link Activity#finish() Activity will be finished}.
     * </p>
     *
     * @param activity  the activity from which the exception has been thrown ; cannot be {@code null}
     * @param component the component responsible for having thrown the exception
     * @param throwable the exception that has been thrown
     * @return {@code true} if and only if the exception has been handled
     * @see #onActivityException(Activity, Object, Throwable)
     */
    protected boolean onActivityExceptionFallback(final Activity activity, Object component, Throwable throwable)
    {
      showDialog(activity, i18n.dialogBoxErrorTitle, i18n.otherProblemHint, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener()
      {
        public void onClick(DialogInterface dialog, int which)
        {
          dialog.dismiss();
          // We leave the activity, because we cannot go any further
          activity.finish();
        }
      }, null, null, null);
      submitToIssueAnalyzer(throwable);
      return true;
    }

    /**
     * This method will be invoked by the {@link #onContextException(boolean, Context, Throwable)} method, as a fallback if the provided exception has not been handled neither
     * by the {@link #handleCommonCauses(Activity, Object, Throwable, ConnectivityUIExperience)} method.
     *
     * @param isRecoverable indicates whether the application is about to crash when the exception has been triggered
     * @param context       the context that issued the exception
     * @param throwable     the exception that has been triggered
     * @return {@code true} if and only if the exception has been handled
     * @see #onContextException(boolean, Context, Throwable)
     */
    protected boolean onContextExceptionFallback(boolean isRecoverable, Context context, Throwable throwable)
    {
      submitToIssueAnalyzer(throwable);
      return false;
    }

    /**
     * This method will be invoked by the {@link #onException(boolean, Throwable)} method, as a fallback if the provided exception has not been handled neither by the
     * {@link #handleCommonCauses(Activity, Object, Throwable, ConnectivityUIExperience)} method.
     *
     * @param isRecoverable indicates whether the application is about to crash when the exception has been triggered
     * @param throwable     the exception that has been triggered
     * @return {@code true} if and only if the exception has been handled
     * @see #onException(boolean, Throwable)
     */
    protected boolean onExceptionFallback(boolean isRecoverable, Throwable throwable)
    {
      submitToIssueAnalyzer(throwable);
      return false;
    }

    /**
     * Checks for an Internet connectivity issue or a memory saturation issue inside the provided exception root causes.
     * <p/>
     * <p>
     * This method is especially useful when overriding the {@link ExceptionHandlers.AbstractExceptionHandler}, in order to let the framework hunt for
     * common troubles.
     * </p>
     *
     * @param activity                 the activity which has triggered the exception; may be {@code null}
     * @param component                the component which has triggered the exception ; may be {@code null}
     * @param throwable                the exception to analyze
     * @param connectivityUIExperience indicates what end-user experience to deliver if the problem is an Internet connectivity issue ; may be {@code null}
     * @return {@code true} if and only if the exception has been handled
     * @see #handleConnectivityProblemInCause(Activity, Object, Throwable, ConnectivityUIExperience)
     * @see #handleMemoryProblemInCause(Throwable)
     */
    protected final boolean handleCommonCauses(Activity activity, Object component, Throwable throwable,
        ConnectivityUIExperience connectivityUIExperience)
    {
      if (ActivityController.IssueAnalyzer.isAConnectivityProblem(throwable) == true && handleConnectivityProblemInCause(activity, component, throwable, connectivityUIExperience) == true)
      {
        return true;
      }
      else if (ActivityController.IssueAnalyzer.isAMemoryProblem(throwable) == true && handleMemoryProblemInCause(throwable) == true)
      {
        return true;
      }
      return false;
    }

    /**
     * If an {@link IssueAnalyzer} is registered, this will invoke the {@link IssueAnalyzer#handleIssue(Throwable)} method.
     *
     * @param throwable the exception that has previously been thrown
     */
    protected final void submitToIssueAnalyzer(Throwable throwable)
    {
      if (issueAnalyzer != null)
      {
        if (issueAnalyzer.handleIssue(throwable) == true)
        {
          if (log.isInfoEnabled())
          {
            log.info("The exception belonging to the class '" + throwable + "' could be analyzed");
          }
        }
        else
        {
          if (log.isWarnEnabled())
          {
            log.warn("The exception belonging to the class '" + throwable + "' could not be analyzed");
          }
        }
      }
    }

    /**
     * The method which should be invoked internally when reporting an error dialog box. The parameters are the same as for the
     * {@link #onShowDialog(Activity, CharSequence, CharSequence, CharSequence, OnClickListener, CharSequence, OnClickListener, OnCancelListener)} method.
     * <p/>
     * <p>
     * It is possible to invoke that method from any thread.
     * </p>
     *
     * @see #onShowDialog(Activity, CharSequence, CharSequence, CharSequence, OnClickListener, CharSequence, OnClickListener, OnCancelListener)
     */
    protected final void showDialog(final Activity activity, final CharSequence dialogTitle,
        final CharSequence dialogMessage, final CharSequence positiveButton,
        final DialogInterface.OnClickListener positiveClickListener, final CharSequence negativeButton,
        final DialogInterface.OnClickListener negativeClickListener,
        final DialogInterface.OnCancelListener onCancelListener)
    {
      if (activity.isFinishing() == true)
      {
        // We do nothing, because there is no user interface any more!
        return;
      }
      // We make sure that the dialog is popped from the UI thread
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          if (activity.isFinishing() == true)
          {
            // We do nothing, because there is no user interface any more!
            return;
          }
          try
          {
            onShowDialog(activity, dialogTitle, dialogMessage, positiveButton, positiveClickListener, negativeButton, negativeClickListener, onCancelListener);
          }
          catch (Throwable throwable)
          {
            if (activity.isFinishing() == false)
            {
              if (log.isErrorEnabled())
              {
                log.error("Could not open an error dialog box, because an exceptin occurred while displaying it!", throwable);
              }
            }
            else
            {
              // It is very likely that the activity has been finished in the meantime, hence we do not log anything
            }
          }
        }
      });
    }

    /**
     * A place holder for handling in a centralized way all kinds of exceptions.
     * <p/>
     * <p>
     * When deriving from the {@link ExceptionHandlers.AbstractExceptionHandler} class, this method should be overridden, so as to handle all
     * application specific exceptions.
     * </p>
     *
     * @param activity  the activity which has triggered the exception
     * @param component the component which has triggered the exception ; may be {@code null}
     * @param throwable the exception to analyze
     * @return {@code true} if and only if the exception has been handled; the current implementation returns {@code false}
     */
    protected boolean handleOtherCauses(Activity activity, Object component, Throwable throwable)
    {
      return false;
    }

    /**
     * Is invoked when a connectivity issue has been detected, and display a dialog box if any.
     * <p/>
     * <p>
     * If such Internet connectivity issue is detected, a {@link Toast} will be displayed if the {@code connectivityUIExperience} parameter is set to
     * {@link ConnectivityUIExperience#Toast} ; otherwise, a dialog box will be popped up: if the end-user does not hit the button which proposes to
     * retry, the {@link Activity#finish() Activity will be finished}.
     * </p>
     *
     * @param activity                 the activity which has triggered the exception ; may be {@code null}, and in that case, the hereby implementation does nothing
     * @param component                the component which has triggered the exception ; may be {@code null}
     * @param throwable                the exception which is supposed to be related to an Internet issue
     * @param connectivityUIExperience indicates the end-user experience to provide if a connectivity problem has been detected
     * @return {@code true} if and only a connection issue has been detected
     */
    protected boolean handleConnectivityProblemInCause(final Activity activity, Object component, Throwable throwable,
        final ExceptionHandlers.AbstractExceptionHandler.ConnectivityUIExperience connectivityUIExperience)
    {
      if (activity == null)
      {
        // In that case, we do nothing!
        return true;
      }
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
            showDialog(activity, i18n.dialogBoxErrorTitle, retry == true ? i18n.connectivityProblemRetryHint : i18n.connectivityProblemHint, activity.getString(android.R.string.ok), new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int which)
              {
                dialog.dismiss();
                if (retry == true)
                {
                  lifeCycle.refreshBusinessObjectsAndDisplay(true, null, false);
                }
                else
                {
                  activity.finish();
                }
              }
            }, activity.getString(android.R.string.no), retry == false ? null : new DialogInterface.OnClickListener()
            {
              public void onClick(DialogInterface dialog, int which)
              {
                dialog.cancel();
                activity.finish();
              }
            }, retry == false ? null : new DialogInterface.OnCancelListener()
            {
              public void onCancel(DialogInterface dialog)
              {
                dialog.dismiss();
                activity.finish();
              }
            });

          }
        }
      });
      return true;
    }

    /**
     * Is invoked when a memory saturation issue has been detected, and display a dialog box if any.
     * <p/>
     * <p>
     * In the exception root cause is a memory saturation issue, a @{link .hprof} dump file will be generated into the directory {@link
     * Environment#getExternalStorageDirectory()}, and the exact name of this file will be traced.
     * </p>
     *
     * @param throwable the exception which is supposed to be related to an out-of-memory
     * @return {@code true} if and only a memory saturation issue has been detected
     * @see IssueAnalyzer#handleIssue(Throwable)
     */
    protected boolean handleMemoryProblemInCause(Throwable throwable)
    {
      submitToIssueAnalyzer(throwable);
      return false;
    }

    /**
     * Is responsible for displaying a dialog box. This enables to customize in a centralized way the dialog boxes look & feel.
     * <p/>
     * <p>
     * It is ensured that the framework will invoke that method from the UI thread, and when invoking that method directly, this must be done from the
     * UI thread as well.
     * </p>
     *
     * @param activity              the activity which is bound to pop up the dialog box
     * @param dialogTitle           the dialog box title
     * @param dialogMessage         the dialog box message
     * @param positiveButton        the label to display for the dialog box positive button ; may be {@code null} if {@code positiveClickListener} is also {@code null}
     * @param positiveClickListener the callback which will be invoked from the UI thread when the end-user hits the positive button
     * @param negativeButton        the label to display for the dialog box positive button ; may be {@code null} if {@code negativeClickListener} is also {@code null}
     * @param negativeClickListener the callback which will be invoked from the UI thread when the end-user hits the negative button ; may be {@code null}, and in that
     *                              case, the "No" button is hidden
     * @param onCancelListener      the callback which will be invoked from the UI thread when the end-user hits the "back" button ; may be {@code null}, and in that
     *                              case, the dialog box will not be {@link Builder#setCancelable(boolean) cancelleable}
     * @see #showDialog(Activity, CharSequence, CharSequence, CharSequence, OnClickListener, CharSequence, OnClickListener, OnCancelListener)
     */
    protected void onShowDialog(Activity activity, CharSequence dialogTitle, CharSequence dialogMessage,
        CharSequence positiveButton, DialogInterface.OnClickListener positiveClickListener, CharSequence negativeButton,
        DialogInterface.OnClickListener negativeClickListener, DialogInterface.OnCancelListener onCancelListener)
    {
      final Builder builder = new AlertDialog.Builder(activity).setTitle(dialogTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(dialogMessage).setPositiveButton(positiveButton, positiveClickListener);
      builder.setCancelable(onCancelListener == null ? false : true);
      if (onCancelListener != null)
      {
        builder.setOnCancelListener(onCancelListener);
      }
      if (negativeClickListener != null)
      {
        builder.setNegativeButton(negativeButton, negativeClickListener);
      }
      builder.show();
    }

  }

  /**
   * A extension implementation, which proposes to send an issue by e-mail.
   *
   * @since 2011.02.17
   */
  public static class DefaultExceptionHandler
      extends ExceptionHandlers.AbstractExceptionHandler
  {

    private final String logReportRecipients;

    public DefaultExceptionHandler(I18N i18n, IssueAnalyzer issueAnalyzer, String logReportRecipients)
    {
      super(i18n, issueAnalyzer);
      this.logReportRecipients = logReportRecipients;
    }

    @Override
    protected boolean onBusinessObjectAvailableExceptionFallback(Activity activity, Object component,
        BusinessObjectUnavailableException exception)
    {
      if (logReportRecipients == null)
      {
        return super.onBusinessObjectAvailableExceptionFallback(activity, component, exception);
      }
      else
      {
        return sendLogs(activity);
      }
    }

    @Override
    protected boolean onActivityExceptionFallback(Activity activity, Object component, Throwable throwable)
    {
      if (logReportRecipients == null)
      {
        return super.onActivityExceptionFallback(activity, component, throwable);
      }
      else
      {
        return sendLogs(activity);
      }
    }

    protected final boolean sendLogs(final Activity activity)
    {
      // If the logger recipient is not set, no e-mail submission is proposed
      showDialog(activity, i18n.dialogBoxErrorTitle, i18n.otherProblemHint, i18n.reportButtonLabel, new OnClickListener()
      {
        public void onClick(DialogInterface dialogInterface, int which)
        {
          new SendLogsTask(activity, i18n.retrievingLogProgressMessage, "[" + i18n.applicationName + "] Error log - v%1s", logReportRecipients).execute(null, null);
        }
      }, activity.getString(android.R.string.cancel), new OnClickListener()
      {
        public void onClick(DialogInterface dialogInterface, int which)
        {
          // We leave the activity, because we cannot go any further
          activity.finish();
        }
      }, new DialogInterface.OnCancelListener()
      {
        public void onCancel(DialogInterface dialog)
        {
          // We leave the activity, because we cannot go any further
          activity.finish();
        }
      });
      return true;
    }

  }

  /**
   * We do not want any instance of that class to be created.
   */
  private ExceptionHandlers()
  {
  }

}
