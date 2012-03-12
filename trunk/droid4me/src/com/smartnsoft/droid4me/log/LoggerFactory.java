/*
 * (C) Copyright 2009-2011 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.log;

import android.util.Log;

/**
 * In order to have an entry point for the logging interface. Because, when we use the Android logger, there are problems during the unitary tests on
 * a desktop machine.
 * 
 * <p>
 * When the the Java system property <code>droid4me.logging</code> is defined with the value "false", the logging uses the standard error and output
 * streams. This is useful when unit-testing the framework.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2008.01.15
 */
public class LoggerFactory
{

  /**
   * Tunes the logging system verbosity. The {@code Logger#isXXXEnabled()} method return values will depend on this trigger level. Defaults to
   * {@code Log.WARN}.
   * 
   * <p>
   * It uses the Android built-in {@link Log} {@code public static int} attributes for defining those log levels.
   * </p>
   */
  public static int logLevel = Log.WARN;

  /**
   * Remembers internally whether the logging system has been initialized.
   */
  private static Boolean enabled;

  /**
   * @param category
   * @return a new instance of {@link Logger} implementation, holding the provided {@code category}
   */
  public static Logger getInstance(String category)
  {
    return LoggerFactory.getInstance(category, null);
  }

  public static Logger getInstance(Class<?> theClass)
  {
    return LoggerFactory.getInstance(null, theClass);
  }

  private static Logger getInstance(String category, Class<?> theClass)
  {
    if (LoggerFactory.enabled == null)
    {
      LoggerFactory.enabled = System.getProperty("droid4me.logging", "true").equals("false") == false;
    }
    if (LoggerFactory.enabled == true)
    {
      if (theClass != null)
      {
        return new AndroidLogger(theClass);
      }
      else
      {
        return new AndroidLogger(category);
      }
    }
    else
    {
      if (theClass != null)
      {
        return new NativeLogger(theClass);
      }
      else
      {
        return new NativeLogger(category);
      }
    }
  }

}
