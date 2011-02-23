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

package com.smartnsoft.droid4me;

/**
 * Identifies a typical workflow for the services (persistence, web service) of an activity.
 * 
 * @author Édouard Mercier
 * @since 2008.05.18
 */
public interface ServiceLifeCycle
{

  /**
   * This exception should be triggered on the framework methods which allow to throw it, when a service is not accessible.
   */
  public final class ServiceException
      extends Exception
  {

    private static final long serialVersionUID = -4835447312079873124L;

    public ServiceException()
    {
    }

    public ServiceException(String message, Throwable cause)
    {
      super(message, cause);
    }

    public ServiceException(String message)
    {
      super(message);
    }

    public ServiceException(Throwable cause)
    {
      super(cause);
    }

  }

  /**
   * Indicates the way (synchronously or asynchronously) the services preparation should be run.
   * 
   * @since 2009.02.19
   */
  public interface ForServicesAsynchronousPolicy
  {

  }

  /**
   * Opens up the persistence medium.
   */
  void prepareServices()
      throws ServiceException;

  /**
   * Closes the persistence medium.
   */
  void disposeServices()
      throws ServiceException;

}
