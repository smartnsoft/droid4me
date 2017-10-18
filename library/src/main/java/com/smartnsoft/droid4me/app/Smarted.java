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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

/**
 * Defines some common methods for all {@link Activity} and {@link Fragment} entities defined in the framework.
 *
 * @param <AggregateClass> the aggregate class accessible though the {@link #setAggregate(Object)} and {@link #getAggregate()} methods
 * @author Édouard Mercier
 * @since 2010.07.10
 */
public interface Smarted<AggregateClass>
{

  /**
   * Gives access to the entity underlying "aggregate" object.
   * <p>
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
   * @param aggregate the object to use as an aggregate ; may be {@code null}
   * @see #getAggregate()
   */
  void setAggregate(AggregateClass aggregate);

  /**
   * Explicitly registers some {@link BroadcastReceiver wrapped broadcast receivers} for the {@link Activity}/{@link Fragment} entity. This method is
   * especially useful to declare and consume at the same place {@link Intent broadcast intents}.
   * <p>
   * <p>
   * Those receivers will finally be unregistered by the {@link Activity#onDestroy()}/{@link Fragment#onDestroy()} method.
   * </p>
   * <p>
   * <p>
   * When invoking that method, all previously registered listeners via the {@link AppPublics.BroadcastListenerProvider} or
   * {@link AppPublics.BroadcastListenersProvider} are kept, and the new provided ones are added.
   * </p>
   *
   * @param broadcastListeners the wrapped {@link BroadcastReceiver receivers} to registers
   * @see AppPublics.BroadcastListener
   * @see AppPublics.BroadcastListenerProvider
   * @see AppPublics.BroadcastListenerProvider
   */
  void registerBroadcastListeners(AppPublics.BroadcastListener[] broadcastListeners);

  /**
   * This is a centralized method which will be invoked by the framework any time an exception is thrown by the entity.
   * <p>
   * <p>
   * It may be also invoked from the implementing entity, when an exception is thrown, so that the {@link ActivityController.ExceptionHandler} handles
   * it.
   * </p>
   *
   * @param fromGuiThread indicates whether the call is done from the GUI thread
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

}
