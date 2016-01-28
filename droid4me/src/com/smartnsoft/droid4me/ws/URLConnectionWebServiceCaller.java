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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import org.apache.http.HttpStatus;

/**
 * @author Ludovic Roland
 * @since 2016.01.28
 */
public abstract class URLConnectionWebServiceCaller
    extends WebServiceCaller
{

  protected final static Logger log = LoggerFactory.getInstance(URLConnectionWebServiceCaller.class);

  protected abstract int getReadTimeout();

  protected abstract int getConnecTimeout();

  public final InputStream getInputStream(String uri)
      throws CallException
  {
    return getInputStream(uri, CallType.Get, (String) null);
  }

  @Override
  public final InputStream getInputStream(String uri, CallType callType, String body)
      throws CallException
  {
    HttpURLConnection httpURLConnection = null;

    try
    {
      httpURLConnection = performHttpRequest(uri, callType, body);
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

  protected boolean onStatusCodeNotOk(String uri, CallType callType, String body, HttpURLConnection httpURLConnection,
      URL url, int statusCode, String statusMessage, int attemptsCount)
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

  protected void onBeforeHttpRequestExecution(URL url, HttpURLConnection httpURLConnection, CallType callType)
      throws CallException
  {
  }

  protected InputStream getContent(String uri, CallType callType, HttpURLConnection urlConnection)
      throws IOException
  {
    final InputStream content = urlConnection.getInputStream();

    if (URLConnectionWebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
    {
      final InputStream debugContent;
      final int length = (int) (urlConnection.getContentLength() <= URLConnectionWebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES ? urlConnection.getContentLength() : URLConnectionWebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES);

      if (content.markSupported() == true)
      {
        debugContent = content;
      }
      else
      {
        final int bufferMaxLength = (int) (length < 0 ? URLConnectionWebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES : length);
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

      return debugContent;
    }

    return content;
  }

  private HttpURLConnection performHttpRequest(String uri, CallType callType, String body, int attemptsCount)
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

    final URL url = new URL(uri);
    final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
    httpURLConnection.setReadTimeout(getReadTimeout());
    httpURLConnection.setConnectTimeout(getConnecTimeout());
    httpURLConnection.setDoInput(true);

    switch (callType.verb)
    {
    default:
    case Get:
      httpURLConnection.setRequestMethod("GET");
      break;
    case Post:
      httpURLConnection.setRequestMethod("POST");
      break;
    case Put:
      httpURLConnection.setRequestMethod("PUT");
      break;
    case Delete:
      httpURLConnection.setRequestMethod("DELETE");
      break;
    }

    if ((callType.verb == Verb.Post || callType.verb == Verb.Put) && body != null)
    {
      final OutputStream outputStream = httpURLConnection.getOutputStream();
      final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, getContentEncoding()));
      bufferedWriter.write(body);
      bufferedWriter.flush();
      bufferedWriter.close();
      outputStream.close();
    }

    onBeforeHttpRequestExecution(url, httpURLConnection, callType);

    if (log.isDebugEnabled() == true)
    {
      final StringBuilder sb = new StringBuilder();
      final StringBuilder curlSb = new StringBuilder();
      boolean logCurlCommand = false;

      if (URLConnectionWebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
      {
        try
        {
          curlSb.append("\n>> ").append("curl --request ").append(callType.toString().toUpperCase()).append(" \"").append(uri).append("\"");

          if (body != null)
          {
            sb.append(" with body '").append(body).append("'");
            curlSb.append(" --data \"").append(body).append("\"");

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

    if (URLConnectionWebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
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
      if (onStatusCodeNotOk(uri, callType, body, httpURLConnection, url, responseCode, responseMessage, attemptsCount + 1) == true)
      {
        return performHttpRequest(uri, callType, body, attemptsCount + 1);
      }
    }

    return httpURLConnection;
  }

  private HttpURLConnection performHttpRequest(String uri, CallType callType, String body)
      throws IOException, CallException
  {
    return performHttpRequest(uri, callType, body, 0);
  }

}