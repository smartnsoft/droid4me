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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.smartnsoft.droid4me.bo.Business;
import com.smartnsoft.droid4me.bo.Business.IOStreamer;
import com.smartnsoft.droid4me.bo.Business.UriStreamParser;
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

  /**
   * Used when requesting a {@link Values.CacheableValue} or a {@link CachedMap} for retrieving a business object, depending on the current workflow.
   *
   * @since 2010.09.09
   */
  public interface CachingEvent
  {

    /**
     * Is invoked when the wrapped business object is bound to be retrieved from an {@link IOStreamer}, i.e. from the persistence layer.
     */
    void onIOStreamer(Cacher.Status status);

    /**
     * Is invoked when the wrapped business object is bound to be retrieved from an {@link UriStreamParser}, i.e. not locally.
     */
    void onUriStreamParser(Cacher.Status status);

  }

  /**
   * Used and required when the business object is not available in memory.
   */
  public interface Instructions<BusinessObjectType, ExceptionType extends Exception, ProblemExceptionType extends Exception>
  {

    /**
     * States the result of an assessment.
     *
     * @see Values.Instructions#assess(Values.Info)
     */
    enum Result
    {
      Rejected, Accepted
    }

    /**
     * Is invoked every time the business object is tested in order to determine whether it is valid in its current state, or should be reloaded.
     *
     * @param info the proposed business object, its associated timestamp, and source
     * @return the status which indicates whether the provided business object state has been validated
     */
    Values.Instructions.Result assess(Values.Info<BusinessObjectType> info);

    /**
     * Is invoked after each {@link #assess(Values.Info) assessment} and enables to store its result.
     *
     * @param source where the business object comes from
     * @param result the result of the assessment
     */
    void remember(Business.Source source, Values.Instructions.Result result);

    /**
     * Is invoked when the underlying business object will not be taken from the memory cache, or when the cached value has been rejected.
     *
     * @param cachingEvent the interface that will be used to notify the caller about the loading workflow ; may be {@code null}
     * @return the wrapped business object that should be attempted to be requested from another place than the cache
     */
    Values.Info<BusinessObjectType> onNotFromLoaded(Values.CachingEvent cachingEvent)
        throws ExceptionType, ProblemExceptionType;

    /**
     * Will be invoked when the business object cannot be eventually retrieved.
     *
     * @param causeException the reason which states why the business object cannot be retrieved
     * @return an exception to be thrown to the caller
     */
    ProblemExceptionType onUnaccessible(Exception causeException);

  }

  /**
   * Defines a basic contract which enables to cache a business object in memory, know when it has been cached, and have a dynamic strategy for
   * refreshing the object.
   *
   * @since 2009.06.18
   */
  public interface CacheableValue<BusinessObjectType, ExceptionType extends Exception, ProblemExceptionType extends Exception>
  {

    // TODO: document this
    boolean isEmpty();

    BusinessObjectType getLoadedValue();

    void setLoadedInfoValue(Values.Info<BusinessObjectType> info);

    BusinessObjectType getValue(
        Values.Instructions<BusinessObjectType, ExceptionType, ProblemExceptionType> instructions,
        Values.CachingEvent cachingEvent)
        throws ExceptionType, ProblemExceptionType;

  }

  public static final class InstructionsException
      extends Exception
  {

    private static final long serialVersionUID = 1724469505270150039L;

    private InstructionsException(String message)
    {
      super(message);
    }

  }

  /**
   * The exception which may be triggered to indicate that a caching method has failed.
   */
  public static final class CacheException
      extends Business.BusinessException
  {

    private static final long serialVersionUID = -2742319642305884562L;

    public CacheException(String message, Throwable cause)
    {
      super(message, cause);
    }

    public CacheException(Throwable cause)
    {
      super(cause);
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

    public final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    public BackedCachedMap(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher)
    {
      this.cacher = cacher;
    }

    /**
     * If two setting and getting methods are called concurrently, the consistency of the results are not granted!
     */
    // TODO: why is this method overloaded?
    @Override
    public final BusinessObjectType getValue(
        Values.Instructions<BusinessObjectType, Values.CacheException, Values.CacheException> instructions,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final boolean toAdd;
      Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue = map.get(parameter);
      if (cachedValue == null)
      {
        toAdd = true;
        cachedValue = new Values.CachedValue<>();
      }
      else
      {
        toAdd = false;
      }
      final BusinessObjectType businessObject = cachedValue.getValue(instructions, cachingEvent);
      if (businessObject != null && toAdd == true)
      {
        map.put(parameter, cachedValue);
      }
      return businessObject;
    }

    public final BusinessObjectType getValue(boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(true, fromCache, cachingEvent, parameter);
    }

    public final BusinessObjectType getValue(boolean fromMemory, final boolean fromCache,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getValue(new Values.MemoryAndCacheInstructions<>(cacher, parameter, fromMemory, fromCache), cachingEvent, parameter);
    }

    public final BusinessObjectType safeGet(Values.CachingEvent cachingEvent, ParameterType parameter)
    {
      return safeGet(true, true, cachingEvent, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
    {
      return safeGet(fromCache, true, cachingEvent, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
    {
      try
      {
        return getValue(fromMemory, fromCache, cachingEvent, parameter);
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
      cachedValue = map.get(parameter);
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

    public final void setValue(ParameterType parameter, BusinessObjectType businessObject)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> info = new Values.Info<>(businessObject, new Date(), Business.Source.Memory);
      // We modify the memory first, so as to make sure that it is actually modified
      setLoadedValue(parameter, info);
      try
      {
        cacher.setValue(parameter, info);
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException(throwable);
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
          log.warn("Failed to save to the cache the business object with parameter '" + parameter + "': only taken into account in memory!", exception);
        }
      }
    }

    public final Values.Info<BusinessObjectType> getOnlyFromCacheInfoValue(boolean fromMemory,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.OnlyFromCacheInstructions<>(cacher, parameter, fromMemory), cachingEvent, parameter);
    }

    public final BusinessObjectType getOnlyFromCacheValue(boolean fromMemory, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getOnlyFromCacheInfoValue(fromMemory, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getSessionInfoValue(Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.SessionInstructions<>(cacher, parameter), cachingEvent, parameter);
    }

    public final BusinessObjectType getSessionValue(Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getSessionInfoValue(cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.MemoryInstructions<>(cacher, parameter, fromCache), cachingEvent, parameter);
    }

    public final BusinessObjectType getMemoryValue(boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromMemory, boolean fromCache,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.MemoryAndCacheInstructions<>(cacher, parameter, fromMemory, fromCache), cachingEvent, parameter);
    }

    public final BusinessObjectType getMemoryValue(boolean fromMemory, boolean fromCache,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromMemory, fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getRetentionInfoValue(boolean fromCache,
        long cachingPeriodInMilliseconds, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.RetentionInstructions<>(cacher, parameter, fromCache, cachingPeriodInMilliseconds), cachingEvent, parameter);
    }

    public final BusinessObjectType getRetentionValue(boolean fromCache, long cachingPeriodInMilliseconds,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getRetentionInfoValue(fromCache, cachingPeriodInMilliseconds, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    private void setLoadedValue(ParameterType parameter, Values.Info<BusinessObjectType> info)
    {
      Values.CachedValue<BusinessObjectType, Values.CacheException> cachedValue = map.get(parameter);
      if (cachedValue == null)
      {
        cachedValue = new Values.CachedValue<>();
        map.put(parameter, cachedValue);
      }
      cachedValue.setLoadedInfoValue(info);
    }

  }

  /**
   * Gathers a business object and its time stamp.
   */
  public static class Info<BusinessObjectType>
  {

    public final BusinessObjectType value;

    public final Date timestamp;

    private Business.Source source;

    public Info(BusinessObjectType value, Date timestamp, Business.Source source)
    {
      this.value = value;
      this.timestamp = timestamp;
      this.source = source;
    }

    public Business.Source getSource()
    {
      return source;
    }

  }

  /**
   * Defines a common class for all "cacheables", i.e. {@link CacheableValue} and {@link BackedCachedMap}, so that we can register them.
   *
   * @since 2010.06.25
   */
  public static abstract class Caching
  {

    private final static List<Values.Caching> instances = new ArrayList<>();

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

    protected Caching()
    {
      Caching.instances.add(this);
    }

    /**
     * Is supposed to empty the underlying cached value(s).
     */
    public abstract void empty();

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

    public final boolean isEmpty()
    {
      return info == null;
    }

    public final BusinessObjectType getLoadedValue()
    {
      if (info == null)
      {
        return null;
      }
      return info.value;
    }

    public final BusinessObjectType getValue(
        Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> instructions,
        Values.CachingEvent cachingEvent)
        throws ExceptionType
    {
      final Info<BusinessObjectType> infoValue = getInfoValue(instructions, cachingEvent);
      return infoValue == null ? null : infoValue.value;
    }

    @Override
    public final void empty()
    {
      info = null;
    }

    public final Values.Info<BusinessObjectType> getLoadedInfoValue()
    {
      return info;
    }

    public final void setLoadedInfoValue(Values.Info<BusinessObjectType> info)
    {
      this.info = info;
    }

    public final Values.Info<BusinessObjectType> getInfoValue(
        Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> instructions,
        Values.CachingEvent cachingEvent)
        throws ExceptionType
    {
      // The business object is first attempted to be retrieved from memory
      if (isEmpty() == false)
      {
        info.source = Business.Source.Memory;
        final Values.Instructions.Result result = instructions.assess(info);
        instructions.remember(info.source, result);
        if (result == Values.Instructions.Result.Accepted)
        {
          return getLoadedInfoValue();
        }
      }
      final Values.Info<BusinessObjectType> newInfo = instructions.onNotFromLoaded(cachingEvent);
      if (newInfo != null)
      {
        // We check whether the newly retrieved info is accepted
        final Values.Instructions.Result result = instructions.assess(newInfo);
        instructions.remember(newInfo.source, result);
        if (result == Values.Instructions.Result.Accepted)
        {
          setLoadedInfoValue(newInfo);
          return newInfo;
        }
        else
        {
          final Values.Info<BusinessObjectType> reloadedInfo = instructions.onNotFromLoaded(cachingEvent);
          if (reloadedInfo != null)
          {
            setLoadedInfoValue(reloadedInfo);
            return reloadedInfo;
          }
        }
      }
      throw instructions.onUnaccessible(new Values.InstructionsException("Cannot access to the live business object when the data should not be taken from the cache!"));
    }

  }

  public static abstract class WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      implements Values.Instructions<BusinessObjectType, Values.CacheException, Values.CacheException>
  {

    protected final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    protected final ParameterType parameter;

    protected WithParameterInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter)
    {
      this.cacher = cacher;
      this.parameter = parameter;
    }

    public void remember(Business.Source source, Values.Instructions.Result result)
    {
    }

    public final Values.CacheException onUnaccessible(Exception exception)
    {
      return new Values.CacheException("Could not access to the business object neither through the cache nor through the IO streamer", exception);
    }

  }

  public static class OnlyFromCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromMemory;

    public OnlyFromCacheInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromMemory)
    {
      super(cacher, parameter);
      this.fromMemory = fromMemory;
    }

    public Values.Instructions.Result assess(Values.Info<BusinessObjectType> info)
    {
      return fromMemory == false ? (info.source == Business.Source.IOStreamer ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected) : Values.Instructions.Result.Accepted;
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded(Values.CachingEvent cachingEvent)
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

  }

  public static class MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    protected final boolean fromCache;

    protected final Map<Business.Source, Values.Instructions.Result> assessments = new LinkedHashMap<>();

    public MemoryInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromCache)
    {
      super(cacher, parameter);
      this.fromCache = fromCache;
    }

    @Override
    public void remember(Business.Source source, Values.Instructions.Result result)
    {
      assessments.put(source, result);
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded(final Values.CachingEvent cachingEvent)
        throws Values.CacheException
    {
      try
      {
        return cacher.getValue(new Cacher.Instructions()
        {
          public boolean queryTimestamp()
          {
            return assessFromCacher(true, null);
          }

          public boolean takeFromCache(Date lastUpdate)
          {
            return assessFromCacher(false, lastUpdate);
          }

          public void onIOStreamer(Cacher.Status status)
          {
            if (cachingEvent != null)
            {
              cachingEvent.onIOStreamer(status);
            }
          }

          public void onUriStreamParser(Cacher.Status status)
          {
            if (cachingEvent != null)
            {
              cachingEvent.onUriStreamParser(status);
            }
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
            public boolean queryTimestamp()
            {
              return false;
            }

            public boolean takeFromCache(Date lastUpdate)
            {
              return true;
            }

            public void onIOStreamer(Cacher.Status status)
            {
              if (cachingEvent != null)
              {
                cachingEvent.onIOStreamer(status);
              }
            }

            public void onUriStreamParser(Cacher.Status status)
            {
              if (cachingEvent != null)
              {
                cachingEvent.onUriStreamParser(status);
              }
            }
          }, parameter);
        }
        catch (Throwable innerThrowable)
        {
          throw new Values.CacheException("Could not read the business object corresponding to the URI '" + parameter + "'", innerThrowable);
        }
      }
    }

    public Values.Instructions.Result assess(Values.Info<BusinessObjectType> info)
    {
      return (fromCache == true || (info.source == Business.Source.UriStreamer || assessments.size() >= 1)) ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected;
    }

    protected boolean assessFromCacher(boolean queryTimestamp, Date lastUpdate)
    {
      if (fromCache == true)
      {
        return queryTimestamp == false;
      }
      else
      {
        if (assessments.containsKey(Business.Source.UriStreamer) == true)
        {
          if (assessments.size() == 1)
          {
            return false;
          }
          else
          {
            return queryTimestamp == false;
          }
        }
        else
        {
          return false;
        }
      }
    }

  }

  public static class MemoryAndCacheInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final boolean fromMemory;

    public MemoryAndCacheInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromMemory, boolean fromCache)
    {
      super(cacher, parameter, fromCache);
      this.fromMemory = fromMemory;
    }

    @Override
    public Values.Instructions.Result assess(Values.Info<BusinessObjectType> info)
    {
      if (fromMemory == true)
      {
        return Values.Instructions.Result.Accepted;
      }
      else if (fromCache == true)
      {
        return info.source != Business.Source.Memory ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected;
      }
      else
      {
        return (info.source == Business.Source.UriStreamer || assessments.size() >= 1) ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected;
      }
    }

  }

  public static class RetentionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends MemoryInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    private final long cachingPeriodInMilliseconds;

    public RetentionInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter, boolean fromCache, long cachingPeriodInMilliseconds)
    {
      super(cacher, parameter, fromCache);
      this.cachingPeriodInMilliseconds = cachingPeriodInMilliseconds;
    }

    @Override
    public Values.Instructions.Result assess(Values.Info<BusinessObjectType> info)
    {
      if (fromCache == false)
      {
        return (info.source == Business.Source.UriStreamer || assessments.size() >= 1) ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected;
      }
      else
      {
        if (cachingPeriodInMilliseconds == -1 || ((System.currentTimeMillis() - info.timestamp.getTime()) <= cachingPeriodInMilliseconds))
        {
          return Values.Instructions.Result.Accepted;
        }
        else
        {
          return (info.source == Business.Source.UriStreamer || assessments.size() >= 1) ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected;
        }
      }
    }

    @Override
    protected boolean assessFromCacher(boolean queryTimestamp, Date lastUpdate)
    {
      if (fromCache == true)
      {
        if (cachingPeriodInMilliseconds == -1)
        {
          return queryTimestamp == false;
        }
        else
        {
          return queryTimestamp == true ? true : (((System.currentTimeMillis() - lastUpdate.getTime()) <= cachingPeriodInMilliseconds) ? true : false);
        }
      }
      else
      {
        if (assessments.containsKey(Business.Source.UriStreamer) == true)
        {
          if (assessments.size() == 1)
          {
            return false;
          }
          else
          {
            return queryTimestamp == false;
          }
        }
        else
        {
          return false;
        }
      }
    }

  }

  public static class SessionInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends WithParameterInstructions<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType>
  {

    public SessionInstructions(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher,
        ParameterType parameter)
    {
      super(cacher, parameter);
    }

    public Values.Instructions.Result assess(Values.Info<BusinessObjectType> info)
    {
      return info.source == Business.Source.UriStreamer ? Values.Instructions.Result.Accepted : Values.Instructions.Result.Rejected;
    }

    public Values.Info<BusinessObjectType> onNotFromLoaded(Values.CachingEvent cachingEvent)
        throws Values.CacheException
    {
      try
      {
        final Values.Info<BusinessObjectType> info = cacher.getValue(parameter);
        return info;
      }
      catch (Throwable throwable)
      {
        throw new Values.CacheException("Could not read the business object corresponding to the URI '" + parameter + "'", throwable);
      }
    }

  }

  /**
   * @since 2009.08.31
   */
  public static class BackedCachedValue<BusinessObjectType, UriType, ParameterType, ParseExceptionType extends Exception, StreamerExceptionType extends Throwable, InputExceptionType extends Exception>
      extends Values.CachedValue<BusinessObjectType, Values.CacheException>
  {

    public final Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher;

    public BackedCachedValue(
        Cacher<BusinessObjectType, UriType, ParameterType, ParseExceptionType, StreamerExceptionType, InputExceptionType> cacher)
    {
      this.cacher = cacher;
    }

    public void setValue(ParameterType parameter, BusinessObjectType businessObject)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> info = new Values.Info<>(businessObject, new Date(), Business.Source.Memory);
      // Even if cacher fails to persist the business object, the memory is up-to-date
      super.setLoadedInfoValue(info);
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
      return safeGet(true, true, null, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromCache, ParameterType parameter)
    {
      return safeGet(fromCache, true, null, parameter);
    }

    public final BusinessObjectType safeGet(boolean fromMemory, boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
    {
      try
      {
        return getMemoryValue(fromMemory, fromCache, cachingEvent, parameter);
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
      setLoadedInfoValue(null);
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

    public final Values.Info<BusinessObjectType> getOnlyFromCacheInfoValue(boolean fromMemory, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.OnlyFromCacheInstructions<>(cacher, parameter, fromMemory), null);
    }

    public final BusinessObjectType getOnlyFromCacheValue(boolean fromMemory, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getOnlyFromCacheInfoValue(fromMemory, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getSessionInfoValue(Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.SessionInstructions<>(cacher, parameter), cachingEvent);
    }

    public final BusinessObjectType getSessionValue(Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getSessionInfoValue(cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.MemoryInstructions<>(cacher, parameter, fromCache), cachingEvent);
    }

    public final BusinessObjectType getMemoryValue(boolean fromCache, Values.CachingEvent cachingEvent,
        ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getMemoryInfoValue(boolean fromMemory, boolean fromCache,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.MemoryAndCacheInstructions<>(cacher, parameter, fromMemory, fromCache), cachingEvent);
    }

    public final BusinessObjectType getMemoryValue(boolean fromMemory, boolean fromCache,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getMemoryInfoValue(fromMemory, fromCache, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
    }

    public final Values.Info<BusinessObjectType> getRetentionInfoValue(boolean fromCache,
        long cachingPeriodInMilliseconds, Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      return getInfoValue(new Values.RetentionInstructions<>(cacher, parameter, fromCache, cachingPeriodInMilliseconds), cachingEvent);
    }

    public final BusinessObjectType getRetentionValue(boolean fromCache, long cachingPeriodInMilliseconds,
        Values.CachingEvent cachingEvent, ParameterType parameter)
        throws Values.CacheException
    {
      final Values.Info<BusinessObjectType> infoValue = getRetentionInfoValue(fromCache, cachingPeriodInMilliseconds, cachingEvent, parameter);
      return infoValue == null ? null : infoValue.value;
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

    protected final Map<KeyType, Values.CachedValue<BusinessObjectType, ExceptionType>> map = new ConcurrentHashMap<>();

    /**
     * This implementation does not empty the {@link Values.CacheableValue} values.
     *
     * @see Values.Caching#empty()
     */
    @Override
    public final void empty()
    {
      map.clear();
    }

    public Values.Info<BusinessObjectType> getInfoValue(
        Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> ifValueNotCached,
        Values.CachingEvent cachingEvent, KeyType key)
        throws ExceptionType
    {
      final Values.CachedValue<BusinessObjectType, ExceptionType> cached;
      if (map.containsKey(key) == false)
      {
        cached = new Values.CachedValue<>();
        map.put(key, cached);
      }
      else
      {
        cached = map.get(key);
      }
      return cached.getInfoValue(ifValueNotCached, cachingEvent);
    }

    public BusinessObjectType getValue(
        Values.Instructions<BusinessObjectType, ExceptionType, ExceptionType> ifValueNotCached,
        Values.CachingEvent cachingEvent, KeyType key)
        throws ExceptionType
    {
      final Values.Info<BusinessObjectType> infoValue = getInfoValue(ifValueNotCached, cachingEvent, key);
      return infoValue == null ? null : infoValue.value;
    }

  }

  private final static Logger log = LoggerFactory.getInstance(Values.class);

}
