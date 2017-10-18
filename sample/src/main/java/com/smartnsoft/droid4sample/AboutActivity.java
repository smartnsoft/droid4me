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

package com.smartnsoft.droid4sample;

import java.io.IOException;
import java.io.InputStream;

import android.util.Xml.Encoding;
import android.webkit.WebView;

import com.smartnsoft.droid4me.LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy;
import com.smartnsoft.droid4me.app.SmartActivity;

import com.smartnsoft.droid4sample.ws.Droid4SampleServices;

/**
 * The "about" screen.
 *
 * @author Édouard Mercier
 * @since 2011.10.19
 */
@BusinessObjectsRetrievalAsynchronousPolicy
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
