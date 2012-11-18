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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;

import com.smartnsoft.droid4me.LifeCycle;

/**
 * A basis activity class which is displayed while the application is loading.
 * 
 * The loading of the business objects is synchronous, but in the derived class, if necessary, think of making it
 * {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy asynchronous} if necessary, so as to relief the UI.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2010.01.05
 */
public abstract class SmartSplashScreenActivity<AggregateClass, BusinessObjectClass>
    extends SmartActivity<AggregateClass>
    implements AppPublics.BroadcastListener
{

  /**
   * Indicates whether an {@link Activity} is marked as initialized.
   * 
   * @param activityClass
   *          the class of the splash screen
   * @return a non {@code null} date, if and only if the given screen has been initialized ; in that case, the returned date is the timestamp when it
   *         has been marked as {@link SmartSplashScreenActivity#markAsInitialized(Class))}
   */
  public static Date isInitialized(Class<? extends Activity> activityClass)
  {
    return SmartSplashScreenActivity.initialized.get(activityClass.getName());
  }

  /**
   * Indicates whether the provided {@code activity} should be considered as initialized.
   * 
   * <p>
   * This is useful within an application which provides a "reset-to-factory" or "log-out" feature.
   * </p>
   * 
   * @param activityClass
   *          the class of the screen
   * @param isInitialized
   *          the flag which indicates the provided activity initialization status
   */
  public static void markAsInitialized(Class<? extends Activity> activityClass, boolean isInitialized)
  {
    if (isInitialized == false)
    {
      SmartSplashScreenActivity.initialized.remove(activityClass.getName());
      SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomStarted = false;
      SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomOverInvoked = false;
      SmartSplashScreenActivity.businessObject = null;
    }
    else
    {
      SmartSplashScreenActivity.initialized.put(activityClass.getName(), new Date());
    }
  }

  private static final String BUSINESS_OBJECTS_LOADED_ACTION = "com.smartnsoft.droid4me.action.BUSINESS_OBJECTS_LOADED";

  private static final Map<String, Date> initialized = new HashMap<String, Date>();

  private static boolean onRetrieveBusinessObjectsCustomStarted;

  private static boolean onRetrieveBusinessObjectsCustomOver;

  private static boolean onRetrieveBusinessObjectsCustomOverInvoked;

  private static Object businessObject;

  private boolean stopActivity;

  private boolean pauseActivity;

  private boolean hasStopped;

  /**
   * This method is run internally, once the application has been totally initialized and is ready for use.
   */
  private final void markAsInitialized()
  {
    SmartSplashScreenActivity.markAsInitialized(getClass(), true);
  }

  @Override
  protected void onStart()
  {
    super.onStart();

    if (log.isDebugEnabled())
    {
      log.debug("Marking the splash screen as un-stopped");
    }
    hasStopped = false;
  }

  @Override
  protected void onStop()
  {
    try
    {
      if (log.isDebugEnabled())
      {
        log.debug("Marking the splash screen as stopped");
      }
      hasStopped = true;
    }
    finally
    {
      super.onStop();
    }
  }

  /**
   * @return the activity class which follows the current splash screen
   */
  protected abstract Class<? extends Activity> getNextActivity();

  /**
   * The method that should be overriden, instead of the usual {@link LifeCycle#onRetrieveDisplayObjects()} method.
   * 
   * <p>
   * This method will be invoked just once for the first {@link Activity} instance.
   * </p>
   * 
   * <p>
   * This method is invoked even if an external storage is required and no external storage is available at runtime.
   * </p>
   */
  protected abstract void onRetrieveDisplayObjectsCustom();

  /**
   * The method that should be overriden, instead of the usual {@link LifeCycle#onRetrieveBusinessObjects()} method.
   * 
   * <p>
   * This method is not invoked when an external storage is required and no external storage is available at runtime.
   * </p>
   * 
   * @return a business object ; may be {@code null}
   */
  protected abstract BusinessObjectClass onRetrieveBusinessObjectsCustom()
      throws BusinessObjectUnavailableException;

  /**
   * This method will be invoked when the business objects retrieval is over. This is the ideal place to run something, before the splash screen is
   * dismissed.
   * 
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * 
   * <p>
   * It is ensured that this callback will be not be invoked anymore as soon as an instance has {@link Runnable#run()} the provided
   * {@code finishRunnable} callback.
   * </p>
   * 
   * <p>
   * The default implementation just invokes the provided {@code finishRunnable} {@link Runnable#run()} method, which {@link Activity#finish()
   * finishes} the current splash screen.
   * </p>
   * 
   * @param businessObject
   *          the business object that has been returned by the {@link #onRetrieveDisplayObjectsCustom()} method ; may be {@code null}
   * @param finishRunnable
   *          the callback that should be {@link Runnable#run()} as soon as the splash screen should be ended. The provided callback may be invoked
   *          from any thread.
   */
  protected void onRetrieveBusinessObjectsCustomOver(BusinessObjectClass businessObject, Runnable finishRunnable)
  {
    finishRunnable.run();
  }

  /**
   * @return {@code true} if and only if the application requires an external storage to work
   */
  protected boolean requiresExternalStorage()
  {
    return false;
  }

  /**
   * If the {@link #requiresExternalStorage()} method returns {@code true} and that, at runtime, non external storage is available, this method will
   * be invoked.
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
   * {@link #markAsUnitialized(Class) uninitialized}. Once this method has been invoked, the splash screen will never be able to resume.
   * 
   * <p>
   * Invoke this method during the {@link #onRetrieveBusinessObjectsCustom() method}. It may be called from any thread.
   * </p>
   * 
   * <p>
   * This may be extremely useful if you to integrate a licensing mechanism, for instance.
   * </p>
   */
  protected final void stopActivity()
  {
    this.stopActivity = true;
    SmartSplashScreenActivity.markAsInitialized(getClass(), false);
  }

  /**
   * Indicates to the splash screen to pause, and prevents the calling Intent from being executed, and the current {@link Activity} from
   * {@link Activity#finish() finishing}.
   * 
   * <p>
   * Invoke this method during the {@link #onRetrieveBusinessObjectsCustom() method}. It may be called from any thread.
   * <p/>
   * 
   * @see #resumeActivity()
   */
  protected final void pauseActivity()
  {
    this.pauseActivity = true;
  }

  /**
   * Indicates to the splash screen to resume: the calling Intent will be executed, and the current {@link Activity} will {@link Activity#finish()
   * finish}.
   * 
   * <p>
   * Invoke this method during the {@link #onRetrieveBusinessObjectsCustom() method}. It may be called from any thread.
   * </p>
   * 
   * @see #pauseActivity()
   */
  protected final void resumeActivity()
  {
    this.pauseActivity = false;
    finishActivity();
  }

  public IntentFilter getIntentFilter()
  {
    final IntentFilter intentFilter = new IntentFilter(SmartSplashScreenActivity.BUSINESS_OBJECTS_LOADED_ACTION);
    intentFilter.addCategory(getPackageName());
    return intentFilter;
  }

  @SuppressWarnings("unchecked")
  public void onReceive(Intent intent)
  {
    if (SmartSplashScreenActivity.BUSINESS_OBJECTS_LOADED_ACTION.equals(intent.getAction()) == true)
    {
      markAsInitialized();
      if (isFinishing() == false)
      {
        // We do not take into account the event on the activity instance which is over
        if (SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomOverInvoked == false)
        {
          onRetrieveBusinessObjectsCustomOver((BusinessObjectClass) SmartSplashScreenActivity.businessObject, new Runnable()
          {
            public void run()
            {
              SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomOverInvoked = true;
              finishActivity();
              // We release the shared business object
              SmartSplashScreenActivity.businessObject = null;
            }
          });
        }
      }
    }
  }

  public final void onRetrieveDisplayObjects()
  {
    // We test whether the SD card is available
    if (requiresExternalStorage() == true && canWriteOnExternalStorage() == false)
    {
      stopActivity = true;
      onNoExternalStorage();
      return;
    }
    onRetrieveDisplayObjectsCustom();
  }

  public final void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    if (stopActivity == true)
    {
      return;
    }
    // We check whether another activity instance is already running the business objects retrieval
    if (SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomStarted == false)
    {
      SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomStarted = true;
      boolean onRetrieveBusinessObjectsCustomSuccess = false;
      try
      {
        SmartSplashScreenActivity.businessObject = onRetrieveBusinessObjectsCustom();
        onRetrieveBusinessObjectsCustomSuccess = true;
      }
      finally
      {
        // If the retrieval of the business objects is a failure, we assume as if it had not been started
        if (onRetrieveBusinessObjectsCustomSuccess == false)
        {
          SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomStarted = false;
        }
      }
      SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomOver = true;
      sendBroadcast(new Intent(SmartSplashScreenActivity.BUSINESS_OBJECTS_LOADED_ACTION).addCategory(getPackageName()));
    }
    else if (SmartSplashScreenActivity.onRetrieveBusinessObjectsCustomOver == true)
    {
      // A previous activity instance has already completed the business objects retrieval, but the current instance was not active at this time
      sendBroadcast(new Intent(SmartSplashScreenActivity.BUSINESS_OBJECTS_LOADED_ACTION).addCategory(getPackageName()));
    }
  }

  /**
   * Redirects to the initial activity when a redirection is ongoing. The activity finishes anyway if has not been {@link #stopActivity stopped} nor
   * {@link #pauseActivity paused}.
   */
  public void onFulfillDisplayObjects()
  {
    if (stopActivity == true)
    {
      return;
    }
  }

  public void onSynchronizeDisplayObjects()
  {
    if (stopActivity == true)
    {
      return;
    }
  }

  private boolean canWriteOnExternalStorage()
  {
    // We test whether the SD card is available
    return Environment.getExternalStorageDirectory().canWrite() == true;
  }

  private void finishActivity()
  {
    if (pauseActivity == true)
    {
      return;
    }
    if (isFinishing() == false && hasStopped == false)
    {
      if (log.isDebugEnabled())
      {
        log.debug("Starting the activity which follows the splash screen");
      }
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
      stopActivity = false;
      pauseActivity = false;
      if (log.isDebugEnabled())
      {
        log.debug("Finishing the splash screen");
      }
      finish();
    }
    else
    {
      if (log.isDebugEnabled())
      {
        log.debug("Gives up the starting the activity which follows the splash screen, because the splash screen is finishing or has been stopped");
      }
    }
  }

  /**
   * Is invoked when the splash screen is bound to complete successfully, and that it has been started with a
   * {@link ActivityController#CALLING_INTENT calling intent}. Should start the {@link Activity} calling intent.
   */
  protected void startCallingIntent()
  {
    final Intent callingIntent = ActivityController.extractCallingIntent(this);
    if (log.isDebugEnabled())
    {
      log.debug("Redirecting to the initial activity for the component with class '" + callingIntent.getComponent().getClassName() + "'");
    }
    // This is essential, in order for the activity to be displayed
    callingIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    try
    {
      startActivity(callingIntent);
    }
    catch (Throwable throwable)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot start the Activity with Intent '" + callingIntent + "'", throwable);
      }
    }
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

}
