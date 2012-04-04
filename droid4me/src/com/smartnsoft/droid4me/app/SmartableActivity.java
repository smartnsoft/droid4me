/*
 * (C) Copyright 2009-2012 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;

/**
 * All {@link Activity activities} of the framework must at least implement this interface.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @see Droid4mizer
 * @author Édouard Mercier
 * @since 2012.04.04
 */
public interface SmartableActivity<AggregateClass>
    extends Smartable<AggregateClass>
{

  /**
   * Indicates the {@link Activity} {@link Intent} to be launched when the {@link ActionBar} "Home" button is hit.
   * 
   * @param intent
   *          a valid {@code Intent}
   */
  void setHomeIntent(Intent intent);

}
