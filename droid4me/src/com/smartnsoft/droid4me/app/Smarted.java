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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

import com.smartnsoft.droid4me.menu.StaticMenuCommand;

/**
 * Defines some common methods for all {@link Activity} and {@link Fragment} entities defined in the framework.
 * 
 * @param <AggregateClass>
 *          the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * 
 * @author Édouard Mercier
 * @since 2010.07.10
 */
public interface Smarted<AggregateClass>
{

  /**
   * Gives access to the entity underlying "aggregate" object.
   * 
   * <p>
   * This "aggregate" is especially useful to provide data to the entity, and is typically used by the {@link ActivityController.Interceptor} through
   * the entity life cycle events.
   * </p>
   * 
   * @return an object that may be used along the {@link Activity}/{@link Fragment} entity life ; may return {@code null}
   * @see #setAggregate(Object)
   */
  AggregateClass getAggregate();

  /**
   * Enables to set an aggregate hat may be used along the {@link Activity}/{@link Fragment} entity life.
   * 
   * @param aggregate
   *          the object to use as an aggregate ; may be {@code null}
   * @see #getAggregate()
   */
  void setAggregate(AggregateClass aggregate);

  /**
   * Explicitly registers some {@link BroadcastReceiver wrapped broadcast receivers} for the {@link Activity}/{@link Fragment} entity. This method is
   * especially useful to declare and consume at the same place {@link Intent broadcast intents}.
   * 
   * <p>
   * Those receivers will finally be unregistered by the {@link Activity#onDestroy()}/{@link Fragment#onDestroy()} method.
   * </p>
   * 
   * <p>
   * When invoking that method, all previously registered listeners via the {@link AppPublics.BroadcastListenerProvider} or
   * {@link AppPublics.BroadcastListenersProvider} are kept, and the new provided ones are added.
   * </p>
   * 
   * @param broadcastListeners
   *          the wrapped {@link BroadcastReceiver receivers} to registers
   */
  void registerBroadcastListeners(AppPublics.BroadcastListener[] broadcastListeners);

  /**
   * This is a centralized method which will be invoked by the framework any time an exception is thrown by the entity.
   * 
   * <p>
   * It may be also invoked from the implementing entity, when an exception is thrown, so that the {@link ActivityController.ExceptionHandler} handles
   * it.
   * </p>
   * 
   * @param fromGuiThread
   *          indicates whether the call is done from the GUI thread
   */
  void onException(Throwable throwable, boolean fromGuiThread);

  /**
   * Gives access to an Android {@link Handler}, which is useful when executing a routine which should be run from the UI thread.
   * 
   * @return a valid handler that may be used by the entity for processing GUI-thread operations
   */
  Handler getHandler();

  /**
   * Gives access to an Android default {@link SharedPreferences} of the hosting Android application.
   * 
   * @return a valid default application preferences object
   */
  SharedPreferences getPreferences();

  /**
   * This callback will be invoked by the framework, so as to discover how the entity contributes to the Android menu system.
   * 
   * @return all the static menu commands that the entity wants to make available ; may return {@code null}, and in that case, this entity will not
   *         expose any menu entry
   */
  List<StaticMenuCommand> getMenuCommands();

}
