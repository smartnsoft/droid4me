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
 * The logger implementation for Android.
 * <p>
 * <p>
 * This implementation can only be used when the code integrating the library runs environment with the Android runtime.
 * </p>
 *
 * @author Édouard Mercier
 * @see LoggerFactory
 * @since 2007.12.23
 */
public class AndroidLogger
    implements Logger
{

  private final String category;

  public AndroidLogger(Class<?> theClass)
  {
    this(theClass.getSimpleName());
  }

  public AndroidLogger(String category)
  {
    this.category = category;
  }

  public void debug(String message)
  {
    Log.d(category, message);
  }

  public void info(String message)
  {
    Log.i(category, message);
  }

  public void warn(String message)
  {
    Log.w(category, message);
  }

  public void warn(String message, Throwable throwable)
  {
    Log.w(category, message, throwable);
  }

  public void warn(StringBuffer message, Throwable throwable)
  {
    warn(message.toString(), throwable);
  }

  public void error(String message)
  {
    Log.e(category, message);
  }

  public void error(String message, Throwable throwable)
  {
    Log.e(category, message, throwable);
  }

  public void error(StringBuffer message, Throwable throwable)
  {
    error(message.toString(), throwable);
  }

  public void fatal(String message)
  {
    error(message);
  }

  public void fatal(String message, Throwable throwable)
  {
    error(message, throwable);
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

}
