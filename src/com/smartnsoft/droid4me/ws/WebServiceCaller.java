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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A basis class for making web service calls easier.
 * 
 * <p>
 * When invoking an HTTP method, the caller goes through the following workflow:
 * <ol>
 * <li>the {@link #isConnected()} method is checked: if the return value is set to <code>false</code>, no request will be attempted and a
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
   * An empty interface which states that the underlying {@link HttpClient} instance should be reused for all HTTP request, instead of creating a new
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
      super();
    }

    public SensibleHttpClient(ClientConnectionManager connectionManager, HttpParams params)
    {
      super(connectionManager, params);
    }

    public SensibleHttpClient(HttpParams params)
    {
      super(params);
    }

    @Override
    protected HttpContext createHttpContext()
    {
      // Same as DefaultHttpClient.createHttpContext() minus the cookie store
      HttpContext context = new BasicHttpContext();
      context.setAttribute(ClientContext.AUTHSCHEME_REGISTRY, getAuthSchemes());
      context.setAttribute(ClientContext.COOKIESPEC_REGISTRY, getCookieSpecs());
      context.setAttribute(ClientContext.CREDS_PROVIDER, getCredentialsProvider());
      return context;
    }

  }

  protected final static Logger log = LoggerFactory.getInstance(WebServiceCaller.class);

  private static DocumentBuilder builder;

  private boolean isConnected = true;

  private HttpClient httpClient;

  public static InputStream createInputStreamFromJson(JSONObject jsonObject)
  {
    return new ByteArrayInputStream(jsonObject.toString().getBytes());
  }

  public static InputStream createInputStreamFromJson(JSONArray jsonArray)
  {
    return new ByteArrayInputStream(jsonArray.toString().getBytes());
  }

  private static XMLReader getNewXmlReader()
      throws FactoryConfigurationError
  {
    final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    XMLReader theReader = null;
    try
    {
      final SAXParser saxParser = saxParserFactory.newSAXParser();
      try
      {
        theReader = saxParser.getXMLReader();
      }
      catch (SAXException exception)
      {
        if (log.isFatalEnabled())
        {
          log.fatal("Cannot create the SAX XML reader", exception);
        }
      }
    }
    catch (Exception exception)
    {
      if (log.isFatalEnabled())
      {
        log.fatal("Cannot create the SAX parser", exception);
      }
    }
    return theReader;
  }

  /**
   * @return the value previously set by the {@link #setConnected(boolean)} method
   */
  public boolean isConnected()
  {
    return isConnected;
  }

  /**
   * Enables to indicate that no Internet connectivity is available, or that the connectivity has been restored. The initial value is
   * <code>true</code>.
   */
  public void setConnected(boolean isConnected)
  {
    this.isConnected = isConnected;
  }

  public final InputStream getInputStream(String uri)
      throws WebServiceCaller.CallException
  {
    return getInputStream(uri, WebServiceCaller.CallType.Get, null);
  }

  /**
   * Performs an HTTP request corresponding to the provided parameters.
   * 
   * @param uri
   *          the URI being requested
   * @param callType
   *          the HTTP method
   * @param body
   *          if the HTTP method is set to {@link WebServiceCaller.CallType#Post} or {@link WebServiceCaller.CallType#Put}, this is the body of the
   *          request
   * @return the input stream of the HTTP method call; cannot be <code>null</code>
   * @throws WebServiceCaller.CallException
   *           if the status code of the HTTP response does not belong to the [{@link HttpStatus.SC_OK}, {@link HttpStatus.SC_MULTI_STATUS}] range.
   *           Also if a connection issue occurred: the exception will {@link Throwable#getCause() embed} the cause of the exception. If the
   *           {@link #isConnected()} method returns <code>false</code>, no request will be attempted and a {@link WebServiceCaller.CallException}
   *           exception will be thrown (embedding a {@link UnknownHostException} exception).
   */
  public final InputStream getInputStream(String uri, WebServiceCaller.CallType callType, HttpEntity body)
      throws WebServiceCaller.CallException
  {
    if (isConnected == false)
    {
      throw new WebServiceCaller.CallException(new UnknownHostException("No connectivity"));
    }
    try
    {
      final HttpResponse response = performHttpRequest(uri, callType, body, 0);
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

  protected abstract String getUrlEncoding();

  /**
   * @return the top XML element of the DOM
   */
  protected final Element performHttpGetDom(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, IllegalStateException, SAXException
  {
    return performHttpGetDom(computeUri(methodUriPrefix, methodUriSuffix, uriParameters));
  }

  /**
   * @see #performHttpGetDom(String, String, Map)
   */
  protected final Element performHttpGetDom(String uri)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, SAXException
  {
    final HttpResponse response = performHttpRequest(uri, WebServiceCaller.CallType.Get, null, 0);
    return WebServiceCaller.getDom(getContent(uri, WebServiceCaller.CallType.Get, response));
  }

  protected final void performHttpGetSAX(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters, ContentHandler contentHandler)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, IllegalStateException, SAXException, ParserConfigurationException,
      WebServiceCaller.CallException
  {
    parseSax(contentHandler, getInputStream(computeUri(methodUriPrefix, methodUriSuffix, uriParameters)));
  }

  protected final String performHttpGetJson(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, JSONException
  {
    return performHttpGetJson(computeUri(methodUriPrefix, methodUriSuffix, uriParameters));
  }

  protected final String performHttpPostJson(String methodUriPrefix, String methodUriSuffix, HttpEntity postContents)
      throws UnsupportedEncodingException, ClientProtocolException, IllegalStateException, IOException, CallException, JSONException
  {
    return performHttpPostJson(computeUri(methodUriPrefix, methodUriSuffix, null), postContents);
  }

  protected final String performHttpPutJson(String methodUriPrefix, String methodUriSuffix, HttpEntity postContents)
      throws UnsupportedEncodingException, ClientProtocolException, IllegalStateException, IOException, CallException, JSONException
  {
    return performHttpPutJson(computeUri(methodUriPrefix, methodUriSuffix, null), postContents);
  }

  protected final String performHttpDeleteJson(String methodUriPrefix, String methodUriSuffix)
      throws UnsupportedEncodingException, ClientProtocolException, IllegalStateException, IOException, CallException, JSONException
  {
    return performHttpDeleteJson(computeUri(methodUriPrefix, methodUriSuffix, null));
  }

  protected final String performHttpGetJson(String uri)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, JSONException
  {
    final HttpResponse response = performHttpRequest(uri, WebServiceCaller.CallType.Get, null, 0);
    return getJson(getContent(uri, WebServiceCaller.CallType.Get, response));
  }

  protected final String performHttpPostJson(String uri, HttpEntity body)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, JSONException
  {
    final HttpResponse response = performHttpRequest(uri, WebServiceCaller.CallType.Post, body, 0);
    return getJson(getContent(uri, WebServiceCaller.CallType.Post, response));
  }

  protected final String performHttpPutJson(String uri, HttpEntity body)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, JSONException
  {
    final HttpResponse response = performHttpRequest(uri, WebServiceCaller.CallType.Put, body, 0);
    return getJson(getContent(uri, WebServiceCaller.CallType.Put, response));
  }

  protected final String performHttpDeleteJson(String uri)
      throws UnsupportedEncodingException, ClientProtocolException, IOException, CallException, JSONException
  {
    final HttpResponse response = performHttpRequest(uri, WebServiceCaller.CallType.Delete, null, 0);
    return getJson(getContent(uri, WebServiceCaller.CallType.Delete, response));
  }

  public static final String getString(InputStream inputStream)
      throws IOException
  {
    final StringWriter writer = new StringWriter();
    final InputStreamReader streamReader = new InputStreamReader(inputStream);
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

  public static final String getJson(InputStream inputStream)
      throws JSONException
  {
    try
    {
      return WebServiceCaller.getString(inputStream);
    }
    catch (IOException exception)
    {
      throw new JSONException(exception.getMessage());
    }
  }

  public static final Element getDom(InputStream inputStream)
      throws SAXException, IOException
  {
    return WebServiceCaller.parseDom(inputStream).getDocumentElement();
  }

  public static Document parseDom(InputStream inputStream)
      throws SAXException, IOException
  {
    // TODO: make this thread-safe one day, even if it is very likely that it is not necessary at all
    if (WebServiceCaller.builder == null)
    {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      factory.setValidating(false);
      try
      {
        WebServiceCaller.builder = factory.newDocumentBuilder();
      }
      catch (ParserConfigurationException exception)
      {
        if (log.isFatalEnabled())
        {
          log.fatal("Cannot create the DOM XML parser factories", exception);
        }
      }
    }
    return builder.parse(inputStream);
  }

  protected final Document parseDom(String uri, WebServiceCaller.CallType callType, HttpResponse response)
      throws CallException
  {
    try
    {
      return WebServiceCaller.parseDom(getContent(uri, callType, response));
    }
    catch (IOException exception)
    {
      throw new WebServiceCaller.CallException("A I/O problem occurred while attempting to parse in DOM the HTTP response!", exception);
    }
    catch (SAXException exception)
    {
      throw new WebServiceCaller.CallException("Cannot parse properly in DOM the input stream!", exception);
    }
  }

  public final void parseSax(ContentHandler contentHandler, InputStream inputStream)
      throws FactoryConfigurationError, IOException, SAXException
  {
    // Now that multiple SAX parsings can be done in parallel, we create a new XML reader each time
    try
    {
      final XMLReader xmlReader = getNewXmlReader();
      xmlReader.setContentHandler(contentHandler);
      final InputSource inputSource = new InputSource(inputStream);
      inputSource.setEncoding(getUrlEncoding());
      xmlReader.parse(inputSource);
    }
    finally
    {
      try
      {
        inputStream.close();
      }
      catch (IOException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Could not properly close the input stream used for parsing the XML in SAX", exception);
        }
      }
    }
  }

  protected final HttpResponse performHttpGet(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, WebServiceCaller.CallException
  {
    return performHttpRequest(computeUri(methodUriPrefix, methodUriSuffix, uriParameters), WebServiceCaller.CallType.Get, null, 0);
  }

  protected final HttpResponse performHttpPost(String uri, HttpEntity body)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, WebServiceCaller.CallException
  {
    return performHttpRequest(uri, WebServiceCaller.CallType.Post, body, 0);
  }

  protected final HttpResponse performHttpPut(String uri, HttpEntity body)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, WebServiceCaller.CallException
  {
    return performHttpRequest(uri, WebServiceCaller.CallType.Put, body, 0);
  }

  private HttpResponse performHttpRequest(String uri, WebServiceCaller.CallType callType, HttpEntity body, int attemptsCount)
      throws UnsupportedEncodingException, IOException, ClientProtocolException, WebServiceCaller.CallException
  {
    if (uri == null)
    {
      throw new WebServiceCaller.CallException("Cannot perform an HTTP request with a null URI!");
    }
    final long start = System.currentTimeMillis();
    final HttpRequestBase request;
    switch (callType.verb)
    {
    default:
    case Get:
      request = new HttpGet(uri);
      break;
    case Post:
      final HttpPost httpPost = new HttpPost(uri);
      // final List<NameValuePair> values = new ArrayList<NameValuePair>();
      // for (Entry<String, String> entry : postContents.entrySet())
      // {
      // values.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
      // }
      // httpPost.setEntity(new UrlEncodedFormEntity(values, getUrlEncoding()));
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
    if (log.isDebugEnabled())
    {
      log.debug("Running the HTTP " + callType + " query '" + uri + "'");
    }
    final HttpClient httpClient = getHttpClient();
    onBeforeHttpRequestExecution(httpClient, request);
    final HttpResponse response = httpClient.execute(request);
    final long stop = System.currentTimeMillis();
    final int statusCode = response.getStatusLine().getStatusCode();
    if (log.isInfoEnabled())
    {
      log.info("The call to the HTTP " + callType + " query '" + uri + "' took " + (stop - start) + " ms with status code " + statusCode);
    }
    if (!(statusCode >= HttpStatus.SC_OK && statusCode <= HttpStatus.SC_MULTI_STATUS))
    {
      if (onStatusCodeNotOk(uri, callType, body, response, statusCode, attemptsCount + 1) == true)
      {
        return performHttpRequest(uri, callType, body, attemptsCount + 1);
      }
    }

    return response;
  }

  /**
   * Invoked when the result of the HTTP request is not <code>20X</code>. The default implementation logs the problem and throws an exception.
   * 
   * @param uri
   *          the URI of the HTTP call
   * @param callType
   *          the type of the HTTP method
   * @param body
   *          the body of the HTTP method when its a {@link WebServiceCaller.CallType#Post} or a {@link WebServiceCaller.CallType#Put} ;
   *          <code>null</code> otherwise
   * @param response
   *          the HTTP response
   * @param statusCode
   *          the status code of the response, which is not <code>20X</code>
   * @param attemptsCount
   *          the number of attempts that have been run for this HTTP method. Starts at <code>1</code>
   * @return <code>true</code> if you want the request to be re-run if it has failed
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
    throw new WebServiceCaller.CallException(message, statusCode);
  }

  /**
   * This is the perfect place for customizing the HTTP request that is bound to be run.
   * 
   * @param httpClient
   *          the Apache HTTP client that will run the HTTP request
   * @param request
   *          the HTTP request
   * @throws WebServiceCaller.CallException
   *           in case the HTTP request cannot be eventually invoked properly
   */
  protected void onBeforeHttpRequestExecution(HttpClient httpClient, HttpRequestBase request)
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
    return response.getEntity().getContent();
  }

  public final String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
  {
    return WebServiceCaller.encodeUri(methodUriPrefix, methodUriSuffix, uriParameters, getUrlEncoding());
  }

  public static String encodeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters, String urlEnconding)
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
        if (first == true)
        {
          buffer.append("?");
          first = false;
        }
        else
        {
          buffer.append("&");
        }
        try
        {
          buffer.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), urlEnconding));
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
   * Is responsible for returning an HTTP client instance, used for actually running the HTTP request.
   * 
   * <p>
   * The current implementation returns a {@link SensibleHttpClient} instance, which is thread-safe.
   * </p>
   * 
   * @return a valid HTTP client
   */
  protected HttpClient getHttpClient()
  {
    if (this instanceof WebServiceCaller.ReuseHttpClient)
    {
      if (httpClient == null)
      {
        // Taken from http://foo.jasonhudgins.com/2010/03/http-connections-revisited.html
        final DefaultHttpClient initialHttpClient = new DefaultHttpClient();
        final ClientConnectionManager clientConnectionManager = initialHttpClient.getConnectionManager();
        final HttpParams params = initialHttpClient.getParams();
        @SuppressWarnings("deprecation")
        final ThreadSafeClientConnManager threadSafeClientConnectionManager = new ThreadSafeClientConnManager(params, clientConnectionManager.getSchemeRegistry());
        httpClient = new SensibleHttpClient(threadSafeClientConnectionManager, params);
      }
      return httpClient;
    }
    else
    {
      return new SensibleHttpClient();
    }
  }

}