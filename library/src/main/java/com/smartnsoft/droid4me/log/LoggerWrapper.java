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

package com.smartnsoft.droid4me.log;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A class which enables to wrap a {@link Logger}, and to delegate all its interface methods to its underlying logger.
 * <p>
 * <p>
 * All instances of this class are recorded, so that it is possible to {@link LoggerWrapper#configure(LoggerFactory.LoggerConfigurator) configure}
 * them later on. This is especially useful, when it is required to change the implementation in the middle of an application.
 * </p>
 *
 * @author Édouard Mercier
 * @see LoggerFactory
 * @since 2013.04.19
 */
public class LoggerWrapper
    implements Logger
{

  // A "Hashtable" is used instead of the "HashMap", because of potential race conditions
  private final static Map<String, LoggerWrapper> instances = new Hashtable<>();

  /**
   * Enables to configure all already created {@link LoggerWrapper} instances.
   *
   * @param loggerConfigurator the interface responsible for returning the {@link Logger} that will be wrapped
   */
  public static void configure(LoggerFactory.LoggerConfigurator loggerConfigurator)
  {
    synchronized (LoggerWrapper.instances)
    {
      /**
       * We do this because of concurrent acces on the list
       * It could cause massive crashs when logging
       */
      final Set<Entry<String, LoggerWrapper>> entries = new HashSet<>(LoggerWrapper.instances.entrySet());
      for (Entry<String, LoggerWrapper> entry : entries)
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
   * @param category the logger category
   * @param logger   the wrapped logger ; may be {@code null}
   */
  public LoggerWrapper(String category, Logger logger)
  {
    this.logger = logger;
    synchronized (LoggerWrapper.instances)
    {
      LoggerWrapper.instances.put(category, this);
    }
  }

  @Override
  public void debug(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.debug(message);
  }

  @Override
  public void info(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.info(message);
  }

  @Override
  public void warn(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.warn(message);
  }

  @Override
  public void warn(String message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.warn(message, throwable);
  }

  @Override
  public void warn(StringBuffer message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.warn(message, throwable);
  }

  @Override
  public void error(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.error(message);
  }

  @Override
  public void error(String message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.error(message, throwable);
  }

  @Override
  public void error(StringBuffer message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.error(message, throwable);
  }

  @Override
  public void fatal(String message)
  {
    if (logger == null)
    {
      return;
    }
    logger.fatal(message);
  }

  @Override
  public void fatal(String message, Throwable throwable)
  {
    if (logger == null)
    {
      return;
    }
    logger.fatal(message, throwable);
  }

  @Override
  public boolean isDebugEnabled()
  {
    return logger == null ? LoggerFactory.logLevel <= android.util.Log.DEBUG : logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled()
  {
    return logger == null ? LoggerFactory.logLevel <= android.util.Log.INFO : logger.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled()
  {
    return logger == null ? LoggerFactory.logLevel <= android.util.Log.WARN : logger.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled()
  {
    return logger == null ? LoggerFactory.logLevel <= android.util.Log.ERROR : logger.isErrorEnabled();
  }

  @Override
  public boolean isFatalEnabled()
  {
    return logger == null ? LoggerFactory.logLevel <= android.util.Log.ERROR : logger.isFatalEnabled();
  }

}
