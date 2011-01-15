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
import com.smartnsoft.droid4me.ws.WebServiceCaller;
import com.smartnsoft.droid4me.ws.WithCacheWSUriStreamParser;
import com.smartnsoft.droid4me.ws.WSUriStreamParser.UrlWithCallTypeAndBody;
import com.smartnsoft.droid4me.ws.WebServiceCaller.CallException;

/**
 * @author Édouard Mercier
 * @since 2009.11.19
 */
public final class BackedWSUriStreamParser
{

  // private final static Logger log = LoggerFactory.getInstance(BackedWSUriStreamParser.class);

  public abstract static class BackedUriStreamedValue<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WithCacheWSUriStreamParser.CacheableWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType, StreamerExceptionType>
  {

    public BackedUriStreamedValue(IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceCaller webServiceCaller)
    {
      super(ioStreamer, webServiceCaller);
    }

    private final Cacher<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException> cacher = new Cacher<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException>(this);

    public final Values.BackedCachedValue<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException> backed = new Values.BackedCachedValue<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException>(cacher);

  }

  public abstract static class BackedUriStreamedMap<BusinessObjectType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable>
      extends WithCacheWSUriStreamParser.CachedWebUriStreamParser<BusinessObjectType, ParameterType, ParseExceptionType, StreamerExceptionType>

  {

    public BackedUriStreamedMap(IOStreamer<String, StreamerExceptionType> ioStreamer, WebServiceCaller webServiceCaller)
    {
      super(ioStreamer, webServiceCaller);
    }

    private final Cacher<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException> cacher = new Cacher<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException>((Cacheable<BusinessObjectType, UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, CallException>) this);

    public final Values.BackedCachedMap<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException> backed = new Values.BackedCachedMap<BusinessObjectType, WSUriStreamParser.UrlWithCallTypeAndBody, ParameterType, ParseExceptionType, StreamerExceptionType, WebServiceCaller.CallException>(cacher);

  }

}
