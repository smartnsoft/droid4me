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

import android.app.Activity;
import android.view.LayoutInflater;

import com.smartnsoft.droid4me.LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy;
import com.smartnsoft.droid4me.app.SmartSplashScreenActivity;

/**
 * The first activity displayed while the application is loading.
 *
 * @author Édouard Mercier
 * @since 2011.10.19
 */
@BusinessObjectsRetrievalAsynchronousPolicy
public final class Droid4SampleSplashScreenActivity
    extends SmartSplashScreenActivity<TitleBar.TitleBarAggregate, Void>
    implements TitleBar.TitleBarDiscarded
{

  @Override
  protected Class<? extends Activity> getNextActivity()
  {
    return AboutActivity.class;
  }

  @Override
  protected void onRetrieveDisplayObjectsCustom()
  {
    setContentView(LayoutInflater.from(this).inflate(R.layout.droid4sample_splash_screen, null));
    setProgressBarIndeterminateVisibility(true);
  }

  @Override
  protected Void onRetrieveBusinessObjectsCustom()
      throws BusinessObjectUnavailableException
  {
    try
    {
      Thread.sleep(15000);
    }
    catch (InterruptedException exception)
    {
      if (log.isErrorEnabled())
      {
        log.error("An interruption occurred while displaying the splash screen", exception);
      }
    }
    return null;
  }

}
