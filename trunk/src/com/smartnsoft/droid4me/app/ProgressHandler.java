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
 *     Smart&Soft - initial API and implementation
 */

package com.smartnsoft.droid4me.app;

import android.app.Activity;

/**
 * Provides a way to handle progress in an application, while it is computed something and the end-user is prompted to wait until a task completion.
 * 
 * @author Édouard Mercier
 * @since 2010.07.10
 */
public abstract class ProgressHandler
{

  /**
   * A structure which encapsulates extra completion and textual message information related to a progression event.
   */
  public static class ProgressExtra
  {

    public final int completionLevel;

    public final String message;

    public ProgressExtra(int completionLevel, String message)
    {
      this.completionLevel = completionLevel;
      this.message = message;
    }

  }

  /**
   * Should be implemented so as to indicate to the end-user that some operation is under progress.
   * 
   * @param activity
   *          the activity that originated a progress event
   * @param progressExtra
   *          the free object that has been passed when declaring the progress event
   * @see #onProgress(Activity, boolean, Object)
   */
  protected abstract void show(Activity activity, Object progressExtra);

  /**
   * Should be implemented so as to indicate to the end-user that no more operation is under progress.
   * 
   * @param activity
   *          the activity that originated a progress event
   * @param progressExtra
   *          the free object that has been passed when declaring the progress event
   * @see #onProgress(Activity, boolean, Object)
   */
  protected abstract void dismiss(Activity activity, Object progressExtra);

  /**
   * Should be invoked when a progress event occurs.
   * 
   * <p>
   * Caution: this method must be exclusively invoked from the GUI thread!
   * <p>
   * 
   * @param activity
   *          the activity from which the progress event is triggered
   * @param inProgress
   *          whether the progression is on or off
   * @param progressExtra
   *          a free object that will be passed along, and which can deliver additional information about the progress. Typically, those extra data
   *          can be the progression level, a title, a text...
   */
  public final void onProgress(Activity activity, boolean inProgress, Object progressExtra)
  {
    if (activity.isFinishing() == true)
    {
      // We do nothing when the activity is finishing
      return;
    }
    if (inProgress == true)
    {
      show(activity, progressExtra);
    }
    else
    {
      dismiss(activity, progressExtra);
    }
  }

  /**
   * The same method as {@link #onProgress(Activity, boolean) except that it can be invoked from a non-GUI thread. In that case, the
   * <code>fromGuiThread</code> parameter must be set to <code>false</code>.
   */
  public final void onProgress(final Activity activity, final boolean isLoading, final Object progressExtra, boolean fromGuiThread)
  {
    if (fromGuiThread == true)
    {
      onProgress(activity, isLoading, progressExtra);
    }
    else
    {
      activity.runOnUiThread(new Runnable()
      {
        public void run()
        {
          onProgress(activity, isLoading, progressExtra);
        }
      });
    }
  }

}
