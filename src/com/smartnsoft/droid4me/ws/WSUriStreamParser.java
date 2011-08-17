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

package com.smartnsoft.droid4me.ws;

import java.util.Date;

import org.apache.http.entity.AbstractHttpEntity;

import com.smartnsoft.droid4me.bo.Business;

/**
 * An abstract class which implements its interface via web service calls.
 * 
 * @since 2009.06.18
 */
public abstract class WSUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception>
    implements Business.UriStreamParser<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType>,
    Business.UriInputStreamer<WSUriStreamParser.UrlWithCallTypeAndBody, WebServiceCaller.CallException>
{

  /**
   * Indicates the type of HTTP request and the underlying HTTP.
   * 
   * @since 2009.11.10
   */
  public final static class UrlWithCallTypeAndBody
  {

    public final String url;

    public final WebServiceCaller.CallType callType;

    public final AbstractHttpEntity body;

    public UrlWithCallTypeAndBody(String url)
    {
      this(url, WebServiceCaller.CallType.Get, null);
    }

    public UrlWithCallTypeAndBody(String url, WebServiceCaller.CallType callType, AbstractHttpEntity body)
    {
      this.url = url;
      this.callType = callType;
      this.body = body;
    }

    @Override
    public String toString()
    {
      return "(" + callType + ") " + url;
    }

  }

  private final WebServiceClient webServiceClient;

  public WSUriStreamParser(WebServiceClient webServiceClient)
  {
    this.webServiceClient = webServiceClient;
  }

//  protected final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
//  {
//    return webServiceCaller.computeUri(methodUriPrefix, methodUriSuffix, uriParameters);
//  }

  public final Business.InputAtom getInputStream(WSUriStreamParser.UrlWithCallTypeAndBody uri)
      throws WebServiceCaller.CallException
  {
    return new Business.InputAtom(new Date(), webServiceClient.getInputStream(uri.url, uri.callType, uri.body));
  }

  public final BusinessObjectType rawGetValue(ParameterType parameter)
      throws ParseExceptionType, WebServiceCaller.CallException
  {
    return parse(parameter, getInputStream(computeUri(parameter)).inputStream);
  }

  public final BusinessObjectType getValue(ParameterType parameter)
      throws WebServiceCaller.CallException
  {
    try
    {
      return parse(parameter, getInputStream(computeUri(parameter)).inputStream);
    }
    catch (Exception exception)
    {
      if (exception instanceof WebServiceCaller.CallException)
      {
        throw (WebServiceCaller.CallException) exception;
      }
      throw new WebServiceCaller.CallException(exception);
    }
  }

}
