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

import android.content.res.AssetManager;

/**
 * A basis class which reads the configuration from a the {@code assets} directory.
 * 
 * @author Édouard Mercier
 * @since 2013.10.19
 * @see AssetManager
 */
public class AssetsConfigurationLoader
    implements ConfigurationLoader
{

  private final AssetManager assetManager;

  private final String assetsFileName;

  private final ConfigurationParser configurationParser;

  public AssetsConfigurationLoader(AssetManager assetManager, String assetsFileName, ConfigurationParser configurationParser)
  {
    this.assetManager = assetManager;
    this.assetsFileName = assetsFileName;
    this.configurationParser = configurationParser;
  }

  public final <T> T getBean(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    final InputStream inputStream;
    try
    {
      inputStream = assetManager.open(assetsFileName);
    }
    catch (IOException exception)
    {
      throw new ConfigurationLoader.ConfigurationLoaderException(exception);
    }
    return configurationParser.getBean(theClass, inputStream);
  }

  @Override
  public <T> T setBean(Class<T> theClass, T bean)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    final InputStream inputStream = getInputStream();
    return configurationParser.setBean(theClass, inputStream, bean);
  }

  private InputStream getInputStream()
  {
    try
    {
      return assetManager.open(assetsFileName);
    }
    catch (IOException exception)
    {
      throw new ConfigurationLoader.ConfigurationLoaderException(exception);
    }
  }

}
