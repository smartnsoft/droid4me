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
 * @date 2008.01.15
 */
public class LoggerFactory
{

  public static int logLevel = Log.WARN;

  private static Boolean enabled;

  /**
   * An implementation which uses the Java standard output and error streams.
   */
  private final static class NullLogger
      implements Logger
  {

    public boolean isDebugEnabled()
    {
      return LoggerFactory.logLevel <= Log.DEBUG;
    }

    public boolean isInfoEnabled()
    {
      return LoggerFactory.logLevel <= Log.INFO;
    }

    public boolean isWarnEnabled()
    {
      return LoggerFactory.logLevel <= Log.WARN;
    }

    public boolean isErrorEnabled()
    {
      return LoggerFactory.logLevel <= Log.ERROR;
    }

    public boolean isFatalEnabled()
    {
      return LoggerFactory.logLevel <= Log.ERROR;
    }

    public void debug(String message)
    {
      System.out.println(message);
    }

    public void error(String message)
    {
      System.err.println(message);
    }

    public void error(String message, Throwable throwable)
    {
      System.err.println(message);
      throwable.printStackTrace(System.err);
    }

    public void error(StringBuffer message, Throwable throwable)
    {
      System.err.println(message);
      throwable.printStackTrace(System.err);
    }

    public void fatal(String message)
    {
      System.err.println(message);
    }

    public void fatal(String message, Throwable throwable)
    {
      System.err.println(message);
      throwable.printStackTrace(System.err);
    }

    public void info(String message)
    {
      System.out.println(message);
    }

    public void warn(String message)
    {
      System.out.println(message);
    }

    public void warn(String message, Throwable throwable)
    {
      System.out.println(message);
      throwable.printStackTrace(System.out);
    }

    public void warn(StringBuffer message, Throwable throwable)
    {
      System.out.println(message);
      throwable.printStackTrace(System.out);
    }

  }

  public static Logger getInstance(String category)
  {
    return getInstance(category, null);
  }

  public static Logger getInstance(Class<?> theClass)
  {
    return getInstance(null, theClass);
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
      return new NullLogger();
    }
  }

}
