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

package com.smartnsoft.droid4me.bo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;

import com.smartnsoft.droid4me.cache.Persistence;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * Gathers various interfaces which handle business objects.
 * 
 * @author Édouard Mercier
 * @since 2009.08.29
 */
public final class Business
{

  private final static Logger log = LoggerFactory.getInstance(Business.class);

  public static class BusinessException
      extends Exception
  {

    private static final long serialVersionUID = -4702275606866556704L;

    public BusinessException(Throwable cause)
    {
      super(cause);
    }

    public BusinessException(String message, Throwable cause)
    {
      super(message, cause);
    }

  }

  /**
   * Associates a time stamp to an object.
   * 
   * @since 2009.08.31
   */
  public static class Atom
  {

    public final Date timestamp;

    public Atom(Date timestamp)
    {
      this.timestamp = timestamp;
    }

  }

  /**
   * Associates a time stamp to an input stream.
   */
  public final static class InputAtom
      extends Business.Atom
  {

    public final InputStream inputStream;

    public final Serializable context;

    public InputAtom(Date timestamp, InputStream inputStream)
    {
      this(timestamp, inputStream, null);
    }

    public InputAtom(Date timestamp, InputStream inputStream, Serializable context)
    {
      super(timestamp);
      this.inputStream = inputStream;
      this.context = context;
    }

  }

  /**
   * Associates a time stamp to an output stream.
   * 
   * @since 2009.08.31
   */
  public final static class OutputAtom
      extends Business.Atom
  {

    public final OutputStream outputStream;

    public OutputAtom(Date timestamp, OutputStream outputStream)
    {
      super(timestamp);
      this.outputStream = outputStream;
    }

  }

  /**
   * An interface which proposes to think each web service call in terms of two separate actions: compute the URI and get the corresponding input
   * stream.
   * 
   * @since 2009.06.19
   */
  public static interface Urier<UriType, ParameterType>
  {

    UriType computeUri(ParameterType parameter);

  }

  /**
   * An interface which proposes in addition to its parent to split each web service call into three separate actions: compute the URI, get the
   * corresponding input stream, and then parse the result.
   * 
   * @since 2009.08.29
   */
  public static interface UriStreamParser<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception>
      extends Business.Urier<UriType, ParameterType>
  {

    BusinessObjectType parse(ParameterType parameter, InputStream inputStream)
        throws ExceptionType;

  }

  /**
   * An interface which proposes to serialize a business object as an input stream.
   * 
   * @since 2009.08.31
   */
  public static interface UriStreamSerializer<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception>
      extends Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ExceptionType>
  {

    InputStream serialize(ParameterType parameter, BusinessObjectType businessObject)
        throws ExceptionType;

  }

  /**
   * @since 2009.08.31
   */
  public static interface UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception>
      extends Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ExceptionType>,
      Business.UriStreamSerializer<BusinessObjectType, UriType, ParameterType, ExceptionType>
  {
  }

  /**
   * Serializes and parses the underlying business object via the native Java mechanism.
   * 
   * @since 2009.08.31
   */
  public static abstract class ObjectUriStreamSerializer<BusinessObjectType extends Serializable, UriType, ParameterType>
      implements Business.UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, Business.BusinessException>
  {

    @SuppressWarnings("unchecked")
    public final BusinessObjectType parse(ParameterType parameter, InputStream inputStream)
        throws Business.BusinessException
    {
      final long start = System.currentTimeMillis();
      try
      {
        final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        try
        {
          final BusinessObjectType object = (BusinessObjectType) objectInputStream.readObject();
          if (log.isDebugEnabled())
          {
            log.debug("Deserialized the object corresponding to the parameter '" + parameter + "' in " + (System.currentTimeMillis() - start) + " ms");
          }
          return object;
        }
        finally
        {
          try
          {
            objectInputStream.close();
          }
          catch (IOException exception)
          {
            // Does not matter
          }
        }
      }
      catch (EOFException exception)
      {
        // The input stream seems to be empty
        return null;
      }
      catch (Exception exception)
      {
        throw new Business.BusinessException("Could not extract the object", exception);
      }
    }

    public final InputStream serialize(ParameterType parameter, BusinessObjectType businessObject)
        throws Business.BusinessException
    {
      final long start = System.currentTimeMillis();
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try
      {
        try
        {
          final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
          try
          {
            objectOutputStream.writeObject(businessObject);
          }
          finally
          {
            try
            {
              objectOutputStream.close();
            }
            catch (IOException exception)
            {
              // Does not matter
            }
          }
        }
        catch (IOException exception)
        {
          throw new Business.BusinessException("Could not serialize the object", exception);
        }
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        if (log.isDebugEnabled())
        {
          log.debug("Serialized the object corresponding to the parameter '" + parameter + "' in " + (System.currentTimeMillis() - start) + " ms");
        }
        return byteArrayInputStream;
      }
      finally
      {
        try
        {
          byteArrayOutputStream.close();
        }
        catch (IOException exception)
        {
          // Does not matter
        }
      }
    }

  }

  /**
   * Is supposed to retrieve the input stream and its underlying object time stamp remotely.
   * 
   * @since 2009.08.31
   */
  public static interface UriInputStreamer<UriType, ExceptionType extends Exception>
  {

    Business.InputAtom getInputStream(UriType uri)
        throws ExceptionType;

  }

  /**
   * Is able to read an input stream corresponding to a locally cached business object, from a URI, and indicate its last update time stamp.
   * 
   * @since 2009.08.31
   */
  public static interface InputStreamer<UriType, ExceptionType extends Throwable>
  {

    Business.InputAtom readInputStream(UriType uri)
        throws ExceptionType;

    Date getLastUpdate(UriType uri);

  }

  /**
   * 
   * Is able to write the input stream corresponding to a local business object, attached to a URI.
   * 
   * @since 2009.08.31
   */
  public static interface OutputStreamer<UriType, ExceptionType extends Throwable>
  {

    /**
     * @param inputAtom
     *          the {@link Business.InputAtom#inputStream} is allowed to be null: in that case, a null-like object must be persisted
     */
    InputStream writeInputStream(UriType uri, Business.InputAtom inputAtom)
        throws ExceptionType;

  }

  /**
   * 
   * @since 2009.08.31
   */
  public static interface IOStreamer<UriType, ExceptionType extends Throwable>
      extends Business.InputStreamer<UriType, ExceptionType>, Business.OutputStreamer<UriType, ExceptionType>
  {

    void remove(UriType uri)
        throws ExceptionType;

  }

  /**
   * @since 2009.08.31
   */
  public static interface Cacheable<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ParseExceptionType>, Business.IOStreamer<UriType, StreamerExceptionType>,
      Business.UriInputStreamer<UriType, InputExceptionType>
  {

  }

  /**
   * @since 2009.08.31
   */
  public static abstract class Cached<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      implements Business.Cacheable<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final Business.IOStreamer<String, StreamerExceptionType> ioStreamer;

    public Cached(Business.IOStreamer<String, StreamerExceptionType> ioStreamer)
    {
      this.ioStreamer = ioStreamer;
    }

    public Business.InputAtom readInputStream(String uri)
        throws StreamerExceptionType
    {
      return ioStreamer.readInputStream(uri);
    }

    public InputStream writeInputStream(String uri, Business.InputAtom inputAtom)
        throws StreamerExceptionType
    {
      return ioStreamer.writeInputStream(uri, inputAtom);
    }

    public Date getLastUpdate(String uri)
    {
      return ioStreamer.getLastUpdate(uri);
    }

    protected final void removeUri(String uri)
        throws StreamerExceptionType
    {
      ioStreamer.remove(uri);
    }

  }

  /**
   * Is supposed to retrieve the input stream and its underlying object time stamp remotely.
   * 
   * @since 2009.08.31
   */
  public static interface NullableUriInputStreamer<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception, NullExceptionType extends Exception>
      extends Business.UriInputStreamer<UriType, ExceptionType>
  {

    BusinessObjectType onNullInputStream(ParameterType parameter, UriType uri)
        throws NullExceptionType;

  }

  /**
   * Indicates that no exception can be triggered by the {@link Business.InputStreamer} interface.
   * 
   * @since 2009.12.29
   */
  public final static class NoUriInputStreamerException
      extends Exception
  {
    private static final long serialVersionUID = 5759991468469326796L;
  }

  /**
   * @since 2009.08.31
   */
  public static abstract class ObjectCached<BusinessObjectType extends Serializable, ParameterType, NullExceptionType extends Exception>
      extends Business.ObjectUriStreamSerializer<BusinessObjectType, String, ParameterType>
      implements
      Business.Cacheable<BusinessObjectType, String, ParameterType, Business.BusinessException, Persistence.PersistenceException, NullExceptionType>,
      Business.NullableUriInputStreamer<BusinessObjectType, String, ParameterType, NullExceptionType, NullExceptionType>
  {

    private final Business.IOStreamer<String, Persistence.PersistenceException> ioStreamer;

    public ObjectCached(Business.IOStreamer<String, Persistence.PersistenceException> ioStreamer)
    {
      this.ioStreamer = ioStreamer;
    }

    public final Business.InputAtom readInputStream(String uri)
        throws Persistence.PersistenceException
    {
      return ioStreamer.readInputStream(uri);
    }

    public final InputStream writeInputStream(String uri, Business.InputAtom inputAtom)
        throws Persistence.PersistenceException
    {
      return ioStreamer.writeInputStream(uri, inputAtom);
    }

    public final Date getLastUpdate(String uri)
    {
      return ioStreamer.getLastUpdate(uri);
    }

    public final Business.InputAtom getInputStream(String uri)
    {
      return null;
    }

    public final void remove(String uri)
        throws Persistence.PersistenceException
    {
      ioStreamer.remove(uri);
    }

    public final void removeWithParameter(ParameterType parameter)
        throws Persistence.PersistenceException
    {
      ioStreamer.remove(computeUri(parameter));
    }

  }

}
