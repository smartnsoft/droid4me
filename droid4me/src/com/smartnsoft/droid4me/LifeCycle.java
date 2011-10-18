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

import android.app.Activity;

import com.smartnsoft.droid4me.app.SmartActivity;

/**
 * Identifies a typical work-flow for an {@link Activity activity of the framework}.
 * 
 * <p>
 * The methods are sorted in chronological order of call by the framework.
 * </p>
 * 
 * @author Édouard Mercier
 * @since 2008.05.18
 */
// TODO: add extra contracts so that the inherited Activity
// "onCreate/onResume/onStart/onPause/onStop/onDestroy/onSaveInstanceState/onRestoreInstanceState" methods can be marked
// "final" at last, and thus propose a solid framework!
public interface LifeCycle
{

  /**
   * This exception should be triggered on the framework methods which allow to throw it, when a business object is not accessible.
   */
  public class BusinessObjectUnavailableException
      extends Exception
  {

    private static final long serialVersionUID = -6764122467050013650L;

    /**
     * The error code associated with the exception. Is equal to <code>0</code>.
     */
    public final int code;

    public BusinessObjectUnavailableException()
    {
      this(0);
    }

    public BusinessObjectUnavailableException(int code)
    {
      this.code = code;
    }

    public BusinessObjectUnavailableException(String message, Throwable cause)
    {
      this(message, cause, 0);
    }

    public BusinessObjectUnavailableException(String message, Throwable cause, int code)
    {
      super(message, cause);
      this.code = code;
    }

    public BusinessObjectUnavailableException(String message)
    {
      this(message, 0);
    }

    public BusinessObjectUnavailableException(String message, int code)
    {
      super(message);
      this.code = code;
    }

    public BusinessObjectUnavailableException(Throwable cause)
    {
      this(cause, 0);
    }

    public BusinessObjectUnavailableException(Throwable cause, int code)
    {
      super(cause);
      this.code = code;
    }

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
   * This is the place where the derived class can get all graphical objects and put them into attributes. The method is invoked during the
   * {@link Activity#onCreate} execution, after the parent {@link Activity#onCreate} method has been invoked.
   * 
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * 
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onRetrieveDisplayObjects();

  /**
   * This is the place where to load the business objects. It is strongly adviced not to modify the GUI during this method execution.
   * 
   * <p>
   * It is NOT ensured that this method will be invoked from the UI thread!
   * </p>
   * 
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException;

  /**
   * This is the typical callback that will be invoked when the business objects have been actually retrieved. It is strongly adviced not to modify
   * the GUI during this method execution.
   * 
   * <p>
   * It is NOT ensured that this method will be invoked from the UI thread! If the {@link Activity} implements the
   * {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface, this method will be invoked from a high-priority worker thread.
   * </p>
   * 
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onBusinessObjectsRetrieved();

  /**
   * This is the place where the derived class can initialize the previously retrieved graphical objects. The method is invoked during the
   * {@link Activity#onResume} execution, and only the very first time.
   * 
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * 
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onFulfillDisplayObjects();

  /**
   * Will be invoked during the {@link Activity#onResume} execution, every time. It is supposed to fulfill the display objects that may need some
   * refreshing due to the underlying business objects that have changed.
   * 
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * 
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onSynchronizeDisplayObjects();

  /**
   * Asks the implementing {@link Activity} to reload its business objects and to synchronize its display. The method invokes the
   * {@link #onRetrieveBusinessObjects()} and {@link #onSynchronizeDisplayObjects()} methods.
   * <p>
   * Must be invoked only from the UI thread, when the {@link SmartActivity} is synchronous!
   * </p>
   * 
   * @param retrieveBusinessObjects
   *          indicates whether the {@link #onRetrieveBusinessObjects()} method should be invoked or not
   * @param onOver
   *          if not <code>null</code>, this method will be eventually invoked from the UI thread
   * @param immediately
   *          if this flag is set to <code>true</code>, even if the implementing {@link Activity} is not currently displayed, the execution will be
   *          run at once. If set to <code>false</code>, the execution will be delayed until the {@link Activity#onRestart()} method is invoked
   */
  void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately);

}
