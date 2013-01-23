package com.smartnsoft.droid4me.support.v4.app;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.smartnsoft.droid4me.framework.DetailsProvider;
import com.smartnsoft.droid4me.framework.SmartAdapters;
import com.smartnsoft.droid4me.framework.SmartAdapters.ObjectEvent;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.ui.SimpleWrappedListView;
import com.smartnsoft.droid4me.ui.WrappedListView;

/**
 * A basis class for designing an Android compatibility library {@link android.support.v4.app.Fragment} compatible with the framework, i.e.
 * droid4me-ready.
 * 
 * <p>
 * Warning: this class is only available for applications running under Android v1.6+, i.e. API level 4+, with the <a
 * href="http://developer.android.com/sdk/compatibility-library.html">Android Support Package</a>!
 * </p>
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2011.12.14
 */
public abstract class SmartListViewFragment<AggregateClass, ListViewClass extends ViewGroup>
    extends SmartFragment<AggregateClass>
    implements DetailsProvider.ForList<SmartAdapters.BusinessViewWrapper<?>, View>, WrappedListView.OnEventObjectListener<SmartAdapters.BusinessViewWrapper<?>>
{

  private WrappedListView<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View> wrappedListView;

  private ViewGroup wrapperLayout;

  /**
   * @return {@code false} by default
   */
  protected boolean containsText(SmartAdapters.BusinessViewWrapper<?> businessObject, String filterTextLower)
  {
    return true;
  }

  /**
   * @return {@code null} by default
   */
  protected Uri getContentUri()
  {
    return null;
  }

  @Override
  public void onPause()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartListActivity::onPause");
    }
    try
    {
      if (shouldKeepOn() == false)
      {
        // We stop here if a redirection is needed or is something went wrong
        return;
      }
      if (getWrappedListView() != null)
      {
        getWrappedListView().onPause();
      }
    }
    finally
    {
      super.onPause();
    }
  }

  @Override
  public void onStop()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartListActivity::onStop");
    }
    try
    {
      if (shouldKeepOn() == false)
      {
        // We stop here if a redirection is needed or is something went wrong
        return;
      }
      if (getWrappedListView() != null)
      {
        getWrappedListView().onStop();
      }
    }
    finally
    {
      super.onStop();
    }
  }

  /**
   * @return the wrapper of the UI list view
   */
  public final WrappedListView<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View> getWrappedListView()
  {
    return wrappedListView;
  }

  /**
   * The list view is wrapped by a {@link LinearLayout}, which is used to enable the display of a fixed header and footer.
   * 
   * @return the view which is the root of the activity layout
   */
  public final ViewGroup getWrapperLayout()
  {
    return wrapperLayout;
  }

  public final void onSelectedObject(View view, SmartAdapters.BusinessViewWrapper<?> selectedObject, int position)
  {
    onInternalEvent(view, selectedObject, SmartAdapters.ObjectEvent.Selected, position);
  }

  public final void onClickedObject(View view, SmartAdapters.BusinessViewWrapper<?> clickedObject, int position)
  {
    onInternalEvent(view, clickedObject, SmartAdapters.ObjectEvent.Clicked, position);
  }

  public final boolean onWipedObject(View view, SmartAdapters.BusinessViewWrapper<?> wipedObject, boolean leftToRight, int position)
  {
    return onInternalEvent(view, wipedObject, leftToRight == true ? SmartAdapters.ObjectEvent.WipedLeftToRight : SmartAdapters.ObjectEvent.WipedRightToLeft,
        position);
  }

  private boolean onInternalEvent(View view, SmartAdapters.BusinessViewWrapper<?> businessObject, ObjectEvent objectEvent, int position)
  {
    if (wrappedListView.getListView().isEnabled() == true)
    {
      // We set a protection against a bad usage from the end-user
      final Intent intent;
      try
      {
        intent = computeIntent(view, businessObject, objectEvent, position);
      }
      catch (Throwable throwable)
      {
        if (log.isErrorEnabled())
        {
          log.error(
              "The computing of the intent related to the business object with id '" + getObjectId(businessObject) + "' and for the UI event '" + objectEvent + "' seems buggy; not taken into account!",
              throwable);
        }
        return false;
      }
      if (intent != null)
      {
        startActivity(intent);
        return true;
      }
      else
      {
        // We set a protection against a bad usage from the end-user
        try
        {
          return onObjectEvent(view, businessObject, objectEvent, position);
        }
        catch (Throwable throwable)
        {
          if (log.isErrorEnabled())
          {
            log.error(
                "The computation of the action related to the business object with id '" + getObjectId(businessObject) + "' and for the UI event '" + objectEvent + "' seems faulty; not taken into account!",
                throwable);
          }
          return false;
        }
      }
    }
    return false;
  }

  protected WrappedListView.SmartAdapter<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View> computeAdapter()
  {
    return null;
  }

  @SuppressWarnings("unchecked")
  protected WrappedListView<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View> computeWrappedListView()
  {
    return (WrappedListView<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View>) new SimpleWrappedListView<SmartAdapters.BusinessViewWrapper<?>, View>(getActivity(), this)
    {
      @Override
      protected List<MenuCommand<SmartAdapters.BusinessViewWrapper<?>>> getContextualMenuCommands(SmartAdapters.BusinessViewWrapper<?> businessObject)
      {
        return SmartListViewFragment.this.getContextualMenuCommands(businessObject);
      }
    };
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
  {
    return wrapperLayout;
  }

  public void onRetrieveDisplayObjects()
  {
    wrappedListView = computeWrappedListView();
    final WrappedListView.SmartAdapter<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View> ownAdapter = computeAdapter();
    wrappedListView.setAdapter(ownAdapter == null ? new WrappedListView.SmartAdapter<SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View>(wrappedListView, this)
        : ownAdapter);
    wrappedListView.declareActionHandlers(getCompositeActionHandler());

    // We listen to the selected object, in case we are in a pick mode
    wrappedListView.setOnEventObjectListener(this);
    wrapperLayout = wrappedListView.createWrapperLayout(getActivity());
    wrappedListView.onRetrieveDisplayObjects();
    // This call is done only at that point, because we need the smart view inner result handler to be initialized
    wrappedListView.declareActivityResultHandlers(getCompositeActivityResultHandler());
    registerForContextMenu(wrappedListView.getListView());
  }

  public final boolean isEnabled(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.isEnabled();
  }

  public final void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    wrappedListView.onRetrieveBusinessObjects();
  }

  public final void onBusinessObjectsRetrieved()
  {
    wrappedListView.onBusinessObjectsRetrieved();
  }

  protected final List<MenuCommand<SmartAdapters.BusinessViewWrapper<?>>> getContextualMenuCommands(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getMenuCommands(getActivity());
  }

  public void onFulfillDisplayObjects()
  {
    wrappedListView.onFulfillDisplayObjects();
  }

  public void onSynchronizeDisplayObjects()
  {
    wrappedListView.onSynchronizeDisplayObjects();
  }

  public final int getViewType(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, int position)
  {
    return businessViewWrapper.getType(position);
  }

  public int getViewTypeCount()
  {
    return 1;
  }

  /**
   * Call this method when you want the list display to be recomputed because its underlying business object list has changed.
   * 
   * @param businessObjectCountAndSortingUnchanged
   *          set to {@code true} when either the business objects count and order have been unchanged, and just the data changed
   */
  public final void notifyBusinessObjectsChanged(boolean businessObjectCountAndSortingUnchanged)
  {
    getWrappedListView().notifyDataSetChanged(businessObjectCountAndSortingUnchanged);
  }

  @Override
  public void refreshBusinessObjectsAndDisplay(boolean retrieveBusinessObjects, final Runnable onOver, boolean immediately)
  {
    super.refreshBusinessObjectsAndDisplay(retrieveBusinessObjects, new Runnable()
    {
      public void run()
      {
        notifyBusinessObjectsChanged(false);
        if (onOver != null)
        {
          onOver.run();
        }
      }
    }, immediately);
  }

  public final List<? extends SmartAdapters.BusinessViewWrapper<?>> getFilteredObjects(List<? extends SmartAdapters.BusinessViewWrapper<?>> fullObjectsList)
  {
    if (getFilterText() == null)
    {
      return fullObjectsList;
    }
    else
    {
      return getFilteredByTextObjects(fullObjectsList);
    }
  }

  protected final List<? extends SmartAdapters.BusinessViewWrapper<?>> getFilteredByTextObjects(
      List<? extends SmartAdapters.BusinessViewWrapper<?>> fullObjectsList)
  {
    final String filterTextLower = getFilterText() == null ? null : getFilterText().toLowerCase();
    final List<SmartAdapters.BusinessViewWrapper<?>> filteredObjectsList = new ArrayList<SmartAdapters.BusinessViewWrapper<?>>();
    if (filterTextLower == null)
    {
      filteredObjectsList.addAll(fullObjectsList);
    }
    else
    {
      for (SmartAdapters.BusinessViewWrapper<?> businessObject : fullObjectsList)
      {
        if (containsText(businessObject, filterTextLower) == true)
        {
          filteredObjectsList.add(businessObject);
        }
      }
    }
    return filteredObjectsList;
  }

  protected final List<? extends SmartAdapters.BusinessViewWrapper<?>> getFilteredObjects()
  {
    return getWrappedListView().getFilteredObjects();
  }

  protected final Intent computeIntent(View view, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, SmartAdapters.ObjectEvent objectEvent, int position)
  {
    return businessViewWrapper.computeIntent(getActivity(), view, objectEvent, position);
  }

  protected final boolean onObjectEvent(View view, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, SmartAdapters.ObjectEvent objectEvent, int position)
  {
    return businessViewWrapper.onObjectEvent(getActivity(), view, objectEvent, position);
  }

  protected SmartAdapters.BusinessViewWrapper<?> getSelectedObject()
  {
    return wrappedListView.getSelectedObject();
  }

  protected void filterHasChanged()
  {
    wrappedListView.filterHasChanged(true);
  }

  protected String getFilterText()
  {
    return wrappedListView.getFilterText();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
  {
    super.onCreateContextMenu(menu, view, menuInfo);
    wrappedListView.onPopulateContextMenu(menu, view, menuInfo, getSelectedObject());
  }

  public final String getBusinessObjectName(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getName();
  }

  public final long getObjectId(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getId();
  }

  public final View getNewView(ViewGroup parent, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getNewView(parent, getActivity());
  }

  public final void updateView(View view, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, int position)
  {
    businessViewWrapper.updateView(getActivity(), view, position);
  }

}
