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

package com.smartnsoft.droid4me.app;

import java.util.ArrayList;
import java.util.List;

import com.smartnsoft.droid4me.framework.DetailsProvider;
import com.smartnsoft.droid4me.framework.Events;
import com.smartnsoft.droid4me.framework.DetailsProvider.ObjectEvent;
import com.smartnsoft.droid4me.framework.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.ui.SimpleSmartListView;
import com.smartnsoft.droid4me.ui.SmartListView;

import android.content.Intent;
import android.net.Uri;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.LinearLayout;

/**
 * In order to propose a list activity which automates many things regarding its underlying business objects.
 * 
 * @author Édouard Mercier
 * @since 2008.04.04
 */
public abstract class SmartListActivity<AggregateClass, BusinessObjectClass, ViewClass extends View>
    extends SmartActivity<AggregateClass>
    implements DetailsProvider.ForList<BusinessObjectClass, ViewClass>, SmartListView.OnEventObjectListener<BusinessObjectClass>
{

  protected final static Logger log = LoggerFactory.getInstance(SmartListActivity.class);

  private SmartListView<BusinessObjectClass, ViewClass> smartListView;

  private ViewGroup wrapperLayout;

  /**
   * @return <code>false</code> by default
   */
  protected boolean containsText(BusinessObjectClass businessObject, String filterTextLower)
  {
    return true;
  }

  /**
   * @return <code>null</code> by default
   */
  protected Uri getContentUri()
  {
    return null;
  }

  @Override
  protected void onPause()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartListActivity::onPause");
    }
    try
    {
      if (isBeingRedirected() == true)
      {
        // We stop here if a redirection is needed
        return;
      }
      if (getSmartListView() != null)
      {
        getSmartListView().onPause();
      }
    }
    finally
    {
      super.onPause();
    }
  }

  @Override
  protected void onStop()
  {
    if (log.isDebugEnabled())
    {
      log.debug("SmartListActivity::onStop");
    }
    try
    {
      if (isBeingRedirected() == true)
      {
        // We stop here if a redirection is needed
        return;
      }
      if (getSmartListView() != null)
      {
        getSmartListView().onStop();
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
  public SmartListView<BusinessObjectClass, ViewClass> getSmartListView()
  {
    return smartListView;
  }

  /**
   * The list view is wrapped by a {@link LinearLayout}, which is used to enable the display of a fixed header and footer.
   * 
   * @return the view which is the root of the activity layout
   */
  public ViewGroup getWrapperLayout()
  {
    return wrapperLayout;
  }

  public final void onSelectedObject(View view, BusinessObjectClass selectedObject)
  {
    onInternalEvent(view, selectedObject, DetailsProvider.ObjectEvent.Selected);
  }

  public final void onClickedObject(View view, BusinessObjectClass clickedObject)
  {
    onInternalEvent(view, clickedObject, DetailsProvider.ObjectEvent.Clicked);
  }

  public final boolean onWipedObject(View view, BusinessObjectClass wipedObject, boolean leftToRight)
  {
    return onInternalEvent(view, wipedObject, leftToRight == true ? DetailsProvider.ObjectEvent.WipedLeftToRight : DetailsProvider.ObjectEvent.WipedRightToLeft);
  }

  private boolean onInternalEvent(View view, BusinessObjectClass businessObject, ObjectEvent objectEvent)
  {
    if (smartListView.getListView().isEnabled() == true)
    {
      // We set a protection against a bad usage from the end-user
      final Intent intent;
      try
      {
        intent = computeIntent(view, businessObject, objectEvent);
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
          return onObjectEvent(view, businessObject, objectEvent);
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

  protected SmartListView.SmartAdapter<BusinessObjectClass, ViewClass> computeAdapter()
  {
    return null;
  }

  public void onRetrieveDisplayObjects()
  {
    smartListView = new SimpleSmartListView<BusinessObjectClass, ViewClass>(this, this)
    {
      @Override
      protected List<MenuCommand<BusinessObjectClass>> getContextualMenuCommands(BusinessObjectClass businessObject)
      {
        return SmartListActivity.this.getContextualMenuCommands(businessObject);
      }

      @Override
      protected void customizeFooter(LinearLayout middleLayout)
      {
        SmartListActivity.this.customizeFooter(middleLayout);
      }
    };
    final SmartListView.SmartAdapter<BusinessObjectClass, ViewClass> ownAdapter = computeAdapter();
    smartListView.setAdapter(ownAdapter == null ? new SmartListView.SmartAdapter<BusinessObjectClass, ViewClass>(smartListView, this) : ownAdapter);
    smartListView.declareActionHandlers(getCompositeActionHandler());

    // We listen to the selected object, in case we are in a pick mode
    smartListView.setOnEventObjectListener(this);
    wrapperLayout = smartListView.createWrapperLayout(this);
    setContentView(wrapperLayout);
    smartListView.onRetrieveDisplayObjects();
    // This call is done only at that point, because we need the smart view inner result handler to be initialized
    smartListView.declareActivityResultHandlers(getCompositeActivityResultHandler());
    registerForContextMenu(smartListView.getListView());
  }

  public boolean isEnabled(BusinessObjectClass businessObject)
  {
    return true;
  }

  public final void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    smartListView.onRetrieveBusinessObjects();
  }

  public final void onBusinessObjectsRetrieved()
  {
    smartListView.onBusinessObjectsRetrieved();
  }

  protected List<MenuCommand<BusinessObjectClass>> getContextualMenuCommands(BusinessObjectClass businessObject)
  {
    return null;
  }

  protected void customizeFooter(LinearLayout middleLayout)
  {
  }

  public void onFulfillDisplayObjects()
  {
    smartListView.onFulfillDisplayObjects();
  }

  public void onSynchronizeDisplayObjects()
  {
    smartListView.onSynchronizeDisplayObjects();
  }

  public int getViewType(BusinessObjectClass businessObject, int position)
  {
    return 0;
  }

  public int getViewTypeCount()
  {
    return 1;
  }

  /**
   * Call this method when you want the list display to be recomputed because its underlying business object list has changed.
   * 
   * @param businessObjectCountAndSortingUnchanged
   *          set to <code>true</code> when either the business objects count and order have been unchanged, and just the data changed
   */
  public final void notifyBusinessObjectsChanged(boolean businessObjectCountAndSortingUnchanged)
  {
    getSmartListView().notifyDataSetChanged(businessObjectCountAndSortingUnchanged);
  }

  /**
   * Forces the retrieving of the business objects list, and eventually asks to the underlying list UI to refresh.
   * 
   * @param businessObjectCountAndSortingUnchanged
   *          set to <code>true</code> when either the business objects count and order have been unchanged, and just the data changed
   * @see #notifyBusinessObjectsChanged(boolean)
   * @see #refreshBusinessObjectsAndDisplay(boolean)
   */
  public final void refreshBusinessObjectsAndDisplayAndNotifyBusinessObjectsChanged(final boolean businessObjectCountAndSortingUnchanged)
  {
    refreshBusinessObjectsAndDisplay(true, new Events.OnCompletion()
    {
      public void done()
      {
        notifyBusinessObjectsChanged(businessObjectCountAndSortingUnchanged);
      }
    });
  }

  public final List<? extends BusinessObjectClass> getFilteredObjects(List<? extends BusinessObjectClass> fullObjectsList)
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

  protected final List<? extends BusinessObjectClass> getFilteredByTextObjects(List<? extends BusinessObjectClass> fullObjectsList)
  {
    final String filterTextLower = getFilterText().toLowerCase();
    List<BusinessObjectClass> filteredObjectsList = new ArrayList<BusinessObjectClass>();
    for (BusinessObjectClass businessObject : fullObjectsList)
    {
      if (containsText(businessObject, filterTextLower) == true)
      {
        filteredObjectsList.add(businessObject);
      }
    }
    return filteredObjectsList;
  }

  protected final List<? extends BusinessObjectClass> getFilteredObjects()
  {
    return getSmartListView().getFilteredObjects();
  }

  /**
   * Indicates what intent to trigger corresponding to this kind of event.
   */
  protected abstract Intent computeIntent(View view, BusinessObjectClass businessObject, DetailsProvider.ObjectEvent objectEvent);

  /**
   * Does nothing by default.
   */
  protected boolean onObjectEvent(View view, BusinessObjectClass businessObject, DetailsProvider.ObjectEvent objectEvent)
  {
    return false;
  }

  protected BusinessObjectClass getSelectedObject()
  {
    return smartListView.getSelectedObject();
  }

  protected void filterHasChanged()
  {
    smartListView.filterHasChanged(true);
  }

  protected String getFilterText()
  {
    return smartListView.getFilterText();
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo)
  {
    super.onCreateContextMenu(menu, view, menuInfo);
    smartListView.onPopulateContextMenu(menu, view, menuInfo, getSelectedObject());
  }

}
