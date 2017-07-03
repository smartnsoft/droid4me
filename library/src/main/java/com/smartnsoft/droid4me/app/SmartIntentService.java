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

package com.smartnsoft.droid4me.app;

import android.app.IntentService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A {@link IntentService} with the logging feature.
 *
 * @author Édouard Mercier
 * @since 2011.01.30
 */
public abstract class SmartIntentService
    extends IntentService
{

  protected static final Logger log = LoggerFactory.getInstance(SmartIntentService.class);

  private SharedPreferences preferences;

  public SmartIntentService()
  {
    this(null);
  }

  public SmartIntentService(String name)
  {
    super(name);
  }

  /**
   * Should always be invoked first by the derived classes.
   */
  @Override
  public void onCreate()
  {
    super.onCreate();
    preferences = PreferenceManager.getDefaultSharedPreferences(this);
  }

  /**
   * @return the preferences of the application
   */
  protected final SharedPreferences getPreferences()
  {
    return preferences;
  }

}
