/*
 * (C) Copyright 2009-2016 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;

/**
 * * A basis class for making web service calls easier.
 * <p/>
 * <p>
 * When invoking an HTTP method, the caller goes through the following workflow:
 * <ol>
 * <li>the {@link #isConnected()} method is checked: if the return value is set to {@code false}, no request will be attempted and a
 * {@link WebServiceCaller.CallException} exception will be thrown (embedding a {@link UnknownHostException} exception) ;</li>
 * <li>the {@link #onBeforeHttpRequestExecution(URL, HttpURLConnection, CallType)} method will be invoked, so as to let the caller tune the HTTP
 * method request ;</li>
 * <li>if a connection issue arises (connection time-out, socket time-out, lost of connectivity), a {@link WebServiceCaller.CallException} exception
 * will be thrown, and it will {@link Throwable#getCause() embed} the reason for the connection issue ;</li>
 * <li>if the status code of the HTTP response does not belong to the [{@link HttpStatus#SC_OK}, {@link HttpStatus#SC_MULTI_STATUS}] range, the
 * {@link #onStatusCodeNotOk(String, CallType, Map, HttpURLConnection, URL, int, String, int)} method will be invoked.</li>
 * </ol>
 * </p>
 *
 * @author Ludovic Roland
 * @since 2016.01.28
 */
public abstract class URLConnectionWebServiceCaller
    extends WebServiceCaller
{

  public static final class URLConnectionMultipartFile
  {

    public final String name;

    public final String fileName;

    public final String contentType;

    public final FileInputStream fileInputStream;

    public URLConnectionMultipartFile(String name, String fileName, String contentType, FileInputStream fileInputStream)
    {
      this.name = name;
      this.fileName = fileName;
      this.contentType = contentType;
      this.fileInputStream = fileInputStream;
    }

  }

  private final static String BOUNDARY = "----URLConnectionWebServiceCaller";

  protected final static Logger log = LoggerFactory.getInstance(URLConnectionWebServiceCaller.class);

  protected abstract int getReadTimeout();

  protected abstract int getConnectTimeout();

  /**
   * Equivalent to calling {@link #getInputStream(String, CallType, Map)} with {@code callType} parameter set to
   * {@code CallType.Get} and {@code body} parameter set to {@code null}.
   *
   * @see #getInputStream(String, CallType, HttpEntity)
   */
  public final InputStream getInputStream(String uri)
      throws CallException
  {
    return getInputStream(uri, CallType.Get, (Map<String, String>) null);
  }

  @Override
  public final InputStream getInputStream(String uri, CallType callType, HttpEntity body)
      throws CallException
  {
    return null;
  }

  /**
   * Equivalent to calling {@link #getInputStream(String, CallType, Map, Map, List)} with {@code headers} and the {@code fileinputstream} parameters set to {@code null}.
   *
   * @see #getInputStream(String, CallType, Map, Map, List)
   */
  @Override
  public final InputStream getInputStream(String uri, CallType callType, Map<String, String> postParameters)
      throws CallException
  {
    return getInputStream(uri, callType, null, postParameters, null);
  }

  /**
   * Performs an HTTP request corresponding to the provided parameters.
   *
   * @param uri            the URI being requested
   * @param callType       the HTTP method
   * @param postParameters if the HTTP method is set to {@link CallType#Post} or {@link CallType#Put}, this is the body of the
   *                       request
   * @param headers        the headers of the HTTP request
   * @return the input stream of the HTTP method call; cannot be {@code null}
   * @throws CallException if the status code of the HTTP response does not belong to the [{@link HttpStatus#SC_OK}, {@link HttpStatus#SC_MULTI_STATUS}] range.
   *                       Also if a connection issue occurred: the exception will {@link Throwable#getCause() embed} the cause of the exception. If the
   *                       {@link #isConnected()} method returns {@code false}, no request will be attempted and a {@link CallException}
   *                       exception will be thrown (embedding a {@link UnknownHostException} exception).
   * @see #getInputStream(String)
   * @see #getInputStream(String, CallType, Map)
   */
  public final InputStream getInputStream(String uri, CallType callType, Map<String, String> headers,
      Map<String, String> postParameters, List<URLConnectionMultipartFile> files)
      throws CallException
  {
    HttpURLConnection httpURLConnection = null;

    try
    {
      httpURLConnection = performHttpRequest(uri, callType, headers, postParameters, files);
      return getContent(uri, callType, httpURLConnection);
    }
    catch (CallException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      throw new CallException(exception);
    }
    finally
    {
      if (httpURLConnection != null)
      {
        httpURLConnection.disconnect();
      }
    }
  }

  /**
   * Invoked when the result of the HTTP request is not <code>20X</code>. The default implementation logs the problem and throws an exception.
   *
   * @param uri               the URI of the HTTP call
   * @param callType          the type of the HTTP method
   * @param postParameters    the body of the HTTP method when its a {@link CallType#Post} or a {@link CallType#Put} ; {@code null}
   *                          otherwise
   * @param httpURLConnection the HttpURLConnection object
   * @param url               the URL object
   * @param statusCode        the status code of the response, which is not <code>20X</code>
   * @param attemptsCount     the number of attempts that have been run for this HTTP method. Starts at <code>1</code>
   * @return {@code true} if you want the request to be re-run if it has failed
   * @throws CallException if you want the call to be considered as not OK
   */
  protected boolean onStatusCodeNotOk(String uri, CallType callType, Map<String, String> postParameters,
      HttpURLConnection httpURLConnection, URL url, int statusCode, String statusMessage, int attemptsCount)
      throws CallException
  {
    final String message = "The result code of the call to the web method '" + uri + "' is not OK (not 20X). Status: " + (statusMessage != null ? statusMessage : "") + " (" + statusCode + ")";

    if (log.isErrorEnabled() == true)
    {
      log.error(message);
    }

    // We make sure that the response body be consumed: read http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html, chapter
    // 1.1.5 on that purpose. Without this consumption, the connection belonging to the pool may hang at next call!
    // An interesting discussion is also available at
    // http://stackoverflow.com/questions/9505358/android-httpclient-hangs-on-second-request-to-the-server-connection-timed-out
    try
    {
      httpURLConnection.disconnect();
    }
    catch (Exception exception)
    {
      // We cannot do much ;(
      if (log.isErrorEnabled())
      {
        log.error("Cannot release the not OK request corresponding to the web method '" + uri + "'", exception);
      }
    }

    throw new CallException(message, statusCode);
  }

  /**
   * This is the perfect place for customizing the HTTP request that is bound to be run.
   *
   * @param url               the URL object that will run the HTTP request
   * @param httpURLConnection the HttpURLConnection object
   * @param callType          the type of the HTTP method
   * @throws CallException in case the HTTP request cannot be eventually invoked properly
   */
  protected void onBeforeHttpRequestExecution(URL url, HttpURLConnection httpURLConnection, CallType callType)
      throws CallException
  {
  }

  /**
   * Invoked on every call, in order to extract the input stream from the response.
   * <p/>
   * <p>
   * If the content type is gzipped, this is the ideal place for unzipping it.
   * </p>
   *
   * @param uri           the web call initial URI
   * @param callType      the kind of request
   * @param urlConnection the HttpURLConnection object
   * @return the (decoded) input stream of the response
   * @throws IOException if some exception occurred while extracting the content of the response
   */
  protected InputStream getContent(String uri, CallType callType, HttpURLConnection urlConnection)
      throws IOException
  {
    final InputStream content = urlConnection.getInputStream();

    final InputStream debugContent;
    final int length = (int) (urlConnection.getContentLength() <= WebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES ? urlConnection.getContentLength() : WebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES);

    if (content.markSupported() == true)
    {
      debugContent = content;
    }
    else
    {
      final int bufferMaxLength = (int) (length < 0 ? WebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES : length);
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      final byte[] buffer = new byte[8192];
      int bufferLength = 0;

      try
      {
        while ((bufferLength = content.read(buffer)) > 0 && bufferLength <= bufferMaxLength)
        {
          outputStream.write(buffer, 0, bufferLength);
        }
      }
      catch (IndexOutOfBoundsException exception)
      {
        if (log.isWarnEnabled())
        {
          log.error("Could not copy the input stream corresponding to the HTTP response content in order to log it", exception);
        }

        return content;
      }

      try
      {
        content.close();
      }
      catch (IOException exception)
      {
        if (log.isWarnEnabled())
        {
          log.error("Could not close the input stream corresponding to the HTTP response content", exception);
        }
      }

      try
      {
        outputStream.close();
      }
      catch (IOException exception)
      {
        if (log.isWarnEnabled())
        {
          log.error("Could not close the input stream corresponding to the copy of the HTTP response content", exception);
        }
      }

      debugContent = new ByteArrayInputStream(outputStream.toByteArray());
    }

    if (WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
    {
      try
      {
        debugContent.mark(length);
        final String bodyAsString = getString(debugContent);
        log.debug("The body of the HTTP response corresponding to the URI '" + uri + "' is : '" + bodyAsString + "'");
      }
      catch (IOException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot log the HTTP body of the response", exception);
        }
      }
      finally
      {
        debugContent.reset();
      }
    }

    return debugContent;
  }

  /**
   * Is responsible for returning an HTTP client instance, used for actually running the HTTP requests.
   * <p/>
   * <p>
   * The current implementation returns a {@link HttpURLConnection} instance.
   * </p>
   *
   * @return a valid HTTP client
   * @throws CallException is the uri is {@code null} or the connectivity has been lost
   */
  private HttpURLConnection performHttpRequest(String uri, CallType callType, Map<String, String> headers,
      Map<String, String> postParamaters, List<URLConnectionMultipartFile> files, int attemptsCount)
      throws IOException, CallException
  {
    if (uri == null)
    {
      throw new CallException("Cannot perform an HTTP request with a null URI!");
    }
    if (isConnected == false)
    {
      throw new CallException(new UnknownHostException("No connectivity"));
    }

    final StringBuilder logBuilder = new StringBuilder();
    final URL url = new URL(uri);
    final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    onBeforeHttpRequestExecution(url, httpURLConnection, callType);

    if ((callType.verb == Verb.Post || callType.verb == Verb.Put) && files != null && files.size() > 0)
    {
      httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + URLConnectionWebServiceCaller.BOUNDARY);
    }

    httpURLConnection.setReadTimeout(getReadTimeout());
    httpURLConnection.setConnectTimeout(getConnectTimeout());
    httpURLConnection.setDoInput(true);

    switch (callType.verb)
    {
    default:
    case Get:
      httpURLConnection.setRequestMethod("GET");
      break;
    case Post:
      httpURLConnection.setRequestMethod("POST");
      httpURLConnection.setDoOutput(true);
      break;
    case Put:
      httpURLConnection.setRequestMethod("PUT");
      httpURLConnection.setDoOutput(true);
      break;
    case Delete:
      httpURLConnection.setRequestMethod("DELETE");
      break;
    }

    if (headers != null && headers.size() > 0)
    {
      for (final Map.Entry<String, String> header : headers.entrySet())
      {
        httpURLConnection.setRequestProperty(header.getKey(), header.getValue());
      }
    }

    if (callType.verb == Verb.Post || callType.verb == Verb.Put)
    {
      final OutputStream outputStream = httpURLConnection.getOutputStream();
      final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, getContentEncoding()));

      if (postParamaters != null)
      {
        for (final Entry<String, String> parameter : postParamaters.entrySet())
        {
          if (log.isDebugEnabled() == true && WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
          {
            logBuilder.append("\n--" + URLConnectionWebServiceCaller.BOUNDARY);
            logBuilder.append("\nContent-Disposition: form-data; name=\"" + parameter.getKey() + "\"");
            logBuilder.append("\n\n");
            logBuilder.append(parameter.getValue());
          }

          bufferedWriter.write("\n--" + URLConnectionWebServiceCaller.BOUNDARY);
          bufferedWriter.write("\nContent-Disposition: form-data; name=\"" + parameter.getKey() + "\"");
          bufferedWriter.write("\n\n");
          bufferedWriter.write(parameter.getValue());
        }
      }

      if (files != null)
      {
        for (final URLConnectionMultipartFile file : files)
        {
          if (log.isDebugEnabled() == true && WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
          {
            logBuilder.append("\n--" + URLConnectionWebServiceCaller.BOUNDARY);
            logBuilder.append("\nContent-Disposition: form-data; name=\"" + file.name + "\"; filename=\"" + file.fileName + "\"");
            logBuilder.append("\nContent-Type: " + file.contentType);
            logBuilder.append("\n\n");
          }

          bufferedWriter.write("\n--" + URLConnectionWebServiceCaller.BOUNDARY);
          bufferedWriter.write("\nContent-Disposition: form-data; name=\"" + file.name + "\"; filename=\"" + file.fileName + "\"");
          bufferedWriter.write("\nContent-Type: " + file.contentType);
          bufferedWriter.write("\n\n");
          bufferedWriter.flush();

          if (file.fileInputStream != null)
          {
            int bytesRead;
            final byte[] dataBuffer = new byte[1024];

            while ((bytesRead = file.fileInputStream.read(dataBuffer)) != -1)
            {
              outputStream.write(dataBuffer, 0, bytesRead);
            }

            outputStream.flush();
          }
        }
      }

      bufferedWriter.write("\n--" + URLConnectionWebServiceCaller.BOUNDARY + "--\n");
      bufferedWriter.flush();

      outputStream.close();
      bufferedWriter.close();
    }

    if (log.isDebugEnabled() == true)
    {
      final StringBuilder sb = new StringBuilder();
      final StringBuilder curlSb = new StringBuilder();
      boolean logCurlCommand = false;

      if (WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
      {
        try
        {
          curlSb.append("\n>> ").append("curl --request ").append(callType.toString().toUpperCase()).append(" \"").append(uri).append("\"");

          if (logBuilder != null && "".equals(logBuilder.toString()) == false)
          {
            sb.append(" with body '").append(logBuilder.toString()).append("'");
            curlSb.append(" --data \"").append(logBuilder.toString()).append("\"");

            for (final Map.Entry<String, List<String>> header : httpURLConnection.getRequestProperties().entrySet())
            {
              for (final String headerValue : header.getValue())
              {
                curlSb.append(" --header \"").append(header.getKey()).append(": ").append(headerValue.replace("\"", "\\\"")).append("\"");
              }
            }
          }
          else
          {
            logCurlCommand = true;
          }
        }
        catch (Exception exception)
        {
          // We simply ignore the issue because it is only a debug feature
        }
      }

      log.debug("Running the HTTP " + callType + " request '" + uri + "'" + sb.toString() + (logCurlCommand == true ? curlSb.toString() : ""));
    }

    final long start = System.currentTimeMillis();
    httpURLConnection.connect();
    final int responseCode = httpURLConnection.getResponseCode();
    final String responseMessage = httpURLConnection.getResponseMessage();
    final StringBuilder responseHeadersSb = new StringBuilder();

    if (WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
    {
      for (final Map.Entry<String, List<String>> header : httpURLConnection.getHeaderFields().entrySet())
      {
        for (final String headerValue : header.getValue())
        {
          if (responseHeadersSb.length() > 0)
          {
            responseHeadersSb.append(",");
          }

          responseHeadersSb.append("(\"").append(header.getKey()).append(": ").append(headerValue.replace("\"", "\\\"")).append("\")");
        }
      }
    }

    if (log.isDebugEnabled() == true)
    {
      log.debug("The call to the HTTP " + callType + " request '" + uri + "' took " + (System.currentTimeMillis() - start) + " ms and returned the status code " + responseCode + (responseHeadersSb.length() <= 0 ? "" : " with the HTTP headers:" + responseHeadersSb.toString()));
    }

    if (!(responseCode >= HttpStatus.SC_OK && responseCode <= HttpStatus.SC_MULTI_STATUS))
    {
      if (onStatusCodeNotOk(uri, callType, postParamaters, httpURLConnection, url, responseCode, responseMessage, attemptsCount + 1) == true)
      {
        return performHttpRequest(uri, callType, headers, postParamaters, files, attemptsCount + 1);
      }
    }

    return httpURLConnection;
  }

  private HttpURLConnection performHttpRequest(String uri, CallType callType, Map<String, String> headers,
      Map<String, String> postParameters, List<URLConnectionMultipartFile> files)
      throws IOException, CallException
  {
    return performHttpRequest(uri, callType, headers, postParameters, files, 0);
  }

}