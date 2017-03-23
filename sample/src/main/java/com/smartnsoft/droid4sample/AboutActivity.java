package com.smartnsoft.droid4sample;

import java.io.IOException;
import java.io.InputStream;

import android.util.Xml.Encoding;
import android.webkit.WebView;

import com.smartnsoft.droid4me.LifeCycle.BusinessObjectsRetrievalAsynchronousPolicyAnnotation;
import com.smartnsoft.droid4me.app.SmartActivity;

import com.smartnsoft.droid4sample.ws.Droid4SampleServices;

/**
 * The "about" screen.
 *
 * @author Édouard Mercier
 * @since 2011.10.19
 */
@BusinessObjectsRetrievalAsynchronousPolicyAnnotation
public final class AboutActivity
    extends SmartActivity<TitleBar.TitleBarAggregate>
    implements TitleBar.TitleBarShowHomeFeature
{

  private String content;

  private WebView webView;

  public void onRetrieveDisplayObjects()
  {
    setContentView(R.layout.about);
    webView = (WebView) findViewById(R.id.webView);
    webView.getSettings().setSupportMultipleWindows(false);
    webView.getSettings().setSupportZoom(false);
    webView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    final InputStream inputStream = getResources().openRawResource(R.raw.about);
    try
    {
      content = Droid4SampleServices.getInstance().getString(inputStream);
    }
    catch (IOException exception)
    {
      throw new BusinessObjectUnavailableException(exception);
    }
  }

  public void onFulfillDisplayObjects()
  {
    webView.loadDataWithBaseURL("file:///android_asset/", content, "text/html", Encoding.UTF_8.toString(), null);
  }

  public void onSynchronizeDisplayObjects()
  {
  }

}
