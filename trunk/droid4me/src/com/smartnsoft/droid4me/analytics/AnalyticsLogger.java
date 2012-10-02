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
 * 
 * <p>
 * Using this marker interface with the {@link ActivityController.Interceptor} makes the reporting of analytics very straightforward, and enables to
 * centralize it in one place.
 * </p>
 * 
 * @see AnalyticsSender
 * @author Édouard Mercier
 * @since 2012.06.02
 */
public interface AnalyticsLogger
{

  /**
   * An analytics logger which states the tag of the analytics event to report.
   * 
   * <p>
   * This interface is to be used when the name of the tag is not known at compile time ; otherwise, the
   * {@link AnalyticsLogger.AnalyticsTagLoggerAnnotation} annotation should be used instead.
   * </p>
   * 
   * @see AnalyticsLogger.AnalyticsTagLoggerAnnotation
   */
  public static interface AnalyticsTagLogger
      extends AnalyticsLogger
  {

    /**
     * @return the tag that will be used when reporting some usage information about the underlying entity ; it may return {@code null}
     */
    String getTag();

  }

  /**
   * An analytics logger which states the tag and a dictionary of the analytics event to report.
   * 
   * @param <DictionaryType>
   *          the type of the dictionary used when reporting events
   */
  public static interface AnalyticsDictionaryLogger<DictionaryType>
      extends AnalyticsLogger.AnalyticsTagLogger
  {

    /**
     * @return the dictionary that will be used when reporting some usage information about the underlying entity ; it may return {@code null}
     */
    DictionaryType getDictionary();

  }

  /**
   * An annotation which enables to express that an entity, usually an {@link Activity} or a {@link Fragment}, wants to report analytics usage.
   * 
   * <p>
   * Use this annotation, when the entity tag is known at compile tag.
   * </p>
   * 
   * @see AnalyticsLogger.AnalyticsTagLogger
   * @see AnalyticsLogger.AnalyticsTagLoggerAnnotation
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface AnalyticsTagLoggerAnnotation
  {

    /**
     * @return the tag that will be used when reporting some usage information about the underlying entity ; it may return {@code null}
     */
    String tag();

  }

  /**
   * An annotation which enables to express that an entity, usually an {@link Activity} or a {@link Fragment}, does not wish to report any analytics
   * usage.
   * 
   * <p>
   * Use this annotation, when you do not want the underlying entity to report any activity.
   * </p>
   * 
   * @see AnalyticsLogger.AnalyticsTagLogger
   * @see AnalyticsLogger.AnalyticsTagLoggerAnnotation
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface AnalyticsDisabledLoggerAnnotation
  {

  }

}
