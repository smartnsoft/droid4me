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

package com.smartnsoft.droid4me.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.framework.DetailsProvider.ForList;
import com.smartnsoft.droid4me.framework.SmartAdapters.BusinessViewWrapper;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * In order to propose a list which automates many things regarding its underlying business objects.
 *
 * @author Édouard Mercier
 * @since 2008.05.19
 */
public abstract class WrappedListView<BusinessObjectClass, ListViewClass extends ViewGroup, ViewClass extends View>
    implements LifeCycle
{

  /**
   * Useful when {@link Context#sendBroadcast(android.content.Intent) sending broadcast} which need to indicate the position of a row.
   */
  public final static String LIST_VIEW_POSITION = "listViewPosition";

  public interface OnEventObjectListener<BusinessObjectClass>
  {

    void onSelectedObject(View view, BusinessObjectClass selectedObject, int position);

    void onClickedObject(View view, BusinessObjectClass clickedObject, int position);

    boolean onWipedObject(View view, BusinessObjectClass wipedObject, boolean leftToRight, int position);

  }

  public static class SmartAdapter<BusinessObjectClass, ListViewClass extends ViewGroup, ViewClass extends View>
      extends BaseAdapter
      implements Filterable
  {

    private final WrappedListView<BusinessObjectClass, ListViewClass, ViewClass> smartListView;

    private final ForList<BusinessObjectClass, ViewClass> forListProvider;

    private Filter filter;

    public SmartAdapter(WrappedListView<BusinessObjectClass, ListViewClass, ViewClass> smartListView,
        ForList<BusinessObjectClass, ViewClass> forListProvider)
    {
      this.smartListView = smartListView;
      this.forListProvider = forListProvider;
    }

    public final int getCount()
    {
      return smartListView.getFilteredObjects().size();
    }

    public final Object getItem(int position)
    {
      if (position >= smartListView.getFilteredObjects().size())
      {
        return null;
      }
      return smartListView.getFilteredObjects().get(position);
    }

    // While filtering, it may happen that the position asked for does not exist anymore
    public final long getItemId(int position)
    {
      if (position >= smartListView.getFilteredObjects().size())
      {
        if (log.isWarnEnabled())
        {
          log.warn("Asking for an item at position " + position + " greater than that the filtered list size which is " + smartListView.getFilteredObjects().size());
        }
        return BusinessViewWrapper.DEFAULT_ITEM_ID;
      }
      try
      {
        return forListProvider.getObjectId(smartListView.getFilteredObjects().get(position));
      }
      catch (Exception exception)
      {
        if (log.isErrorEnabled())
        {
          log.error("Cannot compute properly the identifier of a list view row", exception);
        }
        return BusinessViewWrapper.DEFAULT_ITEM_ID;
      }
    }

    @Override
    public final boolean areAllItemsEnabled()
    {
      return false;
    }

    @Override
    public final boolean isEnabled(int position)
    {
      if (position >= smartListView.getFilteredObjects().size())
      {
        return false;
      }
      return forListProvider.isEnabled(smartListView.getFilteredObjects().get(position));
    }

    @Override
    public final int getItemViewType(int position)
    {
      // log.debug("Asking for the view for list at position " + position);
      final BusinessObjectClass businessObject;
      if (position >= smartListView.getFilteredObjects().size())
      {
        if (log.isWarnEnabled())
        {
          log.warn("Asking for a view type for a position " + position + " greater than that the filtered list size which is " + smartListView.getFilteredObjects().size());
        }
        return Adapter.IGNORE_ITEM_VIEW_TYPE;
      }
      businessObject = smartListView.getFilteredObjects().get(position);
      return forListProvider.getViewType(businessObject, position);
    }

    @Override
    public final boolean hasStableIds()
    {
      // i.e. "false"
      return super.hasStableIds();
    }

    @Override
    public final int getViewTypeCount()
    {
      return forListProvider.getViewTypeCount();
    }

    @SuppressWarnings("unchecked")
    public final View getView(int position, View convertView, ViewGroup parent)
    {
      try
      {
        final BusinessObjectClass businessObject;
        if (position >= smartListView.getFilteredObjects().size())
        {
          if (log.isWarnEnabled())
          {
            log.warn("Asking for a view for a position " + position + " greater than that the filtered list size which is " + smartListView.getFilteredObjects().size());
          }
          return null;
        }
        businessObject = smartListView.getFilteredObjects().get(position);
        final ViewClass innerView;
        boolean recycle = (convertView != null);// && forListProvider.doesViewFit((ViewClass) convertView, businessObject) == true);
        if (recycle == false)
        {
          // log.debug("Creating new view for list at position " + position);
          innerView = forListProvider.getNewView(parent, businessObject);
        }
        else
        {
          // log.debug("Recycling the view for list at position " + position);
          innerView = (ViewClass) convertView;
        }
        forListProvider.updateView(innerView, businessObject, position);
        return innerView;
      }
      catch (Throwable throwable)
      {
        // TODO: find a more elegant way, and report that problem to the main Exception handler
        if (log.isErrorEnabled())
        {
          log.error("Could not get or update the list view at position '" + position + "'", throwable);
        }
        return new TextView(parent.getContext());
      }
    }

    public final synchronized Filter getFilter()
    {
      if (filter == null)
      {
        filter = new Filter()
        {
          // This method does not get called from the main GUI thread
          protected synchronized FilterResults performFiltering(CharSequence constraint)
          {
            smartListView.setFilterText(constraint.toString());
            final FilterResults results = new FilterResults();
            final List<? extends BusinessObjectClass> resultFilteredObjects = smartListView.getForListProvider().getFilteredObjects(smartListView.objects);
            results.values = resultFilteredObjects;
            results.count = resultFilteredObjects.size();
            return results;
          }

          // This method is called from the main GUI thread
          @SuppressWarnings("unchecked")
          protected synchronized void publishResults(CharSequence constraint, FilterResults results)
          {
            smartListView.filteredObjects = (List<? extends BusinessObjectClass>) results.values;
            smartListView.filterHasChanged(false);
          }
        };
      }
      return filter;
    }
  }

  protected final static Logger log = LoggerFactory.getInstance(WrappedListView.class);

  private BusinessObjectClass selectedObject;

  // Just there in order to ensure the consistency of the business objects for the adapter
  private List<? extends BusinessObjectClass> prevousObjects;

  private List<? extends BusinessObjectClass> objects;

  private List<? extends BusinessObjectClass> filteredObjects;

  private String filterText;

  private final Activity activity;

  protected OnEventObjectListener<BusinessObjectClass> onEventObjectListener;

  protected LinearLayout wrapperLayout;

  protected LinearLayout headerLayout;

  protected boolean headerAdded;

  protected LinearLayout footerLayout;

  protected boolean footerAdded;

  protected LinearLayout leftLayout;

  protected boolean leftAdded;

  protected LinearLayout rightLayout;

  protected boolean rightAdded;

  protected LinearLayout listWrapperLayout;

  protected abstract ForList<BusinessObjectClass, ViewClass> getForListProvider();

  /**
   * Is responsible for creating the {@link ListViewClass} widget.
   *
   * @param activity the activity creating the list view
   * @return a valid list view widget
   */
  protected abstract ListViewClass computeListView(Activity activity);

  /**
   * @return underlying list view
   */
  public abstract ListViewClass getListView();

  public abstract void invalidateViews();

  public abstract void notifyDataSetChanged(boolean businessObjectCountAndSortingUnchanged);

  /**
   * Adds a view to the underlying list view, on its top, or on its bottom.
   *
   * @param onTop if {@code true}, the view is added to the header ; otherwise to the footer
   * @param fixed whether the view will scroll or not
   * @param view  the view to add
   */
  public abstract void addHeaderFooterView(boolean onTop, boolean fixed, View view);

  /**
   * Adds a view to the underlying list view, or its left or on its right.
   *
   * @param onLeft if {@code true}, the view is added to left of the view ; otherwise to the right
   * @param view   the view to add
   */
  public abstract void addLeftRightView(boolean onLeft, View view);

  /**
   * Sets the view which will be used instead of the list it it is empty.
   */
  public abstract void setEmptyView(View view);

  public abstract void setAdapter(BaseAdapter adapter);

  /**
   * The method is supposed to associate the adapter to the list view, and register for all selection and click events.
   */
  protected abstract void setAdapter();

  protected abstract void setSelected(int position);

  public WrappedListView(Activity activity)
  {
    this.activity = activity;
  }

  public LinearLayout getListWrapperLayout()
  {
    return listWrapperLayout;
  }

  protected final void setFilterText(String text)
  {
    filterText = text;
  }

  /**
   * Is responsible for creating the {@link ViewGroup wrapper widget} which will hold the underlying {@link ListView}.
   * <p/>
   * <p>
   * This method is responsible for creating the {@link #wrapperLayout} and {@link #listWrapperLayout} widget objects, and for adding the
   * {@link #getListView()} to the created {@link #getListWrapperLayout()}.
   * </p>
   *
   * @param context the Android context
   * @return the created wrapper widget
   */
  public ViewGroup createWrapperLayout(Context context)
  {
    wrapperLayout = new LinearLayout(context);
    wrapperLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
    wrapperLayout.setOrientation(LinearLayout.VERTICAL);

    {
      listWrapperLayout = new LinearLayout(context);
      listWrapperLayout.setOrientation(LinearLayout.HORIZONTAL);
      //
      // // This is essential to give a weight of 1
      listWrapperLayout.addView(getListView(), new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));

      // This is essential to give weight of 1
      wrapperLayout.addView(listWrapperLayout, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1));
    }

    return wrapperLayout;
  }

  protected final void initializeHeader()
  {
    headerLayout = new LinearLayout(wrapperLayout.getContext());
    headerLayout.setOrientation(LinearLayout.VERTICAL);
    wrapperLayout.addView(headerLayout, 0, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
  }

  protected final void initializeFooter()
  {
    footerLayout = new LinearLayout(wrapperLayout.getContext());
    footerLayout.setOrientation(LinearLayout.VERTICAL);
    wrapperLayout.addView(footerLayout, 1 + (headerAdded == true ? 1 : 0), new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
  }

  protected final void initializeLeft()
  {
    leftLayout = new LinearLayout(wrapperLayout.getContext());
    leftLayout.setOrientation(LinearLayout.HORIZONTAL);
    // if (VERSION.SDK_INT <= 4)
    // {
    // // This works-around the bug http://code.google.com/p/android/issues/detail?id=3484
    // leftLayout.addView(new View(wrapperLayout.getContext()), new LinearLayout.LayoutParams(0, 0));
    // }
    listWrapperLayout.addView(leftLayout, 0, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
  }

  protected final void initializeRight()
  {
    rightLayout = new LinearLayout(wrapperLayout.getContext());
    rightLayout.setOrientation(LinearLayout.HORIZONTAL);
    // if (VERSION.SDK_INT <= 4)
    // {
    // // This works-around the bug http://code.google.com/p/android/issues/detail?id=3484
    // rightLayout.addView(new View(wrapperLayout.getContext()), new LinearLayout.LayoutParams(0, 0));
    // }
    listWrapperLayout.addView(rightLayout, 1 + (leftAdded == true ? 1 : 0), new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
  }

  public void onFulfillDisplayObjects()
  {
    // And now that the objects have been retrieved, we can provide our tailored list adaptor
    setAdapter();
  }

  public void onSynchronizeDisplayObjects()
  {
    prevousObjects = null;
  }

  public void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, Runnable onOver, boolean immediately)
  {
  }

  public void onPause()
  {
  }

  public void onStop()
  {
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    prevousObjects = objects;
    // We can safely retrieve the business objects
    objects = getForListProvider().retrieveBusinessObjectsList();

    if (objects == null)
    {
      // If the returned business objects is null, we consider it as an empty list
      objects = new ArrayList<BusinessObjectClass>();
    }

    // Only now, we apply the filter
    recomputeFilterObjectsList();
  }

  public void onBusinessObjectsRetrieved()
  {
  }

  public final void filterHasChanged(boolean recomputeFilteredObjects)
  {
    if (recomputeFilteredObjects == true)
    {
      recomputeFilterObjectsList();
    }
    // We need to indicate the list adapter that the underlying data have changed
    notifyDataSetChanged(!recomputeFilteredObjects);
  }

  public void onRetrieveDisplayObjects()
  {
  }

  public BusinessObjectClass getSelectedObject()
  {
    return selectedObject;
  }

  protected void setSelectedObject(BusinessObjectClass selectedObject)
  {
    this.selectedObject = selectedObject;
  }

  public void setOnEventObjectListener(OnEventObjectListener<BusinessObjectClass> onEventObjectListener)
  {
    this.onEventObjectListener = onEventObjectListener;
  }

  public List<? extends BusinessObjectClass> getFilteredObjects()
  {
    return prevousObjects != null ? prevousObjects : filteredObjects;
  }

  public Activity getActivity()
  {
    return activity;
  }

  /**
   * May return {@code null} if no filter text is currently active.
   */
  public String getFilterText()
  {
    return filterText;
  }

  protected final void recomputeFilterObjectsList()
  {
    filteredObjects = getForListProvider().getFilteredObjects(objects);
  }

}
