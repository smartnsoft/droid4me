package com.smartnsoft.droid4sample.ws;

import com.smartnsoft.droid4sample.Constants;

import com.smartnsoft.droid4me.ws.WebServiceCaller;

/**
 * A single point of access to the web services.
 * 
 * @author Ãdouard Mercier
 * @since 2011.10.19
 */
public final class Droid4SampleServices
    extends WebServiceCaller
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
  }

  @Override
  protected String getUrlEncoding()
  {
    return Constants.WEBSERVICES_HTML_ENCODING;
  }

}
