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

  private final Map<String, Values.Info<BusinessObjectType>> cache = new HashMap<>();

  public MemoryCacher(
      Business.UriStreamParser<BusinessObjectType, String, ParameterType, ParseExceptionType> uriStreamParser,
      Business.IOStreamer<String, StreamerExceptionType> ioStreamer,
      Business.UriInputStreamer<String, InputExceptionType> uriInputStreamer)
  {
    super(uriStreamParser, ioStreamer, uriInputStreamer);
  }

  @Override
  public Values.Info<BusinessObjectType> getCachedValue(ParameterType parameter)
  {
    return cache.get(computeUri(parameter));
  }

  @Override
  public synchronized void setValue(ParameterType parameter, Values.Info<BusinessObjectType> info)
  {
    cache.put(computeUri(parameter), info);
  }

  @Override
  protected synchronized Date getCacheLastUpdate(ParameterType parameter, String uri)
  {
    final Values.Info<BusinessObjectType> cachedValue = cache.get(uri);
    return cachedValue != null ? cachedValue.timestamp : null;
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

}
