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
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.ui.WrappedListView.SmartAdapter;

/**
 * Gathers in one place some classes used by the {@link com.smartnsoft.droid4me.app.AbstractSmartListActivity} class, which handles
 * {@link android.widget.ListView list views}.
 * 
 * @author Édouard Mercier
 * @since 2011.09.27
 */
public abstract class SmartAdapters
{

  protected static final Logger log = LoggerFactory.getInstance("SmartAdapters");

  /**
   * Indicates the type of action on the underlying business object.
   */
  public static enum ObjectEvent
  {
    Clicked, Selected, WipedLeftToRight, WipedRightToLeft
  }

  /**
   * Wraps a business object and its underlying Android {@link android.view.View} in a list or a grid, or whatever kind of {@link Adapter}.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   * @param <ActivityClass>
   *          the {@link Activity} class which will host the current wrapper graphical representation
   * @since 2014.05.23
   */
  protected static abstract class BasicBusinessViewWrapper<BusinessObjectClass, ActivityClass extends Activity, ViewAttributesType>
  {

    /**
     * The default value that will be returned by {@link android.widget.ListAdapter#getItemId(int)} method.
     */
    public static final long DEFAULT_ITEM_ID = 0;

    private final BusinessObjectClass businessObject;

    public BasicBusinessViewWrapper(BusinessObjectClass businessObject)
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

    /**
     * Is responsible for creating a new {@link View}, which is able to represent the provided business object.
     * 
     * @param activity
     *          the Activity which hosts the view
     * @param parent
     *          the parent view, as provided by the {@link Adapter#getView(int, View, ViewGroup)} method
     * @param businessObjectClass
     *          the business object the returned view will represent graphically
     * @return a new view, which will be used by the underlying {@link Adapter}
     */
    protected abstract View createNewView(ActivityClass activity, ViewGroup parent, BusinessObjectClass businessObjectClass);

    protected abstract ViewAttributesType extractNewViewAttributes(ActivityClass activity, View view, BusinessObjectClass businessObjectClass);

    protected abstract void updateView(ActivityClass activity, ViewAttributesType viewAttributes, View view, BusinessObjectClass businessObjectClass,
        int position);

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

    public final boolean containsText(String lowerText)
    {
      return containsText(getBusinessObject(), lowerText);
    }

    public boolean containsText(BusinessObjectClass businessObject, String lowerText)
    {
      return true;
    }

    public final Intent computeIntent(ActivityClass activity, View view, SmartAdapters.ObjectEvent objectEvent, int position)
    {
      return computeIntent(activity, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent, position);
    }

    public Intent computeIntent(ActivityClass activity, Object viewAttributes, View view, BusinessObjectClass businessObject,
        SmartAdapters.ObjectEvent objectEvent, int position)
    {
      return null;
    }

    public final boolean onObjectEvent(ActivityClass activity, View view, SmartAdapters.ObjectEvent objectEvent, int position)
    {
      return onObjectEvent(activity, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent, position);
    }

    public boolean onObjectEvent(ActivityClass activity, Object viewAttributes, View view, BusinessObjectClass businessObject,
        SmartAdapters.ObjectEvent objectEvent, int position)
    {
      return false;
    }

    public final View getNewView(ViewGroup parent, ActivityClass activity)
    {
      final View view = createNewView(activity, parent, getBusinessObject());
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
    final View setNewView(ActivityClass activity, View view)
    {
      final Object viewAttributes = extractNewViewAttributes(activity, view, getBusinessObject());
      view.setTag(viewAttributes);
      return view;
    }

    /**
     * Should only be invoked once the {@link #setNewView(ActivityClass, View)} method has already been invoked.
     * 
     * @param view
     *          the {@code View} which holds the business object representation
     * @return the view attributes that have been attached to the provided view
     */
    @SuppressWarnings("unchecked")
    public final ViewAttributesType getViewAttributes(View view)
    {
      return (ViewAttributesType) view.getTag();
    }

    public final void updateView(ActivityClass activity, View view, int position)
    {
      final ViewAttributesType viewAttributes = getViewAttributes(view);
      updateView(activity, viewAttributes, view, getBusinessObject(), position);
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ViewAttributesType} type parameter is bound to the {@link Object class}.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   * @param <ActivityClass>
   *          the {@link Activity} class which will host the current wrapper graphical representation
   * @since 2014.05.23
   */
  public static abstract class BasisBusinessViewWrapper<BusinessObjectClass, ActivityClass extends Activity>
      extends SmartAdapters.BasicBusinessViewWrapper<BusinessObjectClass, ActivityClass, Object>
  {

    public BasisBusinessViewWrapper(BusinessObjectClass businessObject)
    {
      super(businessObject);
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ActivityClass} type parameter is bound to the actual Android built-in
   * {@link Activity class}.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   * 
   * @since 2009.04.29
   */
  public static abstract class BusinessViewWrapper<BusinessObjectClass>
      extends SmartAdapters.BasisBusinessViewWrapper<BusinessObjectClass, Activity>
  {

    public BusinessViewWrapper(BusinessObjectClass businessObject)
    {
      super(businessObject);
    }

  }

  /**
   * To capture how the simplified {@link SmartAdapters.BasisBusinessViewWrapper} work.
   * 
   * @see SmartAdapters.BasisBusinessViewWrapper
   * @since 2014.05.23
   * 
   * @param <BusinessObjectClass>
   *          the business object class in relation with the {@link SmartAdapter.BasisBusinessViewWrapper} first type parameter
   * @param <ActivityClass>
   *          the {@link Activity} class in relation with the {@link SmartAdapter.BasisBusinessViewWrapper} second type parameter
   */
  public static interface BusinessViewWrapperSimplified<BusinessObjectClass, ActivityClass extends Activity>
  {

    int getType(int position, BusinessObjectClass businessObjectClass);

    View createNewView(ActivityClass activity, ViewGroup parent, BusinessObjectClass businessObject);

  }

  /**
   * An implementation which has been introduced so as to prevent from code duplication.
   * 
   * @see SmartAdapters.BasisBusinessViewWrapper
   * @since 2014.05.23
   * 
   * @param <BusinessObjectClass>
   *          the business object class in relation with the {@link SmartAdapter.BasisBusinessViewWrapper} first type parameter
   * @param <ActivityClass>
   *          the {@link Activity} class in relation with the {@link SmartAdapter.BasisBusinessViewWrapper} second type parameter
   */
  public static class BusinessViewWrapperSimplifier<BusinessObjectClass, ActivityClass extends Activity>
      implements SmartAdapters.BusinessViewWrapperSimplified<BusinessObjectClass, ActivityClass>
  {

    public final int type;

    public final int layoutResourceId;

    public BusinessViewWrapperSimplifier(int type, int layoutResourceId)
    {
      this.type = type;
      this.layoutResourceId = layoutResourceId;
    }

    @Override
    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return type;
    }

    @Override
    public View createNewView(ActivityClass activity, ViewGroup parent, BusinessObjectClass businessObject)
    {
      // It is important that the activity itself be used as a basis context, otherwise, the inflated View context is limited!
      return activity.getLayoutInflater().inflate(layoutResourceId, parent, false);
    }

  }

  /**
   * A basic wrapper implementation, which lets specify its {@link #getType(int, Object)} and representation layout identifier used to
   * {@link LayoutInflater#inflate(int, android.view.ViewGroup) inflate it}.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   * @param <ActivityClass>
   *          the {@link Activity} class which will host the current wrapper graphical representation
   * @since 2014.05.23
   */
  public abstract static class BasisSimpleBusinessViewWrapper<BusinessObjectClass, ActivityClass extends Activity>
      extends SmartAdapters.BasisBusinessViewWrapper<BusinessObjectClass, ActivityClass>
      implements SmartAdapters.BusinessViewWrapperSimplified<BusinessObjectClass, ActivityClass>
  {

    protected final SmartAdapters.BusinessViewWrapperSimplifier<BusinessObjectClass, ActivityClass> simplifier;

    public BasisSimpleBusinessViewWrapper(BusinessObjectClass businessObject, int type, int layoutResourceId)
    {
      super(businessObject);
      this.simplifier = new SmartAdapters.BusinessViewWrapperSimplifier<BusinessObjectClass, ActivityClass>(type, layoutResourceId);
    }

    @Override
    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return simplifier.getType(position, businessObjectClass);
    }

    @Override
    public View createNewView(ActivityClass activity, ViewGroup parent, BusinessObjectClass businessObject)
    {
      return simplifier.createNewView(activity, parent, businessObject);
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ActivityClass} type parameter is bound to the actual Android built-in
   * {@link Activity class}.
   * 
   * @param <BusinessObjectClass>
   *          the business object class which is represented by the current wrapper
   */
  public abstract static class SimpleBusinessViewWrapper<BusinessObjectClass>
      extends SmartAdapters.BusinessViewWrapper<BusinessObjectClass>
      implements SmartAdapters.BusinessViewWrapperSimplified<BusinessObjectClass, Activity>
  {

    protected final SmartAdapters.BusinessViewWrapperSimplifier<BusinessObjectClass, Activity> simplifier;

    public SimpleBusinessViewWrapper(BusinessObjectClass businessObject, int type, int layoutResourceId)
    {
      super(businessObject);
      this.simplifier = new SmartAdapters.BusinessViewWrapperSimplifier<BusinessObjectClass, Activity>(type, layoutResourceId);
    }

    @Override
    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return simplifier.getType(position, businessObjectClass);
    }

    @Override
    public View createNewView(Activity activity, ViewGroup parent, BusinessObjectClass businessObject)
    {
      return simplifier.createNewView(activity, parent, businessObject);
    }

  }

  /**
   * This class wraps the {@link SmartAdapters.BusinessViewWrapper} when not used inside a {@link android.widget.ListView list}.
   * 
   * @param the
   *          business object class which is represented by the current wrapper
   * 
   * @since 2014.05.23
   */
  public static class BasisBusinessViewHolder<BusinessObjectClass, ActivityClass extends Activity>
  {

    private final SmartAdapters.BasisBusinessViewWrapper<BusinessObjectClass, ActivityClass> businessViewWrapper;

    private View view;

    public BasisBusinessViewHolder(SmartAdapters.BasisBusinessViewWrapper<BusinessObjectClass, ActivityClass> businessViewWrapper)
    {
      this.businessViewWrapper = businessViewWrapper;
    }

    /**
     * Is allowed to be invoked once the {@link #getView(ActivityClass)} or {@link #setView(ActivityClass, View)} method has been called.
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
    public final SmartAdapters.BasisBusinessViewWrapper<BusinessObjectClass, ActivityClass> getBusinessViewWrapper()
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
     * This will invoke the {@link DetailsProvider.BusinessViewWrapper#getNewView(ViewGroup, ActivityClass} method.
     * </p>
     * 
     * @param activity
     *          the activity on which the business object is being rendered
     * @return the initialized view that represent the underlying business object
     */
    public final View getView(ViewGroup parent, ActivityClass activity)
    {
      view = businessViewWrapper.getNewView(parent, activity);
      return view;
    }

    /**
     * Sets the view of the of the underlying business view wrapper, so that it is not necessary to invoke the {@link #getView(ActivityClass)} method.
     * 
     * @param activity
     *          the activity on which the business object is being rendered
     * @param view
     *          the view that will be attached to the business view wrapper
     * @return
     */
    public final View setView(ActivityClass activity, View view)
    {
      this.view = view;
      return businessViewWrapper.setNewView(activity, view);
    }

    /**
     * Synchronizes the rendering of the inner {@link View} with the state of the business object.
     * 
     * <p>
     * This will invoke the {@link SmartAdapters.BusinessViewWrapper#updateView(ActivityClass, View, int)} method with a <code>position</code> set to
     * 0.
     * </p>
     * 
     * @param activity
     *          the activity on which the business object is being rendered
     */
    public final void updateView(ActivityClass activity)
    {
      businessViewWrapper.updateView(activity, view, 0);
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ActivityClass} type parameter is bound to the actual Android built-in
   * {@link Activity class}.
   * 
   * @param the
   *          business object class which is represented by the current wrapper
   * 
   * @since 2010.06.23
   */
  public static class BusinessViewHolder<BusinessObjectClass>
      extends SmartAdapters.BasisBusinessViewHolder<BusinessObjectClass, Activity>
  {

    public BusinessViewHolder(BasisBusinessViewWrapper<BusinessObjectClass, Activity> businessViewWrapper)
    {
      super(businessViewWrapper);
    }

  }

  /**
   * A {@link ListView} adapter, which works closely with the {@link SmartAdapters.BusinessViewWrapper}.
   * 
   * @since 2012.11.23
   * @param <ViewClass>
   *          the class which represents each graphical row of the list
   */
  public static class SmartListAdapter<ViewClass extends View>
      extends BaseAdapter
      implements AdapterView.OnItemClickListener
  {

    protected final Activity activity;

    private int viewTypeCount = 1;

    protected List<? extends SmartAdapters.BusinessViewWrapper<?>> wrappers = new ArrayList<SmartAdapters.BusinessViewWrapper<?>>();

    private List<? extends SmartAdapters.BusinessViewWrapper<?>> forthcomingWrappers = null;

    /**
     * 
     * @param activity
     * @param viewTypeCount
     *          since the {@link #getViewTypeCount()} method is invoked only once, we need to state the number of different rows from the start
     */
    // Regarding the 'getViewTypeCount()' method invocation, read
    // http://stackoverflow.com/questions/15099041/listview-baseadapter-getviewtypecount-how-to-force-adapter-to-check-again
    public SmartListAdapter(Activity activity, int viewTypeCount)
    {
      this.activity = activity;
      this.viewTypeCount = viewTypeCount;
    }

    public void setWrappers(List<? extends SmartAdapters.BusinessViewWrapper<?>> wrappers)
    {
      this.forthcomingWrappers = wrappers;
    }

    public void setAdapter(ListView listView)
    {
      listView.setAdapter(this);
      listView.setOnItemClickListener(this);
    }

    public void setAdapter(GridView gridView)
    {
      gridView.setAdapter(this);
      gridView.setOnItemClickListener(this);
    }

    public final int getCount()
    {
      return wrappers.size();
    }

    public final Object getItem(int position)
    {
      return wrappers.get(position);
    }

    public final long getItemId(int position)
    {
      return wrappers.get(position).hashCode();
    }

    @Override
    public final boolean areAllItemsEnabled()
    {
      return false;
    }

    @Override
    public final boolean isEnabled(int position)
    {
      return wrappers.get(position).isEnabled();
    }

    @Override
    public final int getItemViewType(int position)
    {
      return wrappers.get(position).getType(position);
    }

    @Override
    public final boolean hasStableIds()
    {
      // i.e. "false"
      return super.hasStableIds();
    }

    @Override
    public int getViewTypeCount()
    {
      return viewTypeCount;
    }

    @SuppressWarnings("unchecked")
    public final View getView(int position, View convertView, ViewGroup parent)
    {
      try
      {
        final SmartAdapters.BusinessViewWrapper<?> businessObject = wrappers.get(position);
        final ViewClass innerView;
        final boolean isRecycled = (convertView != null);
        if (isRecycled == false)
        {
          innerView = (ViewClass) businessObject.getNewView(parent, activity);
        }
        else
        {
          innerView = (ViewClass) convertView;
        }
        businessObject.updateView(activity, innerView, position);

        // We let the opportunity to catch this update event
        onInterceptGetView(innerView, position, isRecycled);

        return innerView;
      }
      catch (Throwable throwable)
      {
        // TODO: find a more elegant way, and report that problem to the main Exception handler
        if (log.isErrorEnabled())
        {
          log.error("Could not get or update the list view at position '" + position + "'", throwable);
        }
        return new View(parent.getContext());
      }
    }

    @Override
    public void notifyDataSetChanged()
    {
      if (forthcomingWrappers != null)
      {
        wrappers = forthcomingWrappers;
        forthcomingWrappers = null;
      }
      super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated()
    {
      if (forthcomingWrappers != null)
      {
        wrappers = forthcomingWrappers;
        forthcomingWrappers = null;
      }
      super.notifyDataSetInvalidated();
    }

    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id)
    {
      if (adapterView.isEnabled() == false)
      {
        return;
      }
      final int actualPosition = position - (adapterView instanceof ListView ? ((ListView) adapterView).getHeaderViewsCount() : 0);
      if (actualPosition < 0 || actualPosition >= wrappers.size())
      {
        return;
      }

      if (adapterView.isEnabled() == false)
      {
        return;
      }
      if (actualPosition >= wrappers.size())
      {
        if (log.isErrorEnabled())
        {
          log.error("The selected row " + actualPosition + " exceeds the size of the filtered business objetcs list which is " + wrappers.size());
        }
        return;
      }
      onInternalEvent(adapterView, view, wrappers.get(actualPosition), ObjectEvent.Clicked, actualPosition);
    }

    /**
     * Is invoked every time a {@link View} of the adapter is being updated. This is the right place for customizing the adapter.
     * 
     * @param view
     *          the view which holds the graphical representation of the business object
     * @param position
     *          the position in the adapter
     * @param isRecycled
     *          {@code true} if and only if the provided view has just been created and is hence not recycled
     */
    protected void onInterceptGetView(ViewClass view, int position, boolean isRecycled)
    {
    }

    private boolean onInternalEvent(AdapterView<?> adapterView, View view, SmartAdapters.BusinessViewWrapper<?> businessObject, ObjectEvent objectEvent,
        int position)
    {
      if (adapterView.isEnabled() == true)
      {
        // We set a protection against a bad usage from the end-user
        final Intent intent;
        try
        {
          intent = businessObject.computeIntent(activity, view, objectEvent, position);
        }
        catch (Throwable throwable)
        {
          if (log.isErrorEnabled())
          {
            log.error(
                "The computing of the intent related to the business object with id '" + businessObject.getId() + "' and for the UI event '" + objectEvent + "' seems buggy; not taken into account!",
                throwable);
          }
          return false;
        }
        if (intent != null)
        {
          activity.startActivity(intent);
          return true;
        }
        else
        {
          // We set a protection against a bad usage from the end-user
          try
          {
            return businessObject.onObjectEvent(activity, view, objectEvent, position);
          }
          catch (Throwable throwable)
          {
            if (log.isErrorEnabled())
            {
              log.error(
                  "The computation of the action related to the business object with id '" + businessObject.getId() + "' and for the UI event '" + objectEvent + "' seems faulty; not taken into account!",
                  throwable);
            }
            return false;
          }
        }
      }
      return false;
    }

  }

}
