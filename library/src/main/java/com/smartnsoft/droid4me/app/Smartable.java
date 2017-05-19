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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;

import com.smartnsoft.droid4me.LifeCycle;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * All {@link Activity activities} and {@link Fragment fragments} entities of the framework must at least implement this composite interface.
 * <p>
 * <p>
 * Any entity implementing this interface is considered as droid4me-ready (or droid4me-compliant) and benefit from all the framework features.
 * </p>
 * <p>
 * <p>
 * If the implementing entity also implements the {@link AppPublics.BroadcastListener}, or the {@link AppPublics.BroadcastListenerProvider} or the
 * {@link AppPublics.BroadcastListenersProvider} interface, the framework will register one or several {@link BroadcastReceiver broadcast receivers},
 * as explained in the {@link AppPublics.BroadcastListener}.
 * </p>
 * <p>
 * <p>
 * When it is required to have an existing {@link Activity} or {@link android.app.Fragment} implement this interface, you may use the
 * {@link Droid4mizer} on that purpose.
 * </p>
 *
 * @param <AggregateClass> the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @author Édouard Mercier
 * @see SmartableActivity
 * @see Droid4mizer
 * @since 2011.03.04
 */
public interface Smartable<AggregateClass>
    extends Smarted<AggregateClass>, LifeCycle, AppPublics.LifeCyclePublic, AppInternals.LifeCycleInternals
{

  /**
   * A logger for all droid4me-ready entities.
   */
  Logger log = LoggerFactory.getInstance("Smartable");

}
