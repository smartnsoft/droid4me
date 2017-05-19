package com.smartnsoft.droid4sample.ws;

import com.smartnsoft.droid4me.ws.URLConnectionWebServiceCaller;

import com.smartnsoft.droid4sample.Constants;

/**
 * A single point of access to the web services.
 *
 * @author Édouard Mercier
 * @since 2011.10.19
 */
public final class Droid4SampleServices
    extends URLConnectionWebServiceCaller
{

  private static volatile Droid4SampleServices instance;

  // We accept the "out-of-order writes" case
  public static Droid4SampleServices getInstance()
  {
    if (instance == null)
    {
      synchronized (Droid4SampleServices.class)
      {
        if (instance == null)
        {
          instance = new Droid4SampleServices();
        }
      }
    }
    return instance;
  }

  private Droid4SampleServices()
  {
    super(readTimeOutInMilliseconds, connectTimeOutInMilliseconds, acceptGzip);
  }

  @Override
  protected int getReadTimeout()
  {
    return Constants.HTTP_CONNECTION_TIMEOUT_IN_MILLISECONDS;
  }

  @Override
  protected int getConnectTimeout()
  {
    return Constants.HTTP_SOCKET_TIMEOUT_IN_MILLISECONDS;
  }

  @Override
  protected String getContentEncoding()
  {
    return Constants.WEBSERVICES_CONTENT_ENCODING;
  }

  @Override
  protected String getUrlEncoding()
  {
    return Constants.WEBSERVICES_HTML_ENCODING;
  }
}
