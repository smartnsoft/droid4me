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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import android.support.annotation.Nullable;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * * A basis class for making web service calls easier.
 * <p>
 * <p>
 * When invoking an HTTP method, the caller goes through the following workflow:
 * </p>
 * <ol>
 * <li>the {@link #isConnected()} method is checked: if the return value is set to {@code false}, no request will be attempted and a
 * {@link WebServiceCaller.CallException} exception will be thrown (embedding a {@link UnknownHostException} exception) ;</li>
 * <li>the {@link #onBeforeHttpRequestExecution(URL, HttpURLConnection, CallType)} method will be invoked, so as to let the caller tune the HTTP
 * method request ;</li>
 * <li>if a connection issue arises (connection time-out, socket time-out, lost of connectivity), a {@link WebServiceCaller.CallException} exception
 * will be thrown, and it will {@link Throwable#getCause() embed} the reason for the connection issue ;</li>
 * <li>if the status code of the HTTP response does not belong to the [{@link HttpURLConnection#HTTP_OK}, {@link HttpURLConnection#HTTP_MULT_CHOICE}] range, the
 * {@link #onStatusCodeNotOk(String, CallType, Map, String, HttpURLConnection, URL, int, String, int)} method will be invoked.</li>
 * </ol>
 *
 * @author Ludovic Roland
 * @since 2016.01.28
 */
public abstract class URLConnectionWebServiceCaller
    extends WebServiceCaller
{

  protected final static Logger log = LoggerFactory.getInstance(URLConnectionWebServiceCaller.class);

  private final static String BOUNDARY = "URLConnectionWebServiceCaller";

  private final static String HYPHEN_HYPHEN = "--";

  private final static String NEW_LINE = "\r\n";

  private final int readTimeOutInMilliseconds;

  private final int connectTimeOutInMilliseconds;

  private final boolean acceptGzip;

  protected URLConnectionWebServiceCaller(int readTimeOutInMilliseconds, int connectTimeOutInMilliseconds,
      boolean acceptGzip)
  {
    this.readTimeOutInMilliseconds = readTimeOutInMilliseconds;
    this.connectTimeOutInMilliseconds = connectTimeOutInMilliseconds;
    this.acceptGzip = acceptGzip;
  }

  /**
   * Equivalent to calling {@link #runRequest(String, CallType, Map, String)} with {@code callType} parameter set to
   * {@code CallType.Get} and {@code body} and {@code parameters} parameters set to {@code null}.
   *
   * @see #runRequest(String, CallType, Map, String)
   */
  @Override
  public final HttpResponse runRequest(String uri)
      throws CallException
  {
    return runRequest(uri, CallType.Get, null, null);
  }

  /**
   * Equivalent to calling {@link #runRequest(String, CallType, Map, Map, String, List)} with {@code headers} and the {@code file} parameters set to {@code null}.
   *
   * @see #runRequest(String, CallType, Map, Map, String, List)
   */
  @Override
  public final HttpResponse runRequest(String uri, CallType callType, Map<String, String> parameters, String body)
      throws CallException
  {
    return runRequest(uri, callType, null, parameters, body, null);
  }

  /**
   * Performs an HTTP request corresponding to the provided parameters.
   *
   * @param uri        the URI being requested
   * @param callType   the HTTP method
   * @param headers    the headers of the HTTP request
   * @param parameters if the HTTP method is set to {@link CallType#Post} or {@link CallType#Put}, this is the form data of the
   *                   request
   * @param body       if the HTTP method is set to {@link CallType#Post} or {@link CallType#Put}, this is the string body of the
   *                   request
   * @param files      if the HTTP method is set to {@link CallType#Post} or {@link CallType#Put}, this is the file data of the
   *                   request
   * @return the input stream of the HTTP method call; cannot be {@code null}
   * @throws CallException if the status code of the HTTP response does not belong to the [{@link HttpURLConnection#HTTP_OK}, {@link HttpURLConnection#HTTP_MULT_CHOICE}] range.
   *                       Also if a connection issue occurred: the exception will {@link Throwable#getCause() embed} the cause of the exception. If the
   *                       {@link #isConnected()} method returns {@code false}, no request will be attempted and a {@link CallException}
   *                       exception will be thrown (embedding a {@link UnknownHostException} exception).
   * @see #runRequest(String)
   * @see #runRequest(String, CallType, Map, String)
   */
  @Override
  public HttpResponse runRequest(String uri, CallType callType, Map<String, String> headers,
      Map<String, String> parameters, String body, List<MultipartFile> files)
      throws CallException
  {
    HttpURLConnection httpURLConnection = null;

    try
    {
      httpURLConnection = performHttpRequest(uri, callType, headers, parameters, body, files);
      final Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
      final int statusCode = httpURLConnection.getResponseCode();

      InputStream inputStream = null;
      InputStream errorInputStream = null;

      if (shouldTryToConsumeErrorInputstream(statusCode, uri, callType, headers, parameters, body, files) == true)
      {
        errorInputStream = getContent(uri, callType, httpURLConnection, true);
      }
      else
      {
        inputStream = getContent(uri, callType, httpURLConnection, false);
      }

      return new HttpResponse(headerFields, statusCode, inputStream, errorInputStream);
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

  protected int getReadTimeout()
  {
    return readTimeOutInMilliseconds;
  }

  protected int getConnectTimeout()
  {
    return connectTimeOutInMilliseconds;
  }

  /**
   * Invoked when the result of the HTTP request is not <code>20X</code>. The default implementation logs the problem and throws an exception.
   *
   * @param uri               the URI of the HTTP call
   * @param callType          the type of the HTTP method
   * @param postParameters    the body (as form-data fields) of the HTTP method when its a {@link CallType#Post} or a {@link CallType#Put} ; {@code null}
   *                          otherwise
   * @param headers           the original headers of the request. They can be edited in order to manage some scenarios with tokens
   * @param body              the body (as form-data fields) of the HTTP method when its a {@link CallType#Post} or a {@link CallType#Put} ; {@code null}
   *                          otherwise
   * @param httpURLConnection the HttpURLConnection object
   * @param url               the URL object
   * @param statusCode        the status code of the response, which is not <code>20X</code>
   * @param attemptsCount     the number of attempts that have been run for this HTTP method. Starts at <code>1</code>
   * @return {@code true} if you want the request to be re-run if it has failed
   * @throws CallException if you want the call to be considered as not OK
   */
  protected boolean onStatusCodeNotOk(String uri, CallType callType, Map<String, String> postParameters,
      Map<String, String> headers, String body,
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
    if (acceptGzip == true)
    {
      httpURLConnection.setRequestProperty("Accept-Encoding", "gzip");
    }
  }

  /**
   * Invoked on every call, in order to extract the input stream from the response.
   * <p>
   * <p>
   * If the content type is gzipped, this is the ideal place for unzipping it.
   * </p>
   *
   * @param uri           the web call initial URI
   * @param callType      the kind of request
   * @param urlConnection the {@link HttpURLConnection} object
   * @return the (decoded) input stream of the response or null if the {@link CallType} <code>Verb.Head</code>
   * @throws IOException if some exception occurred while extracting the content of the response
   */
  protected InputStream getContent(String uri, CallType callType, HttpURLConnection urlConnection,
      boolean consumeErrorInputStream)
      throws IOException
  {
    final String encoding = urlConnection.getHeaderField("Content-Encoding");

    if ("gzip".equals(encoding) == true)
    {
      return getGzipContent(uri, callType, urlConnection, consumeErrorInputStream);
    }

    return getNotGzipContent(uri, callType, urlConnection, consumeErrorInputStream);
  }

  @Nullable
  protected InputStream getNotGzipContent(String uri, CallType callType, HttpURLConnection urlConnection,
      boolean consumeErrorInputStream)
      throws IOException
  {
    if (callType.verb != Verb.Head)
    {
      final InputStream content = consumeErrorInputStream == false ? urlConnection.getInputStream() : urlConnection.getErrorStream();
      final InputStream markedContent;
      int length = 0;

      if (content.markSupported() == true)
      {
        markedContent = content;
        length = urlConnection.getContentLength();
      }
      else
      {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int bufferLength = 0;

        try
        {
          while ((bufferLength = content.read(buffer)) > 0)
          {
            length += bufferLength;
            outputStream.write(buffer, 0, bufferLength);
          }
        }
        catch (IndexOutOfBoundsException exception)
        {
          if (log.isWarnEnabled())
          {
            log.error("Could not copy the input stream corresponding to the HTTP response content", exception);
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
            log.error("Could not close the input stream corresponding to the copy of the HTTP response content",
                exception);
          }
        }

        markedContent = new ByteArrayInputStream(outputStream.toByteArray());
      }

      if (WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
      {
        try
        {
          markedContent.mark(length);
          final String bodyAsString = getString(markedContent);
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
          markedContent.reset();
        }
      }

      return markedContent;
    }

    return null;
  }

  @Nullable
  protected InputStream getGzipContent(String uri, CallType callType, HttpURLConnection urlConnection,
      boolean consumeErrorInputStream)
      throws IOException
  {
    final GZIPInputStream gzipInputStream = new GZIPInputStream(consumeErrorInputStream == false ? urlConnection.getInputStream() : urlConnection.getErrorStream());
    final InputStream markedContent;
    int length = 0;

    if (gzipInputStream.markSupported() == true)
    {
      markedContent = gzipInputStream;
      length = urlConnection.getContentLength();
    }
    else
    {
      final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      final byte[] buffer = new byte[8192];
      int bufferLength = 0;

      try
      {
        while ((bufferLength = gzipInputStream.read(buffer)) > 0)
        {
          length += bufferLength;
          outputStream.write(buffer, 0, bufferLength);
        }
      }
      catch (IndexOutOfBoundsException exception)
      {
        if (log.isWarnEnabled())
        {
          log.error("Could not copy the input stream corresponding to the HTTP response contentHtml", exception);
        }

        return gzipInputStream;
      }

      try
      {
        gzipInputStream.close();
      }
      catch (IOException exception)
      {
        if (log.isWarnEnabled())
        {
          log.error("Could not close the input stream corresponding to the HTTP response contentHtml", exception);
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
          log.error("Could not close the input stream corresponding to the copy of the HTTP response contentHtml", exception);
        }
      }

      markedContent = new ByteArrayInputStream(outputStream.toByteArray());
    }

    if (WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
    {
      try
      {
        markedContent.mark(length);
        final String bodyAsString = getString(markedContent);
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
        markedContent.reset();
      }
    }

    return markedContent;
  }

  protected HttpURLConnection performHttpRequest(String uri, CallType callType, Map<String, String> headers,
      Map<String, String> parameters, String body, List<MultipartFile> files)
      throws IOException, CallException
  {
    return performHttpRequest(uri, callType, headers, parameters, body, files, 0);
  }

  protected boolean shouldTryToConsumeErrorInputstream(int statusCode, String uri, CallType callType,
      Map<String, String> headers,
      Map<String, String> parameters, String body, List<MultipartFile> files)
  {
    return false;
  }

  /**
   * Is responsible for returning an HTTP client instance, used for actually running the HTTP requests.
   * <p>
   * <p>
   * The current implementation returns a {@link HttpURLConnection} instance.
   * </p>
   *
   * @return a valid HTTP client
   * @throws CallException is the uri is {@code null} or the connectivity has been lost
   */
  private HttpURLConnection performHttpRequest(String uri, CallType callType, Map<String, String> headers,
      Map<String, String> paramaters, String body, List<MultipartFile> files, int attemptsCount)
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

    if (callType.verb == Verb.Post || callType.verb == Verb.Put)
    {
      if (files != null && files.size() > 0)
      {
        httpURLConnection.setRequestProperty("Content-Type",
            "multipart/form-data; boundary=" + URLConnectionWebServiceCaller.BOUNDARY);
      }
      else
      {
        httpURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      }
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
      case Head:
        httpURLConnection.setRequestMethod("HEAD");
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

    if (log.isDebugEnabled() == true && WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
    {
      try
      {
        if (callType.verb == Verb.Post || callType.verb == Verb.Put)
        {
          if (files != null && files.size() > 0)
          {
            if (paramaters != null && paramaters.size() > 0)
            {
              for (final Entry<String, String> parameter : paramaters.entrySet())
              {
                logBuilder.append(
                    " " + URLConnectionWebServiceCaller.HYPHEN_HYPHEN + URLConnectionWebServiceCaller.BOUNDARY);
                logBuilder.append(" Content-Disposition: form-data; name=\"" + parameter.getKey() + "\"");
                logBuilder.append(" " + parameter.getValue());
              }
            }

            for (final MultipartFile file : files)
            {
              logBuilder.append(
                  " " + URLConnectionWebServiceCaller.HYPHEN_HYPHEN + URLConnectionWebServiceCaller.BOUNDARY);
              logBuilder.append(
                  " Content-Disposition: form-data; name=\"" + file.name + "\"; filename=\"" + file.fileName + "\"");
              logBuilder.append(" Content-Type: " + file.contentType);
            }
          }
          else
          {
            logBuilder.append(transformPostParametersToDataString(paramaters));
          }
        }
        else if (paramaters != null && paramaters.size() > 0)
        {
          logBuilder.append(transformPostParametersToDataString(paramaters));
        }

        //headers and curl request
        final StringBuilder sb = new StringBuilder();
        final StringBuilder curlSb = new StringBuilder();
        boolean logCurlCommand = false;

        try
        {
          curlSb.append("\n>> ").append("curl --request ").append(callType.toString().toUpperCase()).append(
              " \"").append(uri).append("\"");

          if (logBuilder != null && "".equals(logBuilder.toString()) == false)
          {
            logCurlCommand = true;

            sb.append(" with body '").append(logBuilder.toString()).append("'");
            curlSb.append(" --data \"").append(logBuilder.toString()).append("\"");

            for (final Map.Entry<String, List<String>> header : httpURLConnection.getRequestProperties().entrySet())
            {
              for (final String headerValue : header.getValue())
              {
                curlSb.append(" --header \"").append(header.getKey()).append(": ").append(
                    headerValue.replace("\"", "\\\"")).append("\"");
              }
            }
          }
        }
        catch (Exception exception)
        {
          // We simply ignore the issue because it is only a debug feature
        }

        log.debug(
            "Running the HTTP " + callType + " request '" + uri + "'" + sb.toString() + (logCurlCommand == true ? curlSb.toString() : ""));
      }
      catch (Exception exception)
      {
        // We simply ignore the issue because it is only a debug feature
      }
    }

    if (callType.verb == Verb.Post || callType.verb == Verb.Put)
    {
      //This a form with a file
      if (files != null && files.size() > 0)
      {
        final DataOutputStream outputStream = new DataOutputStream(httpURLConnection.getOutputStream());

        if (paramaters != null && paramaters.size() > 0)
        {
          for (final Entry<String, String> parameter : paramaters.entrySet())
          {
            outputStream.writeBytes(
                URLConnectionWebServiceCaller.HYPHEN_HYPHEN + URLConnectionWebServiceCaller.BOUNDARY);
            outputStream.writeBytes(
                URLConnectionWebServiceCaller.NEW_LINE + "Content-Disposition: form-data; name=\"" + parameter.getKey() + "\"");
            outputStream.writeBytes(URLConnectionWebServiceCaller.NEW_LINE + URLConnectionWebServiceCaller.NEW_LINE);
            outputStream.write(parameter.getValue().getBytes(getContentEncoding()));
            outputStream.writeBytes(URLConnectionWebServiceCaller.NEW_LINE);
            outputStream.flush();
          }
        }

        for (final MultipartFile file : files)
        {
          outputStream.writeBytes(URLConnectionWebServiceCaller.HYPHEN_HYPHEN + URLConnectionWebServiceCaller.BOUNDARY);
          outputStream.writeBytes(
              URLConnectionWebServiceCaller.NEW_LINE + "Content-Disposition: form-data; name=\"" + file.name + "\"; filename=\"" + file.fileName + "\"");
          outputStream.writeBytes(URLConnectionWebServiceCaller.NEW_LINE + "Content-Type: " + file.contentType);
          outputStream.writeBytes(URLConnectionWebServiceCaller.NEW_LINE + URLConnectionWebServiceCaller.NEW_LINE);
          outputStream.flush();

          if (file.inputStream != null)
          {
            int bytesRead;
            final byte[] dataBuffer = new byte[1024];

            while ((bytesRead = file.inputStream.read(dataBuffer)) != -1)
            {
              outputStream.write(dataBuffer, 0, bytesRead);
            }

            outputStream.writeBytes(URLConnectionWebServiceCaller.NEW_LINE);
            outputStream.flush();
          }
        }

        outputStream.writeBytes(
            URLConnectionWebServiceCaller.NEW_LINE + URLConnectionWebServiceCaller.HYPHEN_HYPHEN + URLConnectionWebServiceCaller.BOUNDARY + URLConnectionWebServiceCaller.HYPHEN_HYPHEN + URLConnectionWebServiceCaller.NEW_LINE);
        outputStream.writeBytes(URLConnectionWebServiceCaller.NEW_LINE);
        outputStream.flush();
        outputStream.close();
      }
      else if (paramaters != null && paramaters.size() > 0)
      {
        body = transformPostParametersToDataString(paramaters);
      }

      if ("".equals(body) == false && body != null)
      {
        final OutputStream outputStream = httpURLConnection.getOutputStream();
        final BufferedWriter bufferedWriter = new BufferedWriter(
            new OutputStreamWriter(outputStream, getContentEncoding()));
        bufferedWriter.write(body);
        bufferedWriter.flush();
        bufferedWriter.close();
        outputStream.close();
      }
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

          responseHeadersSb.append("(\"").append(header.getKey()).append(": ").append(
              headerValue.replace("\"", "\\\"")).append("\")");
        }
      }
    }

    if (log.isDebugEnabled() == true)
    {
      log.debug(
          "The call to the HTTP " + callType + " request '" + uri + "' took " + (System.currentTimeMillis() - start) + " ms and returned the status code " + responseCode + (responseHeadersSb.length() <= 0 ? "" : " with the HTTP headers:" + responseHeadersSb.toString()));
    }

    if (!(responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE))
    {
      if (onStatusCodeNotOk(uri, callType, paramaters, headers, body, httpURLConnection, url, responseCode, responseMessage,
          attemptsCount + 1) == true)
      {
        return performHttpRequest(uri, callType, headers, paramaters, body, files, attemptsCount + 1);
      }
    }

    return httpURLConnection;
  }

  private String transformPostParametersToDataString(Map<String, String> params)
      throws UnsupportedEncodingException
  {
    final StringBuilder bodyBuilder = new StringBuilder();
    boolean first = true;

    for (final Map.Entry<String, String> entry : params.entrySet())
    {
      if (first == true)
      {
        first = false;
      }
      else
      {
        bodyBuilder.append("&");
      }

      bodyBuilder.append(URLEncoder.encode(entry.getKey(), getContentEncoding()));
      bodyBuilder.append("=");
      bodyBuilder.append(URLEncoder.encode(entry.getValue(), getContentEncoding()));
    }

    return bodyBuilder.toString();
  }

}