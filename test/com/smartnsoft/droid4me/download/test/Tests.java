package com.smartnsoft.droid4me.download.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import android.util.Log;

import com.smartnsoft.droid4me.download.BasisBitmapDownloader;
import com.smartnsoft.droid4me.download.BasisDownloadInstructions;
import com.smartnsoft.droid4me.download.BasisDownloadInstructions.InputStreamDownloadInstructor;
import com.smartnsoft.droid4me.download.DownloadContracts.Bitmapable;
import com.smartnsoft.droid4me.download.DownloadContracts.Handlerable;
import com.smartnsoft.droid4me.download.DownloadContracts.Viewable;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * @author Édouard Mercier
 * @since 2011.09.03
 */
public final class Tests
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

    public DummyHandlerable()
    {
    }

    public boolean post(Runnable runnnable)
    {
      new Thread(runnnable).start();
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

    public int onInputStreamDownloaded;

    public int onBitmapReady;

    public int onBindBitmap;

    public int onBitmapBound;

    public int convert;

    public boolean onOver;

    public Boolean onBitmapBoundResult;

    public void onOver()
    {
      onOver = true;
      synchronized (this)
      {
        this.notify();
      }
    }

    public void waitForOnOver()
        throws InterruptedException
    {
      if (onOver == true)
      {
        return;
      }
      synchronized (this)
      {
        wait(5000);
      }
    }

  }

  private static class ExpectedInstructions
      implements BasisDownloadInstructions.Instructions<DummyBitmapable, DummyViewable>
  {

    private final Expectations expectations;

    public ExpectedInstructions(Expectations expectations)
    {
      this.expectations = expectations;
    }

    public String computeUrl(String bitmapUid, Object imageSpecs)
    {
      expectations.computeUrl++;
      return bitmapUid;
    }

    public boolean hasLocalBitmap(String bitmapUid, Object imageSpecs)
    {
      expectations.hasLocalBitmap++;
      return false;
    }

    public boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs)
    {
      expectations.hasTemporaryBitmap++;
      return false;
    }

    public void onBindLocalBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.onBindLocalBitmap++;
    }

    public void onBindTemporaryBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.onBindTemporaryBitmap++;
    }

    public InputStream getInputStream(String bitmapUid, Object imageSpecs, String url, InputStreamDownloadInstructor instructor)
        throws IOException
    {
      expectations.getInputStream++;
      return null;
    }

    public void onBeforeBitmapDownloaded(String bitmapUid, Object imageSpecs, URLConnection urlConnection)
    {
    }

    public InputStream onInputStreamDownloaded(String bitmapUid, Object imageSpecs, String url, InputStream inputStream)
    {
      expectations.onInputStreamDownloaded++;
      return inputStream;
    }

    public void onBitmapReady(boolean allright, DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
    {
      expectations.onBitmapReady++;
    }

    public boolean onBindBitmap(boolean downloaded, DummyViewable view, DummyBitmapable bitmap, String bitmapUid, Object imageSpecs)
    {
      expectations.onBindBitmap++;
      return true;
    }

    public void onBitmapBound(boolean result, DummyViewable view, String bitmapUid, Object imageSpecs)
    {
      expectations.onBitmapBound++;
      expectations.onBitmapBoundResult = result;
      expectations.onOver();
    }

    public DummyBitmapable convert(InputStream inputStream, String bitmapUid, Object imageSpecs)
    {
      expectations.convert++;
      return new DummyBitmapable(300 * 1024);
    }

  }

  @Before
  public void setup()
  {
    LoggerFactory.logLevel = Log.DEBUG;
  }

  @Test
  public void bitmapWithLocal()
      throws InterruptedException
  {
    final BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable> bitmapDownloader = new BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable>("0", 4 * 1024 * 1024, 2 * 1024 * 1024, false, false);
    final DummyHandlerable handler = new DummyHandlerable();
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(new DummyViewable(1234), "http://www.smartnsoft.com/images/site_en_construction.png", null, handler,
        new ExpectedInstructions(expectations)
        {

          @Override
          public boolean hasLocalBitmap(String bitmapUid, Object imageSpecs)
          {
            super.hasLocalBitmap(bitmapUid, imageSpecs);
            return true;
          }

          @Override
          public void onBindLocalBitmap(DummyViewable view, String bitmapUid, Object imageSpecs)
          {
            super.onBindLocalBitmap(view, bitmapUid, imageSpecs);
            expectations.onOver();
          }

        });

    expectations.waitForOnOver();
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
    assertExpectations(expectations, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, getInputStream,
        onInputStreamDownloaded, onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, convert);
  }

  @Test
  public void bitmapWithNoLocalNoTemporaryValidUrl()
      throws InterruptedException
  {
    final BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable> bitmapDownloader = new BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable>("0", 4 * 1024 * 1024, 2 * 1024 * 1024, false, false);
    final DummyHandlerable handler = new DummyHandlerable();
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(new DummyViewable(1234), "http://www.smartnsoft.com/images/site_en_construction.png", null, handler,
        new ExpectedInstructions(expectations));

    expectations.waitForOnOver();
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
    assertExpectations(expectations, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, getInputStream,
        onInputStreamDownloaded, onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, convert);
  }

  @Test
  public void bitmapWithTemporaryValidUrl()
      throws InterruptedException
  {
    final BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable> bitmapDownloader = new BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable>("0", 4 * 1024 * 1024, 2 * 1024 * 1024, false, false);
    final DummyHandlerable handler = new DummyHandlerable();
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(new DummyViewable(1234), "http://www.smartnsoft.com/images/site_en_construction.png", null, handler,
        new ExpectedInstructions(expectations)
        {
          @Override
          public boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs)
          {
            super.hasTemporaryBitmap(bitmapUid, imageSpecs);
            return true;
          }
        });

    expectations.waitForOnOver();
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
    assertExpectations(expectations, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, getInputStream,
        onInputStreamDownloaded, onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, convert);
  }

  @Test
  public void bitmapWithTemporaryNullId()
      throws InterruptedException
  {
    final BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable> bitmapDownloader = new BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable>("0", 4 * 1024 * 1024, 2 * 1024 * 1024, false, false);
    final DummyHandlerable handler = new DummyHandlerable();
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(new DummyViewable(1234), "http://abcd.smartnsoft.com", null, handler, new ExpectedInstructions(expectations)
    {
      @Override
      public boolean hasTemporaryBitmap(String bitmapUid, Object imageSpecs)
      {
        super.hasTemporaryBitmap(bitmapUid, imageSpecs);
        return true;
      }
    });

    expectations.waitForOnOver();
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
    assertExpectations(expectations, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, getInputStream,
        onInputStreamDownloaded, onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, convert);
  }

  @Test
  public void bitmapWithNoTemporaryNullId()
      throws InterruptedException
  {
    final BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable> bitmapDownloader = new BasisBitmapDownloader<DummyBitmapable, DummyViewable, DummyHandlerable>("0", 4 * 1024 * 1024, 2 * 1024 * 1024, false, false);
    final DummyHandlerable handler = new DummyHandlerable();
    final Expectations expectations = new Expectations();
    bitmapDownloader.get(new DummyViewable(1234), null, null, handler, new ExpectedInstructions(expectations));

    expectations.waitForOnOver();
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
    assertExpectations(expectations, computeUrl, hasLocalBitmap, hasTemporaryBitmap, onBindLocalBitmap, onBindTemporaryBitmap, getInputStream,
        onInputStreamDownloaded, onBitmapReady, onBindBitmap, onBitmapBound, onBitmapBoundResult, convert);
  }

  private void assertExpectations(final Expectations expectations, final int computeUrl, final int hasLocalBitmap, final int hasTemporaryBitmap,
      final int onBindLocalBitmap, final int onBindTemporaryBitmap, final int getInputStream, final int onInputStreamDownloaded, final int onBitmapReady,
      final int onBindBitmap, final int onBitmapBound, final Boolean onBitmapBoundResult, final int convert)
  {
    Assert.assertEquals("The 'computeUrl()' method has not been invoked the expected number of times", computeUrl, expectations.computeUrl);
    Assert.assertEquals("The 'hasLocalBitmap()' method has not been invoked the expected number of times", hasLocalBitmap, expectations.hasLocalBitmap);
    Assert.assertEquals("The 'hasTemporaryBitmap()' method has not been invoked the expected number of times", hasTemporaryBitmap,
        expectations.hasTemporaryBitmap);
    Assert.assertEquals("The 'onBindLocalBitmap()' method has not been invoked the expected number of times", onBindLocalBitmap, expectations.onBindLocalBitmap);
    Assert.assertEquals("The 'onBindTemporaryBitmap()' method has not been invoked the expected number of times", onBindTemporaryBitmap,
        expectations.onBindTemporaryBitmap);
    Assert.assertEquals("The 'getInputStream()' method has not been invoked the expected number of times", getInputStream, expectations.getInputStream);
    Assert.assertEquals("The 'onInputStreamDownloaded()' method has not been invoked the expected number of times", onInputStreamDownloaded,
        expectations.onInputStreamDownloaded);
    Assert.assertEquals("The 'onBitmapReady()' method has not been invoked the expected number of times", onBitmapReady, expectations.onBitmapReady);
    Assert.assertEquals("The 'onBindBitmap()' method has not been invoked the expected number of times", onBindBitmap, expectations.onBindBitmap);
    Assert.assertEquals("The 'onBitmapBound()' method has not been invoked the expected number of times", onBitmapBound, expectations.onBitmapBound);
    Assert.assertEquals("The 'onBitmapBound()' method has been invoked with the 'result' value", onBitmapBoundResult, expectations.onBitmapBoundResult);
    Assert.assertEquals("The 'convert()' method has not been invoked the expected number of times", convert, expectations.convert);
  }

}
