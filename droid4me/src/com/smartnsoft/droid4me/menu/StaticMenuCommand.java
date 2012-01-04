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

import com.smartnsoft.droid4me.framework.Commands;

/**
 * Specific to the activity main menu.
 * 
 * @author Édouard Mercier
 * @since 2009.09.02
 */
public class StaticMenuCommand
    extends MenuCommand<Void>
{

  public StaticMenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, Commands.StaticExecutable executable)
  {
    super(text, numericalShortcut, characterShortcut, -1, executable);
  }

  public StaticMenuCommand(CharSequence text, char numericalShortcut, char characterShortcut, int icon, Commands.StaticExecutable executable)
  {
    super(text, numericalShortcut, characterShortcut, icon, executable);
  }

  public StaticMenuCommand(int textId, char numericalShortcut, char characterShortcut, Commands.StaticExecutable executable)
  {
    super(textId, numericalShortcut, characterShortcut, -1, executable);
  }

  public StaticMenuCommand(int textId, char numericalShortcut, char characterShortcut, int icon, Commands.StaticExecutable executable)
  {
    super(textId, numericalShortcut, characterShortcut, icon, executable);
  }

}
