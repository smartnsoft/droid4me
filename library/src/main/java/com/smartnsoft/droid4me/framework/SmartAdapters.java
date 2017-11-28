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

package com.smartnsoft.droid4me.framework;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
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

/**
 * Gathers in one place some classes used by the {@link com.smartnsoft.droid4me.app.SmartActivity} class, which handles
 * {@link ListView list views}.
 *
 * @author Édouard Mercier
 * @since 2011.09.27
 */
public abstract class SmartAdapters
{

  /**
   * To capture how the simplified {@link BusinessViewWrapper} work.
   *
   * @param <BusinessObjectClass> the business object class in relation with the {@link BusinessViewWrapper} first type parameter
   * @see BusinessViewWrapper
   * @since 2014.05.23
   */
  public interface BusinessViewWrapperSimplified<BusinessObjectClass>
  {

    int getType(int position, BusinessObjectClass businessObjectClass);

    View createNewView(Context context, ViewGroup parent, BusinessObjectClass businessObject);

  }

  /**
   * Indicates the type of action on the underlying business object.
   */
  public enum ObjectEvent
  {
    Clicked, Selected, WipedLeftToRight, WipedRightToLeft
  }

  /**
   * Wraps a business object and its underlying Android {@link View} in a list or a grid, or whatever kind of {@link Adapter}.
   *
   * @param <BusinessObjectClass> the business object class which is represented by the current wrapper
   * @since 2014.05.23
   */
  protected static abstract class BasicBusinessViewWrapper<BusinessObjectClass, ViewAttributesType>
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

    public final Intent computeIntent(Context context, View view, ObjectEvent objectEvent,
        int position)
    {
      return computeIntent(context, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent,
          position);
    }

    public Intent computeIntent(Context context, Object viewAttributes, View view, BusinessObjectClass businessObject,
        ObjectEvent objectEvent, int position)
    {
      return null;
    }

    public final boolean onObjectEvent(Context context, View view, ObjectEvent objectEvent,
        int position)
    {
      return onObjectEvent(context, (view == null ? null : view.getTag()), view, getBusinessObject(), objectEvent,
          position);
    }

    public boolean onObjectEvent(Context context, Object viewAttributes, View view, BusinessObjectClass businessObject,
        ObjectEvent objectEvent, int position)
    {
      return false;
    }

    public final View getNewView(ViewGroup parent, Context context)
    {
      final View view = createNewView(context, parent, getBusinessObject());
      return setNewView(context, view);
    }

    /**
     * Should only be invoked once the {@link #setNewView(Context, View)} method has already been invoked.
     *
     * @param view the {@code View} which holds the business object representation
     * @return the view attributes that have been attached to the provided view
     */
    @SuppressWarnings("unchecked")
    public final ViewAttributesType getViewAttributes(View view)
    {
      return (ViewAttributesType) view.getTag();
    }

    public final void updateView(Context context, View view, int position)
    {
      final ViewAttributesType viewAttributes = getViewAttributes(view);
      updateView(context, viewAttributes, view, getBusinessObject(), position);
    }

    /**
     * Is responsible for creating a new {@link View}, which is able to represent the provided business object.
     *
     * @param parent              the parent view, as provided by the {@link Adapter#getView(int, View, ViewGroup)} method
     * @param businessObjectClass the business object the returned view will represent graphically
     * @return a new view, which will be used by the underlying {@link Adapter}
     */
    protected abstract View createNewView(Context context, ViewGroup parent,
        BusinessObjectClass businessObjectClass);

    protected abstract ViewAttributesType extractNewViewAttributes(Context context, View view,
        BusinessObjectClass businessObjectClass);

    protected abstract void updateView(Context context, ViewAttributesType viewAttributes, View view,
        BusinessObjectClass businessObjectClass, int position);

    /**
     * @return the {@link Object#hashCode()} value, by default
     */
    protected long getId(BusinessObjectClass businessObject)
    {
      return businessObject == null ? BusinessViewWrapper.DEFAULT_ITEM_ID : businessObject.hashCode();
    }

    /**
     * Attaches a view to the underlying business object.
     *
     * @param view the view to attach
     * @return the provided view
     */
    final View setNewView(Context context, View view)
    {
      final Object viewAttributes = extractNewViewAttributes(context, view, getBusinessObject());
      view.setTag(viewAttributes);
      return view;
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ViewAttributesType} type parameter is bound to the {@link Object class}.
   *
   * @param <BusinessObjectClass> the business object class which is represented by the current wrapper
   * @since 2014.05.23
   */
  public static abstract class BusinessViewWrapper<BusinessObjectClass>
      extends BasicBusinessViewWrapper<BusinessObjectClass, Object>
  {

    public BusinessViewWrapper(BusinessObjectClass businessObject)
    {
      super(businessObject);
    }

  }

  /**
   * An implementation which has been introduced so as to prevent from code duplication.
   *
   * @param <BusinessObjectClass> the business object class in relation with the {@link BusinessViewWrapper} first type parameter
   * @see BusinessViewWrapper
   * @since 2014.05.23
   */
  public static class BusinessViewWrapperSimplifier<BusinessObjectClass>
      implements BusinessViewWrapperSimplified<BusinessObjectClass>
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
    public View createNewView(Context context, ViewGroup parent, BusinessObjectClass businessObject)
    {
      // It is important that the context itself be used as a basis context, otherwise, the inflated View context is limited!
      return LayoutInflater.from(context).inflate(layoutResourceId, parent, false);
    }

  }

  /**
   * A basic wrapper implementation, which lets specify its {@link #getType(int, Object)} and representation layout identifier used to
   * {@link LayoutInflater#inflate(int, ViewGroup) inflate it}.
   *
   * @param <BusinessObjectClass> the business object class which is represented by the current wrapper
   * @since 2014.05.23
   */
  public static abstract class BasisSimpleBusinessViewWrapper<BusinessObjectClass>
      extends BusinessViewWrapper<BusinessObjectClass>
      implements BusinessViewWrapperSimplified<BusinessObjectClass>
  {

    protected final BusinessViewWrapperSimplifier<BusinessObjectClass> simplifier;

    public BasisSimpleBusinessViewWrapper(BusinessObjectClass businessObject, int type, int layoutResourceId)
    {
      super(businessObject);
      this.simplifier = new BusinessViewWrapperSimplifier<>(type, layoutResourceId);
    }

    @Override
    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return simplifier.getType(position, businessObjectClass);
    }

    @Override
    public View createNewView(Context context, ViewGroup parent, BusinessObjectClass businessObject)
    {
      return simplifier.createNewView(context, parent, businessObject);
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ActivityClass} type parameter is bound to the actual Android built-in
   * {@link Activity class}.
   *
   * @param <BusinessObjectClass> the business object class which is represented by the current wrapper
   */
  public static abstract class SimpleBusinessViewWrapper<BusinessObjectClass>
      extends BusinessViewWrapper<BusinessObjectClass>
      implements BusinessViewWrapperSimplified<BusinessObjectClass>
  {

    protected final BusinessViewWrapperSimplifier<BusinessObjectClass> simplifier;

    public SimpleBusinessViewWrapper(BusinessObjectClass businessObject, int type, int layoutResourceId)
    {
      super(businessObject);
      this.simplifier = new BusinessViewWrapperSimplifier<>(type, layoutResourceId);
    }

    @Override
    public int getType(int position, BusinessObjectClass businessObjectClass)
    {
      return simplifier.getType(position, businessObjectClass);
    }

    @Override
    public View createNewView(Context context, ViewGroup parent, BusinessObjectClass businessObject)
    {
      return simplifier.createNewView(context, parent, businessObject);
    }

  }

  /**
   * This class wraps the {@link BusinessViewWrapper} when not used inside a {@link ListView list}.
   *
   * @param <BusinessObjectClass> the business object class which is represented by the current wrapper
   * @since 2014.05.23
   */
  public static class BasisBusinessViewHolder<BusinessObjectClass>
  {

    private final BusinessViewWrapper<BusinessObjectClass> businessViewWrapper;

    private View view;

    public BasisBusinessViewHolder(BusinessViewWrapper<BusinessObjectClass> businessViewWrapper)
    {
      this.businessViewWrapper = businessViewWrapper;
    }

    /**
     * Is allowed to be invoked once the {@link #getView(ViewGroup, Context)} or {@link #setView(Context, View)} method has been called.
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
    public final BusinessViewWrapper<BusinessObjectClass> getBusinessViewWrapper()
    {
      return businessViewWrapper;
    }

    // public final void updateBusinessObject(BusinessObjectClass businessObject)
    // {
    // businessViewWrapper.updateBusinessObject(businessObject);
    // }

    /**
     * This method should be called only once during the object life cycle.
     * <p>
     * <p>
     * This will invoke the {@link BusinessViewWrapper#getNewView(ViewGroup, Context)} method.
     * </p>
     *
     * @return the initialized view that represent the underlying business object
     */
    public final View getView(ViewGroup parent, Context context)
    {
      view = businessViewWrapper.getNewView(parent, context);
      return view;
    }

    /**
     * Sets the view of the of the underlying business view wrapper, so that it is not necessary to invoke the {@link #getView(ViewGroup, Context)} method.
     *
     * @param view the view that will be attached to the business view wrapper
     * @return
     */
    public final View setView(Context context, View view)
    {
      this.view = view;
      return businessViewWrapper.setNewView(context, view);
    }

    /**
     * Synchronizes the rendering of the inner {@link View} with the state of the business object.
     * <p>
     * <p>
     * This will invoke the {@link BusinessViewWrapper#updateView(Context, View, int)} method with a <code>position</code> set to
     * 0.
     * </p>
     */
    public final void updateView(Context context)
    {
      businessViewWrapper.updateView(context, view, 0);
    }

  }

  /**
   * The same exact purpose of its parent class, except that the {@code ActivityClass} type parameter is bound to the actual Android built-in
   * {@link Activity class}.
   *
   * @param <BusinessObjectClass> the business object class which is represented by the current wrapper
   * @since 2010.06.23
   */
  public static class BusinessViewHolder<BusinessObjectClass>
      extends BasisBusinessViewHolder<BusinessObjectClass>
  {

    public BusinessViewHolder(BusinessViewWrapper<BusinessObjectClass> businessViewWrapper)
    {
      super(businessViewWrapper);
    }

  }

  /**
   * A {@link ListView} adapter, which works closely with the {@link BusinessViewWrapper}.
   *
   * @param <ViewClass> the class which represents each graphical row of the list
   * @since 2012.11.23
   */
  public static class SmartListAdapter<ViewClass extends View>
      extends BaseAdapter
      implements AdapterView.OnItemClickListener
  {

    protected final Context context;

    protected List<? extends BusinessViewWrapper<?>> wrappers = new ArrayList<>();

    private int viewTypeCount = 1;

    private List<? extends BusinessViewWrapper<?>> forthcomingWrappers = null;

    /**
     * @param viewTypeCount since the {@link #getViewTypeCount()} method is invoked only once, we need to state the number of different rows from the start
     */
    // Regarding the 'getViewTypeCount()' method invocation, read
    // http://stackoverflow.com/questions/15099041/listview-baseadapter-getviewtypecount-how-to-force-adapter-to-check-again
    public SmartListAdapter(Context context, int viewTypeCount)
    {
      this.context = context;
      this.viewTypeCount = viewTypeCount;
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
        final BusinessViewWrapper<?> businessObject = wrappers.get(position);
        final ViewClass innerView;
        final boolean isRecycled = (convertView != null);
        if (isRecycled == false)
        {
          innerView = (ViewClass) businessObject.getNewView(parent, context);
        }
        else
        {
          innerView = (ViewClass) convertView;
        }

        businessObject.updateView(context, innerView, position);

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
          log.error(
              "The selected row " + actualPosition + " exceeds the size of the filtered business objetcs list which is " + wrappers.size());
        }
        return;
      }
      onInternalEvent(adapterView, view, wrappers.get(actualPosition), ObjectEvent.Clicked, actualPosition);
    }

    public void setWrappers(List<? extends BusinessViewWrapper<?>> wrappers)
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

    /**
     * Is invoked every time a {@link View} of the adapter is being updated. This is the right place for customizing the adapter.
     *
     * @param view       the view which holds the graphical representation of the business object
     * @param position   the position in the adapter
     * @param isRecycled {@code true} if and only if the provided view has just been created and is hence not recycled
     */
    protected void onInterceptGetView(ViewClass view, int position, boolean isRecycled)
    {
    }

    private boolean onInternalEvent(AdapterView<?> adapterView, View view,
        BusinessViewWrapper<?> businessObject, ObjectEvent objectEvent, int position)
    {
      if (adapterView.isEnabled() == true)
      {
        // We set a protection against a bad usage from the end-user
        final Intent intent;
        try
        {
          intent = businessObject.computeIntent(context, view, objectEvent, position);
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
          context.startActivity(intent);
          return true;
        }
        else
        {
          // We set a protection against a bad usage from the end-user
          try
          {
            return businessObject.onObjectEvent(context, view, objectEvent, position);
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

  protected static final Logger log = LoggerFactory.getInstance("SmartAdapters");

}
