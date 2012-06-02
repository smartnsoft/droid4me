/*
 * (C) Copyright 2009-2012 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.analytics;

import android.app.Activity;
import android.content.Context;

/**
 * A very basic abstraction of an agent which is able to send analytics.
 * 
 * @param <DictionaryType>
 *          the type of the dictionary used when reporting events
 * 
 * @see AnalyticsLogger
 * @author Édouard Mercier
 * @since 2012.06.02
 */
public interface AnalyticsSender<DictionaryType>
{

  /**
   * Logs an analytics event.
   * 
   * @param context
   *          the Android context this event is associated with
   * @param tag
   *          the tag to use when reporting the event ; it is not authorized to be {@code null}
   * @param dictionary
   *          the dictionary to use when reporting the event ; may be {@code null}
   */
  void logEvent(Context context, String tag, DictionaryType dictionary);

  /**
   * Logs the start or resume of an {@link Activity}.
   * 
   * @param activity
   *          the {@link Activity} the event applies on
   * @param tag
   *          the tag to use when reporting the event ; it may be {@code null}, but not empty
   * @param dictionary
   *          the dictionary to use when reporting the event ; may be {@code null}
   */
  void onStartActivity(Activity activity, String tag, DictionaryType dictionary);

  /**
   * Logs the end or pause of an {@link Activity}.
   * 
   * @param activity
   *          the {@link Activity} the event applies on
   */
  void onEndActivity(Activity activity);

}
