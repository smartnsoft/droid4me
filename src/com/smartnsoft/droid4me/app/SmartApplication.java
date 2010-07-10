/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

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

    protected final CharSequence dialogBoxErrorTitle;

    protected final CharSequence businessObjectAvailabilityProblemHint;

    protected final CharSequence serviceProblemHint;

    protected final CharSequence connectivityProblemHint;

    protected final CharSequence otherProblemHint;

    /**
     * @param dialogBoxErrorTitle
     *          the title that will be used when the framework displays an error dialog box
     * @param businessObjectAvailabilityProblemHint
     *          the body of the error dialog box when the business objects are not available on an {@link Activity}
     * @param serviceProblemHint
     *          the body of the error dialog box when a service is not available on an {@link Activity}
     * @param connectivityProblemHint
     *          the body of the error dialog box a connectivity issue occurs an {@link Activity}
     * @param otherProblemHint
     *          the body of the error dialog box when an unhandled problem occurs an {@link Activity}
     */
    public I18N(CharSequence dialogBoxErrorTitle, CharSequence businessObjectAvailabilityProblemHint, CharSequence serviceProblemHint,
        CharSequence connectivityProblemHint, CharSequence otherProblemHint)
    {
      this.dialogBoxErrorTitle = dialogBoxErrorTitle;
      this.businessObjectAvailabilityProblemHint = businessObjectAvailabilityProblemHint;
      this.serviceProblemHint = serviceProblemHint;
      this.connectivityProblemHint = connectivityProblemHint;
      this.otherProblemHint = otherProblemHint;
    }

  }

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
   * @return an instance which indicates how to redirect {@link Activity activities} if necessary. Returns <code>null</code>, which means that no
   *         redirection is handled by default ; override this method, in order to control the redirection mechanism
   */
  protected ActivityController.Redirector getActivityRedirector()
  {
    return null;
  }

  /**
   * @return an instance which will be invoked on every {@link Activity} life-cycle event. Returns <code>null</code>, which means that no interception
   *         is handled by default ; override this method, in order to be notified of activities life-cycle events
   */
  protected ActivityController.Interceptor getActivityInterceptor()
  {
    return null;
  }

  /**
   * @return an instance which will be invoked when an exception occurs during the application, provided the exception is handled by the framework.
   *         Returns a {@link ActivityController.AbstractExceptionHandler} instance ; override this method, in order to handle more specifically some
   *         application-specific exceptions
   */
  protected ActivityController.ExceptionHandler getExceptionHandler()
  {
    return new ActivityController.AbstractExceptionHandler(getI18N());
  }

  /**
   * This method will be invoked by the {@link #getExceptionHandler()} method when building an {@link ActivityController.AbstractExceptionHandler}
   * instance. This internationalization instance will be used to populate default dialog boxes texts popped-up by this default
   * {@link ActivityController.ExceptionHandler}.
   * 
   * @return an instance which contains the internationalized text strings for some built-in error {@link Dialog dialog boxes}. You need to define
   *         that method.
   */
  protected abstract SmartApplication.I18N getI18N();

  @Override
  public final void onCreate()
  {
    LoggerFactory.logLevel = getLogLevel();
    // This boilerplate is printed whatever the log level
    log.info("Application powered by droid4me " + SmartApplication.getFrameworkVersionString() + " - Copyright Smart&Soft");
    if (log.isDebugEnabled())
    {
      log.debug("Application starting");
    }
    super.onCreate();

    // We register the exception handler as soon as possible, in order to be able to handle exceptions
    ActivityController.getInstance().registerExceptionHandlder(getExceptionHandler());

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
   * experience, and a potentional ANR.
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
   * In addition to the default behavior, this event will be logged, and the {@link AppPublics#LOW_PRIORITY_THREAD_POOL} and
   * {@link AppPublics#THREAD_POOL} thread pools stopped.
   */
  @Override
  public void onTerminate()
  {
    if (log.isDebugEnabled())
    {
      log.debug("Application terminating");
    }
    try
    {
      // We stop the thread pools
      AppPublics.LOW_PRIORITY_THREAD_POOL.shutdown();
      AppPublics.THREAD_POOL.shutdown();
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
    if (log.isWarnEnabled())
    {
      log.warn("Application low memory");
    }
    super.onLowMemory();
  }

}
