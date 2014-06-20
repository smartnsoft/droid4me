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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

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
     * Indicates from where a configuration should be loaded.
     */
    public static enum ConfigurationLocation
    {
      /**
       * The configuration will be loaded from a file located in the {@code assets} application directory, embedded in the {@code .apk} installation
       * package file.
       */
      Assets,
      /**
       * The configuration will be loaded from a file located in the application internal storage (hence, outside of the {@code .apk} installation
       * package file).
       */
      InternalStorage,
      /**
       * The configuration will be loaded from the resources located in the {@code res} application sub-directories, embedded in the {@code .apk}
       * installation package file.
       */
      Resources
    }

    /**
     * Indicates in what format a configuration is expressed.
     */
    public static enum ConfigurationFormat
    {
      /**
       * The configuration is expressed in the {@link Properties} Java format.
       */
      Properties,
      /**
       * The configuration is expressed in the JSON format.
       */
      Json,
      /**
       * The configuration is expressed through the Android "resource" system (@link Resource}.
       */
      Resources
    }

    private static Context applicationContext;

    /**
     * This method should be invoked before the {@link ConfigurationFactory#setBean(ConfigurationType, String, Class)} method be invoked, hence very
     * early at the application start-up (typically during the {@link Application#onCreate() method}.
     * 
     * @param applicationContext
     *          the Android application context, usually retrieved from a {@link Activity#getApplicationContext()} or from
     *          {@link Application#getApplicationContext()}, which will be used to load the configuration parameters from the {@code assets} Android
     *          installation package {@code .apk}, or from the internal storage
     * 
     * @see #setBean(ConfigurationType, String, Class)
     * @see #initialize(Context)
     */
    public static void initialize(Context applicationContext)
    {
      ConfigurationFactory.applicationContext = applicationContext;
    }

    /**
     * @return the application context which has been provided to the {@link #initialize(Context)} method (returns {@code} if this method has not been
     *         invoked)
     * @see #initialize(Context)
     */
    public static Context getApplicationContext()
    {
      return ConfigurationFactory.applicationContext;
    }

    @SuppressWarnings("unchecked")
    public static ConfigurationLoader getInstance(ConfigurationFactory.ConfigurationLocation configurationLocation,
        ConfigurationFactory.ConfigurationFormat configurationFormat, String value)
    {
      if (ConfigurationFactory.applicationContext == null)
      {
        throw new ConfigurationLoader.ConfigurationLoaderException("The 'ConfigurationFactory.initialize()' has not been invoked!");
      }
      final ConfigurationParser<?> configurationParser;
      switch (configurationFormat)
      {
      case Properties:
        configurationParser = new PropertiesParser();
        break;
      case Json:
        configurationParser = new JsonParser();
        break;
      case Resources:
        configurationParser = new ResourcesParser();
        break;
      default:
        configurationParser = null;
        break;
      }
      if (configurationParser != null)
      {
        switch (configurationLocation)
        {
        case Assets:
          return new AssetsConfigurationLoader(ConfigurationFactory.applicationContext.getAssets(), value, (ConfigurationParser<InputStream>) configurationParser);
        case InternalStorage:
          return new InternalStorageConfigurationLoader(ConfigurationFactory.applicationContext, value, (ConfigurationParser<InputStream>) configurationParser);
        case Resources:
          return new ResourcesConfigurationLoader(ConfigurationFactory.applicationContext, (ConfigurationParser<Context>) configurationParser);
        }
      }
      throw new ConfigurationLoader.ConfigurationLoaderException("Does not support the '" + configurationLocation + "' configuration location mixed with the '" + configurationFormat + "' format!");
    }

    /**
     * Loads a Java bean.
     * 
     * <p>
     * The method implementation is responsible for creating the right {@link ConfigurationLoader}, and then invoke its
     * {@link ConfigurationLoader#load(Class)} method.
     * </p>
     * 
     * @param configurationType
     *          the kind of configuration which indicates where and how the bean will be fulfilled
     * @param value
     *          will be passed to the internally created {@link ConfigurationLoader}
     * @param theClass
     *          the fully-qualified-name of the class of the bean which should be created. This class should expose a {@code public} no-argument
     *          constructor
     * @return a valid bean (cannot be {@code null})
     * @throws ConfigurationLoader.ConfigurationLoaderException
     *           if something went wrong during the method execution
     * @see #initialize(Context)
     * @see #getInstance(ConfigurationType, String)
     */
    public static <T> T load(ConfigurationFactory.ConfigurationLocation configurationLocation, ConfigurationFactory.ConfigurationFormat configurationFormat,
        String value, Class<T> theClass)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final ConfigurationLoader configurationLoader = ConfigurationFactory.getInstance(configurationLocation, configurationFormat, value);
      return configurationLoader.getBean(theClass);
    }

  }

  /**
   * A basis class which is able to create and fulfill a bean from a {@link Properties} object, and which will be used by the
   * {@link ConfigurationLoader} derived classes, in order to parse the bean.
   * 
   * @since 2013.10.19
   */
  public static abstract class ConfigurationParser<InputClass>
  {

    /**
     * Is responsible for creating a bean, and then fulfilling its fields.
     * 
     * <p>
     * The method is supposed to skip fields available in the {@code input}, but which cannot be mapped properly to any of the created bean fields.
     * </p>
     * 
     * @param theClass
     *          the class the bean to be created belongs to
     * @param input
     *          the source which holds the bean state, and which will be parsed
     * @return a valid and fulfilled bean
     * @throws ConfigurationLoader.ConfigurationLoaderException
     *           if an error occurred during the method
     * @see #setBean(Class, InputStream, Object)
     */
    public abstract <T> T getBean(Class<T> theClass, InputClass input)
        throws ConfigurationLoader.ConfigurationLoaderException;

    /**
     * Does the same job as the {@link #getBean(Class, InputStream)} method, except that the bean is provided. the class the bean to be created
     * belongs to
     * 
     * @param input
     *          the source which holds the bean state, and which will be parsed
     * @param bean
     *          the bean that should be updated
     * @return the provided fulfilled bean
     * @throws ConfigurationLoader.ConfigurationLoaderException
     *           if an error occurred during the method
     * @see #getBean(Class, InputStream)
     */
    public abstract <T> T setBean(Class<T> theClass, InputClass input, T bean)
        throws ConfigurationLoader.ConfigurationLoaderException;

    /**
     * Creates a bean via introspection
     * 
     * @param theClass
     *          the class the bean to be created belongs to
     * @return a valid bean, but whose fields have not been fulfilled yet
     * @throws ConfigurationLoader.ConfigurationLoaderException
     *           if the bean could not be instantiated
     */
    protected final <T> T createBean(Class<T> theClass)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final T bean;
      try
      {
        bean = theClass.newInstance();
      }
      catch (Exception exception)
      {
        throw new ConfigurationLoader.ConfigurationLoaderException(exception);
      }
      return bean;
    }

    /**
     * Sets a value to one of the bean field.
     * 
     * <p>
     * If the mapping was successful, the bean field will have been updated. If an error occurs during this treatment, an error log is issued, and the
     * provided value is simply ignored.
     * </p>
     * 
     * @param theClass
     *          the class the bean to be created belongs to
     * @param bean
     *          the bean the update will apply on
     * @param fieldName
     *          the name of the bean class field to be updated
     * @param rawFieldValue
     *          the string representation of the new field value. The only supported types are: {@link java.lang.int}, {@link java.lang.boolean},
     *          {@link java.lang.float}, {@link java.lang.double} and {@link java.lang.String}
     */
    protected final void setField(Class<?> theClass, Object bean, String fieldName, String rawFieldValue)
    {
      final Field field;
      try
      {
        field = theClass.getDeclaredField(fieldName);
      }
      catch (Exception exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Cannot find the '" + fieldName + "' field on the '" + theClass.getName() + "' class: ignoring this entry!", exception);
        }
        return;
      }
      final Object propertyValue;
      try
      {
        if (field.getType() == int.class)
        {
          propertyValue = Integer.parseInt(rawFieldValue);
        }
        else if (field.getType() == long.class)
        {
          propertyValue = Long.parseLong(rawFieldValue);
        }
        else if (field.getType() == boolean.class)
        {
          propertyValue = Boolean.parseBoolean(rawFieldValue);
        }
        else if (field.getType() == float.class)
        {
          propertyValue = Float.parseFloat(rawFieldValue);
        }
        else if (field.getType() == double.class)
        {
          propertyValue = Double.parseDouble(rawFieldValue);
        }
        else if (field.getType() == String.class)
        {
          propertyValue = rawFieldValue;
        }
        else
        {
          if (log.isErrorEnabled())
          {
            log.error("Cannot set the '" + fieldName + "' field on the '" + theClass.getName() + "' class with value '" + rawFieldValue + "', because its type cannot be handled: ignoring this entry");
          }
          return;
        }
      }
      catch (Exception exception)
      {
        // It is very likely a "NumberFormatException" has occurred
        if (log.isErrorEnabled())
        {
          log.error(
              "Could set the '" + fieldName + "' field on the '" + theClass.getName() + "' class with value '" + rawFieldValue + "', because it could not be parsed to its expected type properly: ignoring this entry",
              exception);
        }
        return;
      }
      try
      {
        // We make sure that the field may be accessed (in the case it is not "public", for instance)
        field.setAccessible(true);
        field.set(bean, propertyValue);
      }
      catch (Exception exception)
      {
        if (log.isErrorEnabled())
        {
          log.error(
              "Cannot set the '" + fieldName + "' field on the '" + theClass.getName() + "' class with value '" + rawFieldValue + "': ignoring this entry",
              exception);
        }
      }
    }

  }

  /**
   * A helper class which is able to create and fulfill a bean from a {@link Properties} object.
   * 
   * @since 2013.10.19
   */
  public final static class PropertiesParser
      extends ConfigurationParser<InputStream>
  {

    @Override
    public <T> T getBean(Class<T> theClass, InputStream input)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final T bean = createBean(theClass);
      return setBean(theClass, input, bean);
    }

    @Override
    public <T> T setBean(Class<T> theClass, InputStream input, T bean)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final Properties properties = new Properties();
      try
      {
        properties.load(input);
      }
      catch (IOException exception)
      {
        throw new ConfigurationLoader.ConfigurationLoaderException(exception);
      }
      for (Entry<Object, Object> entry : properties.entrySet())
      {
        final String propertyName = (String) entry.getKey();
        final String rawPropertyValue = (String) entry.getValue();
        setField(theClass, bean, propertyName, rawPropertyValue);
      }
      return bean;
    }

  }

  /**
   * A helper class which is able to create and fulfill a bean from a JSON string.
   * 
   * @since 2013.10.19
   */
  public final static class JsonParser
      extends ConfigurationParser<InputStream>
  {

    @Override
    public <T> T getBean(Class<T> theClass, InputStream input)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final T bean = createBean(theClass);
      return setBean(theClass, input, bean);
    }

    @Override
    public <T> T setBean(Class<T> theClass, InputStream input, T bean)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final Scanner scanner = new Scanner(input).useDelimiter("\\A");
      final String jsonString = scanner.hasNext() ? scanner.next() : "";
      final JSONObject jsonObject;
      try
      {
        jsonObject = new JSONObject(jsonString);
      }
      catch (JSONException exception)
      {
        throw new ConfigurationLoader.ConfigurationLoaderException(exception);
      }
      @SuppressWarnings("unchecked")
      final Iterator<String> iterator = jsonObject.keys();
      while (iterator.hasNext())
      {
        final String fieldName = iterator.next();
        try
        {
          final String rawPropertyValue = jsonObject.getString(fieldName);
          setField(theClass, bean, fieldName, rawPropertyValue);
        }
        catch (JSONException exception)
        {
          // Cannot happen, hence we silently ignore this issue
        }
      }
      return bean;
    }

  }

  /**
   * A helper class which is able to create and fulfill a bean from the Android {@link Resources resources}.
   * 
   * @since 2014.06.20
   */
  public final static class ResourcesParser
      extends ConfigurationParser<Context>
  {

    @Override
    public <T> T getBean(Class<T> theClass, Context input)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final T bean = createBean(theClass);
      return setBean(theClass, input, bean);
    }

    @Override
    public <T> T setBean(Class<T> theClass, Context input, T bean)
        throws ConfigurationLoader.ConfigurationLoaderException
    {
      final Resources resources = input.getResources();
      final Field[] fields = bean.getClass().getDeclaredFields();
      for (Field field : fields)
      {
        final Class<?> fieldType = field.getType();
        final String fieldName = field.getName();
        if (fieldType == String.class || fieldType == int.class || fieldType == long.class || fieldType == boolean.class || fieldType == float.class || fieldType == double.class)
        {
          final String resourceType;
          if (fieldType == int.class || fieldType == long.class)
          {
            resourceType = "integer";
          }
          else if (fieldType == boolean.class)
          {
            resourceType = "bool";
          }
          else if (fieldType == float.class || fieldType == double.class)
          {
            resourceType = "float";
          }
          else
          {
            resourceType = "string";
          }
          final int identifier = resources.getIdentifier(fieldName, resourceType, input.getPackageName());
          if (identifier != 0)
          {
            final String rawPropertyValue;
            if (fieldType == int.class || fieldType == long.class)
            {
              rawPropertyValue = Long.toString(resources.getInteger(identifier));
            }
            else if (fieldType == boolean.class)
            {
              rawPropertyValue = Boolean.toString(resources.getBoolean(identifier));
            }
            else if (fieldType == float.class || fieldType == double.class)
            {
              final TypedValue typedValue = new TypedValue();
              resources.getValue(identifier, typedValue, true);
              rawPropertyValue = Double.toString(typedValue.getFloat());
            }
            else
            {
              rawPropertyValue = input.getString(identifier);
            }
            setField(theClass, bean, fieldName, rawPropertyValue);
          }
          else
          {
            if (log.isErrorEnabled())
            {
              log.error("Could set the '" + fieldName + "' field on the '" + theClass.getName() + "' class, because it is not an available field within the bean");
            }
          }
        }
        else
        {
          if (log.isErrorEnabled())
          {
            log.error("Could set the '" + fieldName + "' field on the '" + theClass.getName() + "' class, because its type '" + fieldType.getSimpleName() + "' is not supported");
          }
        }
      }
      return bean;
    }

  }

  /**
   * Does the same thing as the {@link #getBean(Class, InputStream)} method, except that the POJO bean is provided.
   * 
   * <p>
   * This method is especially useful, when a pre-defined configuration should be overwritten.
   * </p>
   * 
   * @param theClass
   *          the type of the POJO which should hold the configuration
   * @param bean
   *          an already instantiated bean, which will be updated
   * @return a valid POJO which holds the loaded configuration parameters
   * @throws ConfigurationLoader.ConfigurationLoaderException
   *           if something unrecoverable went wrong during the processing
   * @see #getBean(Class, InputStream)
   */
  <T> T setBean(Class<T> theClass, T bean)
      throws ConfigurationLoader.ConfigurationLoaderException;

  /**
   * Responsible for loading and returning a Plain Old Java Object (POJO) of the given class.
   * 
   * @param theClass
   *          the type of the POJO which should hold the configuration
   * @return a valid POJO which holds the loaded configuration parameters
   * @throws ConfigurationLoader.ConfigurationLoaderException
   *           if something unrecoverable went wrong during the processing
   * @see #setBean(Class, Object)
   */
  <T> T getBean(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException;

}
