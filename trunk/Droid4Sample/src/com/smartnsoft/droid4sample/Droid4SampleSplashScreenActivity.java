package com.smartnsoft.droid4sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.app.SmartSplashScreenActivity;

/**
 * The first activity displayed while the application is loading.
 * 
 * @author Édouard Mercier
 * @since 2011.10.19
 */
public final class Droid4SampleSplashScreenActivity
    extends SmartSplashScreenActivity
    implements LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy, TitleBar.TitleBarDiscarded
{

  private final static int MISSING_SD_CARD_DIALOG_ID = 0;

  @Override
  protected Dialog onCreateDialog(int id)
  {
    if (id == Droid4SampleSplashScreenActivity.MISSING_SD_CARD_DIALOG_ID)
    {
      return new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.applicationName).setMessage(
          R.string.Droid4SampleSplashScreen_dialogMessage_noSdCard).setPositiveButton(android.R.string.ok, new OnClickListener()
      {
        public void onClick(DialogInterface dialog, int which)
        {
          finish();
        }
      }).create();
    }
    return super.onCreateDialog(id);
  }

  @Override
  protected boolean requiresExternalStorage()
  {
    return true;
  }

  @Override
  protected void onNoExternalStorage()
  {
    showDialog(Droid4SampleSplashScreenActivity.MISSING_SD_CARD_DIALOG_ID);
  }

  @Override
  protected Class<? extends Activity> getNextActivity()
  {
    return MainActivity.class;
  }

  @Override
  protected void onRetrieveDisplayObjectsCustom()
  {
    setContentView(LayoutInflater.from(this).inflate(R.layout.droid4sample_splash_screen, null));
    setProgressBarIndeterminateVisibility(true);
  }

  @Override
  protected void onRetrieveBusinessObjectsCustom()
      throws BusinessObjectUnavailableException
  {
    try
    {
      Thread.sleep(15000);
    }
    catch (InterruptedException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("An interruption occurred while displaying the splash screen", exception);
      }
    }
    markAsInitialized();
  }

}
