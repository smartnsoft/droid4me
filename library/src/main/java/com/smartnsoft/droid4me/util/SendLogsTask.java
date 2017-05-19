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

package com.smartnsoft.droid4me.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * An asynchronous task which enables to send the logs of the application, along with the device details.
 * <p>
 * <p>
 * Caution: the <code>android.permission.READ_LOGS</code> Android permission must be set in the <code>AnroidManifest.xml</code> file, when using that
 * task.
 * </p>
 *
 * @author Édouard Mercier
 * @since 2010.10.21
 */
public final class SendLogsTask
    extends AsyncTask<Void, Void, StringBuilder>
{

  private final static Logger log = LoggerFactory.getInstance(SendLogsTask.class);

  public static int MAX_LOG_LENGTH_IN_BYTES = 50 * 1024;

  private final Activity activity;

  private final String progressMessage;

  private final String emailSubjectFormat;

  private final String emailRecipient;

  private final String lineSeparator = System.getProperty("line.separator");

  private ProgressDialog progressDialog;

  public SendLogsTask(Activity activity, String progressMessage, String emailSubjectFormat, String emailRecipient)
  {
    this.activity = activity;
    this.progressMessage = progressMessage;
    this.emailSubjectFormat = emailSubjectFormat;
    this.emailRecipient = emailRecipient;
  }

  @Override
  protected void onPreExecute()
  {
    progressDialog = new ProgressDialog(activity);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage(progressMessage);
    progressDialog.setCancelable(false);
    progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
    {
      public void onCancel(DialogInterface dialog)
      {
        activity.finish();
      }
    });
    progressDialog.show();
  }

  @Override
  protected StringBuilder doInBackground(Void... params)
  {
    final StringBuilder sb = new StringBuilder();
    try
    {
      final Process process = Runtime.getRuntime().exec(new String[] { "logcat", "-d", "-v", "time" });
      final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = bufferedReader.readLine()) != null)
      {
        sb.append(line);
        sb.append(lineSeparator);
      }
    }
    catch (IOException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("Cannot extract the device logcat!", exception);
      }
    }
    return sb;
  }

  @Override
  protected void onPostExecute(StringBuilder logcatSb)
  {
    if (logcatSb != null)
    {
      // We truncate the log if necessary
      final int keepOffset = Math.max(logcatSb.length() - SendLogsTask.MAX_LOG_LENGTH_IN_BYTES, 0);
      if (keepOffset > 0)
      {
        logcatSb.delete(0, keepOffset);
      }

      String version;
      try
      {
        final PackageInfo packagInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
        version = packagInfo.versionName;
      }
      catch (PackageManager.NameNotFoundException exception)
      {
        if (log.isErrorEnabled())
        {
          log.error("Cannot extract the current application packages information", exception);
        }
        version = "?";
      }
      final Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("message/rfc822");
      intent.putExtra(Intent.EXTRA_EMAIL, new String[] { emailRecipient });
      intent.putExtra(Intent.EXTRA_SUBJECT, String.format(emailSubjectFormat, version));
      final StringBuffer sb = new StringBuffer();
      sb.append("Device model: ").append(Build.MODEL).append(lineSeparator);
      sb.append("Firmware version: ").append(Build.VERSION.RELEASE).append(lineSeparator);
      sb.append("Build number: ").append(Build.DISPLAY).append(lineSeparator);
      sb.append("----------").append(lineSeparator).append(logcatSb);
      intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
      // We start the activity that will eventually send the log
      try
      {
        activity.startActivity(intent);
      }
      catch (ActivityNotFoundException exception)
      {
        if (log.isWarnEnabled())
        {
          // TODO: issue a toast!
          log.warn("Cannot send the report, because no application is able to handle it!", exception);
        }
      }
      dismissProgressDialog();
      activity.finish();
    }
    else
    {
      dismissProgressDialog();
    }
  }

  private void dismissProgressDialog()
  {
    if (progressDialog != null && progressDialog.isShowing())
    {
      progressDialog.dismiss();
      progressDialog = null;
    }
  }

}
