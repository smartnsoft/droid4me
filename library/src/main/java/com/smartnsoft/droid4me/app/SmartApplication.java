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

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
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

import com.smartnsoft.droid4me.BuildConfig;
import com.smartnsoft.droid4me.app.ActivityController.ExceptionHandler;
import com.smartnsoft.droid4me.app.ExceptionHandlers.AbstractExceptionHandler;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * An abstract to be implemented when using the framework, because it initializes some of the components, and eases the development.
 * <p>
 * If you use your own implementation, do not forget to declare it in the {@code AndroidManifest.xml}.
 *
 * @author Édouard Mercier
 * @since 2009.08.21
 */
public abstract class SmartApplication
    extends Application
{

  /**
   * Contains various attributes in order to have the default dialog boxes related to exceptions i18ned.
   */
  public static final class I18N
  {

    public CharSequence dialogBoxErrorTitle;

    public CharSequence businessObjectAvailabilityProblemHint;

    public CharSequence serviceProblemHint;

    public CharSequence connectivityProblemHint;

    public CharSequence connectivityProblemRetryHint;

    public CharSequence otherProblemHint;

    public String applicationName;

    public CharSequence reportButtonLabel;

    public String retrievingLogProgressMessage;

    /**
     * @param dialogBoxErrorTitle                   the title that will be used when the framework displays an error dialog box
     * @param businessObjectAvailabilityProblemHint the body of the error dialog box when the business objects are not available on an {@link Activity}
     * @param serviceProblemHint                    the body of the error dialog box when a service is not available on an {@link Activity}
     * @param connectivityProblemHint               the body of the error dialog box a connectivity issue occurs
     * @param connectivityProblemRetryHint          the body of the error dialog box a connectivity issue occurs, and that a "Retry" button is displayed
     * @param otherProblemHint                      the "Retry" button label of the dialog box when a connectivity issue occurs
     * @param applicationName                       the name of the application, used when logging an unexpected problem
     * @param reportButtonLabel                     the "Report" button label displayed in the dialog box when an unexpected problem occurs
     * @param retrievingLogProgressMessage          the message of the toast popped up when preparing the unexpected problem log e-mail
     */
    public I18N(CharSequence dialogBoxErrorTitle, CharSequence businessObjectAvailabilityProblemHint,
        CharSequence serviceProblemHint, CharSequence connectivityProblemHint,
        CharSequence connectivityProblemRetryHint, CharSequence otherProblemHint, String applicationName,
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

  protected final static Logger log = LoggerFactory.getInstance(SmartApplication.class);

  /**
   * A flag which enables to remember when the @link {@link Application#onCreate()} method invocation is over.
   */
  private static boolean isOnCreatedDone;

  /**
   * Enables to know whether the {@link #onCreateCustom()} method has been invoked.
   *
   * @return {@code true} if and only if the {@link #onCreateCustom()} method has been invoked and its executio is over
   */
  public static boolean isOnCreatedDone()
  {
    return SmartApplication.isOnCreatedDone;
  }

  /**
   * @return the human-readable version of the framework the application is using
   */
  public static String getFrameworkVersionString()
  {
    return BuildConfig.VERSION_NAME;
  }

  /**
   * The application preferences.
   */
  private SharedPreferences preferences;

  /**
   * Indicates whether the {@link #onCreate()} method has already been invoked.
   */
  private boolean onCreateInvoked;

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
   * <p>
   * <p>
   * Note: if the method has already been invoked once, the second time (because of a concurrent access), it will do nothing but output an error log.
   * This is the reason why the method has been declared as synchronized.
   * </p>
   * <p>
   * <p>
   * This method normally does not need to be override, if needed override rather the {@link #onCreateCustom() method}
   * </p>
   *
   * @see #getLogLevel()
   * @see #getExceptionHandler()
   * @see #getActivityRedirector()
   * @see #getInterceptor()
   */
  @Override
  public synchronized void onCreate()
  {
    if (onCreateInvoked == true)
    {
      if (log.isErrorEnabled())
      {
        log.error("The 'Application.onCreate()' method has already been invoked!");
      }
      return;
    }
    onCreateInvoked = true;
    try
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
      setupDefaultExceptionHandlers();

      // We check the license of the framework
      if (log.isDebugEnabled())
      {
        log.debug("Checking the droid4me license for the application with package name '" + getPackageName() + "'");
      }
      checkLicense(this);

      // We initialize the default intent actions
      AppPublics.initialize(this);

      // We register the system service provider
      final ActivityController.SystemServiceProvider systemServiceProvider = getSystemServiceProvider();
      if (systemServiceProvider != null)
      {
        ActivityController.getInstance().registerSystemServiceProvider(systemServiceProvider);
      }

      // We register the Activity redirector
      final ActivityController.Redirector redirector = getActivityRedirector();
      if (redirector != null)
      {
        ActivityController.getInstance().registerRedirector(redirector);
      }

      // We register the entities interceptor
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
    finally
    {
      SmartApplication.isOnCreatedDone = true;
    }
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
   * A callback method which enables to indicate whether the process newly created should use the default {@link SmartApplication} workflow.
   * <p>
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
   * This callback will be invoked by the application instance, in order to get a reference on the application
   * {@link ActivityController.SystemServiceProvider}: this method is responsible for creating an implementation of this component interface. Override
   * this method, in order to override the system services which are provided by default by an {@link Activity}.
   * <p>
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which you should not), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   *
   * @return an instance which enables to override or provide additional system services. If {@code null}, the {@link Activity} default system
   * services will be returned
   * @see ActivityController#registerSystemServiceProvider(ActivityController.SystemServiceProvider)
   */
  protected ActivityController.SystemServiceProvider getSystemServiceProvider()
  {
    return null;
  }

  /**
   * This callback will be invoked by the application instance, in order to get a reference on the application {@link ActivityController.Redirector}:
   * this method is responsible for creating an implementation of this component interface. Override this method, in order to control the redirection
   * mechanism.
   * <p>
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which you should not), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   *
   * @return an instance which indicates how to redirect {@link Activity activities} if necessary. If {@code null}, this means that no redirection is
   * handled. Returns {@code null} by default
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
   * <p>
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which you should not), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   *
   * @return an instance which will be invoked on every {@link Activity} life-cycle event. IF {@code null}, this means that no interception is
   * handled. Returns {@code null} by default
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
   * <p>
   * <p>
   * It is ensured that the framework will only call once this method (unless you explicitly invoke it, which should not be the case), during the
   * {@link Application#onCreate()} method execution.
   * </p>
   *
   * @return an instance which will be invoked when an exception occurs during the application, provided the exception is handled by the framework ;
   * may be {@code null}, if no {@link ActivityController.ExceptionHandler} should be used by the application. Returns a new instance of
   * {@link ExceptionHandler} by default
   * @see ActivityController#registerExceptionHandler(ExceptionHandler)
   */
  protected ActivityController.ExceptionHandler getExceptionHandler()
  {
    return new ExceptionHandlers.DefaultExceptionHandler(getI18N(), null);
  }

  /**
   * This method will be invoked by the {@link #getExceptionHandler()} method when building an {@link AbstractExceptionHandler}
   * instance. This internationalization instance will be used to populate default dialog boxes texts popped-up by this default
   * {@link ActivityController.ExceptionHandler}. Hence, the method will be invoked at the application start-up.
   *
   * @return an instance which contains the internationalized text strings for some built-in error {@link Dialog dialog boxes}. You need to define
   * that method.
   */
  protected abstract SmartApplication.I18N getI18N();

  /**
   * This is the place where to register other {@link UncaughtExceptionHandler default exception handlers} like <a
   * href="https://github.com/ACRA/acra">ACRA</a>. The default implementation does nothing, and if overriden, this method should not invoke its
   * {@code super} method.
   * <p>
   * <p>
   * It is ensured that the framework default exception handlers will be set-up after this method, and they will fallback to the already registered
   * default exception handlers.
   * </p>
   */
  protected void onSetupExceptionHandlers()
  {
  }

  /**
   * This method will be invoked if and only if the {@link #shouldBeSilent()} method has returned {@code true}.
   * <p>
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
   * <p>
   * <p>
   * Keep in mind that this method should complete very quickly, in order to prevent from hanging the GUI thread, and thus causing a bad end-user
   * experience, and a potential ANR.
   * </p>
   * <p>
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

  private void setupDefaultExceptionHandlers()
  {
    // We let the overidding application register its exception handlers
    onSetupExceptionHandlers();

    ActivityController.getInstance().registerExceptionHandler(getExceptionHandler());
    // We make sure that all uncaught exceptions will be intercepted and handled
    final UncaughtExceptionHandler builtinUuncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    final SmartCommands.SmartUncaughtExceptionHandler uncaughtExceptionHandler = new SmartCommands.SmartUncaughtExceptionHandler(getApplicationContext(), builtinUuncaughtExceptionHandler);
    if (log.isDebugEnabled())
    {
      log.debug("The application with package name '" + getPackageName() + "' " + (builtinUuncaughtExceptionHandler == null ? "does not have" : "has") + " a built-in default uncaught exception handler");
    }
    Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
  }

}
