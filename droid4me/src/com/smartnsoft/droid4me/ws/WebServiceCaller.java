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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import org.json.JSONException;

/**
 * A basis class for making web service calls easier.
 *
 * @author Édouard Mercier, Ludovic Roland
 * @see URLConnectionWebServiceCaller
 * @since 2009.03.26
 */
public abstract class WebServiceCaller
    implements WebServiceClient
{

  public static final class MultipartFile
  {

    public final String name;

    public final String fileName;

    public final String contentType;

    public final FileInputStream fileInputStream;

    public MultipartFile(String name, String fileName, String contentType, FileInputStream fileInputStream)
    {
      this.name = name;
      this.fileName = fileName;
      this.contentType = contentType;
      this.fileInputStream = fileInputStream;
    }

  }

  public static final class HttpResponse
  {

    public final Map<String, List<String>> headers;

    public final InputStream inputStream;

    public HttpResponse(Map<String, List<String>> headers, InputStream inputStream)
    {
      this.headers = headers;
      this.inputStream = inputStream;
    }

  }

  /**
   * A flag which indicates whether the hereby {@code WebServiceCaller} internal logs should be enabled. Logs will report useful {@code curl}
   * equivalent commands, for instance.
   * <p>
   * Do not set this flag to {@code true} under the production mode!
   * </p>
   *
   * @see WebServiceCaller#BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES
   */
  public static boolean ARE_DEBUG_LOG_ENABLED = false;

  /**
   * If the {@link #ARE_DEBUG_LOG_ENABLED} is {@code true}, indicates the maximum size of an HTTP request and response body to be logged: if an HTTP
   * request body size is beyond that limit, it will not be logged, for performance reasons.
   */
  public static long BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES = 8192;

  protected final static Logger log = LoggerFactory.getInstance(WebServiceCaller.class);

  protected boolean isConnected = true;

  /**
   * @return the charset to use for encoding the URI parameters
   */
  protected abstract String getUrlEncoding();

  /**
   * This method will be invoked when the instance reads a web service result body.
   *
   * @return the charset to use for decoding the web service requests content
   * @see #getString(InputStream)
   * @see #getString(InputStream, String)
   * @see #getJson(InputStream)
   * @see #getJson(InputStream, String)
   */
  protected abstract String getContentEncoding();

  /**
   * @return the value previously set by the {@link #setConnected(boolean)} method
   */
  public boolean isConnected()
  {
    return isConnected;
  }

  /**
   * Enables to indicate that no Internet connectivity is available, or that the connectivity has been restored. The initial value is {@code true}.
   */
  public void setConnected(boolean isConnected)
  {
    if (log.isDebugEnabled())
    {
      log.debug("Setting the connectivity to " + isConnected);
    }
    this.isConnected = isConnected;
  }

  /**
   * Equivalent to {@code WebServiceCaller.getString(inputStream, getContentEncoding())}.
   *
   * @see #getString(InputStream, String)
   * @see #getJson(InputStream)
   */
  public final String getString(InputStream inputStream)
      throws IOException
  {
    return WebServiceCaller.getString(inputStream, getContentEncoding());
  }

  /**
   * Turns the provided {@link InputStream} into a {@link String}, using the provided encoding.
   *
   * @param inputStream the input stream to convert ; not that it will have been {@link InputStream#close() closed}
   * @param encoding    the encoding to use
   * @return the string resulting from the provided input stream
   * @throws IOException if an error happened during the conversion
   * @see #getString(InputStream)
   * @see #getJson(InputStream, String)
   */
  public static String getString(InputStream inputStream, String encoding)
      throws IOException
  {
    final StringWriter writer = new StringWriter();
    final InputStreamReader streamReader = new InputStreamReader(inputStream, encoding);
    // The 8192 parameter is there to please Android at runtime and discard the
    // "INFO/global(16464): INFO: Default buffer size used in BufferedReader constructor. It would be better to be explicit if a 8k-char buffer is required."
    // log
    final BufferedReader buffer = new BufferedReader(streamReader, 8192);
    String line = "";
    while (null != (line = buffer.readLine()))
    {
      writer.write(line);
    }
    return writer.toString();
  }

  /**
   * Equivalent to {@code WebServiceCaller.getJson(inputStream, getContentEncoding())}.
   *
   * @throws JSONException
   * @see #getJson(InputStream, String)
   * @see #getString(InputStream, String)
   */
  public final String getJson(InputStream inputStream)
      throws JSONException
  {
    return WebServiceCaller.getJson(inputStream, getContentEncoding());
  }

  /**
   * Invokes the {@link #getString(InputStream, String)} method, but just turn the potential {@link IOException} into a {@link JSONException}.
   *
   * @see #getString(InputStream)
   * @see #getJson(InputStream)
   */
  public static String getJson(InputStream inputStream, String encoding)
      throws JSONException
  {
    try
    {
      return WebServiceCaller.getString(inputStream, encoding);
    }
    catch (IOException exception)
    {
      throw new JSONException(exception.getMessage());
    }
  }

  /**
   * Just invokes {@code #computeUri(String, String, Map, boolean)}, with {@code false} as last parameter.
   */
  @Override
  public final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
  {
    return WebServiceCaller.encodeUri(methodUriPrefix, methodUriSuffix, uriParameters, false, getUrlEncoding());
  }

  /**
   * Just invokes {@code #encodeUri(String, String, Map, boolean, String)}, with {@code #getUrlEncoding()} as last parameter.
   */
  public final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters,
      boolean alreadyContainsQuestionMark)
  {
    return WebServiceCaller.encodeUri(methodUriPrefix, methodUriSuffix, uriParameters, alreadyContainsQuestionMark, getUrlEncoding());
  }

  /**
   * Computes a URI from its path elements.
   *
   * @param methodUriPrefix             the URI prefix
   * @param methodUriSuffix             the URI suffix ; a {@code /} separator will be appended after the {@code methodUriPrefix} parameter, if not {@code null}. May be
   *                                    {@code null}
   * @param uriParameters               a dictionary with {@link String} keys and {@link String} values, which holds the URI query parameters ; may be {@code null}. If a value
   *                                    is {@code null}, an error log will be issued. If a value is the empty string ({@code ""}), the dictionary key will be used as the
   *                                    name+value URI parameter ; this is especially useful when the parameter value should not be encoded
   * @param alreadyContainsQuestionMark indicates whether the provided {@code methodUriPrefix} or the {@code methodUriSuffix} parameters already contain a question mark {@code ?}, so
   *                                    that, when computing the URI with the additional {@code uriParameters}, it is not appended again. This is especially useful when
   *                                    building an URI from a basis URI, which already contains a {@code ?} for declaring URI parameters
   * @param urlEnconding                the encoding used for writing the URI query parameters values
   * @return a valid URI that may be used for running an HTTP request, for instance
   */
  public static String encodeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters,
      boolean alreadyContainsQuestionMark, String urlEnconding)
  {
    final StringBuffer buffer = new StringBuffer(methodUriPrefix);
    if (methodUriSuffix != null && methodUriSuffix.length() > 0)
    {
      buffer.append("/").append(methodUriSuffix);
    }
    boolean first = true;
    if (uriParameters != null)
    {
      for (Entry<String, String> entry : uriParameters.entrySet())
      {
        final String rawParameterName = entry.getKey();
        final String rawParameterValue = entry.getValue();
        if (rawParameterValue == null)
        {
          if (log.isErrorEnabled())
          {
            log.error("Could not encoce the URI parameter with key '" + rawParameterName + "' because its value is null");
          }
        }
        if (first == true)
        {
          buffer.append(alreadyContainsQuestionMark == false ? "?" : "&");
          first = false;
        }
        else
        {
          buffer.append("&");
        }
        try
        {
          if (rawParameterValue.length() <= 0)
          {
            // The value is empty, and in that case we use the key as a pair URI parameter name+value
            buffer.append(rawParameterName);
          }
          else
          {
            buffer.append(rawParameterName).append("=").append(URLEncoder.encode(rawParameterValue, urlEnconding));
          }
        }
        catch (UnsupportedEncodingException exception)
        {
          if (log.isErrorEnabled())
          {
            log.error("Cannot encode properly the URI", exception);
          }
          return null;
        }
      }
    }
    final String uri = buffer.toString();
    return uri;
  }

}