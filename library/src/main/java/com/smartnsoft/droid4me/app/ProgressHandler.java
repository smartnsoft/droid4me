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
   * Should be invoked when a progress event occurs.
   * <p>
   * <p>
   * If the provided activity is {@link Activity#isFinishing() finishing}, it does nothing.
   * </p>
   * <p>
   * <p>
   * Caution: this method must be exclusively invoked from the GUI thread!
   * <p>
   *
   * @param activity      the activity from which the progress event is triggered
   * @param inProgress    whether the progression is on or off
   * @param progressExtra a free object that will be passed along, and which can deliver additional information about the progress. Typically, those extra data
   *                      can be the progression level, a title, a text...
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
   * The same method as {@link #onProgress(Activity, boolean, Object)} except that it can be invoked from a non-GUI thread. In that case, the
   * <code>fromGuiThread</code> parameter must be set to {@code false}.
   */
  public final void onProgress(final Activity activity, final boolean isLoading, final Object progressExtra,
      boolean fromGuiThread)
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

  /**
   * Should be implemented so as to indicate to the end-user that some operation is under progress.
   *
   * @param activity      the activity that originated a progress event
   * @param progressExtra the free object that has been passed when declaring the progress event
   * @see #onProgress(Activity, boolean, Object)
   */
  protected abstract void show(Activity activity, Object progressExtra);

  /**
   * Should be implemented so as to indicate to the end-user that no more operation is under progress.
   *
   * @param activity      the activity that originated a progress event
   * @param progressExtra the free object that has been passed when declaring the progress event
   * @see #onProgress(Activity, boolean, Object)
   */
  protected abstract void dismiss(Activity activity, Object progressExtra);

}
