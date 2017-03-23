package com.smartnsoft.droid4sample;

import android.app.Activity;
import android.view.LayoutInflater;

import com.smartnsoft.droid4me.LifeCycle.BusinessObjectsRetrievalAsynchronousPolicyAnnotation;
import com.smartnsoft.droid4me.app.SmartSplashScreenActivity;

/**
 * The first activity displayed while the application is loading.
 *
 * @author Édouard Mercier
 * @since 2011.10.19
 */
@BusinessObjectsRetrievalAsynchronousPolicyAnnotation
public final class Droid4SampleSplashScreenActivity
    extends SmartSplashScreenActivity<TitleBar.TitleBarAggregate, Void>
    implements TitleBar.TitleBarDiscarded
{

  @Override
  protected Class<? extends Activity> getNextActivity()
  {
    return AboutActivity.class;
  }

  @Override
  protected void onRetrieveDisplayObjectsCustom()
  {
    setContentView(LayoutInflater.from(this).inflate(R.layout.droid4sample_splash_screen, null));
    setProgressBarIndeterminateVisibility(true);
  }

  @Override
  protected Void onRetrieveBusinessObjectsCustom()
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
    return null;
  }

}
