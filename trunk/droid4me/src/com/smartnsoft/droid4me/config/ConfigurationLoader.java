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

import android.content.res.AssetManager;

import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;

/**
 * An interface responsible for loading configuration parameters.
 * 
 * @author Édouard Mercier
 * @since 2012.04.09
 */
public interface ConfigurationLoader
{

  public final static Logger log = LoggerFactory.getInstance(ConfigurationLoader.class);

  /**
   * The exception used when something wrong happens with this configuration Java package component.
   */
  public static final class ConfigurationLoaderException
      extends RuntimeException
  {

    private static final long serialVersionUID = -1095017022728851956L;

    ConfigurationLoaderException(Throwable throwable)
    {
      super(throwable);
    }

    ConfigurationLoaderException(String detailMessage)
    {
      super(detailMessage);
    }

  }

  /**
   * A factory component, which enables to load configuration parameters.
   */
  public static class ConfigurationFactory
  {

    /**
     * Indicates how a configuration should be loaded.
     */
    public static enum ConfigurationType
    {
      /**
       * The configuration will be loaded from a file located in the {@code assets} application directory, embedded in the {@code .apk} installation
       * package file.
       * 
       * @see ConfigurationFactory#initialize(AssetManager)
       */
      Assets
    }

    private static AssetManager assetManager;

/**
     * This method should be invoked if the {@code ConfigurationFactory.ConfigurationType#Assets} type is used.
     * 
     * @param assetManager
     *          the Android asset manager, which will be used to load the configuration parameters from the {@code assets} Android installation package {@code .apk)
     * 
     * @see ConfigurationFactory.ConfigurationType#Assets 
     */
    public static void initialize(AssetManager assetManager)
    {
      ConfigurationFactory.assetManager = assetManager;
    }

    public static ConfigurationLoader getInstance(ConfigurationFactory.ConfigurationType configurationType, String value)
    {
      switch (configurationType)
      {
      case Assets:
        if (ConfigurationFactory.assetManager == null)
        {
          throw new ConfigurationLoader.ConfigurationLoaderException("The 'ConfigurationFactory.initialize()' has not been invoked!");
        }
        return new PropertiesConfigurationLoader(ConfigurationFactory.assetManager, value);
      default:
        throw new ConfigurationLoader.ConfigurationLoaderException("Does not support the '" + configurationType + "' configuration type");
      }
    }

    public static <T> T load(ConfigurationFactory.ConfigurationType configurationType, String value, Class<T> theClass)
    {
      final ConfigurationLoader configurationLoader = ConfigurationFactory.getInstance(configurationType, value);
      return configurationLoader.load(theClass);
    }

  }

  /**
   * Responsible for loading and returning a Plain Old Java Object (POJO) of the given class.
   * 
   * @param theClass
   *          the type of the POJO which should hold the configuration
   * @return a valid POJO which holds the loaded configuration parameters
   * @throws ConfigurationLoader.ConfigurationLoaderException
   *           if something unrecoverable went wrong during the loading
   */
  <T> T load(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException;

}
