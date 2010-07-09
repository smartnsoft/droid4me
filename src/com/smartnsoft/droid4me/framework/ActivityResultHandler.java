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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

import android.app.Activity;
import android.content.Intent;

/**
 * Enables to handle the launching of an activity and have the handling of the result in one place.
 * 
 * @author Édouard Mercier
 * @since 2007.05.07
 */
public abstract class ActivityResultHandler
{

  protected static final Logger log = LoggerFactory.getInstance(ActivityResultHandler.class);

  /**
   * This interface will be eventually invoked when the activity has returned a result.
   */
  public static interface Event
  {
    void done(int requestCode, int resultCode, Intent data);
  }

  public static class Handler
  {

    private final Map<Integer, ActivityResultHandler.Event> activities = new Hashtable<Integer, ActivityResultHandler.Event>();

    private final Map<Integer, Intent> intents = new Hashtable<Integer, Intent>();

    public final void register(Intent intent, int requestCode, ActivityResultHandler.Event resultEvent)
    {
      activities.put(requestCode, resultEvent);
      intents.put(requestCode, intent);
    }

    public Intent getIntent(int requestCode)
    {
      return intents.get(requestCode);
    }

    public final void execute(Activity activity, Intent intent, int requestCode, ActivityResultHandler.Event resultEvent)
    {
      if (activities.containsKey(requestCode))
      {
        throw new IllegalArgumentException("Attempting to execute twice a result event handler with the request code '" + requestCode + "'");
      }
      activities.put(requestCode, resultEvent);
      if (log.isDebugEnabled())
      {
        log.debug("Starting an activity for a result with a request code set to '" + requestCode + "'");
      }
      activity.startActivityForResult(intent, requestCode);
    }

    public final void execute(Activity activity, int requestCode)
    {
      if (log.isDebugEnabled())
      {
        log.debug("Starting a registered activity for a result with a request code set to '" + requestCode + "'");
      }
      final Intent intent = intents.get(requestCode);
      activity.startActivityForResult(intent, requestCode);
    }

    public boolean handle(int requestCode, int resultCode, Intent data)
    {
      return handleInternal(requestCode, resultCode, data);
    }

    protected final boolean handleInternal(int requestCode, int resultCode, Intent data)
    {
      if (log.isDebugEnabled())
      {
        log.debug("Attempting to handle the result of an activity corresponding to a request code set to '" + requestCode + "' and a result code set to '" + resultCode + "'");
      }
      if (canHandle(requestCode))
      {
        ActivityResultHandler.Event result;
        if (intents.containsKey(requestCode) == false)
        {
          // We do not forget to remove the related entry, because it may be executed back in the future
          result = activities.remove(requestCode);
        }
        else
        {
          result = activities.get(requestCode);
        }
        if (log.isDebugEnabled())
        {
          log.debug("Found the event handlerr corresponding to an activity result with request code set to '" + requestCode + "' and a result code set to '" + resultCode + "'");
        }
        result.done(requestCode, resultCode, data);
        return true;
      }
      return false;
    }

    public boolean canHandle(int requestCode)
    {
      return activities.containsKey(requestCode);
    }

  }

  // TODO: provide a mechanism that ensures that only one request code is generated
  public static class CompositeHandler
      extends ActivityResultHandler.Handler
  {

    private List<Handler> resultHandlers = new ArrayList<Handler>();

    public CompositeHandler()
    {
      add(this);
    }

    public void add(Handler activityResultHandler)
    {
      resultHandlers.add(activityResultHandler);
    }

    public boolean handle(int requestCode, int resultCode, Intent data)
    {
      for (Handler resultHandler : resultHandlers)
      {
        // TODO: understand why the resultHandler object can be null
        if (resultHandler != null && resultHandler.canHandle(requestCode) == true)
        {
          return resultHandler.handleInternal(requestCode, resultCode, data);
        }
      }
      return false;
    }

  }

}
