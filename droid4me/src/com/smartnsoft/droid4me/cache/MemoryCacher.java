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

package com.smartnsoft.droid4me.cache;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.smartnsoft.droid4me.bo.Business;


/**
 * A class which enables to cache the result of web service calls in RAM only.
 *
 * @author Édouard Mercier
 * @since 2009.07.20
 */
public class MemoryCacher<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Exception, InputExceptionType extends Exception>
    extends Cacher<BusinessObjectType, String, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
{

  private final Map<String, Values.Info<BusinessObjectType>> cache = new HashMap<String, Values.Info<BusinessObjectType>>();

  public MemoryCacher(
      Business.UriStreamParser<BusinessObjectType, String, ParameterType, ParseExceptionType> uriStreamParser,
      Business.IOStreamer<String, StreamerExceptionType> ioStreamer,
      Business.UriInputStreamer<String, InputExceptionType> uriInputStreamer)
  {
    super(uriStreamParser, ioStreamer, uriInputStreamer);
  }

  @Override
  protected synchronized Date getCacheLastUpdate(ParameterType parameter, String uri)
  {
    final Values.Info<BusinessObjectType> cachedValue = cache.get(uri);
    return cachedValue != null ? cachedValue.timestamp : null;
  }

  @Override
  public Values.Info<BusinessObjectType> getCachedValue(ParameterType parameter)
  {
    return cache.get(computeUri(parameter));
  }

  @Override
  protected synchronized void onNewBusinessObject(String uri, Values.Info<BusinessObjectType> info)
  {
    cache.put(uri, info);
  }

  @Override
  protected InputStream onNewInputStream(ParameterType parameter, String uri, Business.InputAtom atom,
      boolean returnStream)
  {
    return returnStream == false ? null : atom.inputStream;
  }

  @Override
  public synchronized void setValue(ParameterType parameter, Values.Info<BusinessObjectType> info)
  {
    cache.put(computeUri(parameter), info);
  }

}
