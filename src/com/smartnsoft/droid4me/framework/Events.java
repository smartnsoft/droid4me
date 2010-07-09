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

/**
 * In order to provide closure events.
 * 
 * @author Édouard Mercier
 * @since 2008.05.20
 */
public interface Events
{

  public static interface OnBusinessCompletion<BusinessObjectClass>
  {
    void done(BusinessObjectClass businessObject);
  }

  public static interface OnCompletion// extends Events.OnBusinessCompletion<Void>
  {
    void done();
  }

  //
  // public static class DoNothing
  // implements OnCompletionEvent
  // {
  //
  // private DoNothing()
  // {
  // }
  //
  // public void done()
  // {
  // }
  //
  // }
  //
  // public static final DoNothing DO_NOTHING = new DoNothing();

}
