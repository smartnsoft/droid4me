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
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.smartnsoft.droid4me.app.ActivityController.ExceptionHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.util.SendLogsTask;

/**
 * An abstract to be implemented when using the framework, because it initializes some of the components, and eases the development.
 * 
 * If you use your own implementation, do not forget to declare it in the {@code AndroidManifest.xml}.
 * 
 * @author Édouard Mercier
 * @since 2009.08.21
 */
public abstract class SmartApplication
    extends Application

{

  protected final static Logger log = LoggerFactory.getInstance(SmartApplication.class);

  /**
   * Contains various attributes in order to have the default dialog boxes related to exceptions i18ned.
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

  }

  protected class DefaultExceptionHandler
      extends ActivityController.AbstractExceptionHandler
  {

    public DefaultExceptionHandler(I18N i18n)
    {
      super(i18n);
    }

    @Override
    protected boolean onOtherExceptionFallback(final Activity activity, Object component, Throwable throwable)
    {
      final boolean proposeToSendLog = getLogReportRecipient() != null;
      if (proposeToSendLog == false)
      {
        return super.onOtherExceptionFallback(activity, component, throwable);
      }
      else
      {
        activity.runOnUiThread(new Runnable()
        {
          public void run()
          {
            final I18N i18n = getI18N();
            // If the logger recipient is not set, no e-mail submission is proposed
            showDialog(activity, getI18N().dialogBoxErrorTitle, getI18N().otherProblemHint, i18n.reportButtonLabel, new OnClickListener()
            {
              public void onClick(DialogInterface dialogInterface, int which)
              {
                new SendLogsTask(activity, i18n.retrievingLogProgressMessage, "[" + i18n.applicationName + "] Error log - v%1s", getLogReportRecipient()).execute(
                    null, null);
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
          }
        });
        return true;
      }
    }

  }

  /**
   * If the application uses an {@link SmartApplication.DefaultExceptionHandler} as an {@link ActivityController.ExceptionHandler}, when a managed
   * exception is detected, and is not handled, a dialog box is submitted to the end-user, in order to propose to send the bug cause by inspecting the
   * Android {@code logcat}. In that case, do not forget to declare the {@code android.permission.READ_LOGS} permission in the
   * {@code AndroidManifest.xml}.
   * 
   * @return the e-mail address that will be used when submitting an error log message ; if it returns {@code null}, the application will not propose
   *         to send the bug cause
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

    /**
     * The previous exception handler.
     */
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
        ActivityController.getInstance().handleException(null, null, throwable);
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
   * <p>
   * Caution: this method will return {@code null} as long as the parent {@link Application#onCreate()} has not been invoked.
   * </p>
   * 
   * @return the shared preferences of the application
   */
  protected final SharedPreferences getPreferences()
  {
    return preferences;
  }

  /**
   * @return the human-readable version of the framework the application is using
   */
  public static String getFrameworkVersionString()
  {
    return "@DROID4ME_VERSION_NAME@";
  }

  /**
   * A callback method which enables to indicate whether the process newly created should use the default {@link SmartApplication} workflow.
   * 
   * <p>
   * It is useful when having multiple processes for the same application, and that some of the processes should not use the framework.
   * </p>
   * 
   * @return {@code true} if and only if you want the framework to be ignored for the process. Returns {@code false} by default
   * @see #onCreateCustomSilent()
   */
  protected boolean shouldBeSilent()
  {
    return false;
  }

  /**
   * This callback will be invoked by the application instance, in order to get a reference on the application {@link ActivityController.Redirector}:
   * this method is responsible for creating an implementation of this component interface. Override this method, in order to control the redirection
   * mechanism.
   * 
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which you should not), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   * 
   * @return an instance which indicates how to redirect {@link Activity activities} if necessary. If {@code null}, this means that no redirection is
   *         handled. Returns {@code null} by default
   * @see ActivityController#registerRedirector(ActivityController.Redirector)
   */
  protected ActivityController.Redirector getActivityRedirector()
  {
    return null;
  }

  /**
   * This callback will be invoked by the application instance, in order to get a reference on the application {@link ActivityController.Interceptor}:
   * this method is responsible for creating an implementation of this component interface. Override this method, in order to control the interception
   * mechanism.
   * 
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which you should not), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   * 
   * @return an instance which will be invoked on every {@link Activity} life-cycle event. IF {@code null}, this means that no interception is
   *         handled. Returns {@code null} by default
   * @see ActivityController#registerInterceptor(ActivityController.Interceptor)
   */
  protected ActivityController.Interceptor getInterceptor()
  {
    return null;
  }

  /**
   * This callback will be invoked by the application instance, in order to get a reference on the application
   * {@link ActivityController.ExceptionHandler}: this method is responsible for creating an implementation of this component interface. Override this
   * method, in order to handle more specifically some application-specific exceptions.
   * 
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which should not be the case), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   * 
   * @return an instance which will be invoked when an exception occurs during the application, provided the exception is handled by the framework ;
   *         may be {@code null}, if no {@link ActivityController.ExceptionHandler} should be used by the application. Returns a new instance of
   *         {@link SmartApplication.ExceptionHandler} by default
   * @see ActivityController#registerExceptionHandler(ExceptionHandler)
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

  /**
   * The method will invoke the {@link SmartApplication#onCreateCustom()} method, provided the {@link #shouldBeSilent() method} returns {@code false},
   * and will perform the following things:
   * <ol>
   * <li>set the {@link LoggerFactory#logLevel log level},</li>
   * <li>register the {@link ActivityController.ExceptionHandler},</li>
   * <li>register the {@link ActivityController.Redirector},</li>
   * <li>register the {@link ActivityController.Interceptor},</li>
   * <li>register an internal default {@link Thread.UncaughtExceptionHandler} for the UI and for the background threads,</li>
   * <li>logs how much time the {@link #onCreate()} method execution took.</li>
   * </ol>
   * 
   * @see #getLogLevel()
   * @see #getExceptionHandler()
   * @see #getActivityRedirector()
   * @see #getInterceptor()
   */
  @Override
  public final void onCreate()
  {
    final long start = System.currentTimeMillis();

    LoggerFactory.logLevel = getLogLevel();
    // We initialize the preferences very soon, so that they are available
    preferences = PreferenceManager.getDefaultSharedPreferences(this);

    if (shouldBeSilent() == true)
    {
      if (log.isDebugEnabled())
      {
        log.debug("Application starting in silent mode");
      }
      super.onCreate();
      onCreateCustomSilent();
      return;
    }

    // This boilerplate is printed whatever the log level
    log.info("Application with package name '" + getPackageName() + "' powered by droid4me " + SmartApplication.getFrameworkVersionString() + " - Copyright Smart&Soft");
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
      log.debug("The application with package name '" + getPackageName() + "' " + (uiBuiltinUuncaughtExceptionHandler == null ? "does not have" : "has") + " a built-in GUI uncaught exception handler");
    }
    Thread.currentThread().setUncaughtExceptionHandler(uiUncaughtExceptionHandler);
    // We make sure that other uncaught exceptions will be intercepted and handled
    final UncaughtExceptionHandler builtinUuncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    uncaughtExceptionHandler = new SmartApplication.SmartUncaughtExceptionHandler(builtinUuncaughtExceptionHandler);
    if (log.isDebugEnabled())
    {
      log.debug("The application with package name '" + getPackageName() + "' " + (builtinUuncaughtExceptionHandler == null ? "does not have" : "has") + " a built-in default uncaught exception handler");
    }
    Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

    // We check the license of the framework
    if (log.isDebugEnabled())
    {
      log.debug("Checking the droid4me license for the application with package name '" + getPackageName() + "'");
    }
    checkLicense(this);

    // We initialize the default intent actions
    AppPublics.initialize(this);

    final ActivityController.Redirector redirector = getActivityRedirector();
    if (redirector != null)
    {
      ActivityController.getInstance().registerRedirector(redirector);
    }

    final ActivityController.Interceptor interceptor = getInterceptor();
    if (interceptor != null)
    {
      ActivityController.getInstance().registerInterceptor(interceptor);
    }

    onCreateCustom();

    if (log.isInfoEnabled())
    {
      log.info("The application with package name '" + getPackageName() + "' has started in " + (System.currentTimeMillis() - start) + " ms");
    }
  }

  /**
   * This method will be invoked if and only if the {@link #shouldBeSilent()} method has returned {@code true}.
   * 
   * <p>
   * This enables to execute some code, even if the application runs in silent mode.
   * </p>
   * 
   * @see #shouldBeSilent()
   */
  protected void onCreateCustomSilent()
  {
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
   * 
   * <p>
   * The method does nothing, by default.
   * </p>
   */
  protected void onCreateCustom()
  {
  }

  /**
   * This method will be invoked just once during the {@link Application#onCreate()} method, in order to set the {@link LoggerFactory#logLevel}.
   * 
   * @return the log level trigger of the application; defaults to {@link Log#WARN}
   * @see LoggerFactory#logLevel
   */
  protected int getLogLevel()
  {
    return Log.WARN;
  }

  /**
   * In addition to the default behavior, this event will be logged, and the {@link SmartCommands#LOW_PRIORITY_THREAD_POOL} and
   * {@link AppInternals#THREAD_POOL} thread pools shut down.
   */
  @Override
  public void onTerminate()
  {
    try
    {
      if (shouldBeSilent() == true)
      {
        return;
      }

      if (log.isDebugEnabled())
      {
        log.debug("Application terminating");
      }
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
    try
    {
      if (shouldBeSilent() == true)
      {
        return;
      }

      if (log.isWarnEnabled())
      {
        log.warn("Application low memory");
      }
    }
    finally
    {
      super.onLowMemory();
    }
  }

  /**
   * Logs in {@link Log#INFO} verbosity the hosting device information, such as its model, density, screen size.
   */
  protected void logDeviceInformation()
  {
    if (log.isInfoEnabled())
    {
      final DisplayMetrics displayMetrics = new DisplayMetrics();
      ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
      final int screenLayout = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
      final String screenLayoutString;
      switch (screenLayout)
      {
      case Configuration.SCREENLAYOUT_SIZE_SMALL:
        screenLayoutString = "small";
        break;
      case Configuration.SCREENLAYOUT_SIZE_NORMAL:
        screenLayoutString = "normal";
        break;
      case Configuration.SCREENLAYOUT_SIZE_LARGE:
        screenLayoutString = "large";
        break;
      case 4:
        screenLayoutString = "xlarge";
        break;
      default:
        screenLayoutString = "unknown: " + screenLayout;
      }
      log.info("The application with package name '" + getPackageName() + "' is running on the device '" + Build.MODEL + "', running Android API level " + VERSION.SDK_INT + " (v" + VERSION.RELEASE + ") with density in dpi '" + displayMetrics.densityDpi + "', density '" + displayMetrics.density + "', screen size '" + screenLayoutString + "' (" + displayMetrics.widthPixels + "x" + displayMetrics.heightPixels + ")");
    }
  }

}
