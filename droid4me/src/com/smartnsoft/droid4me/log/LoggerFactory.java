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
 *     E2M - initial API and implementation
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.log;

import android.util.Log;

/**
 * In order to have an entry point for the logging interface. Because, when we use the Android logger, there are problems during the unitary tests on
 * a desktop machine.
 * <p/>
 * <p>
 * By default, the {@link AndroidLogger} implementation is used.
 * </p>
 * <p/>
 * <p>
 * In order to tune the {@link Logger} implementation that should be used at runtime, you may define the {@code SmartConfigurator} class, as explained
 * in {@link LoggerFactory.LoggerConfigurator}.
 * </p>
 * <p/>
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
     * @param category the logger category, which is a common concept to the {@link android.util.Log}, {@link java.util.logging.Logging}, {@code Log4J}
     *                 libraries
     * @return the {@link Logger} that should be used for logging on that category; is not allowed to be {@code null}
     * @see #getLogger(Class)
     */
    Logger getLogger(String category);

    /**
     * The method will be invoked by the {@link LoggerFactory#getInstance(Class)} every time a logger needs to be created.
     *
     * @param category the logger category, which is a common concept to the {@link android.util.Log}, {@link java.util.logging.Logging}, {@code Log4J}
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
    AndroidLogger, NativeLogger, Other;
  }

  /**
   * Tunes the logging system verbosity. The {@code Logger#isXXXEnabled()} method return values will depend on this trigger level. Defaults to
   * {@code Log.WARN}.
   * <p/>
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

  // Used for a synchronization purpose.
  private static final Object synchronizationObject = new Object();

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
        final String loggerConfiguratorClassFqn = "SmartConfigurator";
        try
        {
          final Class<?> loggerConfiguratorClass = Class.forName(loggerConfiguratorClassFqn);
          LoggerFactory.loggerConfigurator = (LoggerConfigurator) loggerConfiguratorClass.newInstance();
          LoggerFactory.loggerImplementation = LoggerImplementation.Other;
        }
        catch (Exception exception)
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

}
