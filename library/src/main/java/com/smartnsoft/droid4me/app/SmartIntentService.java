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

import android.app.IntentService;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * A {@link IntentService} with the logging feature.
 *
 * @author Édouard Mercier
 * @since 2011.01.30
 */
public abstract class SmartIntentService
    extends IntentService
{

  protected static final Logger log = LoggerFactory.getInstance(SmartIntentService.class);

  private SharedPreferences preferences;

  public SmartIntentService()
  {
    this(null);
  }

  public SmartIntentService(String name)
  {
    super(name);
  }

  /**
   * Should always be invoked first by the derived classes.
   */
  @Override
  public void onCreate()
  {
    super.onCreate();
    preferences = PreferenceManager.getDefaultSharedPreferences(this);
  }

  /**
   * @return the preferences of the application
   */
  protected final SharedPreferences getPreferences()
  {
    return preferences;
  }

}
