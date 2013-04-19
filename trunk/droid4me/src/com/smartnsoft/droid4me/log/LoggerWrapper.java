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

import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class which enables to wrap a {@link Logger}, and to delegate all its interface methods to its underlying logger.
 * 
 * <p>
 * All instances of this class are recorded, so that it is possible to {@link LoggerWrapper#configure(LoggerFactory.LoggerConfigurator) configure}
 * them later on. This is especially useful, when it is required to change the implementation in the middle of an application.
 * </p>
 * 
 * @author �douard Mercier
 * @since 2013.04.19
 * @see LoggerFactory
 */
public class LoggerWrapper
    implements Logger
{

  // A "Hashtable" is used instead of the "HashMap", because of potential race conditions
  private final static Map<String, LoggerWrapper> instances = new Hashtable<String, LoggerWrapper>();

  /**
   * Enables to configure all already created {@link LoggerWrapper} instances.
   * 
   * @param loggerConfigurator
   *          the interface responsible for returning the {@link Logger} that will be wrapped
   */
  public static void configure(LoggerFactory.LoggerConfigurator loggerConfigurator)
  {
    synchronized (LoggerWrapper.instances)
    {
      for (Entry<String, LoggerWrapper> entry : LoggerWrapper.instances.entrySet())
      {
        final LoggerWrapper loggerWrapper = entry.getValue();
        loggerWrapper.logger = loggerConfigurator.getLogger(entry.getKey());
      }
    }
  }

  /**
   * The wrapped {@link Logger}.
   */
  private Logger logger;

  /**
   * Equivalent to using the {@link #LoggerWrapper(String, Logger)} constructor, by passing {@code theClass.getSimpleName()} as second argument.
   */
  public LoggerWrapper(Class<?> theClass, Logger logger)
  {
    this(theClass.getSimpleName(), logger);
  }

  /**
   * Creates a wrapper around the provided {@link Logger} instance.
   * 
   * @param category
   *          the logger category
   * @param logger
   *          the wrapped logger ; may be {@code null}
   */
  public LoggerWrapper(String category, Logger logger)
  {
    this.logger = logger;
    LoggerWrapper.instances.put(category, this);
  }

  public void debug(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.debug(message);
  }

  public void info(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.info(message);
  }

  public void warn(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.warn(message);
  }

  public void warn(String message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.warn(message, throwable);
  }

  public void warn(StringBuffer message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.warn(message, throwable);
  }

  public void error(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.error(message);
  }

  public void error(String message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.error(message, throwable);
  }

  public void error(StringBuffer message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.error(message, throwable);
  }

  public void fatal(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.fatal(message);
  }

  public void fatal(String message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.fatal(message, throwable);
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
