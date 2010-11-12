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

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;

import com.smartnsoft.droid4me.framework.LifeCycle;
import com.smartnsoft.droid4me.framework.LifeCycle.BusinessObjectUnavailableException;

/**
 * A basis activity class which is displayed while the application is loading.
 * 
 * The loading of the business objects is synchronous, but in the derived class, if necessary, think of making it
 * {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy asynchronous} if necessary, so as to relief the UI.
 * 
 * @author �douard Mercier
 * @since 2010.01.05
 */
public abstract class SmartSplashScreenActivity
    extends SmartActivity
{

  private static final Set<String> initialized = new HashSet<String>();

  /**
   * @param splashScreenActivity
   *          the class of the splash screen
   * @return whether the given splash screen has been initialized
   */
  public static boolean isInitialized(Class<? extends SmartSplashScreenActivity> splashScreenActivity)
  {
    return SmartSplashScreenActivity.initialized.contains(splashScreenActivity.getName());
  }

  /**
   * Indicates to the framework that the provided splash screen should not be considered initialized anymore.
   * 
   * <p>
   * This is useful within an application which provides a "reset-to-factory" or "log-out" feature.
   * </p>
   * 
   * @param splashScreenActivity
   *          the class of the splash screen
   */
  public static void markAsUnitialized(Class<? extends SmartSplashScreenActivity> splashScreenActivity)
  {
    SmartSplashScreenActivity.initialized.remove(splashScreenActivity.getName());
  }

  private boolean stopActivity;

  private boolean hasStopped;

  /**
   * Run that method once the application has been totally initialized and is ready for use. Should be invoked before the
   * {@link #onFulfillDisplayObjects()} method has been called.
   */
  protected void markAsInitialized()
  {
    SmartSplashScreenActivity.initialized.add(getClass().getName());
  }

  /**
   * @return the activity class which follows the current splash screen
   */
  protected abstract Class<? extends Activity> getNextActivity();

  /**
   * The method that should be overriden, instead of the usual {@link LifeCycle.ForActivity#onRetrieveDisplayObjects()} method.
   * 
   * <p>
   * This method is invoked even if an external storage is required and no external storage is available at runtime.
   * </p>
   */
  protected abstract void onRetrieveDisplayObjectsCustom();

  /**
   * The method that should be overriden, instead of the usual {@link LifeCycle.ForActivity#onRetrieveBusinessObjects()} method.
   * 
   * <p>
   * This method is not invoked when an external storage is required and no external storage is available at runtime.
   * </p>
   */
  protected abstract void onRetrieveBusinessObjectsCustom()
      throws BusinessObjectUnavailableException;

  /**
   * @return <code>true</code> if and only if the application requires an external storage to work
   */
  protected boolean requiresExternalStorage()
  {
    return false;
  }

  /**
   * If the {@link #requiresExternalStorage()} method returns <code>true</code> and that, at runtime, non external storage is available, this method
   * will be invoked.
   * 
   * <p>
   * This is a good place to pop-up an error dialog box and prevent the application from resuming. It is ensured that this callback will be invoked
   * from the GUI thread.
   * <p>
   */
  protected void onNoExternalStorage()
  {
  }

  /**
   * Indicates to the splash screen to stop doing anything, prevents the calling Intent from being executed, and considers the splash-screen as
   * {@link #markAsUnitialized(Class) uninitialized}. Invoke this method during the {@link #onRetrieveBusinessObjectsCustom() method}. May be called
   * from any thread.
   * 
   * <p>
   * This may be extremely useful if you to integrate a licensing mechanism, for instance.
   * </p>
   */
  protected final void stopActivity()
  {
    this.stopActivity = true;
    markAsUnitialized(getClass());
  }

  private boolean canWriteOnExternalStorage()
  {
    // We test whether the SD card is available
    return Environment.getExternalStorageDirectory().canWrite() == true;
  }

  public final void onRetrieveDisplayObjects()
  {
    // We test whether the SD card is available
    if (requiresExternalStorage() == true && canWriteOnExternalStorage() == false)
    {
      stopActivity = true;
      onNoExternalStorage();
    }
    onRetrieveDisplayObjectsCustom();
  }

  @Override
  protected void onStop()
  {
    try
    {
      hasStopped = true;
    }
    finally
    {
      super.onStop();
    }
  }

  public final void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    if (stopActivity == true)
    {
      return;
    }
    onRetrieveBusinessObjectsCustom();
  }

  /**
   * Redirects to the initial activity when a redirection is ongoing. The activity finishes anyway.
   */
  public final void onFulfillDisplayObjects()
  {
    if (stopActivity == true)
    {
      return;
    }
    if (isFinishing() == false && hasStopped == false)
    {
      if (getIntent().hasExtra(ActivityController.CALLING_INTENT) == true)
      {
        // We only resume the previous activity if the splash screen has not been dismissed
        startCallingIntent();
      }
      else
      {
        // We only resume the previous activity if the splash screen has not been dismissed
        startActivity(computeNextIntent());
      }
    }
    finish();
  }

  /**
   * Is invoked when the splash screen is bound to complete successfully, and that it has been started with a
   * {@link ActivityController#CALLING_INTENT calling intent}. Should start the {@link Activity} calling intent.
   */
  protected void startCallingIntent()
  {
    final Intent callingIntent = getIntent().getParcelableExtra(ActivityController.CALLING_INTENT);
    if (log.isDebugEnabled())
    {
      log.debug("Redirecting to the initial activity for the component with class '" + callingIntent.getComponent().getClassName() + "'");
    }
    // This is essential, in order for the activity to be displayed
    callingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(callingIntent);
  }

  /**
   * Invoked when the splash screen is bound to complete successfully, and that it has not been started with a
   * {@link ActivityController#CALLING_INTENT calling intent}.
   * 
   * @return the intent that will be started next
   */
  protected Intent computeNextIntent()
  {
    return new Intent(getApplicationContext(), getNextActivity());
  }

  public void onSynchronizeDisplayObjects()
  {
    if (stopActivity == true)
    {
      return;
    }
  }

}
