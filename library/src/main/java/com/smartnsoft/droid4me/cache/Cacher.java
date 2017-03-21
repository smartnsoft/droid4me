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

package com.smartnsoft.droid4me.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.IOStreamer;
import com.smartnsoft.droid4me.bo.Business.UriInputStreamer;
import com.smartnsoft.droid4me.bo.Business.UriStreamParser;
import com.smartnsoft.droid4me.bo.Business.UriStreamParserSerializer;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A class which enables to cache business objects.
 * <p/>
 * <p>
 * The class is not thread-safe in the sense that if multiple threads attempts to modify or get the underlying cached object, no atomicity is granted.
 * </p>
 *
 * @author Édouard Mercier
 * @since 2009.06.18
 */
public class Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
{

  /**
   * Defines some common statuses.
   *
   * @since 2011.07.31
   */
  public enum Status
  {
    Attempt, Success
    /* , Failure */
  }

  /**
   * Indicates whether the current cached data should be taken from the cache.
   */
  public interface Instructions
  {

    /**
     * Is invoked to determine whether the persistence timestamp field should be requested, on the {@link IOStreamer} layer.
     *
     * @return {@code true} if and only if the timestamp of the underlying business object should be requested on the persistence layer
     */
    boolean queryTimestamp();

    /**
     * Indicates whether the underlying business is still valid regarding its storage timestamp. Is invoked in two ways:
     *
     * @param timestamp the date corresponding to the last data retrieval ; may be {@code null}, and will be if the {@link #queryTimestamp()} previously
     *                  returned {@code false}
     * @return {@code true} if and only if the business object state should be taken from the persistence layer
     */
    boolean takeFromCache(Date timestamp);

    /**
     * Invoked every time the underlying business object is bound to be fetched from the {@link IOStreamer}, or when it has actually been retrieved.
     * from it.
     *
     * @param status indicates the status: {@link Cacher.Status#Attempt} when first attempting, {@link Cacher.Status#Success} when successfully retrieved
     */
    void onIOStreamer(Cacher.Status status);

    /**
     * Invoked every time the underlying business object is bound to be fetched from the {@link UriStreamParser}, or when it has actually been
     * retrieved.
     *
     * @param status indicates the status: {@link Cacher.Status#Attempt} when first attempting, {@link Cacher.Status#Success} when successfully retrieved
     */
    void onUriStreamParser(Cacher.Status status);

  }

  protected final static Logger log = LoggerFactory.getInstance(Cacher.class);

  protected final Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ParseExceptionType> uriStreamParser;

  private final Business.IOStreamer<UriType, StreamerExceptionType> ioStreamer;

  private final Business.UriInputStreamer<UriType, InputExceptionType> uriInputStreamer;

  public Cacher(
      Business.Cacheable<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacheable)
  {
    this(cacheable, cacheable, cacheable);
  }

  public Cacher(
      Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ParseExceptionType> uriStreamParser,
      Business.IOStreamer<UriType, StreamerExceptionType> ioStreamer,
      Business.UriInputStreamer<UriType, InputExceptionType> uriInputStreamer)
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

  /**
   * Retrieves the business object corresponding to the provided parameter, by specifying the routing instructions.
   *
   * @param instructions enables to control the source where the business object should be retrieved: either from a {@link UriInputStreamer} or from an
   *                     {@link IOStreamer}.
   * @param parameter    the URI corresponding to the business object
   * @return a wrapper around the extracted business object, along with a retrieval timestamp and a {@link Business.Source origin}.
   * @throws InputExceptionType    if a problem occurred while attempting to extract the business object raw data {@link Business.InputAtom} from the
   *                               {@link UriInputStreamer}, via the {@link UriInputStreamer#getInputStream(Object)} method
   * @throws StreamerExceptionType if a problem occurred while attempting to extract the business object raw data {@link Business.InputAtom} from the {@link IOStreamer},
   *                               via the {@link IOStreamer#readInputStream(Object)} method
   * @throws ParseExceptionType    if a problem occurred while parsing the business object from its {@link Business.InputAtom} raw data from the {@link UriStreamParser},
   *                               via the {@link UriStreamParser#parse} method
   */
  public final Values.Info<BusinessObjectType> getValue(Cacher.Instructions instructions, ParameterType parameter)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final UriType uri = uriStreamParser.computeUri(parameter);
    // We first ask whether the timestamp associated with the cached data should be retrieved
    final boolean queryTimestamp = instructions.queryTimestamp();
    final Date lastUpdate;
    if (queryTimestamp == true)
    {
      lastUpdate = getCacheLastUpdate(parameter, uri);
    }
    else
    {
      lastUpdate = null;
    }
    if (instructions.takeFromCache(lastUpdate) == true)
    {
      try
      {
        // We notify the instructions that the business object is bound to be read from the IO streamer
        if (instructions != null)
        {
          instructions.onIOStreamer(Cacher.Status.Attempt);
        }
        final Values.Info<BusinessObjectType> cachedValue = getCachedValue(parameter);
        if (cachedValue != null)
        {
          // We notify the instructions that the business object has been successfully extracted from the IO streamer
          if (instructions != null)
          {
            instructions.onIOStreamer(Cacher.Status.Success);
          }
          return cachedValue;
        }
        if (log.isDebugEnabled())
        {
          log.debug("The data corresponding to the URI '" + uri + "' was eventually not present in the cache: a new request will be attempted again!");
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
    else
    {
      if (log.isDebugEnabled())
      {
        log.debug("The data corresponding to the URI '" + uri + "' in the cache has not been accepted: attempting to retrieve it from the IO streamer");
      }
    }
    return fetchValueFromUriStreamParser(instructions, parameter, uri);
  }

  /**
   * The data is first attempted to be retrieved from the URI stream parser (and refreshes the cache in case of success), and then from the cache.
   */
  public final Values.Info<BusinessObjectType> getValue(ParameterType parameter)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final UriType uri = uriStreamParser.computeUri(parameter);
    try
    {
      return fetchValueFromUriStreamParser(null, parameter, uri);
    }
    // This is for the 'InputExceptionType', 'StreamerExceptionType', 'ParseExceptionType' exceptions
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
        return cachedValue;
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
    return fetchValueFromUriStreamParser(null, parameter, uriStreamParser.computeUri(parameter));
  }

  private Values.Info<BusinessObjectType> fetchValueFromUriStreamParser(Cacher.Instructions instructions,
      ParameterType parameter, final UriType uri)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    final Values.Info<BusinessObjectType> info = retrieveRemoteBusinessObject(instructions, parameter, uri);
    onNewBusinessObject(uri, info);
    return info;
  }

  @SuppressWarnings("unchecked")
  private Values.Info<BusinessObjectType> retrieveRemoteBusinessObject(Cacher.Instructions instructions,
      ParameterType parameter, UriType uri)
      throws InputExceptionType, StreamerExceptionType, ParseExceptionType
  {
    // We notify the instructions that the business object is bound to be read from the URI streamer
    if (instructions != null)
    {
      instructions.onUriStreamParser(Cacher.Status.Attempt);
    }

    final Business.InputAtom atom = uriInputStreamer.getInputStream(uri);
    if (atom == null)
    {
      if (uriInputStreamer instanceof Business.NullableUriInputStreamer)
      {
        final BusinessObjectType businessObject = ((Business.NullableUriInputStreamer<BusinessObjectType, UriType, ParameterType, InputExceptionType, InputExceptionType>) uriInputStreamer).onNullInputStream(parameter, uri);
        final Values.Info<BusinessObjectType> info = new Values.Info<>(businessObject, new Date(), Business.Source.IOStreamer);
        if (businessObject == null)
        {
          ioStreamer.writeInputStream(uri, new Business.InputAtom(new Date(), null), true);
          return info;
        }
        else if (uriStreamParser instanceof Business.UriStreamParserSerializer<?, ?, ?, ?>)
        {
          // In that case, we put the business object in the cache
          ioStreamer.writeInputStream(uri, new Business.InputAtom(new Date(), ((Business.UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, ParseExceptionType>) uriStreamParser).serialize(parameter, info.value)), true);
          return info;
        }
        else
        {
          return new Values.Info<>(null, new Date(), Business.Source.UriStreamer);
        }
      }
    }
    // We need to duplicate the input stream, because it will be closed when parsing it!
    final InputStream markableInputStream;
    if (atom.inputStream != null)
    {
      boolean duplicationSuccess = false;
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      int read;
      final byte[] data = new byte[32768];
      try
      {
        while ((read = atom.inputStream.read(data, 0, data.length)) != -1)
        {
          buffer.write(data, 0, read);
        }
        buffer.flush();
        duplicationSuccess = true;
        atom.inputStream.close();
      }
      catch (IOException exception)
      {
        if (log.isErrorEnabled())
        {
          log.error("Cannot duplicate the input stream corresponding to the URI '" + uri + "' and to the parameter '" + parameter + "'", exception);
        }
      }
      if (duplicationSuccess == true)
      {
        // This input stream can be marked!
        markableInputStream = new ByteArrayInputStream(buffer.toByteArray());
        markableInputStream.mark(buffer.size());
      }
      else
      {
        markableInputStream = null;
      }
    }
    else
    {
      markableInputStream = null;
    }
    // We first parse the input stream, so as to make sure that it is valid before persisting it
    final BusinessObjectType businessObject = uriStreamParser.parse(parameter, atom.headers, markableInputStream);
    boolean invokeOnNewInputStream = true;
    if (markableInputStream != null)
    {
      try
      {
        markableInputStream.reset();
      }
      catch (IOException exception)
      {
        invokeOnNewInputStream = false;
        if (log.isErrorEnabled())
        {
          log.error("Cannot reset and hence cannot persist the input stream corresponding to the URI '" + uri + "' and to the parameter '" + parameter + "'", exception);
        }
      }
    }
    if (invokeOnNewInputStream == true)
    {
      // Now, we can persist the input stream corresponding to the business object
      onNewInputStream(parameter, uri, markableInputStream == null ? atom : new Business.InputAtom(atom.timestamp, markableInputStream, atom.context), false);
    }

    // We notify the instructions that the business object has been read from the URI streamer
    if (instructions != null)
    {
      instructions.onUriStreamParser(Cacher.Status.Success);
    }

    return new Values.Info<>(businessObject, atom.timestamp, Business.Source.UriStreamer);
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
      ioStreamer.writeInputStream(uri, new Business.InputAtom(info.timestamp, ((Business.UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, ParseExceptionType>) uriStreamParser).serialize(parameter, info.value)), true);
    }
    else
    {
      if (log.isWarnEnabled())
      {
        log.warn("The call to persist the business object corresponding to the parameter '" + parameter + "' failed, because we do not know how to serialize the business object");
      }
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
      return new Values.Info<>(atom.inputStream == null ? null : uriStreamParser.parse(parameter, atom.headers, atom.inputStream), atom.timestamp, Business.Source.IOStreamer);
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

  protected InputStream onNewInputStream(ParameterType parameter, UriType uri, Business.InputAtom atom,
      boolean returnStream)
      throws StreamerExceptionType
  {
    return ioStreamer.writeInputStream(uri, atom, returnStream);
  }

}
