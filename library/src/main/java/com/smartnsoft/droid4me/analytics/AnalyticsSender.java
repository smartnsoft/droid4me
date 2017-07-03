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

package com.smartnsoft.droid4me.analytics;

import android.app.Activity;
import android.content.Context;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A very basic abstraction of an agent which is able to send analytics.
 *
 * @param <DictionaryType> the type of the dictionary used when reporting events
 * @author Édouard Mercier
 * @see AnalyticsLogger
 * @since 2012.06.02
 */
public interface AnalyticsSender<DictionaryType>
{

  Logger log = LoggerFactory.getInstance(AnalyticsSender.class);

  /**
   * Logs an analytics event.
   *
   * @param context    the Android context this event is associated with
   * @param tag        the tag to use when reporting the event ; it is not authorized to be {@code null}
   * @param dictionary the dictionary to use when reporting the event ; may be {@code null}
   */
  void logEvent(Context context, String tag, DictionaryType dictionary);

  /**
   * Logs an error event.
   *
   * @param context    the Android context this event is associated with
   * @param name       the name to use when reporting the error ; it is not authorized to be {@code null}
   * @param dictionary the dictionary to use when reporting the error ; may be {@code null}
   */
  void logError(Context context, String name, DictionaryType dictionary);

  /**
   * Logs the start or resume of an {@link Activity} or {@code Fragment} (i.e. an entity).
   *
   * @param activity   the {@link Activity} the event applies on
   * @param tag        the tag to use when reporting the event ; it may be {@code null}, but not empty
   * @param dictionary the dictionary to use when reporting the event ; may be {@code null}
   */
  void onStartActivity(Activity activity, String tag, DictionaryType dictionary);

  /**
   * Logs the end or pause of an {@link Activity} or {@code Fragment} (i.e. an entity).
   *
   * @param activity the {@link Activity} the event applies on
   */
  void onEndActivity(Activity activity);

}
