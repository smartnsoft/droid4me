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

import android.os.Bundle;

import com.smartnsoft.droid4me.framework.BusinessObjectLifeCycle;
import com.smartnsoft.droid4me.framework.ForBusinessObjectImplementation;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler.Custom;

/**
 * An activity which handles the persistence properly, which proposes a framework regarding the custom action, and which provides a framework
 * regarding the activity life cycle.
 * 
 * @author Édouard Mercier
 * @date 2008.04.13
 */
public abstract class ForBusinessObjectActivity<AggregateClass, BusinessObjectClass>
    extends SmartActivity<AggregateClass>
    implements BusinessObjectLifeCycle<BusinessObjectClass>
{

  private final ForBusinessObjectImplementation<BusinessObjectClass> forBusinessObjectImplementation = new ForBusinessObjectImplementation<BusinessObjectClass>()
  {

    public List<MenuCommand<BusinessObjectClass>> getCustomActions()
    {
      return ForBusinessObjectActivity.this.getCustomActions();
    }

    public BusinessObjectClass retrieveBusinessObject()
        throws BusinessObjectUnavailableException
    {
      return ForBusinessObjectActivity.this.retrieveBusinessObject();
    }

    public void onBusinessObjectsRetrieved()
    {
      ForBusinessObjectActivity.this.onBusinessObjectsRetrieved();
    }

  };

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    if (isBeingRedirected() == true)
    {
      // We stop here if a redirection is needed
      return;
    }
    getCompositeActionHandler().add(forBusinessObjectImplementation.getActionHandler());
  }

  public final Custom<BusinessObjectClass> getActionHandler()
  {
    return forBusinessObjectImplementation.getActionHandler();
  }

  public final BusinessObjectClass getBusinessObject()
  {
    try
    {
      return forBusinessObjectImplementation.getBusinessObject();
    }
    catch (BusinessObjectUnavailableException exception)
    {
      // The nullity case should be properly handled downwards
      return null;
    }
  }

  public final boolean isBusinessObject()
  {
    return forBusinessObjectImplementation.isBusinessObject();
  }

  public void onRetrieveBusinessObjects()
      throws BusinessObjectUnavailableException
  {
    forBusinessObjectImplementation.retrieveBusinessObjects();
  }

  /**
   * By default, no action is available.
   */
  public List<MenuCommand<BusinessObjectClass>> getCustomActions()
  {
    return null;
  }

}
