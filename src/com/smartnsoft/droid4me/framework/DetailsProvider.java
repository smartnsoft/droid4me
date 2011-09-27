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

import java.util.List;

import android.view.View;

import com.smartnsoft.droid4me.LifeCycle;

/**
 * Introduced in order to define various contract between:
 * <ul>
 * <li>a {@link android.widget.ListView list view} and its {@link android.widget.ListAdapter adapter},
 * <li>
 * <li>a {@link android.widget.GridView grid view} and its {@link android.widget.ListAdapter adapter}.
 * <li>
 * </ul>
 * 
 * @author Édouard Mercier
 * @since 2008.05.13
 */
public abstract class DetailsProvider
{

  /**
   * Introduced in order to define the contract between a {@link android.widget.ListView list view} and its {@link android.widget.ListAdapter adapter}
   * .
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
     * 
     * @return the list of business objects handled; a <code>null</code> is authorized and means an empty list
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
