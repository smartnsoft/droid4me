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

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.Source;

/**
 * An abstract class which implements its interface via web service calls.
 *
 * @since 2009.06.18
 */
public abstract class WSUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType extends Exception>
    implements Business.UriStreamParser<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType>, Business.UriInputStreamer<WSUriStreamParser.KeysAggregator<ParameterType>, WebServiceCaller.CallException>
{

  /**
   * Is responsible for returning a key corresponding to a business object entity, in order to be able to access to it later on.
   *
   * @param <UriType>       the kind of URI that this source will deliver, so as to locate the business object
   * @param <ParameterType> the kind of parameters that will be used to identify and generate the business object URI
   * @since 2011.10.06
   */
  public interface SourceKey<UriType, ParameterType>
      extends Business.Urier<UriType, ParameterType>
  {

  }

  /**
   * A {@link WSUriStreamParser.SourceKey} aggregator, which redirects the computing of a business object entity URI depending on the
   * {@link Business.Source} it is asked for.
   *
   * @param <ParameterType> the parameter class which identify the business object, and enable to compute its URIs
   * @since 2011.10.06
   */
  public static class KeysAggregator<ParameterType>
  {

    private final ParameterType parameter;

    private final Map<Business.Source, WSUriStreamParser.SourceKey<?, ParameterType>> sourceLocators = new HashMap<Business.Source, WSUriStreamParser.SourceKey<?, ParameterType>>();

    /**
     * This constructor is equivalent to invoking {@link WSUriStreamParser.KeysAggregator#KeysAggregator(Object, Source, SourceKey)} with the second and the third parameters set to {@code null}.
     */
    public KeysAggregator(ParameterType parameter)
    {
      this(parameter, null, null);
    }

    /**
     * Creates a keys aggregator and adds immediately a source key to it.
     * <p/>
     * <p>
     * That constructor will invoke the {@link #add(Business.Source, SourceKey)} method.
     * </p>
     *
     * @param parameter the parameter that will be stored, so as to be able to generate the various business object URIs
     * @param source    the source the source key is attached to
     * @param sourceKey the source key to register
     */
    public KeysAggregator(ParameterType parameter, Business.Source source,
        WSUriStreamParser.SourceKey<?, ParameterType> sourceKey)
    {
      this.parameter = parameter;
      if (sourceKey != null && source != null)
      {
        add(source, sourceKey);
      }
    }

    /**
     * @return the parameter which has been provided at construction time, and which will be used so as to compute the URIs.
     */
    public ParameterType getParameter()
    {
      return parameter;
    }

    /**
     * Registers a source key to the aggregator, for a given source.
     * <p/>
     * <p>
     * Note that invoking the method multiple times with the same <coce>source</code> parameter value will just replace the existing source key.
     * </p>
     *
     * @param source    the source the provided key belongs to
     * @param sourceKey a source key that will be registered for the given source
     * @return the aggregator itself, so as to ease the usage
     */
    public WSUriStreamParser.KeysAggregator<ParameterType> add(Business.Source source,
        WSUriStreamParser.SourceKey<?, ParameterType> sourceKey)
    {
      sourceLocators.put(source, sourceKey);
      return this;
    }

    @SuppressWarnings("unchecked")
    public <SourceKeyType extends WSUriStreamParser.SourceKey<?, ParameterType>> SourceKeyType getSourceLocator(
        Business.Source source)
    {
      final SourceKey<?, ParameterType> sourceKey = sourceLocators.get(source);
      return (SourceKeyType) sourceKey;
    }

  }

  /**
   * A source key which is able to define a specific URI for a business entity when it is searched on an {@link Business.Source#UriStreamer} source,
   * i.e. through a {#link {@link Business.UriStreamParser}.
   * <p/>
   * <p>
   * Indicates the type of HTTP request and the underlying HTTP body and URL.
   * </p>
   *
   * @param <ParameterType> the kind of parameters that will be used to identify and generate the business object URI
   * @since 2011.10.06
   */
  public interface UriStreamerSourceKey<ParameterType>
      extends WSUriStreamParser.SourceKey<WebServiceClient.HttpCallTypeAndBody, ParameterType>
  {

  }

  /**
   * A basic implementation of the {@link WSUriStreamParser.UriStreamerSourceKey}.
   *
   * @param <ParameterType> the kind of parameters that will be used to identify and generate the business object URI
   * @since 2011.10.06
   */
  public static class SimpleUriStreamerSourceKey<ParameterType>
      implements WSUriStreamParser.UriStreamerSourceKey<ParameterType>
  {

    /**
     * The actual URL of the HTTP request to execute.
     */
    public final WebServiceClient.HttpCallTypeAndBody httpCallTypeAndBody;

    public SimpleUriStreamerSourceKey(WebServiceClient.HttpCallTypeAndBody httpCallTypeAndBody)
    {
      this.httpCallTypeAndBody = httpCallTypeAndBody;
    }

    public WebServiceClient.HttpCallTypeAndBody computeUri(ParameterType parameter)
    {
      return httpCallTypeAndBody;
    }

    @Override
    public String toString()
    {
      return "UriSourceKey(" + httpCallTypeAndBody.toString() + ")";
    }

  }

  private final WebServiceClient webServiceClient;

  public WSUriStreamParser(WebServiceClient webServiceClient)
  {
    this.webServiceClient = webServiceClient;
  }

  /**
   * A helper method which just wraps the {@link WebServiceCaller#computeUri(String, String, Map)} method.
   */
  protected String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
  {
    return webServiceClient.computeUri(methodUriPrefix, methodUriSuffix, uriParameters);
  }

  // TODO: think on how to set that back
  // protected WSUriStreamParser.KeysAggregator<ParameterType> computeKeysAggregator(ParameterType parameter,
  // WSUriStreamParser.HttpCallTypeAndBody httpCallTypeAndBody)
  // {
  // return new WSUriStreamParser.KeysAggregator<ParameterType>(parameter).add(Source.UriStreamer, computeUriStreamerSourceKey(httpCallTypeAndBody));
  // }

  // TODO: think on how to set that back
  // protected WSUriStreamParser.UriStreamerSourceKey<ParameterType> computeUriStreamerSourceKey(WSUriStreamParser.HttpCallTypeAndBody
  // httpCallTypeAndBody)
  // {
  // return new WSUriStreamParser.SimpleUriStreamerSourceKey<ParameterType>(httpCallTypeAndBody);
  // }

  public final Business.InputAtom getInputStream(WSUriStreamParser.KeysAggregator<ParameterType> uri)
      throws WebServiceCaller.CallException
  {
    final UriStreamerSourceKey<ParameterType> sourceLocator = uri.getSourceLocator(Business.Source.UriStreamer);
    final WebServiceClient.HttpCallTypeAndBody httpCallTypeAndBody = sourceLocator.computeUri(uri.getParameter());
    return new Business.InputAtom(new Date(), webServiceClient.getInputStream(httpCallTypeAndBody.url, httpCallTypeAndBody.callType, httpCallTypeAndBody.parameters, httpCallTypeAndBody.body));
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
