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

package com.smartnsoft.droid4me.menu;

import java.util.ArrayList;
import java.util.List;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import android.content.Context;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/**
 * In order to alleviate the work when creating static and contextual menus for business objects.
 * 
 * @author Édouard Mercier
 * @since 2008.04.10
 */
public abstract class MenuHandler
{

  /**
   * The basis implementation which enables to populate static and contextual menus and to trigger the related command.
   */
  public static abstract class Custom<BusinessObjectClass>
  {

    private List<MenuCommand<BusinessObjectClass>> commands;

    private boolean commandsRetrieved;

    private boolean enabled = true;

    private final int id;

    private static int counter;

    protected abstract BusinessObjectClass getActiveBusinessObject(MenuHandler.Custom<BusinessObjectClass> customCommandHandler);

    protected abstract List<MenuCommand<BusinessObjectClass>> retrieveCommands();

    public Custom()
    {
      id = counter;
      // We make the assumption that there are not more than 20 commands per handler
      counter += 20;
    }

    private int getOffsetIdentifier()
    {
      return id;
    }

    protected final void forceCommmandsRetrieval()
    {
      commandsRetrieved = false;
    }

    public List<MenuCommand<BusinessObjectClass>> getCommands()
    {
      if (commandsRetrieved == false)
      {
        commands = retrieveCommands();
        if (commands != null)
        {
          int index = 0;
          for (MenuCommand<BusinessObjectClass> command : commands)
          {
            command.menuId = index;
            index++;
          }
        }
        commandsRetrieved = true;
      }
      return commands;
    }

    public void onCreateOptionsMenu(Context context, Menu menu)
    {
      // Whatever the activation state, we must fulfill the menu
      if (getCommands() == null)
      {
        return;
      }
      for (MenuCommand<BusinessObjectClass> command : getCommands())
      {
        command.computeMenuItem(context, menu, getOffsetIdentifier());
      }
    }

    public void onPopulateContextMenu(ContextMenu menu, View view, Object menuInfo)
    {
      if (enabled == false)
      {
        return;
      }
      onPopulateContextMenu(menu, view, menuInfo, getActiveBusinessObject(this));
    }

    private void onPopulateContextMenu(ContextMenu menu, View view, Object menuInfo, BusinessObjectClass businessObject)
    {
      if (enabled == false)
      {
        return;
      }
      if (getCommands() == null)
      {
        return;
      }
      for (MenuCommand<BusinessObjectClass> command : getCommands())
      {
        if (command.isEnabled(businessObject) == true)
        {
          command.computeMenuItem(view.getContext(), menu, getOffsetIdentifier());
        }
      }
    }

    public void onPrepareOptionsMenu(Menu menu)
    {
      onPrepareOptionsMenu(menu, getActiveBusinessObject(this));
    }

    private void onPrepareOptionsMenu(Menu menu, BusinessObjectClass businessObject)
    {
      if (getCommands() == null)
      {
        return;
      }
      for (MenuCommand<BusinessObjectClass> command : getCommands())
      {
        boolean isEnabled;
        if (/* businessObject == null || */enabled == false)
        {
          isEnabled = false;
        }
        else
        {
          isEnabled = command.isEnabled(businessObject);
        }
        menu.findItem(command.menuId + getOffsetIdentifier()).setEnabled(isEnabled);
        menu.findItem(command.menuId + getOffsetIdentifier()).setVisible(isEnabled);
      }
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
      if (enabled == false)
      {
        return false;
      }
      return onMenuItemSelected(item, getActiveBusinessObject(this));
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
      if (enabled == false)
      {
        return false;
      }
      return onMenuItemSelected(item, getActiveBusinessObject(this));
    }

    public boolean onContextItemSelected(MenuItem item)
    {
      if (enabled == false)
      {
        return false;
      }
      return onMenuItemSelected(item, getActiveBusinessObject(this));
    }

    private boolean onMenuItemSelected(MenuItem item, BusinessObjectClass businessObject)
    {
      if (getCommands() == null /* || businessObject == null */)
      {
        return false;
      }
      for (MenuCommand<BusinessObjectClass> command : getCommands())
      {
        if (item.getItemId() == (command.menuId + getOffsetIdentifier()))
        {
          command.runAction(businessObject);
          return true;
        }
      }
      return false;
    }

  }

  /**
   * Will only be used on contextual menus.
   * 
   * @author Edouard Mercier
   * @since 2008.05.31
   */
  public abstract static class Contextual<BusinessObjectClass>
      extends MenuHandler.Custom<BusinessObjectClass>
  {

    @Override
    public void onCreateOptionsMenu(Context context, Menu menu)
    {
    }

    @Override
    public void onPopulateContextMenu(ContextMenu menu, View view, Object menuInfo)
    {
      // In the case of a contextual menu, we force the retrieving of the commands each time
      forceCommmandsRetrieval();
      super.onPopulateContextMenu(menu, view, menuInfo);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
      return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
      return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
    }

  }

  public abstract static class Static
      extends MenuHandler.Custom<Void>
  {

    @Override
    protected final Void getActiveBusinessObject(MenuHandler.Custom<Void> customMenuHandler)
    {
      return null;
    }

  }

  /**
   * Concentrates in one place multiple custom menu handlers.
   * 
   * @author Edouard Mercier
   * @since 2008.05.19
   */
  public static class Composite
  {

    protected static final Logger log = LoggerFactory.getInstance(Composite.class);

    private List<MenuHandler.Custom<?>> menuHandlers = new ArrayList<MenuHandler.Custom<?>>();

    public void add(MenuHandler.Custom<?> menuHandler)
    {
      menuHandlers.add(menuHandler);
    }

    public void onCreateOptionsMenu(Context context, Menu menu)
    {
      for (MenuHandler.Custom<?> handler : menuHandlers)
      {
        handler.onCreateOptionsMenu(context, menu);
      }
    }

    public void onPrepareOptionsMenu(Menu menu)
    {
      for (MenuHandler.Custom<?> handler : menuHandlers)
      {
        handler.onPrepareOptionsMenu(menu);
      }
    }

    public void onPopulateContextMenu(ContextMenu contextMenu, View view, Object menuInfo)
    {
      for (MenuHandler.Custom<?> handler : menuHandlers)
      {
        handler.onPopulateContextMenu(contextMenu, view, menuInfo);
      }
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
      if (log.isDebugEnabled())
      {
        log.debug("Searching for a command handler (among " + menuHandlers.size() + ") for the menu item selected with id '" + item.getItemId() + "'");
      }
      for (MenuHandler.Custom<?> handler : menuHandlers)
      {
        if (handler.onOptionsItemSelected(item) == true)
        {
          return true;
        }
      }
      if (log.isWarnEnabled())
      {
        log.warn("Could not find a command for the selected menu item with id '" + item.getItemId() + "'");
      }
      return false;
    }

    public boolean onContextItemSelected(MenuItem item)
    {
      if (log.isDebugEnabled())
      {
        log.debug("Searching for a command handler (among " + menuHandlers.size() + ") for the contextual menu item selected with id '" + item.getItemId() + "'");
      }
      for (MenuHandler.Custom<?> handler : menuHandlers)
      {
        if (handler.onContextItemSelected(item) == true)
        {
          return true;
        }
      }
      if (log.isWarnEnabled())
      {
        log.warn("Could not find a command for the selected contexttual menu item with id '" + item.getItemId() + "'");
      }
      return false;
    }

  }

}
