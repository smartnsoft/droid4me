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

import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;

/**
 * A basis class which reads the configuration from a the {@code assets} directory.
 *
 * @author Édouard Mercier
 * @see AssetManager
 * @since 2013.10.19
 */
public class AssetsConfigurationLoader
    implements ConfigurationLoader
{

  private final AssetManager assetManager;

  private final String assetsFileName;

  private final ConfigurationParser<InputStream> configurationParser;

  public AssetsConfigurationLoader(AssetManager assetManager, String assetsFileName,
      ConfigurationParser<InputStream> configurationParser)
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
