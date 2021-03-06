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

package com.smartnsoft.droid4me;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;

/**
 * Identifies a typical life cycle work-flow for an {@link Activity activity} or a {@link Fragment fragment} of the framework. When referring to the
 * {@link Activity} "life cycle", we do not consider the entity instance re-creation due to <a
 * href="http://developer.android.com/reference/android/app/Activity.html#ConfigurationChanges">configuration changes</a>, for instance. The framework
 * capture those entities work-flow and divides it into typical actions:
 * <p>
 * <ol>
 * <li>set the layout and extract the key {@link android.view.View widgets},</li>
 * <li>retrieve the business objects are represented on the entity,</li>
 * <li>bind the entity widgets to the previously extracted business objects,</li>
 * <li>when an {@link Activity} has been stacked over the current entity task, and the navigation comes back to the entity, refresh the widgets with
 * the potential business objects new values.</li>
 * </ol>
 * <p>
 * <p>
 * When deriving from this interface, just implement this interface method. You do not need to override the traditional {@link Activity}
 * {@link Activity#onCreate(Bundle)}/{@link Activity#onStart()}/{@link Activity#onResume()} method nor the {@link Fragment}
 * {@link Fragment#onCreate(Bundle)}/{@link Fragment#onStart()}/{@link Fragment#onResume()} methods, even if you still are allowed to.
 * </p>
 * <p>
 * <p>
 * The {@code onXXX} methods should never be invoked because they are callbacks, and that only the framework should invoke them during the entity life
 * cycle!
 * </p>
 * <p>
 * <p>
 * In the code, the interface methods are sorted in chronological order of invocation by the framework.
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
   * An annotation which indicates the way (synchronously or asynchronously) the {@link #onRetrieveBusinessObjects()
   * business objects should be retrieved}.
   *
   * @since 2013.04.12
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  @Inherited
  @interface BusinessObjectsRetrievalAsynchronousPolicy
  {

  }

  /**
   * This exception should be triggered on the framework methods which allow to throw it, when a business object is not accessible.
   */
  class BusinessObjectUnavailableException
      extends Exception
  {

    private static final long serialVersionUID = -6764122467050013650L;

    /**
     * The error code associated with the exception. Is equal to {@code 0} by default.
     */
    public final int code;

    public BusinessObjectUnavailableException()
    {
      this(0);
    }

    public BusinessObjectUnavailableException(int code)
    {
      this(null, null, 0);
    }

    public BusinessObjectUnavailableException(String message, Throwable cause)
    {
      this(message, cause, 0);
    }

    public BusinessObjectUnavailableException(String message)
    {
      this(message, 0);
    }

    public BusinessObjectUnavailableException(String message, int code)
    {
      this(message, null, code);
    }

    public BusinessObjectUnavailableException(Throwable cause)
    {
      this(cause, 0);
    }

    public BusinessObjectUnavailableException(Throwable cause, int code)
    {
      this(null, cause, code);
    }

    public BusinessObjectUnavailableException(String message, Throwable cause, int code)
    {
      super(message, cause);
      this.code = code;
    }

  }

  /**
   * This is the place where the derived class should {@code Activity.setContentView() set its layout}, extract all {@link android.view.View widgets}
   * which require a further customization and store them as instance attributes. This method is invoked only once during the entity life cycle.
   * <p>
   * <p>
   * The method is invoked:
   * </p>
   * <ul>
   * <li>for an {@link Activity}, during the {@code Activity.onCreate()} execution, after the parent {@code Activity.onCreate()} method has been
   * invoked ;</li>
   * <li>for an {@link Fragment}, during the {@code Fragment.onCreate()} execution, after the parent {@code Fragment.onCreate()} method has been
   * invoked.</li>
   * </ul>
   * <p>
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * <p>
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onRetrieveDisplayObjects();

  /**
   * This is the place where to load the business objects, from memory, local persistence, via web services, necessary for the entity processing.
   * <p>
   * <p>
   * If the entity implements the {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface, this callback will be invoked from a
   * background thread, and not the UI thread. This method will be invoked a first time once the entity has successfully
   * {@link #onRetrieveDisplayObjects() retrieved its display objects}, and every time the
   * {@link #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)} method is invoked.
   * </p>
   * <p>
   * <p>
   * When the method is invoked the first time, if the entity does not implement the {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy}
   * interface, its execution will occur:
   * </p>
   * <ul>
   * <li>for an {@link Activity}, during the {@link Activity#onResume()} execution, after the parent {@link Activity#onResume()} method has been
   * invoked ;</li>
   * <li>for an {@link Fragment}, during the {@link Fragment#onResume()} execution, after the parent {@link Fragment#onResume()} method has been
   * invoked.</li>
   * </ul>
   * <p>
   * When the method is invoked the first time, if the entity implements the {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface,
   * it is ensured that this method will be invoked at least after the entity {@code onResume()} execution has started.
   * </p>
   * <p>
   * <p>
   * It is NOT ensured that this method will be invoked from the UI thread: if the entity implements the
   * {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface, it is ensured to be invoked from a background thread, otherwise, it is
   * ensured to be invoked from the UI thread.
   * </p>
   * <p>
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   *
   * @throws BusinessObjectUnavailableException if the extraction of the business objects was a failure and that this issue cannot be recovered, this enables to notify the framework
   *                                            that the current entity cannot continue its execution
   */
  void onRetrieveBusinessObjects()
      throws LifeCycle.BusinessObjectUnavailableException;

  /**
   * This is the typical callback that will be invoked every time the business objects have been retrieved, just after the
   * {@link #onRetrieveBusinessObjects()} method has successfully completed, i.e. no {@link LifeCycle.BusinessObjectUnavailableException} exception
   * has been thrown. It is strongly advised not to modify the GUI during this method execution.
   * <p>
   * <p>
   * It is NOT ensured that this method will be invoked from the UI thread! If the {@link Activity} implements the
   * {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface, this method will be invoked from a (high-priority) worker thread.
   * </p>
   * <p>
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onBusinessObjectsRetrieved();

  /**
   * This is the place where the implementing class can initialize the previously retrieved graphical objects. This method is invoked only once during
   * the entity life cycle.
   * <p>
   * <p>
   * If the entity does not implement the {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface, the method is invoked during the:
   * </p>
   * <ol>
   * <li>{@link Activity#onResume()} execution for the {@link Activity}, after the parent {@link Activity#onResume()} method has been invoked,</li>
   * <li>{@link Fragment#onResume()} execution for the {@link Fragment}, after the parent {@link Fragment#onResume()} method has been invoked.</li>
   * </ol>
   * <p>
   * If the entity implements the {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface, this method will be invoked just after the
   * {@link #onRetrieveBusinessObjects()} method first invocation has successfully returned.
   * </p>
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * <p>
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onFulfillDisplayObjects();

  /**
   * This method is supposed to update the display objects that may need some refreshing due to the underlying business objects that may have changed.
   * It will be invoked a first time once the entity has successfully {@link #onFulfillDisplayObjects() fulfilled its display objects}, and every time
   * the {@link #refreshBusinessObjectsAndDisplay(boolean, Runnable, boolean)} method is invoked, provided the previous
   * {@link #onRetrieveBusinessObjects()} callback has successfully executed.
   * <p>
   * <p>
   * When the method is invoked the first time, it will be called just after the {@link #onFulfillDisplayObjects()} callback execution.
   * </p>
   * <p>
   * <p>
   * It is ensured that this method will be invoked from the UI thread!
   * </p>
   * <p>
   * <p>
   * Never invoke this method, only the framework should, because this is a callback!
   * </p>
   */
  void onSynchronizeDisplayObjects();

  /**
   * Asks the implementing entity to reload its business objects and to synchronize its display. The method invokes the
   * {@link #onRetrieveBusinessObjects()} method and then, provided the previous call was successful, the {@link #onSynchronizeDisplayObjects()}
   * method.
   * <p>
   * <p>
   * It must be invoked only from the UI thread, if the current interface does not implement the
   * {@link LifeCycle.BusinessObjectsRetrievalAsynchronousPolicy} interface.
   * </p>
   *
   * @param retrieveBusinessObjects indicates whether the {@link #onRetrieveBusinessObjects()} method should be invoked or not
   * @param onOver                  if not {@code null}, this method will be eventually invoked from the UI thread, once the {@link #onSynchronizeDisplayObjects()} method
   *                                execution has successfully completed
   * @param immediately             if this flag is set to {@code true}, even if the implementing entity is not currently displayed, the execution will be run at once. If
   *                                set to {@code false}, the execution will be delayed until the entity ({@link Activity#onResume()} for the {@link Activity},
   *                                {@link Fragment#onResume()} for the {@link Fragment} method is invoked
   */
  void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately);

}
