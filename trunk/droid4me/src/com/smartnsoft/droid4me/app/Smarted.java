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

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.os.Handler;

import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * Defines common methods for all {@link Activity activities} and {@link Fragment fragments} defined in the framework.
 * 
 * @author Édouard Mercier
 * @since 2010.07.10
 */
public interface Smarted<AggregateClass>
{

  /**
   * @return a valid handler that may be used for processing GUI-thread operations.
   */
  Handler getHandler();

  /**
   * @return an object that may be used along the {@link Activity} life ; may return {@code null}
   * @see #setAggregate(Object)
   */
  AggregateClass getAggregate();

  /**
   * Enables to set an aggregate hat may be used along the {@link Activity} life.
   * 
   * @param aggregate
   *          the object to use as an aggregate
   * @see #getAggregate()
   */
  void setAggregate(AggregateClass aggregate);

  /**
   * Explicitly registers {@link BroadcastReceiver wrapped broadcast receivers} for the {@link Activity}.
   * 
   * <p>
   * Those receivers will be unregistered by the {@link Activity#onDestroy} method.
   * </p>
   * 
   * <p>
   * When invoking that method, all previously registered listeners via the {@link AppPublics.BroadcastListenerProvider} or
   * {@link AppPublics.BroadcastListenersProvider} are kept.
   * </p>
   * 
   * @param broadcastListeners
   *          the wrapped {@link BroadcastReceiver receivers} to registers
   */
  void registerBroadcastListeners(AppPublics.BroadcastListener[] broadcastListeners);

  /**
   * @return all the static menu commands that the activity wants to make available ; may return {@code null}
   */
  List<StaticMenuCommand> getMenuCommands();

  /**
   * Can be invoked when an exception is thrown, so that the {@link ActivityController.ExceptionHandler} handles it.
   * 
   * @param fromGuiThread
   *          indicates whether the call is done from the GUI thread
   */
  void onException(Throwable throwable, boolean fromGuiThread);

}
