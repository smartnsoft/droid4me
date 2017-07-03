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

package com.smartnsoft.droid4me.ws;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.InputAtom;
import com.smartnsoft.droid4me.ws.WSUriStreamParser.UriStreamerSourceKey;
import com.smartnsoft.droid4me.ws.WebServiceCaller.HttpResponse;

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
   * @param <ParameterType> the kind of parameters that will be used to identify and generate the business object URI
   * @since 2011.10.06
   */
  public interface IOStreamerSourceKey<ParameterType>
      extends WSUriStreamParser.SourceKey<String, ParameterType>
  {

  }

  /**
   * A basic implementation of the {@link WithCacheWSUriStreamParser.IOStreamerSourceKey}.
   *
   * @param <ParameterType> the kind of parameters that will be used to identify and generate the business object URI
   * @since 2011.10.06
   */
  public static class SimpleIOStreamerSourceKey<ParameterType>
      implements WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType>
  {

    /**
     * A helper method, which enables to build both an {@link Business.Source#UriStreamer} and {@link Business.Source#IOStreamer} keys aggregator from
     * an HTTP request expression.
     *
     * @param httpCallTypeAndBody the HTTP request that will be used to build the {@link WSUriStreamParser.UriStreamerSourceKey}
     * @param parameter           the parameter that will be used to create the returned keys aggregator
     * @return a new keys aggregator composed of a newly created {@link WSUriStreamParser.UriStreamerSourceKey}, and a
     * {@link WithCacheWSUriStreamParser.IOStreamerSourceKey} the URI of which will be taken from the previously created
     * {@link WSUriStreamParser.UriStreamerSourceKey} via its {@link WSUriStreamParser.UriStreamerSourceKey#computeUri(Object)} method
     */
    public static <ParameterType> WSUriStreamParser.KeysAggregator<ParameterType> fromUriStreamerSourceKey(
        WebServiceClient.HttpCallTypeAndBody httpCallTypeAndBody, ParameterType parameter)
    {
      final WSUriStreamParser.SimpleUriStreamerSourceKey<ParameterType> uriStreamerSourceKey = new WSUriStreamParser.SimpleUriStreamerSourceKey<>(httpCallTypeAndBody);
      return new WSUriStreamParser.KeysAggregator<>(parameter).add(Business.Source.UriStreamer, uriStreamerSourceKey).add(Business.Source.IOStreamer, new WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType>()
      {
        public String computeUri(ParameterType parameter)
        {
          return uriStreamerSourceKey.computeUri(parameter).url;
        }
      });
    }

    /**
     * The URI to use to locate the business object entity when it needs to be located on a {@link Business.IOStreamer}.
     */
    public final String ioStreamerUri;

    /**
     * @param ioStreamerUri the business object entity {@link Business.IOStreamer} URI
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
      implements Business.Cacheable<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>
  {

    private final Business.IOStreamer<String, StreamerExceptionType> ioStreamer;

    public CacheableWebUriStreamParser(Business.IOStreamer<String, StreamerExceptionType> ioStreamer,
        WebServiceClient webServiceClient)
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

    public final InputStream writeInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri,
        Business.InputAtom inputAtom, boolean returnStream)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return ioStreamer.writeInputStream(ioSourceKey.computeUri(uri.getParameter()), inputAtom, returnStream);
    }

    public void remove(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      ioStreamer.remove(ioSourceKey.computeUri(uri.getParameter()));
    }

  }

  public static abstract class CachedWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends Business.Cached<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>
  {

    private final WebServiceClient webServiceClient;

    public CachedWebUriStreamParser(Business.IOStreamer<String, StreamerExceptionType> ioStreamer,
        WebServiceClient webServiceClient)
    {
      super(ioStreamer);
      this.webServiceClient = webServiceClient;
    }

    public Business.InputAtom getInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws WebServiceClient.CallException
    {
      final UriStreamerSourceKey<ParameterType> sourceLocator = uri.getSourceLocator(Business.Source.UriStreamer);
      final WebServiceClient.HttpCallTypeAndBody httpCallTypeAndBody = sourceLocator.computeUri(uri.getParameter());
      final HttpResponse httpResponse = webServiceClient.runRequest(httpCallTypeAndBody.url, httpCallTypeAndBody.callType, httpCallTypeAndBody.headers, httpCallTypeAndBody.parameters, httpCallTypeAndBody.body, httpCallTypeAndBody.files);
      return new Business.InputAtom(new Date(), httpResponse.headers, httpResponse.inputStream, null);

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

    public final InputStream writeInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri,
        Business.InputAtom inputAtom, boolean returnStream)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      return writeInputStream(ioSourceKey.computeUri(uri.getParameter()), inputAtom, returnStream);
    }

    public void remove(WSUriStreamParser.KeysAggregator<ParameterType> uri)
        throws StreamerExceptionType
    {
      final WithCacheWSUriStreamParser.IOStreamerSourceKey<ParameterType> ioSourceKey = uri.getSourceLocator(Business.Source.IOStreamer);
      removeUri(ioSourceKey.computeUri(uri.getParameter()));
    }

    public final BusinessObjectType rawGetValue(ParameterType parameter)
        throws ParseExceptionType, WebServiceClient.CallException, Business.BusinessException
    {
      final InputAtom inputAtom = getInputStream(computeUri(parameter));
      return parse(parameter, inputAtom.headers, inputAtom.inputStream);
    }

    public final BusinessObjectType getValue(ParameterType parameter)
        throws WebServiceClient.CallException
    {
      try
      {
        final InputAtom inputStream = getInputStream(computeUri(parameter));
        return parse(parameter, inputStream.headers, inputStream.inputStream);
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

    protected String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
    {
      return webServiceClient.computeUri(methodUriPrefix, methodUriSuffix, uriParameters);
    }

  }

}
