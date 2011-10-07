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
import java.util.HashMap;
import java.util.Map;

import org.apache.http.entity.AbstractHttpEntity;

import com.smartnsoft.droid4me.bo.Business;

/**
 * An abstract class which implements its interface via web service calls.
 * 
 * @since 2009.06.18
 */
public abstract class WSUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception>
    implements Business.UriStreamParser<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType>,
    Business.UriInputStreamer<WSUriStreamParser.KeysAggregator<ParameterType>, WebServiceCaller.CallException>
{

  /**
   * Is responsible for returning a string key corresponding to a business object entity, in order to access to it.
   * 
   * @since 2011.10.06
   * 
   */
  public static interface SourceKey
  {

    /**
     * The method is responsible for returning the key of a business object entity.
     * 
     * @param parameter
     *          the parameters which enable to compute the key, and which are specific to the business object
     * @return may be <code>null</code> if no key is available for the underlying business object
     */
    <UriType, ParameterType> UriType getUri(ParameterType parameter);

  }

  /**
   * A {@link WSUriStreamParser.SourceKey} aggregator, which redirects the computing of a business object entity URI depending on the
   * {@link Business.Source} it is asked for.
   * 
   * @param <ParameterType>
   *          the parameter class which identify the business object, and enable to compute its URIs
   * 
   * @since 2009.11.10
   */
  public static class KeysAggregator<ParameterType>
  {

    private final ParameterType parameter;

    private final Map<Business.Source, WSUriStreamParser.SourceKey> sourceLocators = new HashMap<Business.Source, WSUriStreamParser.SourceKey>();

    public KeysAggregator(ParameterType parameter)
    {
      this(parameter, null, null);
    }

    public KeysAggregator(ParameterType parameter, WSUriStreamParser.SourceKey sourceLocator)
    {
      this(parameter, Business.Source.URIStreamer, sourceLocator);
    }

    public KeysAggregator(ParameterType parameter, Business.Source source, WSUriStreamParser.SourceKey sourceLocator)
    {
      this.parameter = parameter;
      if (sourceLocator != null && source != null)
      {
        add(source, sourceLocator);
      }
    }

    public WSUriStreamParser.KeysAggregator<ParameterType> add(Business.Source source, WSUriStreamParser.SourceKey sourceLocator)
    {
      sourceLocators.put(source, sourceLocator);
      return this;
    }

    @SuppressWarnings("unchecked")
    public <SourceKeyType extends WSUriStreamParser.SourceKey> SourceKeyType getSourceLocator(Business.Source source)
    {
      return (SourceKeyType) sourceLocators.get(source);
    }

    public String computeUri(Business.Source source)
    {
      final WSUriStreamParser.SourceKey sourceLocator = sourceLocators.get(source);
      if (sourceLocator == null)
      {
        // We fall back to the URIStreamer source locator
        final WSUriStreamParser.SourceKey uriSourceLocator = sourceLocators.get(Business.Source.URIStreamer);
        if (uriSourceLocator == null)
        {
          return null;
        }
        else
        {
          final String uri = uriSourceLocator.getUri(parameter);
          return uri;
        }
      }
      else
      {
        return sourceLocator.getUri(parameter);
      }
    }

  }

  /**
   * A source key which is able to define a specific URI for a business entity when it is searched on an {@link Business.Source#URIStreamer} source,
   * i.e. through a {#link {@link Business.UriStreamParser}.
   * 
   * <p>
   * Indicates the type of HTTP request and the underlying HTTP body and URL.
   * </p>
   * 
   * @since 2009.11.10
   */
  public static class URISourceKey
      implements WSUriStreamParser.SourceKey
  {

    /**
     * The actual URL of the HTTP request to execute.
     */
    public final String url;

    /**
     * The HTTP request method.
     */
    public final WebServiceCaller.CallType callType;

    /**
     * If the HTTP method is a {@link WebServiceClient.Verb#Post} or a {@link WebServiceClient.Verb#Put}, the body of the request.
     */
    public final AbstractHttpEntity body;

    /**
     * This will create a {@link WebServiceClient.Verb#Get} HTTP request method.
     * 
     * @param url
     *          the URL to use when performing the HTTP request
     */
    public URISourceKey(String url)
    {
      this(url, WebServiceCaller.CallType.Get, null);
    }

    /**
     * @param url
     *          the URL to use when performing the HTTP request
     * @param callType
     *          the HTTP request method
     * @param body
     *          the HTTP request body, if the 'callType" is a {@link WebServiceClient.Verb#Post POST} or a {@link WebServiceClient.Verb#Put PUT}
     */
    public URISourceKey(String url, WebServiceCaller.CallType callType, AbstractHttpEntity body)
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

    @SuppressWarnings("unchecked")
    public <UriType, ParameterType> UriType getUri(ParameterType parameter)
    {
      return (UriType) url;
    }

  }

  private final WebServiceClient webServiceClient;

  public WSUriStreamParser(WebServiceClient webServiceClient)
  {
    this.webServiceClient = webServiceClient;
  }

  protected final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
  {
    return webServiceClient.computeUri(methodUriPrefix, methodUriSuffix, uriParameters);
  }

  public final Business.InputAtom getInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri)
      throws WebServiceCaller.CallException
  {
    final URISourceKey sourceLocator = uri.getSourceLocator(Business.Source.URIStreamer);
    return new Business.InputAtom(new Date(), webServiceClient.getInputStream((String) sourceLocator.getUri(Business.Source.IOStreamer),
        sourceLocator.callType, sourceLocator.body));
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
