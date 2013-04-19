/*
 * (C) Copyright 2009-2013 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.log;

import org.apache.log4j.Level;

/**
 * The logger implementation for Log4J, which works with the "android-logging-log4j" library.
 * 
 * <p>
 * When using this implementation (see {@link LoggerFactory.LoggerConfigurator}), the classpath should have {@code log4j.jar} and {@link android
 * -logging-log4j.jar} libraries.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2013.04.18
 * @see LoggerFactory
 */
public class Log4JLogger
    implements Logger
{

  private final org.apache.log4j.Logger log;

  public static Level getLogLevel(int logLevel)
  {
    switch (logLevel)
    {
    case android.util.Log.ASSERT:
    default:
      return Level.TRACE;
    case android.util.Log.DEBUG:
      return Level.DEBUG;
    case android.util.Log.INFO:
      return Level.INFO;
    case android.util.Log.WARN:
      return Level.WARN;
    case android.util.Log.ERROR:
      return Level.ERROR;
    }
  }

  public Log4JLogger(Class<?> theClass)
  {
    this(theClass.getSimpleName());
  }

  public Log4JLogger(String category)
  {
    log = org.apache.log4j.Logger.getLogger(category);
  }

  public void debug(String message)
  {
    log.debug(message);
  }

  public void info(String message)
  {
    log.info(message);
  }

  public void warn(String message)
  {
    log.warn(message);
  }

  public void warn(String message, Throwable throwable)
  {
    log.warn(message, throwable);
  }

  public void warn(StringBuffer message, Throwable throwable)
  {
    log.warn(message.toString(), throwable);
  }

  public void error(String message)
  {
    log.error(message);
  }

  public void error(String message, Throwable throwable)
  {
    log.error(message, throwable);
  }

  public void error(StringBuffer message, Throwable throwable)
  {
    log.error(message.toString(), throwable);
  }

  public void fatal(String message)
  {
    log.fatal(message);
  }

  public void fatal(String message, Throwable throwable)
  {
    log.fatal(message, throwable);
  }

  public boolean isDebugEnabled()
  {
    return LoggerFactory.logLevel <= android.util.Log.DEBUG;
  }

  public boolean isInfoEnabled()
  {
    return LoggerFactory.logLevel <= android.util.Log.INFO;
  }

  public boolean isWarnEnabled()
  {
    return LoggerFactory.logLevel <= android.util.Log.WARN;
  }

  public boolean isErrorEnabled()
  {
    return LoggerFactory.logLevel <= android.util.Log.ERROR;
  }

  public boolean isFatalEnabled()
  {
    return LoggerFactory.logLevel <= android.util.Log.ERROR;
  }

}
