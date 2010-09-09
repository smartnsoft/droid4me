/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.cache;

import java.io.InputStream;
import java.util.Date;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.IOStreamer;
import com.smartnsoft.droid4me.bo.Business.UriStreamParser;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A class which enables to cache business objects.
 * 
 * <p>
 * The class is not thread-safe in the sense that if multiple threads attempts to modify or get the underlying cached object, no atomicity is granted.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2009.06.18
 */
public class Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
{

  public static enum Origin
  {
    Cache, UriStreamParser
  }

  /**
   * Contains additional information regarding the source of the business object.
   * 
   * @since 2009.11.25
   */
  public static final class CachedInfo<BusinessObjectType>
      extends Values.Info<BusinessObjectType>
  {

    public final Cacher.Origin origin;

    public CachedInfo(Values.Info<BusinessObjectType> info, Cacher.Origin origin)
    {
      super(info.value, info.timestamp, origin == Cacher.Origin.Cache ? Values.Info.SOURCE2 : Values.Info.SOURCE3);
      this.origin = origin;
    }

  }

  /**
   * Indicates whether the current cached data should be taken from the cache.
   */
  public static interface Instructions
  {
    
    /**
     * Is invoked in two ways:
     * <ol>
     * <li>a first time with a null argument: the returned value indicates whether the timestamp should be taken into account and hence extracted to
     * determine whether the data should be taken from the cache. If the method returns <code>false</code> at this time, no second call is performed
     * and it is considered that the data should be taken from the cache ;</li>
     * <li>a second time with a non-null argument: the returned value indicates whether the data with this timestamp should be taken from the cache ;</li>
     * </ol>
     * 
     * @param timestamp
     *          <code>null</code> the first time ; the second time, the date corresponding to the last data retrieval
     */
    boolean takeFromCache(Date timestamp);

    /**
     * Invoked when the underlying business object is bound to be fetched from the {@link UriStreamParser}.
     */
    void onFetchingFromUriStreamParser();
    
    
  }

  protected final static Logger log = LoggerFactory.getInstance(Cacher.class);

  protected final Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ParseExceptionType> uriStreamParser;

  private final Business.IOStreamer<UriType, StreamerExceptionType> ioStreamer;

  private final Business.UriInputStreamer<UriType, InputExceptionType> uriInputStreamer;

  public Cacher(Business.Cacheable<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacheable)
  {
    this(cacheable, cacheable, cacheable);
  }

  public Cacher(Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ParseExceptionType> uriStreamParser,
      Business.IOStreamer<UriType, StreamerExceptionType> ioStreamer, Business.UriInputStreamer<UriType, InputExceptionType> uriInputStreamer)
  {
    this.uriStreamParser = uriStreamParser;
    this.ioStreamer = ioStreamer;
    this.uriInputStreamer = uriInputStreamer;
  }

  protected void onNewBusinessObject(UriType uri, Values.Info<BusinessObjectType> info)
  {
  }

  protected Date getCacheLastUpdate(ParameterType parameter, UriType uri)
  {
    return ioStreamer.getLastUpdate(uri);
  }

  protected final UriType computeUri(ParameterType parameter)
  {
    return uriStreamParser.computeUri(parameter);
  }

  public final Cacher.CachedInfo<BusinessObjectType> getValue(Cacher.Instructions instructions, ParameterType parameter)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final UriType uri = uriStreamParser.computeUri(parameter);
    // We first ask whether the timestamp associated with the cached data should be retrieved
    final boolean queryWithTimestamp = instructions.takeFromCache(null);
    final Date lastUpdate;
    if (queryWithTimestamp == true)
    {
      lastUpdate = getCacheLastUpdate(parameter, uri);
    }
    else
    {
      lastUpdate = null;
    }
    if (queryWithTimestamp == false || (lastUpdate != null && instructions.takeFromCache(lastUpdate) == true))
    {
      try
      {
        if (log.isDebugEnabled())
        {
          log.debug("The data corresponding to the URI '" + uri + "' seems available in the cache: reusing it");
        }
        final Values.Info<BusinessObjectType> cachedValue = getCachedValue(parameter);
        if (cachedValue != null)
        {
          return new Cacher.CachedInfo<BusinessObjectType>(cachedValue, Cacher.Origin.Cache);
        }
        if (log.isWarnEnabled())
        {
          log.debug("The data corresponding to the URI '" + uri + "' was eventually not present on the cache: a new request will be attempted again!");
        }
      }
      catch (Exception exception)
      {
        // The persisted data may be corrupted, and in that case, we need to make a new web service call
        if (log.isWarnEnabled())
        {
          log.warn("The persisted data corresponding to the URI '" + uri + "' seems to be corrupted: a request will be attempted", exception);
        }
      }
    }
    else if (lastUpdate == null)
    {
      if (log.isDebugEnabled())
      {
        log.debug("The data corresponding to the URI '" + uri + "' in not available in the cache: attempting to retrieve it from the IO streamer");
      }
    }
    instructions.onFetchingFromUriStreamParser();
    return new Cacher.CachedInfo<BusinessObjectType>(fetchValueFromUriStreamParser(parameter, uri), Cacher.Origin.UriStreamParser);
  }

  /**
   * The data is first attempted to be retrieved from the URI stream parser (and refreshes the cache in case of success), and then from the cache.
   */
  public final Cacher.CachedInfo<BusinessObjectType> getValue(ParameterType parameter)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final UriType uri = uriStreamParser.computeUri(parameter);
    try
    {
      return new Cacher.CachedInfo<BusinessObjectType>(fetchValueFromUriStreamParser(parameter, uri), Cacher.Origin.UriStreamParser);
    }
    // This is for the ' InputExceptionType', 'StreamerExceptionType', 'ParseExceptionType' exceptions
    catch (Exception exception)
    {
      // The data cannot be taken from the UriStreamParser: we get it from the cache
      if (log.isDebugEnabled())
      {
        log.debug("The data corresponding to the URI '" + uri + "' in not accessible from the URI stream parser: attempting to use its cached value");
      }
      final Values.Info<BusinessObjectType> cachedValue = getCachedValue(parameter);
      if (cachedValue != null)
      {
        return new Cacher.CachedInfo<BusinessObjectType>(cachedValue, Cacher.Origin.Cache);
      }
      return null;
    }
  }

  /**
   * Retrieves directly the business object from the underlying {@link UriStreamParser}.
   */
  public final Values.Info<BusinessObjectType> fetchValueFromUriStreamParser(ParameterType parameter)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    return fetchValueFromUriStreamParser(parameter, uriStreamParser.computeUri(parameter));
  }

  private Values.Info<BusinessObjectType> fetchValueFromUriStreamParser(ParameterType parameter, final UriType uri)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final Values.Info<BusinessObjectType> info = retrieveRemoteBusinessObject(parameter, uri);
    onNewBusinessObject(uri, info);
    return info;
  }

  @SuppressWarnings("unchecked")
  private Values.Info<BusinessObjectType> retrieveRemoteBusinessObject(ParameterType parameter, UriType uri)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final Business.InputAtom atom = uriInputStreamer.getInputStream(uri);
    if (atom == null && uriInputStreamer instanceof Business.NullableUriInputStreamer)
    {
      final BusinessObjectType businessObject = ((Business.NullableUriInputStreamer<BusinessObjectType, UriType, ParameterType, InputExceptionType, InputExceptionType>) uriInputStreamer).onNullInputStream(
          parameter, uri);
      final Values.Info<BusinessObjectType> info = new Values.Info<BusinessObjectType>(businessObject, new Date(), Values.Info.SOURCE2);
      if (businessObject == null)
      {
        ioStreamer.writeInputStream(uri, new Business.InputAtom(new Date(), null));
        return info;
      }
      else if (uriStreamParser instanceof Business.UriStreamParserSerializer)
      {
        // In that case, we put the business object in the cache
        ioStreamer.writeInputStream(
            uri,
            new Business.InputAtom(new Date(), ((Business.UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, ParseExceptionType>) uriStreamParser).serialize(
                parameter, info.value)));
        return info;
      }
    }
    final InputStream inputStream = onNewInputStream(parameter, uri, atom);
    final BusinessObjectType businessObject = uriStreamParser.parse(parameter, inputStream);
    return new Values.Info<BusinessObjectType>(businessObject, atom.timestamp, Values.Info.SOURCE3);
  }

  /**
   * If the cacher underlying {@link UriStreamParser} is actually a {@link UriStreamParserSerializer}, serializes persistently the business object and
   * its associated time stamp.
   */
  public void setValue(ParameterType parameter, Values.Info<BusinessObjectType> info)
      throws StreamerExceptionType, ParseExceptionType
  {
    if (uriStreamParser instanceof Business.UriStreamParserSerializer<?, ?, ?, ?>)
    {
      final UriType uri = computeUri(parameter);
      ioStreamer.writeInputStream(
          uri,
          new Business.InputAtom(info.timestamp, ((Business.UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, ParseExceptionType>) uriStreamParser).serialize(
              parameter, info.value)));
    }
  }

  /**
   * Retrieves the business object from the persistence layer only, without attempting to refresh it from the {@link IOStreamer}.
   */
  public Values.Info<BusinessObjectType> getCachedValue(ParameterType parameter)
      throws StreamerExceptionType, ParseExceptionType
  {
    final UriType uri = computeUri(parameter);
    final Business.InputAtom atom = ioStreamer.readInputStream(uri);
    if (atom != null)
    {
      // If the input stream is null but not the atom, we return a null business object
      return new Values.Info<BusinessObjectType>(atom.inputStream == null ? null : uriStreamParser.parse(parameter, atom.inputStream), atom.timestamp, Values.Info.SOURCE2);
    }
    return null;
  }

  /**
   * Removes the business object from the persistence layer.
   */
  public void remove(ParameterType parameter)
      throws StreamerExceptionType
  {
    ioStreamer.remove(computeUri(parameter));
  }

  protected InputStream onNewInputStream(ParameterType parameter, UriType uri, Business.InputAtom atom)
      throws StreamerExceptionType
  {
    return ioStreamer.writeInputStream(uri, atom);
  }

}
