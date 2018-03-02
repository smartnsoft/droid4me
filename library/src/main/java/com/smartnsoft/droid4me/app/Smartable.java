// The MIT License (MIT)
//
// Copyright (c) 2017 Smart&Soft
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

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
