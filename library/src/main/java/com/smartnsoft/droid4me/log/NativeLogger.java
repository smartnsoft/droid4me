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

  protected final String getPrefix()
  {
    return "[" + System.currentTimeMillis() + "] " + prefix + " [" + Thread.currentThread().getName() + "] ";
  }

}