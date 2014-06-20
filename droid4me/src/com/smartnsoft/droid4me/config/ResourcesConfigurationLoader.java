/*
 * (C) Copyright 2009-2014 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import android.content.Context;

/**
 * A basis class which reads the configuration from a the {@code res} sub-directories.
 * 
 * @author Jocelyn Girard, Édouard Mercier
 * @since 2014.06.20
 */
public class ResourcesConfigurationLoader
    implements ConfigurationLoader
{

  private final Context context;

  private final ConfigurationParser<Context> configurationParser;

  public ResourcesConfigurationLoader(Context context, ConfigurationParser<Context> configurationParser)
  {
    this.context = context;
    this.configurationParser = configurationParser;
  }

  public final <T> T getBean(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    return configurationParser.getBean(theClass, context);
  }

  @Override
  public <T> T setBean(Class<T> theClass, T bean)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    return configurationParser.setBean(theClass, context, bean);
  }

}
