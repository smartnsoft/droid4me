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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Adapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.menu.MenuCommand;

/**
 * Introduced in order to define various contract between:
 * <ul>
 * <li>a {@link ListView list view} and its {@link ListAdapter adapter},
 * <li>
 * <li>a {@link GridView grid view} and its {@link Adapter adapter}.
 * <li>
 * </ul>
 * 
 * @author Édouard Mercier
 * @since 2008.05.13
 */
public abstract class DetailsProvider
{

  /**
   * Introduced in order to define the contract between a {@link ListView list view} and its {@link ListAdapter adapter}.
   * 
   * @author Edouard Mercier
   * @since 2008.05.13
   */
  public static interface ForListHandler<BusinessObjectClass>
  {

    /**
     * Indicates the name of the business objects handled.
     */
    String getBusinessObjectName(BusinessObjectClass businessObject);

    /**
     * This is specific to the smart list.
     */
    List<? extends BusinessObjectClass> retrieveBusinessObjectsList()
        throws LifeCycle.BusinessObjectUnavailableException;

    /**
     * If some filtering is applied, do not touch the provided list, but create a new one from scratch.
     */
    List<? extends BusinessObjectClass> getFilteredObjects(List<? extends BusinessObjectClass> fullObjectsList);

    /**
     * @return a unique and immutable (during the activity life time) identifier for the business object
     */
    long getObjectId(BusinessObjectClass businessObject);

    /**
     * @return <code>true</code> if the business object is enabled (can be selected)
     */
    boolean isEnabled(BusinessObjectClass businessObject);

  }

  /**
   * Indicates the type of action on the underlying business object.
   */
  public static enum ObjectEvent
  {
    Clicked, Selected, WipedLeftToRight, WipedRightToLeft
  }

  /**
   * Wraps a business object and its underlying Android {@link View} in a list or a grid.
   * 
   * @since 2009.04.29
   */
  public static abstract class BusinessViewWrapper<BusinessObjectClass>
  {

    private final BusinessObjectClass businessObject;

    public BusinessViewWrapper(BusinessObjectClass businessObject)
    {
      this.businessObject = businessObject;
    }

    /**
     * @return the underlying business object
     */
    public final BusinessObjectClass getBusinessObject()
    {
      return businessObject;
    }

//    final void updateBusinessObject(BusinessObjectClass businessObject)
//    {
//      this.businessObject = businessObject;
//    }

    public final int getType(int position)
    {
      return getType(position, getBusinessObject());
    }

    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return 0;
    }

    protected abstract View createNewView(Activity activity, BusinessObjectClass businessObjectClass);

    protected abstract Object extractNewViewAttributes(Activity activity, View view, BusinessObjectClass businessObjectClass);

    protected abstract void updateView(Activity activity, Object viewAttributes, View view, BusinessObjectClass businessObjectClass, int position);

    /**
     * @return the {@link Object#hashCode()} value, by default
     */
    protected long getId(BusinessObjectClass businessObject)
    {
      return businessObject == null ? DetailsProvider.DEFAULT_ITEM_ID : businessObject.hashCode();
    }

    public final long getId()
    {
      return getId(getBusinessObject());
    }

    public final boolean isEnabled()
    {
      return isEnabled(getBusinessObject());
    }

    public boolean isEnabled(BusinessObjectClass businessObject)
    {
      return true;
    }

    public String getName(BusinessObjectClass businessObject)
    {
      return "";
    }

    public final String getName()
    {
      return getName(getBusinessObject());
    }

    public final boolean containsText(String lowerText)
    {
      return containsText(getBusinessObject(), lowerText);
    }

    public boolean containsText(BusinessObjectClass businessObject, String lowerText)
    {
      return true;
    }

    public final Intent computeIntent(Activity activity, View view, DetailsProvider.ObjectEvent objectEvent)
    {
      return computeIntent(activity, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent);
    }

    public Intent computeIntent(Activity activity, Object viewAttributes, View view, BusinessObjectClass businessObject, DetailsProvider.ObjectEvent objectEvent)
    {
      return null;
    }

    public final boolean onObjectEvent(Activity activity, View view, DetailsProvider.ObjectEvent objectEvent)
    {
      return onObjectEvent(activity, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent);
    }

    public boolean onObjectEvent(Activity activity, Object viewAttributes, View view, BusinessObjectClass businessObject,
        DetailsProvider.ObjectEvent objectEvent)
    {
      return false;
    }

    // TODO: rework on that!
    public final List<MenuCommand<BusinessViewWrapper<?>>> getMenuCommands(Activity activity)
    {
      final List<MenuCommand<BusinessObjectClass>> objectMenuCommands = getMenuCommands(activity, getBusinessObject());
      if (objectMenuCommands == null)
      {
        return null;
      }
      final List<MenuCommand<BusinessViewWrapper<?>>> menuCommands = new ArrayList<MenuCommand<BusinessViewWrapper<?>>>();
      for (final MenuCommand<BusinessObjectClass> objectMenuCommand : objectMenuCommands)
      {
        menuCommands.add(new MenuCommand<BusinessViewWrapper<?>>(objectMenuCommand.text, objectMenuCommand.textId, objectMenuCommand.numericalShortcut, objectMenuCommand.characterShortcut, objectMenuCommand.icon, new Commands.Executable<BusinessViewWrapper<?>>()
        {
          @SuppressWarnings("unchecked")
          public boolean isEnabled(BusinessViewWrapper<?> businessObject)
          {
            return objectMenuCommand.executable.isEnabled((BusinessObjectClass) businessObject.getBusinessObject());
          }

          @SuppressWarnings("unchecked")
          public void run(BusinessViewWrapper<?> businessObject)
          {
            objectMenuCommand.executable.run((BusinessObjectClass) businessObject.getBusinessObject());
          }
        }));
      }
      return menuCommands;
    }

    public List<MenuCommand<BusinessObjectClass>> getMenuCommands(Activity activity, BusinessObjectClass businessObject)
    {
      return null;
    }

    public final View getNewView(Activity activity)
    {
      final View view = createNewView(activity, getBusinessObject());
      return setNewView(activity, view);
    }

    /**
     * Attaches a view to the underlying business object.
     * 
     * @param activity
     *          the activity the view belongs to
     * @param view
     *          the view to attach
     * @return the provided view
     */
    final View setNewView(Activity activity, View view)
    {
      view.setTag(extractNewViewAttributes(activity, view, getBusinessObject()));
      return view;
    }

    public final void updateView(Activity activity, View view, int position)
    {
      updateView(activity, view.getTag(), view, getBusinessObject(), position);
    }

  }

  /**
   * This class wraps the {@link DetailsProvider.BusinessViewWrapper} when not used inside a list.
   * 
   * @since 2010.06.23
   */
  public static class BusinessViewHolder<BusinessObjectClass>
  {

    private final DetailsProvider.BusinessViewWrapper<BusinessObjectClass> businessViewWrapper;

    private View view;

    public BusinessViewHolder(DetailsProvider.BusinessViewWrapper<BusinessObjectClass> businessViewWrapper)
    {
      this.businessViewWrapper = businessViewWrapper;
    }

    /**
     * Is allowed to be invoked once the {@link #getView(Activity)} or {@link #setView(Activity, View)} method has been called.
     * 
     * @return the view which represents the underlying business object
     */
    public final View getView()
    {
      return view;
    }

    /**
     * @return the wrapper passed in the constructor
     */
    public final DetailsProvider.BusinessViewWrapper<BusinessObjectClass> getBusinessViewWrapper()
    {
      return businessViewWrapper;
    }

//    public final void updateBusinessObject(BusinessObjectClass businessObject)
//    {
//      businessViewWrapper.updateBusinessObject(businessObject);
//    }

    /**
     * This method should be called only once during the object life cycle.
     * 
     * <p>
     * This will invoke the {@link DetailsProvider.BusinessViewWrapper#getNewView(Activity} method.
     * </p>
     * 
     * @param activity
     *          the activity on which the business object is being rendered
     * @return the initialized view that represent the underlying business object
     */
    public final View getView(Activity activity)
    {
      view = businessViewWrapper.getNewView(activity);
      return view;
    }

    /**
     * Sets the view of the of the underlying business view wrapper, so that it is not necessary to invoke the {@link #getView(Activity)} method.
     * 
     * @param activity
     *          the activity on which the business object is being rendered
     * @param view
     *          the view that will be attached to the business view wrapper
     * @return
     */
    public final View setView(Activity activity, View view)
    {
      this.view = view;
      return businessViewWrapper.setNewView(activity, view);
    }

    /**
     * Synchronizes the rendering of the inner {@link View} with the state of the business object.
     * 
     * <p>
     * This will invoke the {@link DetailsProvider.BusinessViewWrapper#updateView(Activity, View, int)} method with a <code>position</code> set to 0.
     * </p>
     * 
     * @param activity
     *          the activity on which the business object is being rendered
     */
    public final void updateView(Activity activity)
    {
      businessViewWrapper.updateView(activity, view, 0);
    }

  }

  /**
   * The default value that will be returned by {@link ListAdapter#getItemId(int)} method.
   */
  public static final long DEFAULT_ITEM_ID = 0;

  public static interface ForListView<BusinessObjectClass, ViewClass>
  {

    /**
     * @return the number of different {@link View views} that the underlying list handles
     * @see #getViewType
     */
    int getViewTypeCount();

    /**
     * @return the type of the view which displays the given business object
     * @see #getViewTypeCount()
     */
    int getViewType(BusinessObjectClass businessObject, int position);

    ViewClass getNewView(BusinessObjectClass businessObject);

    void updateView(ViewClass view, BusinessObjectClass businessObject, int position);

  }

  public static interface ForList<BusinessObjectClass, ViewClass>
      extends DetailsProvider.ForListHandler<BusinessObjectClass>, DetailsProvider.ForListView<BusinessObjectClass, ViewClass>
  {
  }

}
