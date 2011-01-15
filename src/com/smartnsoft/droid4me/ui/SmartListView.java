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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.smartnsoft.droid4me.framework.ActivityResultHandler;
import com.smartnsoft.droid4me.framework.DetailsProvider;
import com.smartnsoft.droid4me.framework.LifeCycle;
import com.smartnsoft.droid4me.framework.DetailsProvider.ForList;
import com.smartnsoft.droid4me.framework.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;

/**
 * In order to propose a list which automates many things regarding its underlying business objects.
 * 
 * @author Édouard Mercier
 * @since 2008.05.19
 */
public abstract class SmartListView<BusinessObjectClass, ViewClass extends View>
    implements LifeCycle.ForActivity
{

  public static interface OnEventObjectListener<BusinessObjectClass>
  {
    void onSelectedObject(View view, BusinessObjectClass selectedObject);

    void onClickedObject(View view, BusinessObjectClass clickedObject);

    boolean onWipedObject(View view, BusinessObjectClass wipedObject, boolean leftToRight);
  }

  public static class SmartAdapter<BusinessObjectClass, ViewClass extends View>
      extends BaseAdapter
      implements Filterable
  {

    private final SmartListView<BusinessObjectClass, ViewClass> smartListView;

    private final ForList<BusinessObjectClass, ViewClass> forListProvider;

    private Filter filter;

    public SmartAdapter(SmartListView<BusinessObjectClass, ViewClass> smartListView, ForList<BusinessObjectClass, ViewClass> forListProvider)
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
        return DetailsProvider.DEFAULT_ITEM_ID;
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
        return DetailsProvider.DEFAULT_ITEM_ID;
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
      // synchronized (filteredObjectsSynchronization)
      {
        if (position >= smartListView.getFilteredObjects().size())
        {
          if (log.isWarnEnabled())
          {
            log.warn("Asking for a view type for a position " + position + " greater than that the filtered list size which is " + smartListView.getFilteredObjects().size());
          }
          return Adapter.IGNORE_ITEM_VIEW_TYPE;
        }
        businessObject = smartListView.getFilteredObjects().get(position);
      }
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
        // synchronized (filteredObjectsSynchronization)
        {
          if (position >= smartListView.getFilteredObjects().size())
          {
            if (log.isWarnEnabled())
            {
              log.warn("Asking for a view for a position " + position + " greater than that the filtered list size which is " + smartListView.getFilteredObjects().size());
            }
            return null;
          }
          businessObject = smartListView.getFilteredObjects().get(position);
        }
        final ViewClass innerView;
        boolean recycle = (convertView != null);// && forListProvider.doesViewFit((ViewClass) convertView, businessObject) == true);
        if (recycle == false)
        {
          // log.debug("Creating new view for list at position " + position);
          innerView = forListProvider.getNewView(businessObject);
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
            smartListView.recomputeFilterObjectsList();
            final FilterResults results = new FilterResults();
            synchronized (smartListView.filteredObjectsSynchronization)
            {
              results.count = smartListView.getFilteredObjects().size();
              results.values = smartListView.getFilteredObjects();
            }
            return results;
          }

          // This method is called from the main GUI thread
          protected synchronized void publishResults(CharSequence constraint, FilterResults results)
          {
            smartListView.filterHasChanged(false);
          }
        };
      }
      return filter;
    }

  }

  protected final static Logger log = LoggerFactory.getInstance(SmartListView.class);

  private BusinessObjectClass selectedObject;

  private List<? extends BusinessObjectClass> objects;

  private List<? extends BusinessObjectClass> filteredObjects;

  public final Object filteredObjectsSynchronization = new Object();

  private String filterText;

  protected MenuHandler.Custom<BusinessObjectClass> contextualActionHandler;

  private ActivityResultHandler.Handler activityResultHandler;

  private final Activity activity;

  protected abstract ForList<BusinessObjectClass, ViewClass> getForListProvider();

  /**
   * @return underlying list view
   */
  public abstract ListView getListView();

  public abstract void invalidateViews();

  public abstract void notifyDataSetChanged(boolean businessObjectCountAndSortingUnchanged);

  /**
   * Adds a view to the underlying list view, on its top, or on its bottom.
   * 
   * @param onTop
   *          if <code>true</code>, the view is added to the header ; otherwise to the footer
   * @param fixed
   *          whether the view will scroll or not
   * @param view
   *          the view to add
   */
  public abstract void addHeaderFooterView(boolean onTop, boolean fixed, View view);

  /**
   * Adds a view to the underlying list view, or its left or on its right.
   * 
   * @param onLeft
   *          if <code>true</code>, the view is added to left of the view ; otherwise to the right
   * @param view
   *          the view to add
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

  public SmartListView(Activity activity)
  {
    this.activity = activity;

    // We ask now for all custom actions, because it will be accessed very early
    contextualActionHandler = new MenuHandler.Contextual<BusinessObjectClass>()
    {

      @Override
      protected List<MenuCommand<BusinessObjectClass>> retrieveCommands()
      {
        return getContextualMenuCommands(getActiveBusinessObject(contextualActionHandler));
      }

      @Override
      protected BusinessObjectClass getActiveBusinessObject(MenuHandler.Custom<BusinessObjectClass> customActionHandler)
      {
        return getSelectedObject();
      }

    };
  }

  public LinearLayout getListWrapperLayout()
  {
    return listWrapperLayout;
  }

  protected final void setFilterText(String text)
  {
    filterText = text;
  }

  public final ViewGroup createWrapperLayout(Context context)
  {
    wrapperLayout = new LinearLayout(context);
    wrapperLayout.setOrientation(LinearLayout.VERTICAL);

    {
      listWrapperLayout = new LinearLayout(context);
      listWrapperLayout.setOrientation(LinearLayout.HORIZONTAL);

      // This is essential to give a width of 0 and a weight of 1
      listWrapperLayout.addView(getListView(), new LinearLayout.LayoutParams(0, LayoutParams.FILL_PARENT, 1));

      // This is essential to give a height of 0 and a weight of 1
      wrapperLayout.addView(listWrapperLayout, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, 0, 1));
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
//    if (VERSION.SDK_INT <= 4)
//    {
//      // This works-around the bug http://code.google.com/p/android/issues/detail?id=3484
//      leftLayout.addView(new View(wrapperLayout.getContext()), new LinearLayout.LayoutParams(0, 0));
//    }
    listWrapperLayout.addView(leftLayout, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
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

  public final void declareActionHandlers(MenuHandler.Composite compositeActionHandler)
  {
    compositeActionHandler.add(contextualActionHandler);
  }

  public final void declareActivityResultHandlers(ActivityResultHandler.CompositeHandler compositeActivityResultHandler)
  {
    compositeActivityResultHandler.add(activityResultHandler);
  }

  public void onFulfillDisplayObjects()
  {
    // And now that the objects have been retrieved, we can provide our tailored list adaptor
    setAdapter();
  }

  public void onSynchronizeDisplayObjects()
  {
  }

  public void onPause()
  {
  }

  public void onStop()
  {
  }

  public void onPopulateContextMenu(ContextMenu menu, View view, Object menuInfo, BusinessObjectClass businessObject)
  {
    if (businessObject == null)
    {
      // This may be the list header or footer
      return;
    }
    final String headerTitle = getForListProvider().getBusinessObjectName(businessObject);
    if (headerTitle != null)
    {
      menu.setHeaderTitle(headerTitle);
    }
    // We handle the commands coming from the custom actions
    contextualActionHandler.onPopulateContextMenu(menu, view, menuInfo);
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    // We can safely retrieve the business objects
    objects = getForListProvider().retrieveBusinessObjectsList();

    // Only now, we apply the filter
    recomputeFilterObjectsList();
  }

  public void onBusinessObjectsRetrieved()
  {
  }

  protected List<MenuCommand<BusinessObjectClass>> getContextualMenuCommands(BusinessObjectClass businessObject)
  {
    return null;
  }

  protected void customizeFooter(LinearLayout middleLayout)
  {
  }

  public final void filterHasChanged(boolean recomputeFilteredObjects)
  {
    synchronized (filteredObjectsSynchronization)
    {
      if (recomputeFilteredObjects == true)
      {
        recomputeFilterObjectsList();
      }
      // We need to indicate the list adapter that the underlying data have changed
      notifyDataSetChanged(!recomputeFilteredObjects);
    }
  }

  protected void recomputeObjectsAndRefreshDisplay()
      throws BusinessObjectUnavailableException
  {
    onRetrieveBusinessObjects();
    filterHasChanged(true);
    getListView().invalidate();
  }

  public void onRetrieveDisplayObjects()
  {
    activityResultHandler = new ActivityResultHandler.Handler();
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

  private List<? extends BusinessObjectClass> getObjects()
  {
    return objects;
  }

  public List<? extends BusinessObjectClass> getFilteredObjects()
  {
    synchronized (filteredObjectsSynchronization)
    {
      return filteredObjects;
    }
  }

  public Activity getActivity()
  {
    return activity;
  }

  /**
   * May return <code>null</code> if no filter text is currently active.
   */
  public String getFilterText()
  {
    return filterText;
  }

  protected final void recomputeFilterObjectsList()
  {
    synchronized (filteredObjectsSynchronization)
    {
      filteredObjects = getForListProvider().getFilteredObjects(getObjects());
    }
  }

}
