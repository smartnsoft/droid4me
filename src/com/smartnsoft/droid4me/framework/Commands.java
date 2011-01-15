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

/**
 * Defines contracts for designing commands on a business object.
 * 
 * @author Édouard Mercier
 * @since 2009.09.02
 */
public interface Commands
{

  /**
   * The interface that makes possible to run a command on a business object.
   */
  public static interface Executable<BusinessObjectClass>
  {

    public void run(BusinessObjectClass businessObject);

    public boolean isEnabled(BusinessObjectClass businessObject);

  }

  public static abstract class StaticExecutable
      implements Executable<Void>
  {

    public abstract void run();

    public abstract boolean isEnabled();

    public final void run(Void businessObject)
    {
      run();
    }

    public final boolean isEnabled(Void businessObject)
    {
      return isEnabled();
    }
  }

  /**
   * Here, no need to indicate whether the command is enabled or not: it is always enabled.
   */
  public static abstract class EnabledExecutable<BusinessObjectClass>
      implements Executable<BusinessObjectClass>
  {

    public final boolean isEnabled(BusinessObjectClass businessObject)
    {
      return true;
    }

  }

  public static abstract class StaticEnabledExecutable
      extends StaticExecutable
  {

    public final boolean isEnabled()
    {
      return true;
    }

  }

}
