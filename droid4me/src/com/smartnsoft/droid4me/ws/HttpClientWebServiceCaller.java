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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

/**
 * A basis class for making web service calls easier.
 * <p/>
 * <p>
 * When invoking an HTTP method, the caller goes through the following workflow:
 * <ol>
 * <li>the {@link #isConnected()} method is checked: if the return value is set to {@code false}, no request will be attempted and a
 * {@link WebServiceCaller.CallException} exception will be thrown (embedding a {@link UnknownHostException} exception) ;</li>
 * <li>the {@link #onBeforeHttpRequestExecution(HttpClient, HttpRequestBase, CallType)} method will be invoked, so as to let the caller tune the HTTP
 * method request ;</li>
 * <li>if a connection issue arises (connection time-out, socket time-out, lost of connectivity), a {@link WebServiceCaller.CallException} exception
 * will be thrown, and it will {@link Throwable#getCause() embed} the reason for the connection issue ;</li>
 * <li>if the status code of the HTTP response does not belong to the [{@link HttpStatus#SC_OK}, {@link HttpStatus#SC_MULTI_STATUS}] range, the
 * {@link #onStatusCodeNotOk(String, WebServiceClient.CallType, HttpEntity, HttpResponse, int, int)} method will be invoked.</li>
 * </ol>
 * </p>
 *
 * @author Ludovic Roland
 * @since 2016.01.28
 */
public abstract class HttpClientWebServiceCaller
    extends WebServiceCaller
{

  /**
   * Overrides the default {@link DefaultHttpClient} by disabling the cookies storing.
   * <p/>
   * <p>
   * The code is inspired from http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/AndroidHttpClient.java.
   * </p>
   *
   * @since 2011.09.16
   */
  protected static class SensibleHttpClient
      extends DefaultHttpClient
  {

    public SensibleHttpClient()
    {
      this(null, null);
    }

    public SensibleHttpClient(HttpParams params)
    {
      this(null, params);
    }

    public SensibleHttpClient(ClientConnectionManager connectionManager, HttpParams params)
    {
      super(connectionManager, params);
    }

    @Override
    protected HttpContext createHttpContext()
    {
      // Same as DefaultHttpClient.createHttpContext() minus the cookie store
      final HttpContext context = new BasicHttpContext();
      context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
      context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
      context.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
      return context;
    }

  }

  protected final static Logger log = LoggerFactory.getInstance(HttpClientWebServiceCaller.class);

  private HttpClient httpClient;

  /**
   * Equivalent to calling {@link #getInputStream(String, WebServiceClient.CallType, HttpEntity)} with {@code callType} parameter set to
   * {@code WebServiceCaller.CallType.Get} and {@code body} parameter set to {@code null}.
   *
   * @see #getInputStream(String, WebServiceClient.CallType, HttpEntity)
   * @see #getInputStream(String, HttpRequestBase)
   */
  public final InputStream getInputStream(String uri)
      throws WebServiceCaller.CallException
  {
    return getInputStream(uri, WebServiceCaller.CallType.Get, (HttpEntity) null);
  }

  @Override
  public final InputStream getInputStream(String uri, CallType callType, String body)
      throws CallException
  {
    return null;
  }

  /**
   * Performs an HTTP request corresponding to the provided parameters.
   * <p/>
   * <p>
   * Caution: it is the responsibility of the caller to release (for instance, by invoking eventually the {@link InputStream#close()} method) the
   * returned {@link InputStream}! Otherwise, the underlying {@link #httpClient} connection (taken from its pool), may hang at next call! Note that if
   * an exception is raised, the hereby code will release the content by default.
   * </p>
   *
   * @param uri      the URI being requested
   * @param callType the HTTP method
   * @param body     if the HTTP method is set to {@link CallType#Post} or {@link CallType#Put}, this is the body of the
   *                 request
   * @return the input stream of the HTTP method call; cannot be {@code null}
   * @throws CallException if the status code of the HTTP response does not belong to the [{@link HttpStatus#SC_OK}, {@link HttpStatus#SC_MULTI_STATUS}] range.
   *                       Also if a connection issue occurred: the exception will {@link Throwable#getCause() embed} the cause of the exception. If the
   *                       {@link #isConnected()} method returns {@code false}, no request will be attempted and a {@link CallException}
   *                       exception will be thrown (embedding a {@link UnknownHostException} exception).
   * @see #getInputStream(String)
   * @see #getInputStream(String, HttpRequestBase)
   */
  @Override
  public final InputStream getInputStream(String uri, CallType callType, HttpEntity body)
      throws CallException
  {
    try
    {
      final HttpResponse response = performHttpRequest(uri, callType, body);
      return getContent(uri, callType, response);
    }
    catch (CallException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      throw new CallException(exception);
    }
  }

  /**
   * Performs an HTTP request corresponding to the provided parameters.
   * <p/>
   * <p>
   * Caution: read the {@link #getInputStream(String, CallType, HttpEntity)} documentation.
   * </p>
   *
   * @param uri     the URI being requested
   * @param request the HTTP request which should be executed
   * @return
   * @throws CallException read the {@link #getInputStream(String, CallType, HttpEntity)} documentation
   */
  public final InputStream getInputStream(String uri, HttpRequestBase request)
      throws CallException
  {
    try
    {
      final AtomicReference<CallType> callTypeHolder = new AtomicReference<CallType>();
      final HttpResponse response = performHttpRequest(uri, request, callTypeHolder, 0);
      return getContent(uri, callTypeHolder.get(), response);
    }
    catch (CallException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      throw new CallException(exception);
    }
  }

  /**
   * Invoked when the result of the HTTP request is not <code>20X</code>. The default implementation logs the problem and throws an exception.
   *
   * @param uri           the URI of the HTTP call
   * @param callType      the type of the HTTP method
   * @param body          the body of the HTTP method when its a {@link CallType#Post} or a {@link CallType#Put} ; {@code null}
   *                      otherwise
   * @param response      the HTTP response
   * @param statusCode    the status code of the response, which is not <code>20X</code>
   * @param attemptsCount the number of attempts that have been run for this HTTP method. Starts at <code>1</code>
   * @return {@code true} if you want the request to be re-run if it has failed
   * @throws CallException if you want the call to be considered as not OK
   */
  protected boolean onStatusCodeNotOk(String uri, CallType callType, HttpEntity body, HttpResponse response,
      int statusCode, int attemptsCount)
      throws CallException
  {
    final String message = "The result code of the call to the web method '" + uri + "' is not OK (not 20X). Status: " + response.getStatusLine();
    if (log.isErrorEnabled())
    {
      log.error(message);
    }

    // We make sure that the response body be consumed: read http://hc.apache.org/httpcomponents-client-ga/tutorial/html/fundamentals.html, chapter
    // 1.1.5 on that purpose. Without this consumption, the connection belonging to the pool may hang at next call!
    // An interesting discussion is also available at
    // http://stackoverflow.com/questions/9505358/android-httpclient-hangs-on-second-request-to-the-server-connection-timed-out
    try
    {
      response.getEntity().consumeContent();
    }
    catch (IOException exception)
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
   * @param httpClient the Apache HTTP client that will run the HTTP request
   * @param request    the HTTP request
   * @param callType   the type of the HTTP method
   * @throws CallException in case the HTTP request cannot be eventually invoked properly
   */
  protected void onBeforeHttpRequestExecution(HttpClient httpClient, HttpRequestBase request, CallType callType)
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
   * @param uri      the web call initial URI
   * @param callType the kind of request
   * @param response the HTTP response
   * @return the (decoded) input stream of the response
   * @throws IOException if some exception occurred while extracting the content of the response
   */
  protected InputStream getContent(String uri, CallType callType, HttpResponse response)
      throws IOException
  {
    final HttpEntity entity = response.getEntity();
    final InputStream content = entity.getContent();
    if (WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
    {
      final InputStream debugContent;
      final int length = (int) (entity.getContentLength() <= WebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES ? entity.getContentLength() : WebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES);

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

  /**
   * Is responsible for returning an HTTP client instance, used for actually running the HTTP requests. The method implementation relies on the
   * {@link #computeHttpClient()} method, if no {@link HttpClient} is currently created.
   * <p/>
   * <p>
   * The current implementation returns a {@link SensibleHttpClient} instance, which is thread-safe, in case the extending class uses the
   * {@link HttpClientWebServiceCaller.ReuseHttpClient} annotation.
   * </p>
   *
   * @return a valid HTTP client
   * @throws CallException is the invocation of the {@link #computeHttpClient()} threw an exception
   * @see #computeHttpClient()
   * @see #resetHttpClient()
   */
  protected synchronized final HttpClient getHttpClient()
      throws CallException
  {
    final ReuseHttpClient reuseHttpClientAnnotation = this.getClass().getAnnotation(ReuseHttpClient.class);
    if (reuseHttpClientAnnotation != null)
    {
      if (httpClient == null)
      {
        try
        {
          httpClient = computeHttpClient();
        }
        catch (Exception exception)
        {
          throw new CallException("Cannot instantiate the 'HttpClient'", exception);
        }
      }
      return httpClient;
    }
    else
    {
      try
      {
        return computeHttpClient();
      }
      catch (Exception exception)
      {
        throw new CallException("Cannot instantiate the 'HttpClient'", exception);
      }
    }
  }

  /**
   * Forces the internal {@link HttpClient} to be renewed the next time the {@link #getHttpClient()} method will be invoked, i.e. the next time an
   * HTTP method will be executed a new {@link HttpClient} instance will be created. This will not affect any pending HTTP method execution.
   * <p/>
   * <p>
   * It is up to the caller to previously {@link ClientConnectionManager#shutdown()} the connection manager if necessary.
   * </p>
   *
   * @see #getHttpClient()
   */
  public synchronized final void resetHttpClient()
  {
    if (log.isInfoEnabled())
    {
      log.info("Resetting the HTTP client");
    }
    httpClient = null;
  }

  /**
   * This method will be invoked by the {@link #getHttpClient()} method, when it needs to use a new {@link HttpClient}. The method should be
   * overridden, when the {@link HttpClient} to use should be customized; a typical case is when the connection time-outs, the HTTP {@code User-Agent}
   * parameters need to fine-tuned.
   * <p/>
   * <p>
   * In the case the class uses {@link HttpClientWebServiceCaller.ReuseHttpClient} annotation, this method will be invoked only once.
   * </p>
   *
   * @return an HTTP client that will be used for running HTTP requests
   */
  protected HttpClient computeHttpClient()
  {
    final ReuseHttpClient reuseHttpClientAnnotation = this.getClass().getAnnotation(ReuseHttpClient.class);
    if (reuseHttpClientAnnotation != null)
    {
      // Taken from http://foo.jasonhudgins.com/2010/03/http-connections-revisited.html
      final DefaultHttpClient initialHttpClient = new DefaultHttpClient();
      final ClientConnectionManager clientConnectionManager = initialHttpClient.getConnectionManager();
      final HttpParams params = initialHttpClient.getParams();

      // Taken from https://github.com/android/platform_frameworks_base/blob/master/core/java/android/net/http/AndroidHttpClient.java
      // This turns off stale checking. Our connections breaks all the time anyway, and it's not worth it to pay the penalty of checking every time
      HttpConnectionParams.setStaleCheckingEnabled(params, false);

      final ThreadSafeClientConnManager threadSafeClientConnectionManager = new ThreadSafeClientConnManager(params, clientConnectionManager.getSchemeRegistry());
      return new SensibleHttpClient(threadSafeClientConnectionManager, params);
    }
    else
    {
      return new SensibleHttpClient();
    }
  }

  private HttpResponse performHttpRequest(String uri, HttpRequestBase request, AtomicReference<CallType> callTypeHolder,
      int attemptsCount)
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

    final HttpClient httpClient = getHttpClient();
    final CallType callType;
    final HttpEntity body;
    if (request instanceof HttpPost)
    {
      callType = CallType.Post;
      body = ((HttpPost) request).getEntity();
    }
    else if (request instanceof HttpPut)
    {
      callType = CallType.Put;
      body = ((HttpPut) request).getEntity();
    }
    else if (request instanceof HttpDelete)
    {
      callType = CallType.Delete;
      body = null;
    }
    else
    {
      callType = CallType.Get;
      body = null;
    }

    onBeforeHttpRequestExecution(httpClient, request, callType);
    callTypeHolder.set(callType);

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
          if (body != null && body.getContent() != null)
          {
            if (body.getContentLength() <= WebServiceCaller.BODY_MAXIMUM_SIZE_LOGGED_IN_BYTES && body.getContent().markSupported() == true)
            {
              logCurlCommand = true;
              body.getContent().mark((int) body.getContentLength());
              try
              {
                final String bodyAsString = getString(body.getContent());
                sb.append(" with body '").append(bodyAsString).append("'");
                curlSb.append(" --data \"").append(bodyAsString).append("\"");
              }
              catch (IOException exception)
              {
                if (log.isWarnEnabled())
                {
                  log.warn("Cannot log the HTTP body", exception);
                }
              }
              finally
              {
                body.getContent().reset();
              }
            }
            for (final Header header : request.getAllHeaders())
            {
              curlSb.append(" --header \"").append(header.getName()).append(": ").append(header.getValue().replace("\"", "\\\"")).append("\"");
            }
          }
          else
          {
            logCurlCommand = true;
          }
        }
        catch (Exception exception)
        {
          // The exception is very likely to be due to a body with a non-consumable "body.getContent()"!
          // We simply ignore the issue
        }
      }
      log.debug("Running the HTTP " + callType + " request '" + uri + "'" + sb.toString() + (logCurlCommand == true ? curlSb.toString() : ""));
    }
    final long start = System.currentTimeMillis();
    final HttpResponse response = httpClient.execute(request);
    final int statusCode = response.getStatusLine().getStatusCode();
    final StringBuilder responseHeadersSb = new StringBuilder();
    if (HttpClientWebServiceCaller.ARE_DEBUG_LOG_ENABLED == true && log.isDebugEnabled() == true)
    {
      for (final Header header : response.getAllHeaders())
      {
        if (responseHeadersSb.length() > 0)
        {
          responseHeadersSb.append(",");
        }
        responseHeadersSb.append("(\"").append(header.getName()).append(": ").append(header.getValue().replace("\"", "\\\"")).append("\")");
      }
    }
    if (log.isDebugEnabled())
    {
      log.debug("The call to the HTTP " + callType + " request '" + uri + "' took " + (System.currentTimeMillis() - start) + " ms and returned the status code " + statusCode + (responseHeadersSb.length() <= 0 ? "" : " with the HTTP headers:" + responseHeadersSb.toString()));
    }

    if (!(statusCode >= HttpStatus.SC_OK && statusCode <= HttpStatus.SC_MULTI_STATUS))
    {
      if (onStatusCodeNotOk(uri, callType, body, response, statusCode, attemptsCount + 1) == true)
      {
        return performHttpRequest(uri, request, callTypeHolder, attemptsCount + 1);
      }
    }

    return response;
  }

  private HttpResponse performHttpRequest(String uri, CallType callType, HttpEntity body)
      throws IOException, CallException
  {
    final HttpRequestBase request;
    switch (callType.verb)
    {
    default:
    case Get:
      request = new HttpGet(uri);
      break;
    case Post:
      final HttpPost httpPost = new HttpPost(uri);
      httpPost.setEntity(body);
      request = httpPost;
      break;
    case Put:
      final HttpPut httpPut = new HttpPut(uri);
      httpPut.setEntity(body);
      request = httpPut;
      break;
    case Delete:
      final HttpDelete httpDelete = new HttpDelete(uri);
      request = httpDelete;
      break;
    }
    return performHttpRequest(uri, request, new AtomicReference<CallType>(), 0);
  }

}