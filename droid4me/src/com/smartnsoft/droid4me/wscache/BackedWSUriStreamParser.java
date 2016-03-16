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

package com.smartnsoft.droid4me.wscache;

import com.smartnsoft.droid4me.bo.Business.Cacheable;
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
      final Cacher<BusinessObjectType, com.smartnsoft.droid4me.ws.WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, CallException> cacher = new Cacher<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>(this);
      backed = new Values.BackedCachedValue<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>(cacher);
    }

  }

  public static abstract class BackedUriStreamedMap<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WithCacheWSUriStreamParser.CachedWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType, StreamerExceptionType>

  {

    public final Values.BackedCachedMap<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException> backed;

    public BackedUriStreamedMap(IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceClient webServiceClient)
    {
      super(ioStreamer, webServiceClient);
      final Cacher<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException> cacher = new Cacher<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>((Cacheable<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>) this);
      backed = new Values.BackedCachedMap<BusinessObjectType, WSUriStreamParser.KeysAggregator<ParameterType>, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceClient.CallException>(cacher);
    }

  }

}
