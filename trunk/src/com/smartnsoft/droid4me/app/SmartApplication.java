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

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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

  protected final SharedPreferences getPreferences()
  {
    return preferences;
  }

  public static String getFrameworkVersionString()
  {
    return "@DROID4ME_VERSION_NAME@";
  }

  protected ActivityController.Redirector getActivityRedirector()
  {
    return null;
  }

  protected ActivityController.Interceptor getActivityInterceptor()
  {
    return null;
  }

  protected ActivityController.ExceptionHandler getExceptionHandler()
  {
    return new ActivityController.AbstractExceptionHandler(getI18N());
  }

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

  protected void onCreateCustom()
  {
  }

  protected int getLogLevel()
  {
    return Log.WARN;
  }

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
