// The MIT License (MIT)
//
// Copyright (c) 2017 Smart&Soft
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

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
    super(5000, 5000, true);
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
