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

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.smartnsoft.droid4me.framework.Commands;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * In order to collect all the custom commands that are available for a business object.
 * 
 * @author Édouard Mercier
 * @since 2008.05.29
 */
public class MenuCommand<BusinessObjectClass>
{

  private static final Logger log = LoggerFactory.getInstance(MenuCommand.class);

  public int menuId;

  public final int textId;

  public final CharSequence text;

  public final char numericalShortcut;

  public final char characterShortcut;

  /**
   * When set to a negative value, no icon is set.
   */
  public final int icon;

  public final Commands.Executable<BusinessObjectClass> executable;

  public MenuCommand(int textId, char numericalShortcut, char characterShortcut, Commands.Executable<BusinessObjectClass> executable)
  {
    this(null, textId, numericalShortcut, characterShortcut, -1, executable);
  }

  public MenuCommand(int textId, char numericalShortcut, char characterShortcut, int icon, Commands.Executable<BusinessObjectClass> executable)
  {
    this(null, textId, numericalShortcut, characterShortcut, icon, executable);
  }

  public MenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, Commands.Executable<BusinessObjectClass> executable)
  {
    this(text, -1, numericalShortcut, characterShortcut, -1, executable);
  }

  public MenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, int icon, Commands.Executable<BusinessObjectClass> executable)
  {
    this(text, -1, numericalShortcut, characterShortcut, icon, executable);
  }

  public MenuCommand(CharSequence text, int textId, char numericalShortcut, char characterShortcut, int icon,
      Commands.Executable<BusinessObjectClass> executable)
  {
    this.text = text;
    this.textId = textId;
    this.numericalShortcut = numericalShortcut;
    this.characterShortcut = characterShortcut;
    this.icon = icon;
    this.executable = executable;
  }

  public final void runAction(BusinessObjectClass businessObject)
  {
    executable.run(businessObject);
  }

  public final boolean isVisible(BusinessObjectClass businessObject)
  {
    return executable.isVisible(businessObject);
  }

  public final boolean isEnabled(BusinessObjectClass businessObject)
  {
    return executable.isEnabled(businessObject);
  }

  public void computeMenuItem(Context context, Menu menu, int identifierOffset)
  {
    final int theMenuId = menuId + identifierOffset;
    final MenuItem menuEntry;
    if (text == null)
    {
      menuEntry = menu.add(Menu.NONE, theMenuId, Menu.NONE, textId);
    }
    else
    {
      menuEntry = menu.add(Menu.NONE, theMenuId, Menu.NONE, text);
    }
    menuEntry.setShortcut(numericalShortcut, characterShortcut);
    if (icon >= 0)
    {
      menuEntry.setIcon(icon);
    }
    if (log.isDebugEnabled())
    {
      log.debug("Added to the menu the entry with id '" + theMenuId + "'");
    }
  }

}
