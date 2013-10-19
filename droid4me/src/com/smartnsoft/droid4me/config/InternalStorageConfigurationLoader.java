/*
 * (C) Copyright 2009-2013 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

package com.smartnsoft.droid4me.config;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;

/**
 * A basis class which reads the configuration from a file located in the Android application internal storage.
 * 
 * @author Édouard Mercier
 * @since 2013.10.19
 */
public class InternalStorageConfigurationLoader
    implements ConfigurationLoader
{

  private final Context context;

  private final String fileName;

  private final ConfigurationParser configurationParser;

  public InternalStorageConfigurationLoader(Context context, String fileName, ConfigurationParser configurationParser)
  {
    this.context = context;
    this.fileName = fileName;
    this.configurationParser = configurationParser;
  }

  public final <T> T load(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    final InputStream inputStream;
    try
    {
      inputStream = context.openFileInput(fileName);
    }
    catch (IOException exception)
    {
      throw new ConfigurationLoader.ConfigurationLoaderException(exception);
    }
    return load(theClass, inputStream);
  }

  protected <T> T load(Class<T> theClass, InputStream inputStream)
  {
    return configurationParser.load(theClass, inputStream);
  }

}
