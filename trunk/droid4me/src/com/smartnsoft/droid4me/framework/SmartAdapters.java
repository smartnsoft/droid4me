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
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.framework;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.smartnsoft.droid4me.app.AbstractSmartListActivity;
import com.smartnsoft.droid4me.menu.MenuCommand;

/**
 * Gathers in one place some classes used by the {@link AbstractSmartListActivity} class, which handles {@link android.widget.ListView list views}.
 * 
 * @author Édouard Mercier
 * @since 2011.09.27
 */
public abstract class SmartAdapters
{

  /**
   * Indicates the type of action on the underlying business object.
   */
  public static enum ObjectEvent
  {
    Clicked, Selected, WipedLeftToRight, WipedRightToLeft
  }

  /**
   * Wraps a business object and its underlying Android {@link android.view.View} in a list or a grid.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   * 
   * @since 2009.04.29
   */
  public static abstract class BusinessViewWrapper<BusinessObjectClass>
  {

    /**
     * The default value that will be returned by {@link android.widget.ListAdapter#getItemId(int)} method.
     */
    public static final long DEFAULT_ITEM_ID = 0;

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

    // final void updateBusinessObject(BusinessObjectClass businessObject)
    // {
    // this.businessObject = businessObject;
    // }

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
      return businessObject == null ? BusinessViewWrapper.DEFAULT_ITEM_ID : businessObject.hashCode();
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

    public final Intent computeIntent(Activity activity, View view, SmartAdapters.ObjectEvent objectEvent)
    {
      return computeIntent(activity, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent);
    }

    public Intent computeIntent(Activity activity, Object viewAttributes, View view, BusinessObjectClass businessObject, SmartAdapters.ObjectEvent objectEvent)
    {
      return null;
    }

    public final boolean onObjectEvent(Activity activity, View view, SmartAdapters.ObjectEvent objectEvent)
    {
      return onObjectEvent(activity, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent);
    }

    public boolean onObjectEvent(Activity activity, Object viewAttributes, View view, BusinessObjectClass businessObject, SmartAdapters.ObjectEvent objectEvent)
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
          public boolean isVisible(BusinessViewWrapper<?> businessObject)
          {
            return objectMenuCommand.executable.isVisible((BusinessObjectClass) businessObject.getBusinessObject());
          }
          
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
   * A basic wrapper implementation, which lets specify its {@link #getType(int, Object)} and representation layout identifier used to
   * {@link LayoutInflater#inflate(int, android.view.ViewGroup) inflate it}.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   */
  public abstract static class SimpleBusinessViewWrapper<BusinessObjectClass>
      extends SmartAdapters.BusinessViewWrapper<BusinessObjectClass>
  {

    protected final int type;

    protected final int layoutResourceId;

    public SimpleBusinessViewWrapper(BusinessObjectClass businessObject, int type, int layoutResourceId)
    {
      super(businessObject);
      this.type = type;
      this.layoutResourceId = layoutResourceId;
    }

    @Override
    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return type;
    }

    @Override
    protected View createNewView(Activity activity, BusinessObjectClass businessObjectClass)
    {
      return LayoutInflater.from(activity.getApplicationContext()).inflate(layoutResourceId, null);
    }

  }

  /**
   * This class wraps the {@link SmartAdapters.BusinessViewWrapper} when not used inside a {@link android.widget.ListView list}.
   * 
   * @param the
   *          business object class which is represented by the current wrapper
   * 
   * @since 2010.06.23
   */
  public static class BusinessViewHolder<BusinessObjectClass>
  {

    private final SmartAdapters.BusinessViewWrapper<BusinessObjectClass> businessViewWrapper;

    private View view;

    public BusinessViewHolder(SmartAdapters.BusinessViewWrapper<BusinessObjectClass> businessViewWrapper)
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
    public final SmartAdapters.BusinessViewWrapper<BusinessObjectClass> getBusinessViewWrapper()
    {
      return businessViewWrapper;
    }

    // public final void updateBusinessObject(BusinessObjectClass businessObject)
    // {
    // businessViewWrapper.updateBusinessObject(businessObject);
    // }

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
     * This will invoke the {@link SmartAdapters.BusinessViewWrapper#updateView(Activity, View, int)} method with a <code>position</code> set to 0.
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

}
