package com.smartnsoft.droid4sample;

import org.apache.http.protocol.HTTP;

import android.util.Log;

/**
 * Gathers in one place the constants of the application.
 * 
 * @author Ãdouard Mercier
 * @since 2011.10.19
 */
public abstract class Constants
{

  /**
   * The logging level of the application and of the droid4me framework.
   */
  public static final int LOG_LEVEL = Log.DEBUG;

  /**
   * The e-mail that will receive error reports.
   */
  public static final String REPORT_LOG_RECIPIENT_EMAIL = "android@smartnsoft.com";

  /**
   * The encoding used for wrapping the URL of the HTTP requests.
   */
  public static final String WEBSERVICES_HTML_ENCODING = HTTP.ISO_8859_1;

}
