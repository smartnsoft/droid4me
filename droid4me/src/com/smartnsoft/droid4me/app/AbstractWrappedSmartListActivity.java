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

import java.util.List;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.smartnsoft.droid4me.framework.SmartAdapters;
import com.smartnsoft.droid4me.menu.MenuCommand;

/**
 * Uses wrapped business objects for simplifying the handling of heterogeneous business objects.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @param <ListViewClass>
 *          the @{link ViewGroup} implementation class for the list view widget
 * 
 * @author Édouard Mercier
 * @since 2009.04.29
 */
public abstract class AbstractWrappedSmartListActivity<AggregateClass, ListViewClass extends ViewGroup>
    extends AbstractSmartListActivity<AggregateClass, SmartAdapters.BusinessViewWrapper<?>, ListViewClass, View>
{

  @Override
  protected final Intent computeIntent(View view, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, SmartAdapters.ObjectEvent objectEvent, int position)
  {
    return businessViewWrapper.computeIntent(this, view, objectEvent, position);
  }

  @Override
  protected final boolean onObjectEvent(View view, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, SmartAdapters.ObjectEvent objectEvent, int position)
  {
    return businessViewWrapper.onObjectEvent(this, view, objectEvent, position);
  }

  @Override
  protected final boolean containsText(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, String lowerText)
  {
    return businessViewWrapper.containsText(lowerText);
  }

  public final String getBusinessObjectName(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getName();
  }

  public final long getObjectId(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getId();
  }

  @Override
  public final boolean isEnabled(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.isEnabled();
  }

  @Override
  public final int getViewType(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, int position)
  {
    return businessViewWrapper.getType(position);
  }

  public final View getNewView(ViewGroup parent, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getNewView(parent, this);
  }

  public final void updateView(View view, SmartAdapters.BusinessViewWrapper<?> businessViewWrapper, int position)
  {
    businessViewWrapper.updateView(this, view, position);
  }

  @Override
  protected final List<MenuCommand<SmartAdapters.BusinessViewWrapper<?>>> getContextualMenuCommands(SmartAdapters.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getMenuCommands(this);
  }

}
