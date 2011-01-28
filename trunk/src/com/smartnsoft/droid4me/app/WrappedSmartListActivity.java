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

import com.smartnsoft.droid4me.framework.DetailsProvider;
import com.smartnsoft.droid4me.menu.MenuCommand;

import android.content.Intent;
import android.view.View;

/**
 * Uses wrapped business objects for simplifying the handling of heterogeneous business objects.
 * 
 * @author Édouard Mercier
 * @since 2009.04.29
 */
public abstract class WrappedSmartListActivity<AggregateClass>
    extends SmartListActivity<AggregateClass, DetailsProvider.BusinessViewWrapper<?>, View>
{

  @Override
  protected final Intent computeIntent(View view, DetailsProvider.BusinessViewWrapper<?> businessViewWrapper, DetailsProvider.ObjectEvent objectEvent)
  {
    if (businessViewWrapper != null)
    {
      return businessViewWrapper.computeIntent(this, view, objectEvent);
    }
    return null;
  }

  @Override
  protected final boolean onObjectEvent(View view, DetailsProvider.BusinessViewWrapper<?> businessViewWrapper, DetailsProvider.ObjectEvent objectEvent)
  {
    if (businessViewWrapper != null)
    {
      return businessViewWrapper.onObjectEvent(this, view, objectEvent);
    }
    return false;
  }

  @Override
  protected final boolean containsText(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper, String lowerText)
  {
    return businessViewWrapper.containsText(lowerText);
  }

  public final String getBusinessObjectName(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getName();
  }

  public final long getObjectId(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getId();
  }

  @Override
  public final boolean isEnabled(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.isEnabled();
  }

  @Override
  public final int getViewType(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper, int position)
  {
    return businessViewWrapper.getType(position);
  }

  public final View getNewView(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getNewView(this);
  }

  public final void updateView(View view, DetailsProvider.BusinessViewWrapper<?> businessViewWrapper, int position)
  {
    businessViewWrapper.updateView(this, view, position);
  }

  @Override
  protected final List<MenuCommand<DetailsProvider.BusinessViewWrapper<?>>> getContextualMenuCommands(DetailsProvider.BusinessViewWrapper<?> businessViewWrapper)
  {
    return businessViewWrapper.getMenuCommands(this);
  }

}
