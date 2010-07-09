/*
 * (C) Copyright 2009-20010 Smart&Soft SAS (http://www.smartnsoft.com/) and contributors.
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.log.Logger;
import com.smartnsoft.droid4me.log.LoggerFactory;


/**
 * Gathers interfaces and classes for handling business objects in cache.
 * 
 * @author Édouard Mercier
 * @since 2009.08.30
 */
public final class Values
{

  private final static Logger log = LoggerFactory.getInstance(Values.class);

  /**
   * Gathers a business object and its time stamp.
   */
  public static class Info<BusinessObjectType>
  {

    public final static int SOURCE1 = 0;

    public final static int SOURCE2 = SOURCE1 + 1;

    public final static int SOURCE3 = SOURCE2 + 1;

    public final BusinessObjectType value;

    public final Date timestamp;

    public final int source;

    public Info(BusinessObjectType value, Date timestamp, int source)
    {
      this.value = value;
      this.timestamp = timestamp;
      this.source = source;
    }

  }

  /**
   * Used and required when the business object is not available in memory.
   */
  public static interface Instructions<BusinessObjectType, ExceptionType extends Exception, ProblemExceptionType extends Exception>
  {

    boolean tryLoadedFirst();

    boolean accept(Values.Info<BusinessObjectType> info);

    Values.Info<BusinessObjectType> onNotFromLoaded()
        throws ExceptionType, ProblemExceptionType;

    ProblemExceptionType onUnaccessible(Exception causeException);

  }

  public final static class InstructionsException
      extends Exception
  {

    private static final long serialVersionUID = 1724469505270150039L;

    private InstructionsException(String message)
    {
      super(message);
    }

  }

  /**
   * Defines a basic contract which enables to cache a business object in memory, know when it has been cached, and have a dynamic strategy for
   * refreshing the object.
   * 
   * @since 2009.06.18
   */
  public static interface CacheableValue<BusinessObjectType, ExceptionType extends Exception, ProblemExceptionType extends Exception>
  {

    boolean isEmpty();

    BusinessObjectType getLoadedValue();

    void setLoadedValue(Values.Info<BusinessObjectType> info);

    BusinessObjectType getValue(Values.Instructions<BusinessObjectType, ExceptionType, ProblemExceptionType> instructions)
        throws ExceptionType, ProblemExceptionType;

  }

  /**
   * Defines a common class for all "cacheables", i.e. {@link CacheableValue} and {@link BackedCachedMap}, so that we can register them.
   * 
   * @since 2010.06.25
   */
  public static abstract class Caching
  {

    private final static List<Values.Caching> instances = new ArrayList<Values.Caching>();

    protected Caching()
    {
      Caching.instances.add(this);
    }

    /**
     * Is supposed to empty the underlying cached value(s). Invoking {@link #isEmpty()} afterwards will return <code>true</code.
     */
    public abstract void empty();

    /**
     * Empties all {@link Values.Caching} instances.
     */
    public static synchronized void emptyAll()
    {
      for (Values.Caching caching : instances)
      {
        caching.empty();
      }
    }

  }

  /**
   * Enables to cache a business object in memory only.
   * 
   * @since 2009.06.18
   */
  public static class CachedValue<BusinessObjectType, ExceptionType extends Exception>
      extends Values.Caching
      implements Values.CacheableValue<BusinessObjectType, ExceptionType, ExceptionType>
  {

    private Values.Info<BusinessObjectType> info;

    public final synchronized boolean isEmpty()
    {
      return info == null;
    }

    public final synchronized BusinessObjectType getLoadedValue()
    {
      if (info == null)
      {
        return null;
      }
      return info.value;
    }

    public final synchronized void setLoadedValue(Values.Info<BusinessObjectType> info)
    {
      this.info = info;
    }

    public final synchronized BusinessObjectType getValue(Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> instructions)
        throws ExceptionType
    {
      if (instructions.tryLoadedFirst() == false)
      {
        Exception onNotFromLoadedException;
        try
        {
          final Values.Info<BusinessObjectType> newInfo = instructions.onNotFromLoaded();
          if (newInfo != null)
          {
            setLoadedValue(newInfo);
            return newInfo.value;
          }
          else
          {
            throw instructions.onUnaccessible(new Values.InstructionsException("Cannot access to the live data when the data should not be taken from the cache!"));
          }
        }
        // The 'ExceptionType' exception
        catch (Exception exception)
        {
          // We try to retrieve the object via the cache
          onNotFromLoadedException = exception;
        }
        if (isEmpty() == false && instructions.accept(info) == true)
        {
          return getLoadedValue();
        }
        throw instructions.onUnaccessible(onNotFromLoadedException);
      }
      else
      {
        // The business object is first attempted to be retrieved from memory
        if (isEmpty() == false && instructions.accept(info) == true)
        {
          return getLoadedValue();
        }
        final Values.Info<BusinessObjectType> newInfo = instructions.onNotFromLoaded();
        if (newInfo != null)
        {
          setLoadedValue(newInfo);
          return newInfo.value;
        }
        else
        {
          throw instructions.onUnaccessible(new Values.InstructionsException("Cannot access to the live business object when the data should not be taken from the cache!"));
        }
      }
    }

    @Override
    public final synchronized void empty()
    {
      info = null;
    }

  }

  public final static class CacheException
      extends Business.BusinessException
  {

    private static final long serialVersionUID = -2742319642305884562L;

    private CacheException(String message, Throwable cause)
    {
      super(message, cause);
    }

    private CacheException(Throwable cause)
    {
      super(cause);
    }

  }

  public abstract static class WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      implements Values.Instructions<BusinessObjectType, Values.CacheException, Values.CacheException>
  {

    protected final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    protected final ParameterType parameter;

    protected WithParameterInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher, ParameterType parameter)
    {
      this.cacher = cacher;
      this.parameter = parameter;
    }

    public final Values.CacheException onUnaccessible(Exception exception)
    {
      return new Values.CacheException("Could not access to the business object neither through the cache nor through the IO streamer", exception);
    }

  };

  public static class OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromMemory;

    public OnlyFromCacheInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromMemory)
    {
      super(cacher, parameter);
      this.fromMemory = fromMemory;
    }

    public boolean tryLoadedFirst()
    {
      return fromMemory == true;
    }

    public boolean accept(Values.Info<BusinessObjectType> info)
    {
      return true;
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded()
        throws Values.CacheException
    {
      try
      {
        return cacher.getCachedValue(parameter);
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not read from the cache the business object corresponding to the URI '" + parameter + "'", throwable);
      }
    }

  };

  public static class MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromCache;

    public MemoryInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromCache)
    {
      super(cacher, parameter);
      this.fromCache = fromCache;
    }

    public boolean tryLoadedFirst()
    {
      return true;
    }

    public boolean accept(Values.Info<BusinessObjectType> info)
    {
      return fromCache(fromCache, info.timestamp);
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded()
        throws Values.CacheException
    {
      try
      {
        return cacher.getValue(new Cacher.Instructions()
        {
          public boolean takeFromCache(Date lastUpdate)
          {
            return fromCache(fromCache, lastUpdate);
          }
        }, parameter);
      }
      catch (Throwable throwable)
      {
        // TODO: add a flag which controls whether a last attempts should be run
        // A last attempt is done to retrieve the data from the cache
        try
        {
          return cacher.getValue(new Cacher.Instructions()
          {
            public boolean takeFromCache(Date lastUpdate)
            {
              return true;
            }
          }, parameter);
        }
        catch (Throwable innerThrowable)
        {
          throw new Values.CacheException("Could not read the business object corresponding to the URI '" + parameter + "'", innerThrowable);
        }
      }
    }

    protected boolean fromCache(boolean fromCache, Date lastUpdate)
    {
      return fromCache == true;
    }

  };

  public static class MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromMemory;

    public MemoryAndCacheInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromMemory, boolean fromCache)
    {
      super(cacher, parameter, fromCache);
      this.fromMemory = fromMemory;
    }

    public boolean tryLoadedFirst()
    {
      return fromMemory == true;
    }

    protected boolean fromCache(boolean fromCache, Date lastUpdate)
    {
      return fromCache == true;
    }

  };

  public static class RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final long cachingPeriodInMilliseconds;

    public RetentionInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromCache, long cachingPeriodInMilliseconds)
    {
      super(cacher, parameter, fromCache);
      this.cachingPeriodInMilliseconds = cachingPeriodInMilliseconds;
    }

    protected boolean fromCache(boolean fromCache, Date lastUpdate)
    {
      return fromCache == true && (cachingPeriodInMilliseconds == -1 || ((System.currentTimeMillis() - lastUpdate.getTime()) < cachingPeriodInMilliseconds));
    }

  };

  public static class SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    // private boolean alreadyLoadedFromUriStreamParser;

    public SessionInstructions(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter)
    {
      super(cacher, parameter);
    }

    public boolean tryLoadedFirst()
    {
      return true;
    }

    public boolean accept(Values.Info<BusinessObjectType> info)
    {
      return info.source == Values.Info.SOURCE3;
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded()
        throws Values.CacheException
    {
      try
      {
        final Cacher.CachedInfo<BusinessObjectType> info = cacher.getValue(parameter);
        // if (info != null && info.origin == Cacher.Origin.UriStreamParser)
        // {
        // alreadyLoadedFromUriStreamParser = true;
        // }
        return info;
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not read the business object corresponding to the URI '" + parameter + "'", throwable);
      }
    }

  };

  /**
   * @since 2009.08.31
   */
  public static class BackedCachedValue<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Values.CachedValue<BusinessObjectType, Values.CacheException>
  {

    private final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    public BackedCachedValue(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher)
    {
      this.cacher = cacher;
    }

    public void setValue(ParameterType parameter, BusinessObjectType businessObject)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> info = new Values.Info<BusinessObjectType>(businessObject, new Date(), Values.Info.SOURCE1);
      // Even if cacher fails to persist the business object, the memory is up-to-date
      super.setLoadedValue(info);
      try
      {
        cacher.setValue(parameter, info);
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not save the business object corresponding to the parameter '" + parameter + "'", throwable);
      }
    }

    public final BusinessObjectType safeGet(ParameterType parameter)
    {
      return safeGet(true, true, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromCache, ParameterType parameter)
    {
      return safeGet(fromCache, true, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromMemory, boolean fromCache, ParameterType parameter)
    {
      try
      {
        return getMemoryValue(fromMemory, fromCache, parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed reading of the business object with parameter '" + parameter + "': returning the cached value if any", exception);
        }
        return getLoadedValue();
      }
    }

    public final void safeSet(ParameterType parameter, BusinessObjectType businessObject)
    {
      try
      {
        setValue(parameter, businessObject);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed saving of the business object with URI '" + parameter + "'", exception);
        }
      }
    }

    public final void remove(ParameterType parameter)
        throws Values.CacheException
    {
      try
      {
        cacher.remove(parameter);
      }
      catch (Throwable throwable)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Could not remove from the persistent cache te business object with parameter '" + parameter + "'", throwable);
          throw new Values.CacheException(throwable);
        }
      }
      setLoadedValue(null);
    }

    public final void safeRemove(ParameterType parameter)
    {
      try
      {
        remove(parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed removing of the business object with URI '" + parameter + "'", exception);
        }
      }
    }

    public final BusinessObjectType getOnlyFromCacheValue(boolean fromMemory, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(new Values.OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory));
    }

    public final BusinessObjectType getSessionValue(ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(new Values.SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter));
    }

    public final BusinessObjectType getMemoryValue(boolean fromCache, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(new Values.MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache));
    }

    public final BusinessObjectType getMemoryValue(boolean fromMemory, boolean fromCache, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(new Values.MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory, fromCache));
    }

    public final BusinessObjectType getRetentionValue(boolean fromCache, long cachingPeriodInMilliseconds, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(new Values.RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache, cachingPeriodInMilliseconds));
    }

  }

  /**
   * Enables to cache in memory a map of business objects.
   * 
   * @since 2009.06.18
   */

  public static class CachedMap<BusinessObjectType, KeyType, ExceptionType extends Exception>
      extends Values.Caching
  {

    protected final Map<KeyType, Values.CachedValue<BusinessObjectType, ExceptionType>> map = new HashMap<KeyType, Values.CachedValue<BusinessObjectType, ExceptionType>>();

    public BusinessObjectType getValue(Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> ifValueNotCached, KeyType key)
        throws ExceptionType
    {
      Values.CachedValue<BusinessObjectType, ExceptionType> cached;
      synchronized (map)
      {
        cached = map.get(key);
        if (map.containsKey(key) == false)
        {
          cached = new Values.CachedValue<BusinessObjectType, ExceptionType>();
          map.put(key, cached);
        }
      }
      return cached.getValue(ifValueNotCached);
    }

    /**
     * This implementation does not empty the {@link Values.CacheableValue} values.
     * 
     *@see Values.Caching#empty()
     */
    @Override
    public final synchronized void empty()
    {
      map.clear();
    }

  }

  /**
   * Enables to cache in memory and on persistent cache a map of business objects.
   * 
   * @since 2009.09.09
   */
  public static final class BackedCachedMap<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Values.CachedMap<BusinessObjectType, ParameterType, Values.CacheException>

  {

    private final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    public BackedCachedMap(Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher)
    {
      this.cacher = cacher;
    }

    @Override
    public final synchronized BusinessObjectType getValue(Values.Instructions<BusinessObjectType, Values.CacheException, Values.CacheException> instructions,
        ParameterType parameter)
        throws Values.CacheException
    {
      final boolean toAdd;
      Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue = map.get(parameter);
      if (cachedValue == null)
      {
        toAdd = true;
        cachedValue = new Values.CachedValue<BusinessObjectType, Values.CacheException>();
      }
      else
      {
        toAdd = false;
      }
      final BusinessObjectType businessObject = cachedValue.getValue(instructions);
      if (businessObject != null && toAdd == true)
      {
        map.put(parameter, cachedValue);
      }
      return businessObject;
    }

    public final BusinessObjectType getValue(boolean fromCache, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(true, fromCache, parameter);
    }

    public final BusinessObjectType getValue(boolean fromMemory, final boolean fromCache, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory, fromCache),
          parameter);
    }

    public final BusinessObjectType safeGet(ParameterType parameter)
    {
      return safeGet(true, true, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromCache, ParameterType parameter)
    {
      return safeGet(true, fromCache, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromMemory, boolean fromCache, ParameterType parameter)
    {
      try
      {
        return getValue(fromMemory, fromCache, parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed reading of the business object with parameter '" + parameter + "': returning the cached value if any", exception);
        }
        return getLoadedValue(parameter);
      }
    }

    public final BusinessObjectType getLoadedValue(ParameterType parameter)
    {
      final Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue;
      synchronized (map)
      {
        cachedValue = map.get(parameter);
      }
      if (cachedValue != null)
      {
        return cachedValue.getLoadedValue();
      }
      return null;
    }

    public final void remove(ParameterType parameter)
        throws Values.CacheException
    {
      try
      {
        cacher.remove(parameter);
      }
      catch (Throwable throwable)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Could not remove from the persistent cache te business object with parameter '" + parameter + "'", throwable);
          throw new Values.CacheException(throwable);
        }
      }
      // We clean-up the memory cache, only provided the IO streamer has been cleaned-up
      map.remove(parameter);
    }

    public final void safeRemove(ParameterType parameter)
    {
      try
      {
        remove(parameter);
      }
      catch (Values.CacheException exception)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Ignoring the failed removing of the business object with URI '" + parameter + "'", exception);
        }
      }
    }

    public final void safeSet(ParameterType parameter, BusinessObjectType businessObject)
    {
      final Values.Info<BusinessObjectType> info = new Values.Info<BusinessObjectType>(businessObject, new Date(), Values.Info.SOURCE1);
      // We modify the memory first, so as to make sure that it is actually modified
      setLoadedValue(parameter, info);
      try
      {
        cacher.setValue(parameter, info);
      }
      catch (Throwable throwable)
      {
        if (log.isWarnEnabled())
        {
          log.warn("Failed to save to the cache the business object with parameter '" + parameter + "': only taken into account in memory!", throwable);
        }
      }
    }

    private synchronized void setLoadedValue(ParameterType parameter, Values.Info<BusinessObjectType> info)
    {
      Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue = map.get(parameter);
      if (cachedValue == null)
      {
        cachedValue = new Values.CachedValue<BusinessObjectType, Values.CacheException>();
        map.put(parameter, cachedValue);
      }
      cachedValue.setLoadedValue(info);
    }

    public final BusinessObjectType getOnlyFromCacheValue(boolean fromMemory, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory),
          parameter);
    }

    public final BusinessObjectType getSessionValue(ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter),
          parameter);
    }

    public final BusinessObjectType getMemoryValue(boolean fromCache, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache),
          parameter);
    }

    public final BusinessObjectType getMemoryValue(boolean fromMemory, boolean fromCache, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromMemory, fromCache),
          parameter);
    }

    public final BusinessObjectType getRetentionValue(boolean fromCache, long cachingPeriodInMilliseconds, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(
          new Values.RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>(cacher, parameter, fromCache, cachingPeriodInMilliseconds),
          parameter);
    }

  }

}
