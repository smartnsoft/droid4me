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
import java.io.StreamCorruptedException;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

  /**
   * Enables to define the way to reach a business object entity depending on its source.
   *
   * @param <UriType>       the type of URI returned to locate the business object entity
   * @param <ParameterType> the type of parameter used for getting the business object entity
   * @since 2009.06.19
   */
  public interface Urier<UriType, ParameterType>
  {

    /**
     * Is responsible for returning the URI of a business object entity.
     *
     * @param parameter the parameters which enable to compute the entity URI
     * @return the URI which enables to access to the underlying business object entity ; should return {@code null} if no URI is available
     */
    UriType computeUri(ParameterType parameter);

  }

  /**
   * An interface which proposes in addition to its parent to split each web service call into three separate actions: compute the URI, get the
   * corresponding input stream, and then parse the result.
   *
   * @since 2009.08.29
   */
  public interface UriStreamParser<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception>
      extends Business.Urier<UriType, ParameterType>
  {

    BusinessObjectType parse(ParameterType parameter, Map<String, List<String>> headers, InputStream inputStream)
        throws ExceptionType;

  }

  /**
   * An interface which proposes to serialize a business object as an input stream.
   *
   * @since 2009.08.31
   */
  public interface UriStreamSerializer<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception>
      extends Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ExceptionType>
  {

    InputStream serialize(ParameterType parameter, BusinessObjectType businessObject)
        throws ExceptionType;

  }

  /**
   * @since 2009.08.31
   */
  public interface UriStreamParserSerializer<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception>
      extends Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ExceptionType>, Business.UriStreamSerializer<BusinessObjectType, UriType, ParameterType, ExceptionType>
  {

  }

  /**
   * Is supposed to retrieve the input stream and its underlying object time stamp remotely.
   *
   * @since 2009.08.31
   */
  public interface UriInputStreamer<UriType, ExceptionType extends Exception>
  {

    /**
     * Is responsible for returning the date and input stream related to the the provided URI.
     *
     * @param uri the URI for which the data are asked for
     * @return a wrapper which contains the last modification date and the data {@link InputStream}
     * @throws ExceptionType if a problem occurred while accessing to the data
     */
    Business.InputAtom getInputStream(UriType uri)
        throws ExceptionType;

  }

  /**
   * Is able to read an input stream corresponding to a locally cached business object, from a URI, and indicate its last update time stamp.
   *
   * @since 2009.08.31
   */
  public interface InputStreamer<UriType, ExceptionType extends Throwable>
  {

    /**
     * Should return an input stream related to the provided URI.
     *
     * @param uri an URI which identifies the resource; it is not allowed to be {@code null}
     * @return a wrapper of the resulting input stream, which may be {@code null}; the {@link Business.InputAtom#inputStream} and
     * {@link Business.InputAtom#context} are allowed to be null
     * @throws ExceptionType whenever a problem occurred while processing
     */
    Business.InputAtom readInputStream(UriType uri)
        throws ExceptionType;

    /**
     * Should return the last modification date of the input stream related to the provided URI.
     *
     * @param uri an URI which identifies the resource; it is not allowed to be {@code null}
     * @return the last modification date of the related input stream, or {@code null} if there is no such input stream
     */
    Date getLastUpdate(UriType uri);

  }

  /**
   * Is able to write the input stream corresponding to a local business object, attached to a URI.
   *
   * @since 2009.08.31
   */
  public interface OutputStreamer<UriType, ExceptionType extends Throwable>
  {

    /**
     * Is responsible for writing the provided data.
     * <p>
     * <p>
     * The {@code returnStream} parameter enables to tune whether the implementation is supposed to return the provided input stream.
     * throws {@link Throwable} if any problem occurs while persisting the data
     * </p>
     *
     * @param uri          the URI the data belongs to
     * @param inputAtom    the wrapper that contains the data to be persisted the {@link Business.InputAtom#inputStream} is allowed to be null: in that case, a
     *                     null-like object must be persisted
     * @param returnStream if set {@code true}, a valid {@link InputStream} corresponding to the provided {@link Business.InputAtom#inputStream} must be returned
     *                     ; if set to {@code false}, {@code null} must be returned
     * @return a valid input stream corresponding to the persisted data; it is not allowed to be {@code null} when the {@code returnStream} parameter
     * is {@code true}. Otherwise, {@code null} must be returned
     */
    InputStream writeInputStream(UriType uri, Business.InputAtom inputAtom, boolean returnStream)
        throws ExceptionType;

  }

  /**
   * @since 2009.08.31
   */
  public interface IOStreamer<UriType, ExceptionType extends Throwable>
      extends Business.InputStreamer<UriType, ExceptionType>, Business.OutputStreamer<UriType, ExceptionType>
  {

    /**
     * Is responsible for removing any data related to the provided URI. A further attempt to access to this same URI should be a failure.
     *
     * @param uri the URI corresponding to the data to erase
     * @throws ExceptionType if a problem occurred while removing the data
     */
    void remove(UriType uri)
        throws ExceptionType;

  }

  /**
   * @since 2009.08.31
   */
  public interface Cacheable<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Business.UriStreamParser<BusinessObjectType, UriType, ParameterType, ParseExceptionType>, Business.IOStreamer<UriType, StreamerExceptionType>, Business.UriInputStreamer<UriType, InputExceptionType>
  {

  }

  /**
   * Is supposed to retrieve the input stream and its underlying object time stamp remotely.
   *
   * @since 2009.08.31
   */
  public interface NullableUriInputStreamer<BusinessObjectType, UriType, ParameterType, ExceptionType extends Exception, NullExceptionType extends Exception>
      extends Business.UriInputStreamer<UriType, ExceptionType>
  {

    BusinessObjectType onNullInputStream(ParameterType parameter, UriType uri)
        throws NullExceptionType;

  }

  /**
   * Indicates the origin of a business object.
   */
  public enum Source
  {
    Memory, IOStreamer, UriStreamer
  }

  /**
   * Associates a time stamp to an input stream.
   */
  public static final class InputAtom
      extends Business.Atom
  {

    public final Map<String, List<String>> headers;

    public final InputStream inputStream;

    public final Serializable context;

    public InputAtom(Date timestamp, InputStream inputStream)
    {
      this(timestamp, inputStream, null);
    }

    public InputAtom(Date timestamp, InputStream inputStream, Serializable context)
    {
      this(timestamp, null, inputStream, context);
    }

    public InputAtom(Date timestamp, Map<String, List<String>> headers, InputStream inputStream, Serializable context)
    {
      super(timestamp);
      this.headers = headers;
      this.inputStream = inputStream;
      this.context = context;
    }

  }

  /**
   * Associates a time stamp to an output stream.
   *
   * @since 2009.08.31
   */
  public static final class OutputAtom
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
   * Indicates that no exception can be triggered by the {@link Business.InputStreamer} interface.
   *
   * @since 2009.12.29
   */
  public final static class NoUriInputStreamerException
      extends Exception
  {

    private static final long serialVersionUID = 5759991468469326796L;
  }

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
  public static abstract class Atom
  {

    public final Date timestamp;

    public Atom(Date timestamp)
    {
      this.timestamp = timestamp;
    }

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
    public final BusinessObjectType parse(ParameterType parameter, Map<String, List<String>> headers,
        InputStream inputStream)
        throws Business.BusinessException
    {
      final long start = System.currentTimeMillis();
      try
      {
        final ObjectInputStream objectInputStream = createObjectInputStream(inputStream);
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
          final ObjectOutputStream objectOutputStream = createObjectOutputStream(byteArrayOutputStream);
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

    /**
     * A hook for creating an {@link ObjectInputStream} when attempting to deserialize the underlying object.
     * <p>
     * <p>
     * This implementation just returns {@code new ObjectInputStream(inputStream)}.
     * </p>
     *
     * @param inputStream the stream which holds the object representation
     * @return a valid object input stream, which will be used when deserializing the object
     * @throws StreamCorruptedException
     * @throws IOException
     */
    protected ObjectInputStream createObjectInputStream(InputStream inputStream)
        throws StreamCorruptedException, IOException
    {
      return new ObjectInputStream(inputStream);
    }

    /**
     * A hook for creating an {@link ObjectOutputStream} when attempting to serialize the underlying object.
     * <p>
     * <p>
     * This implementation just returns {@code new ObjectOutputStream(outputStream)}.
     * </p>
     *
     * @param outputStream the stream which will hold the object representation
     * @return a valid object object stream, which will be used when serializing the object
     * @throws IOException
     */
    protected ObjectOutputStream createObjectOutputStream(OutputStream outputStream)
        throws IOException
    {
      return new ObjectOutputStream(outputStream);
    }

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

    public InputStream writeInputStream(String uri, Business.InputAtom inputAtom, boolean returnStream)
        throws StreamerExceptionType
    {
      return ioStreamer.writeInputStream(uri, inputAtom, returnStream);
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
   * @since 2009.08.31
   */
  public static abstract class ObjectCached<BusinessObjectType extends Serializable, ParameterType, NullExceptionType extends Exception>
      extends Business.ObjectUriStreamSerializer<BusinessObjectType, String, ParameterType>
      implements Business.Cacheable<BusinessObjectType, String, ParameterType, Business.BusinessException, Persistence.PersistenceException, NullExceptionType>, Business.NullableUriInputStreamer<BusinessObjectType, String, ParameterType, NullExceptionType, NullExceptionType>
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

    public final InputStream writeInputStream(String uri, Business.InputAtom inputAtom, boolean returnStream)
        throws Persistence.PersistenceException
    {
      return ioStreamer.writeInputStream(uri, inputAtom, returnStream);
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

  private final static Logger log = LoggerFactory.getInstance(Business.class);

}
