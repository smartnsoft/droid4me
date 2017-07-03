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

import java.util.List;

import android.view.View;
import android.view.ViewGroup;

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
  public interface ForListHandler<BusinessObjectClass>
  {

    /**
     * Indicates the name of the business objects handled.
     */
    String getBusinessObjectName(BusinessObjectClass businessObject);

    /**
     * This is specific to the smart list.
     *
     * @return the list of business objects handled; a {@code null} is authorized and means an empty list
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
     * @return {@code true} if the business object is enabled (can be selected)
     */
    boolean isEnabled(BusinessObjectClass businessObject);

  }

  public interface ForListView<BusinessObjectClass, ViewClass>
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

    /**
     * @param parent         the parent view which is going to host the newly created view
     * @param businessObject the business object the created view will be initially attached to
     * @return a new valid view
     */
    ViewClass getNewView(ViewGroup parent, BusinessObjectClass businessObject);

    void updateView(ViewClass view, BusinessObjectClass businessObject, int position);

  }

  public interface ForList<BusinessObjectClass, ViewClass>
      extends DetailsProvider.ForListHandler<BusinessObjectClass>, DetailsProvider.ForListView<BusinessObjectClass, ViewClass>
  {

  }

}
