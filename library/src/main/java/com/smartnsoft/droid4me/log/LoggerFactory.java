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

import android.util.Log;

/**
 * In order to have an entry point for the logging interface. Because, when we use the Android logger, there are problems during the unitary tests on
 * a desktop machine.
 * <p>
 * <p>
 * By default, the {@link AndroidLogger} implementation is used.
 * </p>
 * <p>
 * <p>
 * In order to tune the {@link Logger} implementation that should be used at runtime, you may define the {@code SmartConfigurator} class, as explained
 * in {@link LoggerFactory.LoggerConfigurator}.
 * </p>
 * <p>
 * <p>
 * If no {@code SmartConfigurator} class is present in the classpath, when the the Java system property <code>droid4me.logging</code> is defined with
 * the value "false", the logging uses the standard error and output streams. This is useful when unit-testing the framework.
 * </p>
 *
 * @author Édouard Mercier
 * @since 2008.01.15
 */
public class LoggerFactory
{

  /**
   * The interface that should be implemented through the {@code SmartConfigurator} class (with no package name, because of Android restriction), in
   * order to indicate to the framework which {@link Logger} implementation should be used.
   */
  public interface LoggerConfigurator
  {

    /**
     * The method will be invoked by the {@link LoggerFactory#getInstance(String)} every time a logger needs to be created.
     *
     * @param category the logger category, which is a common concept to the {@link android.util.Log}, {@code java java.util.logging.Logging}, {@code Log4J}
     *                 libraries
     * @return the {@link Logger} that should be used for logging on that category; is not allowed to be {@code null}
     * @see #getLogger(Class)
     */
    Logger getLogger(String category);

    /**
     * The method will be invoked by the {@link LoggerFactory#getInstance(Class)} every time a logger needs to be created.
     *
     * @param theClass the logger category, which is a common concept to the {@link android.util.Log}, {@code java.util.logging.Logging}, {@code Log4J}
     *                 libraries
     * @return the {@link Logger} that should be used for logging on that category; is not allowed to be {@code null}
     * @see #getLogger(String)
     */
    Logger getLogger(Class<?> theClass);

  }

  /**
   * Enumerates various logger implementations.
   */
  private enum LoggerImplementation
  {
    AndroidLogger, NativeLogger, Other
  }

  // Used for a synchronization purpose.
  private static final Object synchronizationObject = new Object();

  /**
   * Tunes the logging system verbosity. The {@code Logger#isXXXEnabled()} method return values will depend on this trigger level. Defaults to
   * {@code Log.WARN}.
   * <p>
   * <p>
   * It uses the Android built-in {@link android.util.Log} attributes for defining those log levels.
   * </p>
   */
  public static int logLevel = android.util.Log.WARN;

  /**
   * Remembers internally which {@link Logger} implementation to use.
   */
  private static LoggerImplementation loggerImplementation;

  /**
   * Remembers the {@link LoggerFactory.LoggerConfigurator} that will be used to instantiate {@link Logger} instances.
   */
  private static LoggerConfigurator loggerConfigurator;

  /**
   * @param category the category used for logging
   * @return a new instance of {@link Logger} implementation, holding the provided {@code category}
   * @see #getInstance(Class)
   */
  public static Logger getInstance(String category)
  {
    return LoggerFactory.getInstance(category, null);
  }

  /**
   * @param theClass the class used for computing the logging category
   * @return a new instance of {@link Logger} implementation, holding the provided {@code category}
   */
  public static Logger getInstance(Class<?> theClass)
  {
    return LoggerFactory.getInstance(null, theClass);
  }

  private static Logger getInstance(String category, Class<?> theClass)
  {
    synchronized (synchronizationObject)
    {
      // We need to synchronize this part of the code
      if (LoggerFactory.loggerImplementation == null)
      {
        // The logger implementation has not been decided yet
        if (LoggerFactory.retrieveCustomLoggerInstance("SmartConfigurator") == false)
        {
          if (LoggerFactory.retrieveCustomLoggerInstance("com.smartnsoft.droid4me.SmartConfigurator") == false)
          {
            // This means that the project does not expose the class which enables to configure the logging system
            if (System.getProperty("droid4me.logging", "true").equals("false") == true)
            {
              LoggerFactory.loggerImplementation = LoggerImplementation.NativeLogger;
            }
            else
            {
              LoggerFactory.loggerImplementation = LoggerImplementation.AndroidLogger;
            }
          }
        }
        // The logger implementation has not been decided yet
        final String loggerConfiguratorClassFqn = "SmartConfigurator";
        try
        {
          final Class<?> loggerConfiguratorClass = Class.forName(loggerConfiguratorClassFqn);
          LoggerFactory.loggerConfigurator = (LoggerConfigurator) loggerConfiguratorClass.newInstance();
          LoggerFactory.loggerImplementation = LoggerImplementation.Other;
        }
        catch (Exception exception)
        {


        }
        if (LoggerFactory.logLevel >= android.util.Log.INFO)
        {
          Log.d("LoggerFactory", "Using the logger '" + LoggerFactory.loggerImplementation + "'");
        }
      }
    }

    switch (LoggerFactory.loggerImplementation)
    {
      case Other:
        if (theClass != null)
        {
          return LoggerFactory.loggerConfigurator.getLogger(theClass);
        }
        else
        {
          return LoggerFactory.loggerConfigurator.getLogger(category);
        }
      case AndroidLogger:
      default:
        if (theClass != null)
        {
          return new AndroidLogger(theClass);
        }
        else
        {
          return new AndroidLogger(category);
        }
      case NativeLogger:
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

  private static boolean retrieveCustomLoggerInstance(String loggerConfiguratorClassFqn)
  {
    try
    {
      final Class<?> loggerConfiguratorClass = Class.forName(loggerConfiguratorClassFqn);
      LoggerFactory.loggerConfigurator = (LoggerConfigurator) loggerConfiguratorClass.newInstance();
      LoggerFactory.loggerImplementation = LoggerImplementation.Other;
      return true;
    }
    catch (Exception rollbackException)
    {
      // This means that the project does not expose the class which enables to configure the logging system
      return false;
    }
  }

}
