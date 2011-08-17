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

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import com.smartnsoft.droid4me.bo.Business;

/**
 * @author Édouard Mercier
 * @since 2009.11.19
 */
public final class WithCacheWSUriStreamParser
{

  /**
   * @since 2009.11.19
   */
  public static abstract class CacheableWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WSUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType>
      implements
      Business.Cacheable<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>
  {

    private final Business.IOStreamer<String, StreamerExceptionType> ioStreamer;

    public CacheableWebUriStreamParser(Business.IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceClient webServiceClient)
    {
      super(webServiceClient);
      this.ioStreamer = ioStreamer;
    }

    public final Date getLastUpdate(WSUriStreamParser.UrlWithCallTypeAndBody uri)
    {
      return ioStreamer.getLastUpdate(uri.url);
    }

    public final Business.InputAtom readInputStream(WSUriStreamParser.UrlWithCallTypeAndBody uri)
        throws StreamerExceptionType
    {
      return ioStreamer.readInputStream(uri.url);
    }

    public final InputStream writeInputStream(WSUriStreamParser.UrlWithCallTypeAndBody uri, Business.InputAtom inputAtom)
        throws StreamerExceptionType
    {
      return ioStreamer.writeInputStream(uri.url, inputAtom);
    }

    public void remove(WSUriStreamParser.UrlWithCallTypeAndBody uri)
        throws StreamerExceptionType
    {
      ioStreamer.remove(uri.url);
    }

  }

  public static abstract class CachedWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends
      Business.Cached<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>
  {

    private final WebServiceClient webServiceClient;

    public CachedWebUriStreamParser(Business.IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceClient webServiceClient)
    {
      super(ioStreamer);
      this.webServiceClient = webServiceClient;
    }

    protected final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
    {
      return webServiceClient.computeUri(methodUriPrefix, methodUriSuffix, uriParameters);
    }

    public Business.InputAtom getInputStream(WSUriStreamParser.UrlWithCallTypeAndBody uri)
        throws WebServiceClient.CallException
    {
      return new Business.InputAtom(new Date(), webServiceClient.getInputStream(uri.url, uri.callType, uri.body));
    }

    public final BusinessObjectType rawGetValue(ParameterType parameter)
        throws ParseExceptionType, WebServiceClient.CallException, Business.BusinessException
    {
      return parse(parameter, getInputStream(computeUri(parameter)).inputStream);
    }

    public final BusinessObjectType getValue(ParameterType parameter)
        throws WebServiceClient.CallException
    {
      try
      {
        return parse(parameter, getInputStream(computeUri(parameter)).inputStream);
      }
      catch (Exception exception)
      {
        if (exception instanceof WebServiceClient.CallException)
        {
          throw (WebServiceClient.CallException) exception;
        }
        throw new WebServiceClient.CallException(exception);
      }
    }

    public final Date getLastUpdate(WSUriStreamParser.UrlWithCallTypeAndBody uri)
    {
      return getLastUpdate(uri.url);
    }

    public final Business.InputAtom readInputStream(WSUriStreamParser.UrlWithCallTypeAndBody uri)
        throws StreamerExceptionType
    {
      return readInputStream(uri.url);
    }

    public final InputStream writeInputStream(WSUriStreamParser.UrlWithCallTypeAndBody uri, Business.InputAtom inputAtom)
        throws StreamerExceptionType
    {
      return writeInputStream(uri.url, inputAtom);
    }

    public void remove(WSUriStreamParser.UrlWithCallTypeAndBody uri)
        throws StreamerExceptionType
    {
      removeUri(uri.url);
    }

  }

}
