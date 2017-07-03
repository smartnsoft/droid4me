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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import android.app.Activity;
import android.app.Fragment;

import com.smartnsoft.droid4me.app.ActivityController;

/**
 * Enables to mark that an entity needs to log some analytics when being used. This interface is usually applied to an {@link Activity} or a
 * {@link Fragment} entity.
 * <p>
 * <p>
 * Using this marker interface with the {@link ActivityController.Interceptor} makes the reporting of analytics very straightforward, and enables to
 * centralize it in one place.
 * </p>
 *
 * @author Édouard Mercier
 * @see AnalyticsSender
 * @since 2012.06.02
 */
public interface AnalyticsLogger
{

  /**
   * An analytics logger which states the tag of the analytics event to report.
   * <p>
   * <p>
   * This interface is to be used when the name of the tag is not known at compile time ; otherwise, the
   * {@link AnalyticsLogger.AnalyticsTagLoggerAnnotation} annotation should be used instead.
   * </p>
   *
   * @see AnalyticsLogger.AnalyticsTagLoggerAnnotation
   */
  interface AnalyticsTagLogger
      extends AnalyticsLogger
  {

    /**
     * @return the tag that will be used when reporting some usage information about the underlying entity ; it may return {@code null}
     */
    String getAnalyticsTag();

  }

  /**
   * An analytics logger which states the tag and a dictionary of the analytics event to report.
   *
   * @param <DictionaryType> the type of the dictionary used when reporting events
   */
  interface AnalyticsDictionaryLogger<DictionaryType>
      extends AnalyticsLogger.AnalyticsTagLogger
  {

    /**
     * @return the dictionary that will be used when reporting some usage information about the underlying entity ; it may return {@code null}
     */
    DictionaryType getDictionary();

  }

  /**
   * An annotation which enables to express that an entity, usually an {@link Activity} or a {@link Fragment}, wants to report analytics usage.
   * <p>
   * <p>
   * Use this annotation, when the entity tag is known at compile tag.
   * </p>
   *
   * @see AnalyticsLogger.AnalyticsTagLogger
   * @see AnalyticsLogger.AnalyticsTagLoggerAnnotation
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface AnalyticsTagLoggerAnnotation
  {

    /**
     * @return the tag that will be used when reporting some usage information about the underlying entity ; it may return {@code null}
     */
    String tag();

  }

  /**
   * An annotation which enables to express that an entity, usually an {@link Activity} or a {@link Fragment}, wishes to report analytics usage.
   * <p>
   * <p>
   * Use this annotation, when you explicitly want the underlying entity to report an activity.
   * </p>
   *
   * @see AnalyticsLogger.AnalyticsDisabledLoggerAnnotation
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface AnalyticsEnabledLoggerAnnotation
  {

  }

  /**
   * An annotation which enables to express that an entity, usually an {@link Activity} or a {@link Fragment}, does not wish to report any analytics
   * usage.
   * <p>
   * <p>
   * Use this annotation, when you do not want the underlying entity to report any activity.
   * </p>
   *
   * @see AnalyticsLogger.AnalyticsTagLogger
   * @see AnalyticsLogger.AnalyticsTagLoggerAnnotation
   * @see AnalyticsLogger.AnalyticsEnabledLoggerAnnotation
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @interface AnalyticsDisabledLoggerAnnotation
  {

  }

}
