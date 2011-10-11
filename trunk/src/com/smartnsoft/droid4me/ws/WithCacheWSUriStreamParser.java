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
import com.smartnsoft.droid4me.ws.WSUriStreamParser.HttpCallTypeAndBody;
import com.smartnsoft.droid4me.ws.WSUriStreamParser.UriStreamerSourceKey;

/**
 * @author Édouard Mercier
 * @since 2009.11.19
 */
public final class WithCacheWSUriStreamParser
{

  /**
   * A source key which is able to define a specific URI for a business entity when it is searched on an {@link Business.Source#IOStreamer} source,
   * i.e. through a {#link {@link Business.IOStreamer}.
   * 
   * @param <ParameterType>
   *          the kind of parameters that will be used to identify and generate the business object URI
   * 
   * @since 2011.10.06
   */
  public static interface IOStreamerSourceKey<ParameterType>
      extends WSUriStreamParser.SourceKey<String, ParameterType>
  {

  }

  /**
   * A basic implementation of the {@link WithCacheWSUriStreamParser.IOStreamerSourceKey}.
   * 
   * @param <ParameterType>
   *          the kind of parameters that will be used to identify and generate the business object URI
   * 
   * @since 2011.10.06
   */
  public static class SimpleIOStreamerSourceKey<ParameterType>
      implements WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType>
  {

    /**
     * The URI to use to locate the business object entity when it needs to be located on a {@link Business.IOStreamer}.
     */
    public final String ioStreamerUri;

    /**
     * @param uriStreamerUri
     *          the business object entity {@link Business.UriStreamParser} URI
     * @param ioStreamerUri
     *          the business object entity {@link Business.IOStreamer} URI
     */
    public SimpleIOStreamerSourceKey(String ioStreamerUri)
    {
      this.ioStreamerUri = ioStreamerUri;
    }

    public String computeUri(ParameterType parameter)
    {
      return ioStreamerUri;
    }

    @Override
    public String toString()
    {
      return "IOSourceKey(" + ioStreamerUri.toString() + ")";
    }

  }

  /**
   * @since 2009.11.19
   */
  public static abstract class CacheableWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WSUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType>
      implements
      Business.Cacheable<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>
  {

    private final Business.IOStreamer<String, StreamerExceptionType> ioStreamer;

    public CacheableWebUriStreamParser(Business.IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceClient webServiceClient)
    {
      super(webServiceClient);
      this.ioStreamer = ioStreamer;
    }

    // TODO: think on how to set that back
    // protected WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> computeIOStreamerSourceKey(String uri)
    // {
    // return new WithCacheWSUriStreamParser.SimpleIOStreamerSourceKey<ParameterType>(uri);
    // }

    public final Date getLastUpdate(WSUriStreamParser.KeysAggregator<ParameterType> uri)
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return ioStreamer.getLastUpdate(ioSourceKey.computeUri(uri.getParameter()));
    }

    public final Business.InputAtom readInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return ioStreamer.readInputStream(ioSourceKey.computeUri(uri.getParameter()));
    }

    public final InputStream writeInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri, Business.InputAtom inputAtom)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return ioStreamer.writeInputStream(ioSourceKey.computeUri(uri.getParameter()), inputAtom);
    }

    public void remove(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      ioStreamer.remove(ioSourceKey.computeUri(uri.getParameter()));
    }

  }

  public static abstract class CachedWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends
      Business.Cached<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>
  {

    private final WebServiceClient webServiceClient;

    public CachedWebUriStreamParser(Business.IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceClient webServiceClient)
    {
      super(ioStreamer);
      this.webServiceClient = webServiceClient;
    }

    protected String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
    {
      return webServiceClient.computeUri(methodUriPrefix, methodUriSuffix, uriParameters);
    }

    public Business.InputAtom getInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws WebServiceClient.CallException
    {
      final UriStreamerSourceKey<ParameterType> sourceLocator = uri.getSourceLocator(Business.Source.UriStreamer);
      final HttpCallTypeAndBody httpCallTypeAndBody = sourceLocator.computeUri(uri.getParameter());
      return new Business.InputAtom(new Date(), webServiceClient.getInputStream(httpCallTypeAndBody.url, httpCallTypeAndBody.callType, httpCallTypeAndBody.body));
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

    public final Date getLastUpdate(WSUriStreamParser.KeysAggregator<ParameterType> uri)
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return getLastUpdate(ioSourceKey.computeUri(uri.getParameter()));
    }

    public final Business.InputAtom readInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return readInputStream(ioSourceKey.computeUri(uri.getParameter()));
    }

    public final InputStream writeInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri, Business.InputAtom inputAtom)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return writeInputStream(ioSourceKey.computeUri(uri.getParameter()), inputAtom);
    }

    public void remove(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      removeUri(ioSourceKey.computeUri(uri.getParameter()));
    }

  }

}
