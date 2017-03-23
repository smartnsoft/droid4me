package com.smartnsoft.droid4sample;

import android.util.Log;
import android.util.Xml.Encoding;

/**
 * Gathers in one place the constants of the application.
 *
 * @author Édouard Mercier
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
   * The encoding used for decoding the contents of HTTP requests.
   */
  public static final String WEBSERVICES_CONTENT_ENCODING = Encoding.UTF_8.toString();

  /**
   * The encoding used for wrapping the URL of the HTTP requests.
   */
  public static final String WEBSERVICES_HTML_ENCODING = Encoding.ISO_8859_1.toString();

  // The HTTP requests server side response time-out
  public static final int HTTP_CONNECTION_TIMEOUT_IN_MILLISECONDS = 5000;

  // The HTTP requests socket connection time-out
  public static final int HTTP_SOCKET_TIMEOUT_IN_MILLISECONDS = 5000;

}
