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

package com.smartnsoft.droid4me.framework;

import java.util.List;

import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;

import android.app.Activity;

/**
 * In order to have a clean way to handle the persistence and the business objects inside an activity.
 * 
 * @author Édouard Mercier
 * @since 2008.04.11
 */
public final class LifeCycle
{

  /**
   * This exception should be triggered on the framework methods which allow to throw it, when a business object is not accessible.
   */
  public final static class BusinessObjectUnavailableException
      extends Exception
  {

    private static final long serialVersionUID = -6764122467050013650L;

    public BusinessObjectUnavailableException()
    {
    }

    public BusinessObjectUnavailableException(String message, Throwable cause)
    {
      super(message, cause);
    }

    public BusinessObjectUnavailableException(String message)
    {
      super(message);
    }

    public BusinessObjectUnavailableException(Throwable cause)
    {
      super(cause);
    }

  }

  /**
   * This exception should be triggered on the framework methods which allow to throw it, when a service is not accessible.
   */
  public final static class ServiceException
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
   * Identifies a typical work-flow for an activity.
   * 
   * <p>
   * The methods are sorted in chronological order of call by the framework.
   * </p>
   * 
   * @since 2008.05.18
   */
  // TODO: add extra contracts so that the inherited Activity
  // "onCreate/onResume/onStart/onPause/onStop/onDestroy/onSaveInstanceState/onRestoreInstanceState" methods can be marked
  // "final" at last, and thus propose a solid framework!
  public interface ForActivity
  {

    /**
     * This is the place where the derived class can get all graphical objects and put them into attributes. The method is invoked during the
     * {@link Activity#onCreate} execution, after the parent {@link Activity#onCreate} method has been invoked.
     */
    void onRetrieveDisplayObjects();

    /**
     * This is the place where to load the business objects.
     */
    void onRetrieveBusinessObjects()
        throws BusinessObjectUnavailableException;

    /**
     * This is the typical callback that will be invoked when the business objects have been actually retrieved.
     */
    void onBusinessObjectsRetrieved();

    /**
     * This is the place where the derived class can initialize the previously retrieved graphical objects. The method is invoked during the
     * {@link Activity#onResume} execution, and only the very first time.
     */
    void onFulfillDisplayObjects();

    /**
     * Will be invoked during the {@link Activity#onResume} execution, every time. It is supposed to fulfill the display objects that may need some
     * refreshing due to the underlying business objects that have changed.
     */
    void onSynchronizeDisplayObjects();

  }

  /**
   * Indicates the way (synchronously or asynchronously) the business objects should be retrieved.
   * 
   * @since 2009.02.19
   */
  public interface BusinessObjectsRetrievalAsynchronousPolicy
  {

  }

  /**
   * Identifies a typical workflow for the services (persistence, web service) of an activity.
   * 
   * @since 2008.05.18
   */
  public interface ForServices
  {

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

  /**
   * Indicates the way (synchronously or asynchronously) the services preparation should be run.
   * 
   * @since 2009.02.19
   */
  public interface ForServicesAsynchronousPolicy
  {

  }

  /**
   * An interface which is bound to a business object.
   * 
   * @author Edouard Mercier
   * @date 2008.06.11
   */
  public interface ForBusinessObject<BusinessObjectClass>
  {

    List<MenuCommand<BusinessObjectClass>> getCustomActions();

    MenuHandler.Custom<BusinessObjectClass> getActionHandler();

    BusinessObjectClass retrieveBusinessObject()
        throws BusinessObjectUnavailableException;

    void discardBusinessObject();

    void onBusinessObjectsRetrieved();

    BusinessObjectClass getBusinessObject()
        throws BusinessObjectUnavailableException;

  }

}
