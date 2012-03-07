package com.smartnsoft.droid4sample;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;

import com.smartnsoft.droid4me.app.ActivityController;
import com.smartnsoft.droid4me.app.SmartApplication;
import com.smartnsoft.droid4me.bo.Business.InputAtom;
import com.smartnsoft.droid4me.cache.DbPersistence;
import com.smartnsoft.droid4me.cache.Persistence;
import com.smartnsoft.droid4me.download.BasisDownloadInstructions;
import com.smartnsoft.droid4me.download.BitmapDownloader;
import com.smartnsoft.droid4me.download.DownloadInstructions;

/**
 * The entry point of the application.
 * 
 * @author Édouard Mercier
 * @since 2011.10.19
 */
public final class Droid4SampleApplication
    extends SmartApplication
{

  public static class CacheInstructions
      extends DownloadInstructions.AbstractInstructions
  {

    @Override
    public InputStream getInputStream(String imageUid, Object imageSpecs, String url, BasisDownloadInstructions.InputStreamDownloadInstructor downloadInstructor)
        throws IOException
    {
      final InputAtom inputAtom = Persistence.getInstance(1).extractInputStream(url);
      return inputAtom == null ? null : inputAtom.inputStream;
    }

    @Override
    public InputStream onInputStreamDownloaded(String imageUid, Object imageSpecs, String url, InputStream inputStream)
    {
      final InputAtom inputAtom = Persistence.getInstance(1).flushInputStream(url, new InputAtom(new Date(), inputStream));
      return inputAtom == null ? null : inputAtom.inputStream;
    }

  }

  public final static DownloadInstructions.Instructions CACHE_IMAGE_INSTRUCTIONS = new Droid4SampleApplication.CacheInstructions();

  @Override
  protected int getLogLevel()
  {
    return Constants.LOG_LEVEL;
  }

  @Override
  protected SmartApplication.I18N getI18N()
  {
    return new SmartApplication.I18N(getText(R.string.problem), getText(R.string.unavailableItem), getText(R.string.unavailableService), getText(R.string.connectivityProblem), getText(R.string.connectivityProblemRetry), getText(R.string.unhandledProblem), getString(R.string.applicationName), getText(R.string.dialogButton_unhandledProblem), getString(R.string.progressDialogMessage_unhandledProblem));
  }

  @Override
  protected String getLogReportRecipient()
  {
    return Constants.REPORT_LOG_RECIPIENT_EMAIL;
  }

  @Override
  public void onCreateCustom()
  {
    super.onCreateCustom();

    // We initialize the persistence
    final String directoryName = getPackageManager().getApplicationLabel(getApplicationInfo()).toString();
    final File contentsDirectory = new File(Environment.getExternalStorageDirectory(), directoryName);
    Persistence.CACHE_DIRECTORY_PATHS = new String[] { contentsDirectory.getAbsolutePath(), contentsDirectory.getAbsolutePath() };
    DbPersistence.FILE_NAMES = new String[] { DbPersistence.DEFAULT_FILE_NAME, DbPersistence.DEFAULT_FILE_NAME };
    DbPersistence.TABLE_NAMES = new String[] { "data", "images" };
    Persistence.INSTANCES_COUNT = 2;
    Persistence.IMPLEMENTATION_FQN = DbPersistence.class.getName();

    // We set the ImageDownloader instances
    BitmapDownloader.IMPLEMENTATION_FQN = BitmapDownloader.class.getName();
    BitmapDownloader.INSTANCES_COUNT = 1;
    BitmapDownloader.MAX_MEMORY_IN_BYTES = new long[] { 3 * 1024 * 1024 };
    BitmapDownloader.LOW_LEVEL_MEMORY_WATER_MARK_IN_BYTES = new long[] { 1 * 1024 * 1024 };
    BitmapDownloader.USE_REFERENCES = new boolean[] { false };
    BitmapDownloader.RECYCLE_BITMAP = new boolean[] { false };
  }

  @Override
  protected ActivityController.Redirector getActivityRedirector()
  {
    return new ActivityController.Redirector()
    {
      public Intent getRedirection(Activity activity)
      {
        if (Droid4SampleSplashScreenActivity.isInitialized(Droid4SampleSplashScreenActivity.class) == null)
        {
          // We re-direct to the splash screen activity if the application has not been yet initialized
          if (activity.getComponentName() == null || activity.getComponentName().getClassName().equals(Droid4SampleSplashScreenActivity.class.getName()) == true)
          {
            return null;
          }
          else
          {
            return new Intent(activity, Droid4SampleSplashScreenActivity.class);
          }
        }
        return null;
      }
    };
  }

  @Override
  protected ActivityController.Interceptor getActivityInterceptor()
  {
    final Intent homeActivityIntent = new Intent(getApplicationContext(), MainActivity.class);
    final TitleBar titleBar = new TitleBar(homeActivityIntent, R.drawable.title_bar_home, R.style.Theme_Droid4Sample);
    return new ActivityController.Interceptor()
    {
      public void onLifeCycleEvent(Activity activity, Object component, ActivityController.Interceptor.InterceptorEvent event)
      {
        titleBar.onLifeCycleEvent(activity, event);
      }
    };
  }

  @Override
  protected ActivityController.ExceptionHandler getExceptionHandler()
  {
    return super.getExceptionHandler();
  }

}
