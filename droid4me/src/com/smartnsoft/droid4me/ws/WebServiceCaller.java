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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
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
import org.json.JSONException;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A basis class for making web service calls easier.
 * 
 * <p>
 * When invoking an HTTP method, the caller goes through the following workflow:
 * <ol>
 * <li>the {@link #isConnected()} method is checked: if the return value is set to {@code false}, no request will be attempted and a
 * {@link WebServiceCaller.CallException} exception will be thrown (embedding a {@link UnknownHostException} exception) ;</li>
 * <li>the {@link #onBeforeHttpRequestExecution(DefaultHttpClient, HttpRequestBase)} method will be invoked, so as to let the caller tune the HTTP
 * method request ;</li>
 * <li>if a connection issue arises (connection time-out, socket time-out, lost of connectivity), a {@link WebServiceCaller.CallException} exception
 * will be thrown, and it will {@link Throwable#getCause() embed} the reason for the connection issue ;</li>
 * <li>if the status code of the HTTP response does not belong to the [{@link HttpStatus.SC_OK}, {@link HttpStatus.SC_MULTI_STATUS}] range, the
 * {@link #onStatusCodeNotOk(String, WebServiceClient.CallType, HttpEntity, HttpResponse, int, int)} method will be invoked.</li>
 * </ol>
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2009.03.26
 */
public abstract class WebServiceCaller
    implements WebServiceClient
{

  /**
   * An empty interface which states that the underlying {@link HttpClient} instance should be reused for all HTTP requests, instead of creating a new
   * one each time.
   */
  public static interface ReuseHttpClient
  {
  }

  /**
   * Overrides the default {@link DefaultHttpClient} by disabling the cookies storing.
   * 
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

  /**
   * A flag which indicates whether the hereby {@code WebServiceCaller} internal logs should be enabled. Logs will report useful {@code curl}
   * equivalent commands, for instance.
   * <p>
   * Do not set this flag to {@code true} under the production mode!
   * </p>
   * 
   * @see WebServiceCaller#BODY_MAXIMUM_SIZE_IN_BYTES_LOGGED
   */
  public static boolean ARE_DEBUG_LOG_ENABLED = false;

  /**
   * If the {@link #ARE_DEBUG_LOG_ENABLED} is {@code true}, indicates the maximum size of an HTTP request body to be logged: if an HTTP request body
   * size is beyond that limit, it will not be logged, for performance reasons.
   */
  public static long BODY_MAXIMUM_SIZE_IN_BYTES_LOGGED = 4096;

  protected final static Logger log = LoggerFactory.getInstance(WebServiceCaller.class);

  private boolean isConnected = true;

  private HttpClient httpClient;

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
   * Equivalent to calling {@link #getInputStream(String, WebServiceClient.CallType, HttpEntity)} with {@code callType} parameter set to
   * {@code WebServiceCaller.CallType.Get} and {@code body} parameter set to {@code null}.
   * 
   * @see #getInputStream(String, WebServiceClient.CallType, HttpEntity)
   * @see #getInputStream(String, HttpRequestBase)
   */
  public final InputStream getInputStream(String uri)
      throws WebServiceCaller.CallException
  {
    return getInputStream(uri, WebServiceCaller.CallType.Get, null);
  }

  /**
   * Performs an HTTP request corresponding to the provided parameters.
   * 
   * <p>
   * Caution: it is the responsibility of the caller to release (for instance, by invoking eventually the {@link InputStream#close()} method) the
   * returned {@link InputStream}! Otherwise, the underlying {@link #httpClient} connection (taken from its pool), may hang at next call! Note that if
   * an exception is raised, the hereby code will release the content by default.
   * </p>
   * 
   * @param uri
   *          the URI being requested
   * @param callType
   *          the HTTP method
   * @param body
   *          if the HTTP method is set to {@link WebServiceCaller.CallType#Post} or {@link WebServiceCaller.CallType#Put}, this is the body of the
   *          request
   * @return the input stream of the HTTP method call; cannot be {@code null}
   * @throws WebServiceCaller.CallException
   *           if the status code of the HTTP response does not belong to the [{@link HttpStatus.SC_OK}, {@link HttpStatus.SC_MULTI_STATUS}] range.
   *           Also if a connection issue occurred: the exception will {@link Throwable#getCause() embed} the cause of the exception. If the
   *           {@link #isConnected()} method returns {@code false}, no request will be attempted and a {@link WebServiceCaller.CallException}
   *           exception will be thrown (embedding a {@link UnknownHostException} exception).
   * @see #getInputStream(String)
   * @see #getInputStream(String, HttpRequestBase)
   */
  @Override
  public final InputStream getInputStream(String uri, WebServiceCaller.CallType callType, HttpEntity body)
      throws WebServiceCaller.CallException
  {
    try
    {
      final HttpResponse response = performHttpRequest(uri, callType, body);
      return getContent(uri, callType, response);
    }
    catch (WebServiceCaller.CallException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      throw new WebServiceCaller.CallException(exception);
    }
  }

  /**
   * 
   * Performs an HTTP request corresponding to the provided parameters.
   * 
   * <p>
   * Caution: read the {@link #getInputStream(String, WebServiceClient.CallType, HttpEntity)} documentation.
   * </p>
   * 
   * @param uri
   *          the URI being requested
   * @param request
   *          the HTTP request which should be executed
   * @return
   * @throws WebServiceCaller.CallException
   *           read the {@link #getInputStream(String, WebServiceClient.CallType, HttpEntity)} documentation
   */
  public final InputStream getInputStream(String uri, HttpRequestBase request)
      throws WebServiceCaller.CallException
  {
    try
    {
      final AtomicReference<CallType> callTypeHolder = new AtomicReference<WebServiceClient.CallType>();
      final HttpResponse response = performHttpRequest(uri, request, callTypeHolder, 0);
      return getContent(uri, callTypeHolder.get(), response);
    }
    catch (WebServiceCaller.CallException exception)
    {
      throw exception;
    }
    catch (Exception exception)
    {
      throw new WebServiceCaller.CallException(exception);
    }
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
   * @param inputStream
   *          the input stream to convert ; not that it will have been {@link InputStream#close() closed}
   * @param encoding
   *          the encoding to use
   * @return the string resulting from the provided input stream
   * @throws IOException
   *           if an error happened during the conversion
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
   * 
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
   * Invoked when the result of the HTTP request is not <code>20X</code>. The default implementation logs the problem and throws an exception.
   * 
   * @param uri
   *          the URI of the HTTP call
   * @param callType
   *          the type of the HTTP method
   * @param body
   *          the body of the HTTP method when its a {@link WebServiceCaller.CallType#Post} or a {@link WebServiceCaller.CallType#Put} ; {@code null}
   *          otherwise
   * @param response
   *          the HTTP response
   * @param statusCode
   *          the status code of the response, which is not <code>20X</code>
   * @param attemptsCount
   *          the number of attempts that have been run for this HTTP method. Starts at <code>1</code>
   * @return {@code true} if you want the request to be re-run if it has failed
   * @throws WebServiceCaller.CallException
   *           if you want the call to be considered as not OK
   */
  protected boolean onStatusCodeNotOk(String uri, WebServiceCaller.CallType callType, HttpEntity body, HttpResponse response, int statusCode, int attemptsCount)
      throws WebServiceCaller.CallException
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

    throw new WebServiceCaller.CallException(message, statusCode);
  }

  /**
   * This is the perfect place for customizing the HTTP request that is bound to be run.
   * 
   * @param httpClient
   *          the Apache HTTP client that will run the HTTP request
   * @param request
   *          the HTTP request
   * @param callType
   *          the type of the HTTP method
   * @throws WebServiceCaller.CallException
   *           in case the HTTP request cannot be eventually invoked properly
   */
  protected void onBeforeHttpRequestExecution(HttpClient httpClient, HttpRequestBase request, WebServiceClient.CallType callType)
      throws WebServiceCaller.CallException
  {
  }

  /**
   * Invoked on every call, in order to extract the input stream from the response.
   * 
   * <p>
   * If the content type is gzipped, this is the ideal place for unzipping it.
   * </p>
   * 
   * @param uri
   *          the web call initial URI
   * @param callType
   *          the kind of request
   * @param response
   *          the HTTP response
   * @return the (decoded) input stream of the response
   * @throws IOException
   *           if some exception occurred while extracting the content of the response
   */
  protected InputStream getContent(String uri, WebServiceCaller.CallType callType, HttpResponse response)
      throws IOException
  {
    if (log.isDebugEnabled() == true && WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
    {
      final HttpEntity entity = response.getEntity();
      final InputStream content = entity.getContent();
      InputStream debugContent;

      if (content.markSupported() == true)
      {
        debugContent = content;
      }
      else
      {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final byte[] buffer = new byte[8192];
        int bufferLength;

        while ((bufferLength = content.read(buffer)) > 0)
        {
          outputStream.write(buffer, 0, bufferLength);
        }

        content.close();
        outputStream.close();
        debugContent = new ByteArrayInputStream(outputStream.toByteArray());
      }

      final int length = (int) (entity.getContentLength() <= WebServiceCaller.BODY_MAXIMUM_SIZE_IN_BYTES_LOGGED ? entity.getContentLength()
          : WebServiceCaller.BODY_MAXIMUM_SIZE_IN_BYTES_LOGGED);
      debugContent.mark(length);

      try
      {
        final String bodyAsString = getString(debugContent);
        log.debug("The body of the response is : '" + bodyAsString + "'");
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

    return response.getEntity().getContent();
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
  public final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters, boolean alreadyContainsQuestionMark)
  {
    return WebServiceCaller.encodeUri(methodUriPrefix, methodUriSuffix, uriParameters, alreadyContainsQuestionMark, getUrlEncoding());
  }

  /**
   * Computes a URI from its path elements.
   * 
   * @param methodUriPrefix
   *          the URI prefix
   * @param methodUriSuffix
   *          the URI suffix ; a {@code /} separator will be appended after the {@code methodUriPrefix} parameter, if not {@code null}. May be
   *          {@code null}
   * @param uriParameters
   *          a dictionary with {@link String} keys and {@link String} values, which holds the URI query parameters ; may be {@code null}. If a value
   *          is {@code null}, an error log will be issued. If a value is the empty string ({@code ""}), the dictionary key will be used as the
   *          name+value URI parameter ; this is especially useful when the parameter value should not be encoded
   * @param indicates
   *          whether the provided {@code methodUriPrefix} or the {@code methodUriSuffix} parameters already contain a question mark {@code ?}, so
   *          that, when computing the URI with the additional {@code uriParameters}, it is not appended again. This is especially useful when
   *          building an URI from a basis URI, which already contains a {@code ?} for declaring URI parameters
   * @param urlEnconding
   *          the encoding used for writing the URI query parameters values
   * @return a valid URI that may be used for running an HTTP request, for instance
   */
  public static String encodeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters, boolean alreadyContainsQuestionMark,
      String urlEnconding)
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

  /**
   * Is responsible for returning an HTTP client instance, used for actually running the HTTP requests. The method implementation relies on the
   * {@link #computeHttpClient()} method, if no {@link HttpClient} is currently created.
   * 
   * <p>
   * The current implementation returns a {@link SensibleHttpClient} instance, which is thread-safe, in case the extending class implements the
   * {@link WebServiceCaller.ReuseHttpClient} interface.
   * </p>
   * 
   * @return a valid HTTP client
   * @throws WebServiceCaller.CallException
   *           is the invocation of the {@link #computeHttpClient()} threw an exception
   * @see #computeHttpClient()
   * @see #resetHttpClient()
   */
  protected synchronized final HttpClient getHttpClient()
      throws WebServiceCaller.CallException
  {
    if (this instanceof WebServiceCaller.ReuseHttpClient)
    {
      if (httpClient == null)
      {
        try
        {
          httpClient = computeHttpClient();
        }
        catch (Exception exception)
        {
          throw new WebServiceCaller.CallException("Cannot instantiate the 'HttpClient'", exception);
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
        throw new WebServiceCaller.CallException("Cannot instantiate the 'HttpClient'", exception);
      }
    }
  }

  /**
   * Forces the internal {@link HttpClient} to be renewed the next time the {@link #getHttpClient()} method will be invoked, i.e. the next time an
   * HTTP method will be executed a new {@link HttpClient} instance will be created. This will not affect any pending HTTP method execution.
   * 
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
   * 
   * <p>
   * In the case the class implements {@link WebServiceCaller.ReuseHttpClient} interface, this method will be invoked only once.
   * </p>
   * 
   * @return an HTTP client that will be used for running HTTP requests
   */
  protected HttpClient computeHttpClient()
  {
    if (this instanceof WebServiceCaller.ReuseHttpClient)
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

  private HttpResponse performHttpRequest(String uri, HttpRequestBase request, AtomicReference<WebServiceCaller.CallType> callTypeHolder, int attemptsCount)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, WebServiceCaller.CallException
  {
    if (uri == null)
    {
      throw new WebServiceCaller.CallException("Cannot perform an HTTP request with a null URI!");
    }
    if (isConnected == false)
    {
      throw new WebServiceCaller.CallException(new UnknownHostException("No connectivity"));
    }

    final HttpClient httpClient = getHttpClient();
    final WebServiceClient.CallType callType;
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
    if (log.isDebugEnabled())
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
            if (body.getContentLength() <= WebServiceCaller.BODY_MAXIMUM_SIZE_IN_BYTES_LOGGED && body.getContent().markSupported() == true)
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
            for (Header header : request.getAllHeaders())
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
    if (log.isDebugEnabled())
    {
      log.debug("The call to the HTTP " + callType + " request '" + uri + "' took " + (System.currentTimeMillis() - start) + " ms and returned the status code " + statusCode);
    }

    if (log.isDebugEnabled() == true && WebServiceCaller.ARE_DEBUG_LOG_ENABLED == true)
    {
      final StringBuilder sb = new StringBuilder("The headers of the response are :'");
      for (final Header header : response.getAllHeaders())
      {
        sb.append("\"").append(header.getName()).append(": ").append(header.getValue().replace("\"", "\\\"")).append("\"");
      }

      log.debug(sb.toString());
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

  private HttpResponse performHttpRequest(String uri, WebServiceCaller.CallType callType, HttpEntity body)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, WebServiceCaller.CallException
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
    return performHttpRequest(uri, request, new AtomicReference<WebServiceClient.CallType>(), 0);
  }

}