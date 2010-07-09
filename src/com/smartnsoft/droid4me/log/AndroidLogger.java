/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 * The logger implementation for Android.
 * 
 * @author Édouard Mercier
 * @since 2007.12.23
 */
public class AndroidLogger
    implements com.smartnsoft.droid4me.log.Logger
{

  private String category;

  @SuppressWarnings("unchecked")
  public AndroidLogger(Class theClass)
  {
    category = theClass.getSimpleName();
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
