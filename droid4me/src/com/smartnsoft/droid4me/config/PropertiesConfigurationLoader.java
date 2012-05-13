/*
 * (C) Copyright 2009-2012 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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
import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.Properties;

import android.content.res.AssetManager;

/**
 * Reads the configuration from a Java {@code .properties} file located in the Android installation file {@code assets} directory.
 * 
 * @author Édouard Mercier
 * @since 2012.04.09
 * @see Properties
 */
public class PropertiesConfigurationLoader
    implements ConfigurationLoader
{

  private final AssetManager assetManager;

  private final String assetsFileName;

  public PropertiesConfigurationLoader(AssetManager assetManager, String assetsFileName)
  {
    this.assetManager = assetManager;
    this.assetsFileName = assetsFileName;
  }

  public <T> T load(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    final Properties properties = new Properties();
    final InputStream inputStream;
    try
    {
      inputStream = assetManager.open(assetsFileName);
    }
    catch (IOException exception)
    {
      throw new ConfigurationLoader.ConfigurationLoaderException(exception);
    }
    try
    {
      properties.load(inputStream);
    }
    catch (IOException exception)
    {
      throw new ConfigurationLoader.ConfigurationLoaderException(exception);
    }
    final T bean;
    try
    {
      bean = theClass.newInstance();
    }
    catch (Exception exception)
    {
      throw new ConfigurationLoader.ConfigurationLoaderException(exception);
    }
    for (Entry<Object, Object> entry : properties.entrySet())
    {
      final String propertyName = (String) entry.getKey();
      final Field field;
      try
      {
        field = theClass.getDeclaredField(propertyName);
      }
      catch (Exception exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot find the '" + propertyName + "' field on the '" + theClass.getName() + "' class: ignoring this parameter", exception);
        }
        continue;
      }
      final String rawPropertyValue = (String) entry.getValue();
      final Object propertyValue;
      if (field.getType() == int.class)
      {
        propertyValue = Integer.parseInt(rawPropertyValue);
      }
      else if (field.getType() == long.class)
      {
        propertyValue = Long.parseLong(rawPropertyValue);
      }
      else if (field.getType() == boolean.class)
      {
        propertyValue = Boolean.parseBoolean(rawPropertyValue);
      }
      else if (field.getType() == float.class)
      {
        propertyValue = Float.parseFloat(rawPropertyValue);
      }
      else if (field.getType() == double.class)
      {
        propertyValue = Double.parseDouble(rawPropertyValue);
      }
      else
      {
        propertyValue = rawPropertyValue;
      }
      try
      {
        field.set(bean, propertyValue);
      }
      catch (Exception exception)
      {
        if (log.isErrorEnabled())
        {
          log.error(
              "Cannot set the '" + propertyName + "' field on the '" + theClass.getName() + "' class with value '" + rawPropertyValue + "': ignoring this parameter",
              exception);
        }
      }
    }
    return bean;
  }

}
