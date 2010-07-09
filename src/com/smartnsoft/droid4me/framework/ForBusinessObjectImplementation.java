/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import com.smartnsoft.droid4me.framework.LifeCycle.BusinessObjectUnavailableException;
import com.smartnsoft.droid4me.menu.MenuCommand;
import com.smartnsoft.droid4me.menu.MenuHandler;
import com.smartnsoft.droid4me.menu.MenuHandler.Custom;


/**
 * A basic implementation.
 * 
 * @author Édouard Mercier
 * @date 2008.08.11
 */
public abstract class ForBusinessObjectImplementation<BusinessObjectClass>
    implements LifeCycle.ForBusinessObject<BusinessObjectClass>
{

  private BusinessObjectClass businessObject;

  private Custom<BusinessObjectClass> customActionHandler;

  public final void discardBusinessObject()
  {
    businessObject = null;
  }

  public final Custom<BusinessObjectClass> getActionHandler()
  {
    if (customActionHandler == null)
    {
      customActionHandler = new MenuHandler.Custom<BusinessObjectClass>()
      {

        @Override
        protected List<MenuCommand<BusinessObjectClass>> retrieveCommands()
        {
          return getCustomActions();
        }

        @Override
        protected BusinessObjectClass getActiveBusinessObject(MenuHandler.Custom<BusinessObjectClass> customActionHandler)
        {
          return businessObject;
        }

      };
    }
    return customActionHandler;
  }

  public final void retrieveBusinessObjects()
      throws LifeCycle.BusinessObjectUnavailableException
  {
    businessObject = retrieveBusinessObject();
    if (businessObject != null)
    {
      onBusinessObjectsRetrieved();
    }
    else
    {
      // There was a problem and the business object could not be retrieved
      throw new LifeCycle.BusinessObjectUnavailableException("Business object null");
    }
  }

  public final BusinessObjectClass getBusinessObject()
      throws BusinessObjectUnavailableException
  {
    return businessObject;
  }

  public final boolean isBusinessObject()
  {
    return businessObject != null;
  }

}
