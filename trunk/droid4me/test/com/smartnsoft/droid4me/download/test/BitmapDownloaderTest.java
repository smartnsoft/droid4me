package com.smartnsoft.droid4me.download.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.smartnsoft.droid4me.download.BasisBitmapDownloader;
import com.smartnsoft.droid4me.download.BasisBitmapDownloader.BasisAnalyticsData;
import com.smartnsoft.droid4me.download.BasisDownloadInstructions;
import com.smartnsoft.droid4me.download.BasisDownloadInstructions.InputStreamDownloadInstructor;
import com.smartnsoft.droid4me.download.CoreBitmapDownloader;
import com.smartnsoft.droid4me.download.CoreBitmapDownloader.CoreAnalyticsData;
import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;
import com.smartnsoft.droid4me.test.BasisTests;

/**
 * @author ɉdouard Mercier
 * @since 2011.09.03
 */
public final class BitmapDownloaderTest
    extends BasisTests
{

  private final static class DummyBitmapable
      implements Bitmapable
  {

    private final int sizeInBytes;

    public DummyBitmapable(int sizeInBytes)
    {
      this.sizeInBytes = sizeInBytes;
    }

    public int getSizeInBytes()
    {
      return sizeInBytes;
    }

    public void recycle()
    {
    }

  }

  public final static class DummyViewable
      implements Viewable
  {

    private final int id;

    private Object tag;

    public DummyViewable(int id)
    {
      this.id = id;
    }

    public int getId()
    {
      return id;
    }

    public Object getTag()
    {
      return tag;
    }

    public void setTag(Object tag)
    {
      this.tag = tag;
    }

  }

  public final static class DummyHandlerable
      implements Handlerable
  {

    private int count;

    public DummyHandlerable()
    {
    }

    public boolean post(Runnable runnnable)
    {
      final Thread thread = new Thread(runnnable);
      thread.setName("handlerable #" + count++);
      thread.start();
      return true;
    }

  }

  private final static class Expectations
  {

    public int computeUrl;

    public int hasLocalBitmap;

    public int hasTemporaryBitmap;

    public int onBindLocalBitmap;

    public int onBindTemporaryBitmap;

    public int getInputStream;

    public int downloadInputStream;

    public int onInputStreamDownloaded;

    public int onBitmapReady;

    public int onBindBitmap;

    public int onBitmapBound;

    public int convert;

    private int onOver;

    private boolean markedAsOnOver;

    public Boolean onBitmapBoundResult;

    public void onOver()
    {
      markedAsOnOver = true;
      synchronized (this)
      {
        this.notify();
      }
    }

    public void waitForOnOver()
        throws InterruptedException
    {
      waitForOnOver(5000);
    }

    public void waitForOnOver(int timeOutInMilliseconds)
        throws InterruptedException
    {
      if (markedAsOnOver == true)
      {
        return;
      }
      synchronized (this)
      {
        wait(timeOutInMilliseconds);
      }
      Thread.sleep(200);
    }

  }

  private static class ExpectedInstructions
      implements BasisDownloadInstructions.Instructions<DummyBitmapable, DummyViewable>
  {

    private final Expectations expectations;

    private final boolean hasLocalBitmap;

    private final boolean hasTemporaryBitmap;

    private final SimulationdMethod getInputStreamMethod;

    private final SimulationdMethod downloadBitmapMethod;

    private static enum SimulationdMethod
    {
      Null, FakeSuccess, FakeFailure, ActuallyRun
    }

    public ExpectedInstructions(Expectations expectations, boolean hasLocalBitmap, boolean hasTemporaryBitmap, SimulationdMethod bitmapDownloadMethod)
    {
      this(expectations, hasLocalBitmap, hasTemporaryBitmap, ExpectedInstructions.SimulationdMethod.Null, bitmapDownloadMethod);
    }

    public ExpectedInstructions(Expectations expectations, boolean hasLocalBitmap, boolean hasTemporaryBitmap, SimulationdMethod getInputStreamMethod,
        SimulationdMethod bitmapDownloadMethod)
    {
      this.expectations = expectations;
      this.hasLocalBitmap = hasLocalBitmap;
      this.hasTemporaryBitmap = hasTemporaryBitmap;
      this.getInputStreamMethod = getInputStreamMethod;
      this.downloadBitmapMethod = bitmapDownloadMethod;
    }

    @Override
    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      expectations.computeUrl++;
      return bitmapUid;
    }

    @Override
    public DummyBitmapable hasLocalBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.hasLocalBitmap++;
      return hasLocalBitmap == true ? new DummyBitmapable(100 * 1024) : null;
    }

    @Override
    public DummyBitmapable hasTemporaryBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.hasTemporaryBitmap++;
      return hasTemporaryBitmap == true ? new DummyBitmapable(200 * 1024) : null;
    }

    @Override
    public void onBindLocalBitmap(DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
    {
      expectations.onBindLocalBitmap++;
    }

    @Override
    public void onBindTemporaryBitmap(DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
    {
      expectations.onBindTemporaryBitmap++;
    }

    @Override
    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, InputStreamDownloadInstructor instructor)
        throws IOException
    {
      expectations.getInputStream++;
      switch (getInputStreamMethod)
      {
      case Null:
      default:
        return null;
      case FakeSuccess:
        return generateBitmapInputStream();
      case FakeFailure:
        throw new IOException("Cannot get from the cache the bitmap with URL '" + url + "'");
      }
    }

    @Override
    public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
        throws IOException
    {
      expectations.downloadInputStream++;

      switch (downloadBitmapMethod)
      {
      case ActuallyRun:
      default:
        return downloadBitmap(url);
      case FakeSuccess:
        return generateBitmapInputStream();
      case FakeFailure:
        throw new IOException("Cannot download the bitmap with URL '" + url + "'");
      }
    }

    @Override
    public InputStream onInputStreamDownloaded(String bitmapUid, Object imageSpecs, String url, InputStream inputStream)
    {
      expectations.onInputStreamDownloaded++;
      return inputStream;
    }

    @Override
    public void onBitmapReady(boolean allright, DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
    {
      expectations.onBitmapReady++;
    }

    @Override
    public boolean onBindBitmap(boolean downloaded, DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
    {
      expectations.onBindBitmap++;
      return true;
    }

    @Override
    public void onBitmapBound(boolean result, DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.onBitmapBound++;
      expectations.onBitmapBoundResult = result;
    }

    @Override
    public DummyBitmapable convert(InputStream inputStream, String bitmapUid, Object imageSpecs, String url)
    {
      expectations.convert++;
      return new DummyBitmapable(300 * 1024);
    }

    @Override
    public void onOver(boolean aborted, DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.onOver++;
      expectations.onOver();
    }

    private InputStream downloadBitmap(String url)
        throws MalformedURLException, IOException
    {
      final URL aURL = new URL(url);
      final URLConnection connection = aURL.openConnection();
      connection.connect();
      final InputStream inputStream = connection.getInputStream();
      return inputStream;
    }

  }

  private final String VALID_BITMAP_URL = "http://www.smartnsoft.com/images/home/illu.png";

  private final String INVALID_BITMAP_URL = "http://abcd.smartnsoft.com";

  private BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable> bitmapDownloader;

  private DummyViewable view;

  private DummyHandlerable handler;

  private BasisAnalyticsData analyticsData;

  @Before
  public void setup()
  {
    super.setup();
    BasisBitmapDownloader.IS_DEBUG_TRACE = true;
    BasisBitmapDownloader.IS_DUMP_TRACE = true;
    analyticsData = null;
    bitmapDownloader = new BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable>(0, "0", 4 * 1024 * 1024, 2 * 1024 * 1024, false, false);
    view = new DummyViewable(1234);
    handler = new DummyHandlerable();
    BasisBitmapDownloader.ANALYTICS_LISTENER = new CoreBitmapDownloader.AnalyticsListener()
    {
      @Override
      public void onAnalytics(CoreBitmapDownloader<?, ?, ?> coreBitmapDownloader, CoreAnalyticsData analyticsData)
      {
        BitmapDownloaderTest.this.analyticsData = (BasisAnalyticsData) analyticsData;
      }
    };
  }

  @Test
  public void bitmapValidUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 1;
    final int onInputStreamDownloaded = 1;
    final int onBitmapReady = 1;
    final int onBindBitmap = 1;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.TRUE;
    final int convert = 1;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapInvalidUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, INVALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeFailure));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 1;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 1;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapNullId()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, null, null, handler, new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapComputeUrlException()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, null, null, handler, new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
    {
      @Override
      public String computeUrl(String bitmapUid, Object imageSpecs)
      {
        super.computeUrl(bitmapUid, imageSpecs);
        throw new NullPointerException();
      }
    });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 0;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 0;
    final Boolean onBitmapBoundResult = null;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapNullUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, null, null, handler, new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
    {
      @Override
      public String computeUrl(String bitmapUid, Object imageSpecs)
      {
        super.computeUrl(bitmapUid, imageSpecs);
        return null;
      }
    });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithTemporaryValidUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, true, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 1;
    final int getInputStream = 1;
    final int onInputStreamDownloaded = 1;
    final int onBitmapReady = 1;
    final int onBindBitmap = 1;
    final int onBitmapBound = 1;
    final int convert = 1;
    final Boolean onBitmapBoundResult = Boolean.TRUE;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithTemporaryInvalidUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, INVALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, true, ExpectedInstructions.SimulationdMethod.FakeFailure));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 1;
    final int getInputStream = 1;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 1;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithTemporaryNullId()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, null, null, handler, new ExpectedInstructions(expectations, false, true, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 1;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithTemporaryNullUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, null, null, handler, new ExpectedInstructions(expectations, false, true, ExpectedInstructions.SimulationdMethod.FakeSuccess)
    {
      @Override
      public String computeUrl(String bitmapUid, Object imageSpecs)
      {
        super.computeUrl(bitmapUid, imageSpecs);
        return null;
      }
    });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 1;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithTemporaryNullView()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(null, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, true, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {
          @Override
          public String computeUrl(String bitmapUid, Object imageSpecs)
          {
            super.computeUrl(bitmapUid, imageSpecs);
            return null;
          }
        });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 0;
    final int hasTemporaryBitmap = 0;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithTemporaryNullViewNullUrl()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(null, null, null, handler, new ExpectedInstructions(expectations, false, true, ExpectedInstructions.SimulationdMethod.FakeSuccess)
    {
      @Override
      public String computeUrl(String bitmapUid, Object imageSpecs)
      {
        super.computeUrl(bitmapUid, imageSpecs);
        return null;
      }
    });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 0;
    final int hasTemporaryBitmap = 0;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithLocal()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, true, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {
          @Override
          public void onBindLocalBitmap(DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
          {
            super.onBindLocalBitmap(view, bitmap, bitmapUid, imageSpecs);
            expectations.onOver();
          }
        });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 0;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 0;
    final int onBindLocalBitmap = 1;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.TRUE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapWithLocalWithTemporary()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, true, true, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 0;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 0;
    final int onBindLocalBitmap = 1;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 0;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 0;
    final int onBindBitmap = 0;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.TRUE;
    final int convert = 0;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void bitmapFromInputStream()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, "faked", null, handler, new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
    {
      @Override
      public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, InputStreamDownloadInstructor instructor)
          throws IOException
      {
        super.getInputStream(bitmapUid, imageSpecs, url, instructor);
        return generateBitmapInputStream();
      }
    });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 1;
    final int onInputStreamDownloaded = 0;
    final int onBitmapReady = 1;
    final int onBindBitmap = 1;
    final int onBitmapBound = 1;
    final Boolean onBitmapBoundResult = Boolean.TRUE;
    final int convert = 1;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void sameAvailableBitmapDownloadedMultipleTimes()
      throws InterruptedException
  {
    final Expectations expectations1 = new Expectations();
    final Expectations expectations2 = new Expectations();
    final Expectations expectations3 = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, "get1", handler,
        new ExpectedInstructions(expectations1, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {
          public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
              throws IOException
          {
            final InputStream inputStream = super.downloadInputStream(bitmapUid, imageSpecs, url);
            // We introduce some latency on purpose
            try
            {
              Thread.sleep(200);
            }
            catch (InterruptedException exception)
            {
            }
            return inputStream;
          }
        });
    bitmapDownloader.get(view, VALID_BITMAP_URL, "get2", handler,
        new ExpectedInstructions(expectations2, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));
    // We ask for the same bitmap for another view
    bitmapDownloader.get(new DummyViewable(5678), VALID_BITMAP_URL, "get3", handler,
        new ExpectedInstructions(expectations3, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations1.waitForOnOver();
    expectations2.waitForOnOver();
    expectations3.waitForOnOver();
    // The first command must have been partially aborted because of the second one
    assertPreExpectations(expectations1, true, 1, 1, 1, 0, 0);
    assertPreAndBindingExpectations(expectations2, true, 1, 1, 1, 0, 0, 1, 1, 1, Boolean.TRUE);
    assertPreAndBindingExpectations(expectations3, true, 1, 1, 1, 0, 0, 1, 1, 1, Boolean.TRUE);
    Assert.assertEquals("The 'getInputStream()' method has not been invoked the expected number of times", 1,
        expectations1.getInputStream + expectations2.getInputStream + expectations3.getInputStream);
    Assert.assertEquals("The 'downloadInputStream()' method has not been invoked the expected number of times", 1,
        expectations1.downloadInputStream + expectations2.downloadInputStream + expectations3.downloadInputStream);
    Assert.assertEquals("The 'onBindBitmap()' method has not been invoked the expected number of times", 2,
        expectations1.onBindBitmap + expectations2.onBindBitmap + expectations3.onBindBitmap);
    checkAnalyticsData();
  }

  @Test
  public void sameAvailableBitmapInCacheMultipleTimes()
      throws InterruptedException
  {
    final Expectations expectations1 = new Expectations();
    final Expectations expectations2 = new Expectations();
    final Expectations expectations3 = new Expectations();
    bitmapDownloader.get(
        view,
        VALID_BITMAP_URL,
        "get1",
        handler,
        new ExpectedInstructions(expectations1, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {
          @Override
          public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, InputStreamDownloadInstructor instructor)
              throws IOException
          {
            super.getInputStream(bitmapUid, imageSpecs, url, instructor);
            // We introduce some latency on purpose
            try
            {
              Thread.sleep(200);
            }
            catch (InterruptedException exception)
            {
            }
            return generateBitmapInputStream();
          }
        });
    bitmapDownloader.get(
        view,
        VALID_BITMAP_URL,
        "get2",
        handler,
        new ExpectedInstructions(expectations2, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess, ExpectedInstructions.SimulationdMethod.FakeSuccess));
    // We ask for the same bitmap for another view
    bitmapDownloader.get(
        new DummyViewable(5678),
        VALID_BITMAP_URL,
        "get3",
        handler,
        new ExpectedInstructions(expectations3, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess, ExpectedInstructions.SimulationdMethod.FakeSuccess));

    expectations1.waitForOnOver();
    expectations2.waitForOnOver();
    expectations3.waitForOnOver();
    // The first command must have been partially aborted because of the second one
    assertPreExpectations(expectations1, true, 1, 1, 1, 0, 0);
    assertPreAndBindingExpectations(expectations2, true, 1, 1, 1, 0, 0, 1, 1, 1, Boolean.TRUE);
    assertPreAndBindingExpectations(expectations3, true, 1, 1, 1, 0, 0, 1, 1, 1, Boolean.TRUE);
    Assert.assertEquals("The 'getInputStream()' method has not been invoked the expected number of times", 1,
        expectations1.getInputStream + expectations2.getInputStream + expectations3.getInputStream);
    Assert.assertEquals("The 'downloadInputStream()' method has not been invoked the expected number of times", 0,
        expectations1.downloadInputStream + expectations2.downloadInputStream + expectations3.downloadInputStream);
    Assert.assertEquals("The 'onBindBitmap()' method has not been invoked the expected number of times", 2,
        expectations1.onBindBitmap + expectations2.onBindBitmap + expectations3.onBindBitmap);
    checkAnalyticsData();
  }

  @Test
  public void sameUnavailableBitmapMultipleTimesOnSameView()
      throws InterruptedException
  {
    final Expectations expectations1 = new Expectations();
    final Expectations expectations2 = new Expectations();
    final Expectations expectations3 = new Expectations();
    bitmapDownloader.get(view, INVALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations1, false, false, ExpectedInstructions.SimulationdMethod.FakeFailure)
        {

          public InputStream downloadInputStream(String bitmapUid, Object imageSpecs, String url)
              throws IOException
          {
            super.downloadInputStream(bitmapUid, imageSpecs, url);
            try
            {
              Thread.sleep(200);
            }
            catch (InterruptedException exception)
            {
            }
            // We simulate that the bitmap cannot be downloaded
            return null;
          }

        });
    bitmapDownloader.get(view, INVALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations2, false, false, ExpectedInstructions.SimulationdMethod.FakeFailure));
    bitmapDownloader.get(view, INVALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations3, false, false, ExpectedInstructions.SimulationdMethod.FakeFailure));

    expectations3.waitForOnOver();
    assertPreExpectations(expectations1, true, 1, 1, 1, 0, 0);
    assertPreExpectations(expectations2, true, 1, 1, 1, 0, 0);
    assertPreExpectations(expectations3, true, 1, 1, 1, 0, 0);
    assertBindingExpectations(expectations3, true, 1, 0, 1, Boolean.FALSE);
    checkAnalyticsData();
  }

  @Test
  public void bitmapViaDownloadInstructor()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {

          @Override
          public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, final InputStreamDownloadInstructor instructor)
              throws IOException
          {
            super.getInputStream(bitmapUid, imageSpecs, url, instructor);
            instructor.setAsynchronous();
            new Thread(new Runnable()
            {
              public void run()
              {
                try
                {
                  Thread.sleep(500);
                }
                catch (InterruptedException exception)
                {
                  // Does not matter
                }
                instructor.onDownloaded(generateBitmapInputStream());
              }
            }).start();
            return null;
          }

        });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = 1;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = 1;
    final int onInputStreamDownloaded = 1;
    final int onBitmapReady = 1;
    final int onBindBitmap = 1;
    final int onBitmapBound = 1;
    final int convert = 1;
    final Boolean onBitmapBoundResult = Boolean.TRUE;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    checkAnalyticsData();
  }

  @Test
  public void onBindBitmapException()
      throws InterruptedException
  {
    onBindBitmapExceptionInternal(false, new NullPointerException(), null);
    checkAnalyticsData();
  }

  @Test
  public void onBindBitmapExceptionFromCache()
      throws InterruptedException
  {
    onBindBitmapExceptionInternal(false, new NullPointerException(), null);
    onBindBitmapExceptionInternal(true, new NullPointerException(), null);
    checkAnalyticsData();
  }

  @Test
  public void onBindBitmapOutOfMemoryException()
      throws InterruptedException
  {
    onBindBitmapExceptionInternal(false, null, new OutOfMemoryError());
    checkAnalyticsData();
  }

  @Test
  public void onBindBitmapOutOfMemoryExceptionFromCache()
      throws InterruptedException
  {
    onBindBitmapExceptionInternal(false, null, new OutOfMemoryError());
    onBindBitmapExceptionInternal(true, null, new OutOfMemoryError());
    checkAnalyticsData();
  }

  @Test
  public void onCommandOverwrittenWithLocalBitmap()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    final Expectations secondExpectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, true, false, ExpectedInstructions.SimulationdMethod.Null)
        {
          @Override
          public DummyBitmapable hasLocalBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
          {
            // We take the opportunity to ask a new BitmapDownloader "get" command while the first is asking for a local bitmap
            bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
                new ExpectedInstructions(secondExpectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));
            return super.hasLocalBitmap(view, bitmapUid, imageSpecs);
          }
        });
    secondExpectations.waitForOnOver();

    {
      final boolean onOver = true;
      final int computeUrl = 0;
      final int hasLocalBitmap = 1;
      final int hasTemporaryBitmap = 0;
      final int onBindLocalBitmap = 0;
      final int onBindTemporaryBitmap = 0;
      final int getInputStream = 0;
      final int onInputStreamDownloaded = 0;
      final int onBitmapReady = 0;
      final int onBindBitmap = 0;
      final int onBitmapBound = 0;
      final int convert = 0;
      final Boolean onBitmapBoundResult = null;
      assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
          onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    }
    {
      final boolean onOver = true;
      final int computeUrl = 1;
      final int hasLocalBitmap = 1;
      final int hasTemporaryBitmap = 1;
      final int onBindLocalBitmap = 0;
      final int onBindTemporaryBitmap = 0;
      final int getInputStream = 1;
      final int onInputStreamDownloaded = 1;
      final int onBitmapReady = 1;
      final int onBindBitmap = 1;
      final int onBitmapBound = 1;
      final int convert = 1;
      final Boolean onBitmapBoundResult = Boolean.TRUE;
      assertAllExpectations(secondExpectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap,
          onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
      checkAnalyticsData();
    }
  }

  @Test
  public void onCommandOverwrittenWithDownloadRequired()
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    final Expectations secondExpectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {
          @Override
          public DummyBitmapable hasLocalBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
          {
            // We take the opportunity to ask a new BitmapDownloader "get" command while the first is asking for a local bitmap
            bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
                new ExpectedInstructions(secondExpectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));
            return super.hasLocalBitmap(view, bitmapUid, imageSpecs);
          }
        });
    secondExpectations.waitForOnOver();

    {
      final boolean onOver = true;
      final int computeUrl = 1;
      final int hasLocalBitmap = 1;
      final int hasTemporaryBitmap = 1;
      final int onBindLocalBitmap = 0;
      final int onBindTemporaryBitmap = 0;
      final int getInputStream = 0;
      final int onInputStreamDownloaded = 0;
      final int onBitmapReady = 0;
      final int onBindBitmap = 0;
      final int onBitmapBound = 0;
      final int convert = 0;
      final Boolean onBitmapBoundResult = null;
      assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
          onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    }
    {
      final boolean onOver = true;
      final int computeUrl = 1;
      final int hasLocalBitmap = 1;
      final int hasTemporaryBitmap = 1;
      final int onBindLocalBitmap = 0;
      final int onBindTemporaryBitmap = 0;
      final int getInputStream = 1;
      final int onInputStreamDownloaded = 1;
      final int onBitmapReady = 1;
      final int onBindBitmap = 1;
      final int onBitmapBound = 1;
      final int convert = 1;
      final Boolean onBitmapBoundResult = Boolean.TRUE;
      assertAllExpectations(secondExpectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap,
          onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    }
    checkAnalyticsData();
  }

  @Test
  public void onCommandOverwrittenDuringDownload()
      throws InterruptedException
  {
    final Expectations firstExpectations = new Expectations();
    final Expectations secondExpectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(firstExpectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess)
        {
          @Override
          public InputStream onInputStreamDownloaded(String bitmapUid, Object imageSpecs, String url, InputStream inputStream)
          {
            // We take the opportunity to ask a new BitmapDownloader "get" command while the first is asking for a local bitmap
            bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
                new ExpectedInstructions(secondExpectations, false, false, ExpectedInstructions.SimulationdMethod.FakeSuccess));
            return super.onInputStreamDownloaded(bitmapUid, imageSpecs, url, inputStream);
          }
        });
    firstExpectations.waitForOnOver();
    secondExpectations.waitForOnOver();

    {
      final boolean onOver = true;
      final int computeUrl = 1;
      final int hasLocalBitmap = 1;
      final int hasTemporaryBitmap = 1;
      final int onBindLocalBitmap = 0;
      final int onBindTemporaryBitmap = 0;
      final int getInputStream = 1;
      final int onInputStreamDownloaded = 1;
      final int onBitmapReady = 1;
      final int onBindBitmap = 0;
      final int onBitmapBound = 0;
      final int convert = 1;
      final Boolean onBitmapBoundResult = null;
      assertAllExpectations(firstExpectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
          onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    }
    {
      final boolean onOver = true;
      final int computeUrl = 1;
      final int hasLocalBitmap = 1;
      final int hasTemporaryBitmap = 1;
      final int onBindLocalBitmap = 0;
      final int onBindTemporaryBitmap = 0;
      final int getInputStream = 0;
      final int onInputStreamDownloaded = 0;
      final int onBitmapReady = 1;
      final int onBindBitmap = 1;
      final int onBitmapBound = 1;
      final int convert = 0;
      final Boolean onBitmapBoundResult = Boolean.TRUE;
      assertAllExpectations(secondExpectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap,
          onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    }
    checkAnalyticsData();
  }

  private void onBindBitmapExceptionInternal(boolean fromCache, final RuntimeException exception, final Error error)
      throws InterruptedException
  {
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(view, VALID_BITMAP_URL, null, handler,
        new ExpectedInstructions(expectations, false, false, ExpectedInstructions.SimulationdMethod.ActuallyRun)
        {

          @Override
          public boolean onBindBitmap(boolean downloaded, DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
          {
            super.onBindBitmap(downloaded, view, bitmap, bitmapUid, imageSpecs);
            if (exception != null)
            {
              throw exception;
            }
            else
            {
              throw error;
            }
          }

        });

    expectations.waitForOnOver();
    final boolean onOver = true;
    final int computeUrl = 1;
    final int hasLocalBitmap = 1;
    final int hasTemporaryBitmap = fromCache == false ? 1 : 0;
    final int onBindLocalBitmap = 0;
    final int onBindTemporaryBitmap = 0;
    final int getInputStream = fromCache == false ? 1 : 0;
    final int onInputStreamDownloaded = fromCache == false ? 1 : 0;
    final int onBitmapReady = 1;
    final int onBindBitmap = 1;
    final int onBitmapBound = 1;
    final int convert = fromCache == false ? 1 : 0;
    final Boolean onBitmapBoundResult = Boolean.FALSE;
    assertAllExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, onBitmapReady,
        onBindBitmap, onBitmapBound, onBitmapBoundResult, getInputStream, onInputStreamDownloaded, convert);
    final AtomicInteger prioritiesPreStack = new AtomicInteger();
    final AtomicInteger prioritiesStack = new AtomicInteger();
    final AtomicInteger prioritiesDownloadStack = new AtomicInteger();
    bitmapDownloader.getStacks(prioritiesPreStack, prioritiesStack, prioritiesDownloadStack, new AtomicInteger(), new AtomicInteger());
    Assert.assertEquals("The 'prioritiesPreStack' size does not have the right size", 0, prioritiesPreStack.get());
    Assert.assertEquals("The 'prioritiesStack' size does not have the right size", 0, prioritiesStack.get());
    Assert.assertEquals("The 'prioritiesDownloadStack' size does not have the right size", 0, prioritiesDownloadStack.get());
    checkAnalyticsData();
  }

  private void checkAnalyticsData()
  {
    bitmapDownloader.notifyAnalyticsListener();
    Assert.assertNotNull("The 'analyticsData' should not be null", analyticsData);
    Assert.assertEquals("The 'analyticsData' in-progress downloads size is incorrect", 0, analyticsData.inProgressDownloadsSize);
    Assert.assertEquals("The 'analyticsData' priorities-download stack size is incorrect", 0, analyticsData.prioritiesDownloadStackSize);
    Assert.assertEquals("The 'analyticsData' priorities-pre stack size is incorrect", 0, analyticsData.prioritiesPreStackSize);
    Assert.assertEquals("The 'analyticsData' priorities stack size is incorrect", 0, analyticsData.prioritiesStackSize);
  }

  private void assertAllExpectations(final Expectations expectations, boolean onOver, final int computeUrl, final int hasLocalBitmap,
      final int hasTemporaryBitmap, final int onBindLocalBitmap, final int onBindTemporaryBitmap, final int onBitmapReady, final int onBindBitmap,
      final int onBitmapBound, final Boolean onBitmapBoundResult, final int getInputStream, final int onInputStreamDownloaded, final int convert)
  {
    assertPreAndBindingExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap,
        onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult);
    Assert.assertEquals("The 'getInputStream()' method has not been invoked the expected number of times", getInputStream, expectations.getInputStream);
    Assert.assertEquals("The 'onInputStreamDownloaded()' method has not been invoked the expected number of times", onInputStreamDownloaded,
        expectations.onInputStreamDownloaded);
    Assert.assertEquals("The 'convert()' method has not been invoked the expected number of times", convert, expectations.convert);
  }

  private void assertPreAndBindingExpectations(final Expectations expectations, boolean onOver, final int computeUrl, final int hasLocalBitmap,
      final int hasTemporaryBitmap, final int onBindLocalBitmap, final int onBindTemporaryBitmap, final int onBitmapReady, final int onBindBitmap,
      final int onBitmapBound, final Boolean onBitmapBoundResult)
  {
    assertPreExpectations(expectations, onOver, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap);
    assertBindingExpectations(expectations, onOver, onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult);
  }

  private void assertPreExpectations(final Expectations expectations, boolean onOver, final int computeUrl, final int hasLocalBitmap,
      final int hasTemporaryBitmap, final int onBindLocalBitmap, final int onBindTemporaryBitmap)
  {
    Assert.assertEquals("The 'computeUrl()' method has not been invoked the expected number of times", computeUrl, expectations.computeUrl);
    Assert.assertEquals("The 'hasLocalBitmap()' method has not been invoked the expected number of times", hasLocalBitmap, expectations.hasLocalBitmap);
    Assert.assertEquals("The 'hasTemporaryBitmap()' method has not been invoked the expected number of times", hasTemporaryBitmap,
        expectations.hasTemporaryBitmap);
    Assert.assertEquals("The 'onBindLocalBitmap()' method has not been invoked the expected number of times", onBindLocalBitmap, expectations.onBindLocalBitmap);
    Assert.assertEquals("The 'onBindTemporaryBitmap()' method has not been invoked the expected number of times", onBindTemporaryBitmap,
        expectations.onBindTemporaryBitmap);
    Assert.assertEquals("The 'onOver()' method has not been invoked the expected number of times", onOver == true ? 1 : 0, expectations.onOver);
  }

  private void assertBindingExpectations(final Expectations expectations, boolean onOver, final int onBitmapReady, final int onBindBitmap,
      final int onBitmapBound, final Boolean onBitmapBoundResult)
  {
    Assert.assertEquals("The 'onBitmapReady()' method has not been invoked the expected number of times", onBitmapReady, expectations.onBitmapReady);
    Assert.assertEquals("The 'onBindBitmap()' method has not been invoked the expected number of times", onBindBitmap, expectations.onBindBitmap);
    Assert.assertEquals("The 'onBitmapBound()' method has not been invoked the expected number of times", onBitmapBound, expectations.onBitmapBound);
    Assert.assertEquals("The 'onBitmapBound()' method has been invoked with the expected 'result' value", onBitmapBoundResult, expectations.onBitmapBoundResult);
    Assert.assertEquals("The 'onOver()' method has not been invoked the expected number of times", onOver == true ? 1 : 0, expectations.onOver);
  }

  private static ByteArrayInputStream generateBitmapInputStream()
  {
    return new ByteArrayInputStream(new byte[300 * 200 * 4]);
  }

}
