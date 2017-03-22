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
 * An implementation which uses the Java standard output and error streams.
 * <p>
 * <p>
 * This implementation can be used when the code integrating the library needs to run on an environment with no Android runtime available.
 * </p>
 *
 * @author Édouard Mercier
 * @see LoggerFactory
 * @since 2007.12.23
 */
public class NativeLogger
    implements Logger
{

  private final String prefix;

  public NativeLogger(Class<?> theClass)
  {
    this(theClass.getSimpleName());
  }

  public NativeLogger(String category)
  {
    this.prefix = "[" + category + "] ";
  }

  protected final String getPrefix()
  {
    return "[" + System.currentTimeMillis() + "] " + prefix + " [" + Thread.currentThread().getName() + "] ";
  }

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
    System.out.println(getPrefix() + "[D] " + message);
  }

  public void error(String message)
  {
    System.err.println(getPrefix() + "[E] " + message);
  }

  public void error(String message, Throwable throwable)
  {
    System.err.println(getPrefix() + "[E] " + message);
    throwable.printStackTrace(System.err);
  }

  public void error(StringBuffer message, Throwable throwable)
  {
    System.err.println(getPrefix() + message);
    throwable.printStackTrace(System.err);
  }

  public void fatal(String message)
  {
    System.err.println(getPrefix() + "[F] " + message);
  }

  public void fatal(String message, Throwable throwable)
  {
    System.err.println(getPrefix() + "[F] " + message);
    throwable.printStackTrace(System.err);
  }

  public void info(String message)
  {
    System.out.println(getPrefix() + "[I] " + message);
  }

  public void warn(String message)
  {
    System.out.println(getPrefix() + "[W] " + message);
  }

  public void warn(String message, Throwable throwable)
  {
    System.out.println(getPrefix() + "[W] " + message);
    throwable.printStackTrace(System.out);
  }

  public void warn(StringBuffer message, Throwable throwable)
  {
    System.out.println(getPrefix() + "[W] " + message);
    throwable.printStackTrace(System.out);
  }

}