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

/**
 * Just in order to have various loggers.
 * 
 * @author Édouard Mercier
 * @since 2007.12.23
 */
public interface Logger
{

  void debug(String message);

  void info(String message);

  void warn(String message);

  void warn(String message, Throwable throwable);

  void warn(StringBuffer append, Throwable throwable);

  void error(String message);

  void error(String message, Throwable throwable);

  void error(StringBuffer message, Throwable throwable);

  void fatal(String message);

  void fatal(String message, Throwable throwable);

  boolean isDebugEnabled();

  boolean isInfoEnabled();

  boolean isWarnEnabled();

  boolean isErrorEnabled();

  boolean isFatalEnabled();

}
