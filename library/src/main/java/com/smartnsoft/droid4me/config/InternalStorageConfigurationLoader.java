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

package com.smartnsoft.droid4me.config;

import java.io.FileNotFoundException;
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

  private final ConfigurationParser<InputStream> configurationParser;

  public InternalStorageConfigurationLoader(Context context, String fileName,
      ConfigurationParser<InputStream> configurationParser)
  {
    this.context = context;
    this.fileName = fileName;
    this.configurationParser = configurationParser;
  }

  public final <T> T getBean(Class<T> theClass)
      throws ConfigurationLoader.ConfigurationLoaderException
  {
    final InputStream inputStream;
    try
    {
      inputStream = getInputStream();
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
    final InputStream inputStream;
    try
    {
      inputStream = getInputStream();
    }
    catch (FileNotFoundException exception)
    {
      // Does not matter
      return bean;
    }
    return configurationParser.setBean(theClass, inputStream, bean);
  }

  private InputStream getInputStream()
      throws FileNotFoundException
  {
    return context.openFileInput(fileName);
  }

}
