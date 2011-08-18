package com.smartnsoft.droid4me.ws.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;

import android.util.Log;

import com.smartnsoft.droid4me.cache.DbPersistence;
import com.smartnsoft.droid4me.cache.FilePersistence;
import com.smartnsoft.droid4me.cache.Persistence;
import com.smartnsoft.droid4me.cache.Persistence.PersistenceException;
import com.smartnsoft.droid4me.cache.Values.CacheException;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.ws.WebServiceCaller;
import com.smartnsoft.droid4me.ws.WebServiceClient;
import com.smartnsoft.droid4me.ws.WSUriStreamParser.UrlWithCallTypeAndBody;
import com.smartnsoft.droid4me.ws.WebServiceClient.CallType;
import com.smartnsoft.droid4me.wscache.BackedWSUriStreamParser;

/**
 * @author ɉdouard Mercier
 * @since 2011.08.16
 */
public final class Tests
{

  private final static class StreamParameter
  {

    public Map<String, String> computeUriParameters()
    {
      return null;
    }

  }

  @SuppressWarnings("serial")
  private final static class TestException
      extends Exception
  {

    public TestException(Throwable throwable)
    {
      super(throwable);
    }

  }

  private static final String WEBSERVICES_BASE_URL = "http://www.google.com";

  @Before
  public void setup()
  {
    LoggerFactory.logLevel = Log.DEBUG;
    final File contentsDirectory = new File("tmp");
    Persistence.CACHE_DIRECTORY_PATHS = new String[] { contentsDirectory.getAbsolutePath() };
    DbPersistence.FILE_NAMES = new String[] { DbPersistence.DEFAULT_FILE_NAME };
    DbPersistence.TABLE_NAMES = new String[] { DbPersistence.DEFAULT_TABLE_NAME };
    Persistence.INSTANCES_COUNT = 1;
    Persistence.IMPLEMENTATION_FQN = FilePersistence.class.getName();
    Persistence.clearAll();
  }

  @Test
  public void expiredRetentionValue()
      throws CacheException, InterruptedException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    {
      streamParser.backed.getRetentionValue(false, 1000000, null, new StreamParameter());
    }
    {
      Thread.sleep(200);
      final String value = streamParser.backed.getRetentionValue(true, 100, null, new StreamParameter());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, value);
      Assert.assertEquals("'getInputStream()' has been invoked too many times", 1, getInputStreamCallsCount.get());
    }
  }

  @Test
  public void fromCacheRetentionValue()
      throws CacheException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    {
      streamParser.backed.getRetentionValue(false, 1000000, null, new StreamParameter());
    }
    {
      getInputStreamCallsCount.set(0);
      final String value = streamParser.backed.getRetentionValue(true, 1000000, null, new StreamParameter());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, value);
      Assert.assertEquals("'getInputStream()' has been invoked too many times", 0, getInputStreamCallsCount.get());
    }
  }

  @Test
  public void notFromCacheRetentionValue()
      throws CacheException
  {
    final AtomicInteger getInputStreamCallsCount = new AtomicInteger(0);
    final String expectedValue = new String("Test");

    final WebServiceClient webServiceClient = computeWebServiceClient(getInputStreamCallsCount, expectedValue);
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = computeStreamParser(webServiceClient);

    {
      final String value = streamParser.backed.getRetentionValue(false, 1000000, null, new StreamParameter());
      Assert.assertEquals("The returned business object is not the right one", expectedValue, value);
      Assert.assertEquals("'getInputStream()' has been invoked too many times", 1, getInputStreamCallsCount.get());
    }
  }

  private WebServiceClient computeWebServiceClient(final AtomicInteger getInputStreamCallsCount, final String expectedValue)
  {
    final WebServiceClient webServiceClient = new WebServiceClient()
    {

      public String computeUri(String methodUriPrefix, String methodUriSuffix, Map<String, String> uriParameters)
      {
        return methodUriPrefix;
      }

      public InputStream getInputStream(String uri, CallType callType, HttpEntity body)
          throws CallException
      {
        getInputStreamCallsCount.incrementAndGet();
        return new ByteArrayInputStream(expectedValue.getBytes());
      }

    };
    return webServiceClient;
  }

  private BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> computeStreamParser(
      final WebServiceClient webServiceClient)
  {
    final BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException> streamParser = new BackedWSUriStreamParser.BackedUriStreamedMap<String, StreamParameter, TestException, PersistenceException>(Persistence.getInstance(0), webServiceClient)
    {

      public UrlWithCallTypeAndBody computeUri(StreamParameter parameters)
      {
        return new UrlWithCallTypeAndBody(webServiceClient.computeUri(Tests.WEBSERVICES_BASE_URL, "method", parameters.computeUriParameters()), CallType.Get, null);
      }

      public String parse(StreamParameter parameter, InputStream inputStream)
          throws TestException
      {
        try
        {
          return WebServiceCaller.getString(inputStream);
        }
        catch (IOException exception)
        {
          throw new TestException(exception);
        }
      }

    };
    return streamParser;
  }

}
