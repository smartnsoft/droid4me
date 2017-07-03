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

package com.smartnsoft.droid4me.wscache;

import com.smartnsoft.droid4me.bo.Business.IOStreamer;
import com.smartnsoft.droid4me.cache.Cacher;
import com.smartnsoft.droid4me.cache.Values;
import com.smartnsoft.droid4me.ws.WSUriStreamParser;
import com.smartnsoft.droid4me.ws.WebServiceClient;
import com.smartnsoft.droid4me.ws.WebServiceClient.CallException;
import com.smartnsoft.droid4me.ws.WithCacheWSUriStreamParser;

/**
 * @author Édouard Mercier
 * @since 2009.11.19
 */
public final class BackedWSUriStreamParser
{

  public static abstract class BackedUriStreamedValue<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WithCacheWSUriStreamParser.CacheableWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType, StreamerExceptionType>
  {

    public final Values.BackedCachedValue<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException> backed;

    public BackedUriStreamedValue(IOStreamer<String, StreamerExceptionType> ioStreamer,
        WebServiceClient webServiceClient)
    {
      super(ioStreamer, webServiceClient);
      final Cacher<BusinessObjectType, com.smartnsoft.droid4me.ws.WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, CallException> cacher = new Cacher<>(this);
      backed = new Values.BackedCachedValue<>(cacher);
    }

  }

  public static abstract class BackedUriStreamedMap<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WithCacheWSUriStreamParser.CachedWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType, StreamerExceptionType>
  {

    public final Values.BackedCachedMap<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException> backed;

    public BackedUriStreamedMap(IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceClient webServiceClient)
    {
      super(ioStreamer, webServiceClient);
      final Cacher<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException> cacher = new Cacher<>(this);
      backed = new Values.BackedCachedMap<>(cacher);
    }

  }

}
