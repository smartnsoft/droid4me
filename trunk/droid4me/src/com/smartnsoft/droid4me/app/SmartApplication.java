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
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.util.SendLogsTask;

/**
 * To be derived from when using the framework.
 * 
 * @author Édouard Mercier
 * @since 2009.08.21
 */
public abstract class SmartApplication
    extends Application

{

  protected final static Logger log = LoggerFactory.getInstance(SmartApplication.class);

  /**
   * Contains various attributes in order to have the default dialog boxes related to exceptions I18Ned.
   */
  protected final static class I18N
  {

    public final CharSequence dialogBoxErrorTitle;

    public final CharSequence businessObjectAvailabilityProblemHint;

    public final CharSequence serviceProblemHint;

    public final CharSequence connectivityProblemHint;

    public final CharSequence connectivityProblemRetryHint;

    public final CharSequence otherProblemHint;

    public final String applicationName;

    public final CharSequence reportButtonLabel;

    public final String retrievingLogProgressMessage;

    /**
     * @param dialogBoxErrorTitle
     *          the title that will be used when the framework displays an error dialog box
     * @param businessObjectAvailabilityProblemHint
     *          the body of the error dialog box when the business objects are not available on an {@link Activity}
     * @param serviceProblemHint
     *          the body of the error dialog box when a service is not available on an {@link Activity}
     * @param connectivityProblemHint
     *          the body of the error dialog box a connectivity issue occurs
     * @param connectivityProblemRetryHint
     *          the body of the error dialog box a connectivity issue occurs, and that a "Retry" button is displayed
     * @param otherProblemHint
     *          the "Retry" button label of the dialog box when a connectivity issue occurs
     * @param applicationName
     *          the name of the application, used when logging an unexpected problem
     * @param reportButtonLabel
     *          the "Report" button label displayed in the dialog box when an unexpected problem occurs
     * @param retrievingLogProgressMessage
     *          the message of the toast popped up when preparing the unexpected problem log e-mail
     */
    public I18N(CharSequence dialogBoxErrorTitle, CharSequence businessObjectAvailabilityProblemHint, CharSequence serviceProblemHint,
        CharSequence connectivityProblemHint, CharSequence connectivityProblemRetryHint, CharSequence otherProblemHint, String applicationName,
        CharSequence reportButtonLabel, String retrievingLogProgressMessage)
    {
      this.dialogBoxErrorTitle = dialogBoxErrorTitle;
      this.businessObjectAvailabilityProblemHint = businessObjectAvailabilityProblemHint;
      this.serviceProblemHint = serviceProblemHint;
      this.connectivityProblemHint = connectivityProblemHint;
      this.connectivityProblemRetryHint = connectivityProblemRetryHint;
      this.otherProblemHint = otherProblemHint;
      this.applicationName = applicationName;
      this.reportButtonLabel = reportButtonLabel;
      this.retrievingLogProgressMessage = retrievingLogProgressMessage;
    }

    // /**
    // * @param stringResourcesId
    // * the string array identifier which contains all textual resources required in the framework
    // */
    // public I18N(Context context, int stringResourcesId)
    // {
    // final String[] strings = context.getResources().getStringArray(stringResourcesId);
    // this.dialogBoxErrorTitle = strings[0];
    // this.businessObjectAvailabilityProblemHint = strings[1];
    // this.serviceProblemHint = strings[2];
    // this.connectivityProblemHint = strings[3];
    // this.otherProblemHint = strings[4];
    // this.applicationName = strings[5];
    // this.reportButtonLabel = strings[6];
    // this.retrievingLogProgressMessage = strings[7];
    // }

  }

  protected class DefaultExceptionHandler
      extends ActivityController.AbstractExceptionHandler
  {

    public DefaultExceptionHandler(I18N i18n)
    {
      super(i18n);
    }

    @Override
    public boolean onOtherException(final Activity activity, Throwable throwable)
    {
      if (checkConnectivityProblemInCause(activity, throwable, ActivityController.AbstractExceptionHandler.ConnectivityUIExperience.Toast) == true)
      {
        return true;
      }
      // We log the exception in that case
      if (log.isErrorEnabled())
      {
        log.error("An unexpected error occured!", throwable);
      }
      // We make sure that the dialog is popped from the UI thread
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          final I18N i18n = getI18N();
          // If the logger recipient is not set, no e-mail submission is proposed
          final boolean proposeToSendLog = getLogReportRecipient() != null;
          final Builder alertBuilder = new AlertDialog.Builder(activity).setTitle(getI18N().dialogBoxErrorTitle).setIcon(android.R.drawable.ic_dialog_alert).setMessage(
              getI18N().otherProblemHint);
          if (proposeToSendLog == true)
          {
            alertBuilder.setNegativeButton(android.R.string.cancel, new OnClickListener()
            {
              public void onClick(DialogInterface dialogInterface, int i)
              {
                // We leave the activity, because we cannot go any further
                activity.finish();
              }
            }).setPositiveButton(i18n.reportButtonLabel, new OnClickListener()
            {
              public void onClick(DialogInterface dialogInterface, int i)
              {
                new SendLogsTask(activity, i18n.retrievingLogProgressMessage, "[" + i18n.applicationName + "] Error log - v%1s", getLogReportRecipient()).execute(
                    null, null);
              }
            });
          }
          else
          {
            alertBuilder.setPositiveButton(android.R.string.ok, new OnClickListener()
            {
              public void onClick(DialogInterface dialogInterface, int i)
              {
                // We leave the activity, because we cannot go any further
                activity.finish();
              }
            });
          }
          alertBuilder.setCancelable(false).show();
        }
      });
      return true;
    }

  }

  /**
   * @return the e-mail address that will be used when submitting an error log message
   */
  protected abstract String getLogReportRecipient();

  /**
   * Defined as a wrapper over the built-in {@link Thread.UncaughtExceptionHandler uncaught exception handlers}.
   * 
   * @since 2010.07.21
   */
  private final static class SmartUncaughtExceptionHandler
      implements Thread.UncaughtExceptionHandler
  {

    private final Thread.UncaughtExceptionHandler builtinUncaughtExceptionHandler;

    /**
     * @param builtinUncaughtExceptionHandler
     *          the built-in uncaught exception handler that will be invoked eventually.
     */
    public SmartUncaughtExceptionHandler(Thread.UncaughtExceptionHandler builtinUncaughtExceptionHandler)
    {
      this.builtinUncaughtExceptionHandler = builtinUncaughtExceptionHandler;
    }

    public final void uncaughtException(Thread thread, Throwable throwable)
    {
      try
      {
        ActivityController.getInstance().handleException(null, throwable);
      }
      finally
      {
        if (builtinUncaughtExceptionHandler != null)
        {
          if (log.isDebugEnabled())
          {
            log.debug("Resorting to the built-in uncaught exception handler");
          }
          builtinUncaughtExceptionHandler.uncaughtException(thread, throwable);
        }
      }
    }

  }

  /**
   * The overridden default thread uncaught exception handler.
   * 
   * @see SmartApplication#uncaughtExceptionHandler
   */
  private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

  /**
   * The overridden GUI thread uncaught exception handler.
   * 
   * @see SmartApplication#uncaughtExceptionHandler
   */
  private Thread.UncaughtExceptionHandler uiUncaughtExceptionHandler;

  /**
   * The application preferences.
   */
  private SharedPreferences preferences;

  /**
   * @return the shared preferences of the application
   */
  protected final SharedPreferences getPreferences()
  {
    return preferences;
  }

  /**
   * @return the version of the framework the application is using
   */
  public static String getFrameworkVersionString()
  {
    return "@DROID4ME_VERSION_NAME@";
  }

  /**
   * A callback method which enables to indicate whether the process newly created should use the usual workflow.
   * 
   * <p>
   * It is useful when having multiple processes for the same application, and that some of the processes should not use the framework.
   * </p>
   * 
   * @return {@code true} if and only if you want the framework to be ignored for the process. Returns {@code false} by default
   */
  protected boolean shouldBeSilent()
  {
    return false;
  }

  /**
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it).
   * 
   * @return an instance which indicates how to redirect {@link Activity activities} if necessary. Returns {@code null}, which means that no
   *         redirection is handled by default ; override this method, in order to control the redirection mechanism
   */
  protected ActivityController.Redirector getActivityRedirector()
  {
    return null;
  }

  /**
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it).
   * 
   * @return an instance which will be invoked on every {@link Activity} life-cycle event. Returns {@code null}, which means that no interception
   *         is handled by default ; override this method, in order to be notified of activities life-cycle events
   */
  protected ActivityController.Interceptor getActivityInterceptor()
  {
    return null;
  }

  /**
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it).
   * 
   * @return an instance which will be invoked when an exception occurs during the application, provided the exception is handled by the framework.
   *         Returns a {@link ActivityController.AbstractExceptionHandler} instance ; override this method, in order to handle more specifically some
   *         application-specific exceptions
   */
  protected ActivityController.ExceptionHandler getExceptionHandler()
  {
    return new SmartApplication.DefaultExceptionHandler(getI18N());
  }

  /**
   * This method will be invoked by the {@link #getExceptionHandler()} method when building an {@link ActivityController.AbstractExceptionHandler}
   * instance. This internationalization instance will be used to populate default dialog boxes texts popped-up by this default
   * {@link ActivityController.ExceptionHandler}. Hence, the method will be invoked at the application start-up.
   * 
   * @return an instance which contains the internationalized text strings for some built-in error {@link Dialog dialog boxes}. You need to define
   *         that method.
   */
  protected abstract SmartApplication.I18N getI18N();

  @Override
  public final void onCreate()
  {
    final long start = System.currentTimeMillis();
    if (shouldBeSilent() == true)
    {
      return;
    }

    LoggerFactory.logLevel = getLogLevel();
    // This boilerplate is printed whatever the log level
    log.info("Application powered by droid4me " + SmartApplication.getFrameworkVersionString() + " - Copyright Smart&Soft");
    if (log.isDebugEnabled())
    {
      log.debug("Application starting");
    }
    super.onCreate();

    // We register the application exception handler as soon as possible, in order to be able to handle exceptions
    ActivityController.getInstance().registerExceptionHandler(getExceptionHandler());
    final Thread uiThread = Thread.currentThread();
    // We explicitly intercept the GUI thread exceptions, so as to override the default behavior
    final UncaughtExceptionHandler uiBuiltinUuncaughtExceptionHandler = uiThread.getUncaughtExceptionHandler();
    uiUncaughtExceptionHandler = new SmartApplication.SmartUncaughtExceptionHandler(uiBuiltinUuncaughtExceptionHandler);
    if (log.isDebugEnabled())
    {
      log.debug("The application " + (uiBuiltinUuncaughtExceptionHandler == null ? "does not have" : "has") + " a built-in GUI uncaught exception handler");
    }
    Thread.currentThread().setUncaughtExceptionHandler(uiUncaughtExceptionHandler);
    // We make sure that other uncaught exceptions will be intercepted and handled
    final UncaughtExceptionHandler builtinUuncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    uncaughtExceptionHandler = new SmartApplication.SmartUncaughtExceptionHandler(builtinUuncaughtExceptionHandler);
    if (log.isDebugEnabled())
    {
      log.debug("The application " + (builtinUuncaughtExceptionHandler == null ? "does not have" : "has") + " a built-in default uncaught exception handler");
    }
    Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

    // We check the license of the framework
    if (log.isDebugEnabled())
    {
      log.debug("Checking the droid4me license");
    }
    checkLicense(this);

    // We initialize the default intent actions
    AppPublics.initialize(this);

    final ActivityController.Redirector redirector = getActivityRedirector();
    if (redirector != null)
    {
      ActivityController.getInstance().registerRedirector(redirector);
    }

    final ActivityController.Interceptor interceptor = getActivityInterceptor();
    if (interceptor != null)
    {
      ActivityController.getInstance().registerInterceptor(interceptor);
    }

    preferences = PreferenceManager.getDefaultSharedPreferences(this);

    onCreateCustom();

    if (log.isInfoEnabled())
    {
      log.info("The application has started in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  /**
   * @return the path of the directory on the SD card where the persistence can be stored
   */
  final protected File getExternalStorageApplicationDirectory()
  {
    String actualDirectoryName;
    try
    {
      actualDirectoryName = getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(getPackageName(), 0)).toString();
    }
    catch (NameNotFoundException exception)
    {
      // Should never happen!
      if (log.isWarnEnabled())
      {
        log.warn("Cannot find the application info", exception);
      }
      actualDirectoryName = "SmartNSoft";
    }
    return new File(Environment.getExternalStorageDirectory(), actualDirectoryName);
  }

  protected void checkLicense(Application application)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Using the open source droid4me license");
    }
  }

  /**
   * This method will be invoked at the end of the {@link #onCreate()} method, once the framework initialization is over. You can override this
   * method, which does nothing by default, in order to initialize your application specific variables, invoke some services.
   * 
   * <p>
   * Keep in mind that this method should complete very quickly, in order to prevent from hanging the GUI thread, and thus causing a bad end-user
   * experience, and a potential ANR.
   * </p>
   */
  protected void onCreateCustom()
  {
  }

  /**
   * @return the log level trigger of the application
   */
  protected int getLogLevel()
  {
    return Log.WARN;
  }

  /**
   * In addition to the default behavior, this event will be logged, and the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL} and
   * {@link AppInternals#THREAD_POOL} thread pools stopped.
   */
  @Override
  public void onTerminate()
  {
    if (shouldBeSilent() == true)
    {
      return;
    }

    if (log.isDebugEnabled())
    {
      log.debug("Application terminating");
    }
    try
    {
      // We stop the threads pools
      SmartCommands.LOW_PRIORITY_THREAD_POOL.shutdown();
      AppInternals.THREAD_POOL.shutdown();
    }
    finally
    {
      super.onTerminate();
    }
  }

  /**
   * In addition to the default behavior, this event will be logged.
   */
  @Override
  public void onLowMemory()
  {
    if (shouldBeSilent() == true)
    {
      return;
    }

    if (log.isWarnEnabled())
    {
      log.warn("Application low memory");
    }
    super.onLowMemory();
  }

}
