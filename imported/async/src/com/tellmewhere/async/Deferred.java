package com.tellmewhere.async;

import java.util.LinkedList;
import java.util.Queue;

import android.os.Handler;


/**
 * DeferredRequest handles a sequence of DeferredCallbacks. The request can be
 * fired only once via call() and DeferredCallbacks can be added before or after 
 * the request is fired. Every callback is called with the returned value of the
 * previous one or the value passed to call(), in insertion order. If the value
 * inherits from Exception, then callback handleFailure is called instead of
 * handleSuccess. Then the handleXXX returned value or thrown exception is captured
 * and propagated to the next callback.
 * 
 * Be careful that returning an Exception is equivalent to throwing it.
 * 
 * If the returned value is a DeferredRequest, then the current callbacks are paused.
 * The sub request will wake them up after having executed all its callbacks. Result
 * returned from the subrequest is passed to the remaining callback when the execution
 * resumes.
 */
public class Deferred
{
	private final Queue<DeferredCallable> listeners_ = new LinkedList<DeferredCallable>();
	private volatile boolean fired_ = false;
	private Object lastResult_ = null;
	private volatile boolean cancelled_ = false;
	private int paused_ = 0;
	private Handler handler_ = null;
	private String name_ = null;
	
	public Deferred()
	{
		this(false, null);
	}
	
	/**
	 * @param onThread is true to make call() automatically callback on the caller
	 * thread when fired.
	 */
	public Deferred(boolean onThread, String name) {
		if(onThread) {
			handler_ = Reactor.getHandler();
			name_ = name;
		}
	}
	
	public static Deferred failure(Exception e)
	{
		Deferred rq = new Deferred();
		rq.call(e != null ? e : new Exception("Generic failure"));
		return rq;
	}
	
	public static Deferred success(Object param)
	{
		Deferred rq = new Deferred();
		rq.call(param);
		return rq;
	}
	
	/**
	 * Fire the request with the given parameter. If param is an Exception, the first
	 * callback will be fired on handleFailure(). If no callback is registered yet, the
	 * parameter is kept for later.
	 */
	public void call(final Object param)
	{
		if(handler_ != null) {
			// Recurse in original caller thread
			Handler h = handler_;
			handler_ = null;
			h.post(new Runnable() {
				public void run() {
					Deferred.this.call(param);
				}
			});
			return;
		}
		
		// Detect multiple executions
		if(fired_)
			throw new RuntimeException("DeferredCallback can only be run once");
		
		if(param instanceof Deferred)
			throw new RuntimeException("Cannot fire a DeferredRequest with another DeferredRequest");
		
		fired_ = true;
		
		try {
			run(param);
		} catch(StackOverflowError e) {
			if(name_ != null)
				throw new Error("Deferred.call() caused stack overflow for " + name_, e);
			throw e;
		}
	}
	
	/**
	 * Register a new callback. If the request was fired already, the callback will
	 * be executed synchronously.
	 * @return this.
	 */
	public Deferred addCallback(DeferredCallable callback)
	{
		if(callback == null)
			return this;
		listeners_.add(callback);
		
		if(!fired_)
			return this;
		// Execute the callback immediately
		run(lastResult_);
		return this;
	}
	
	public Deferred chainDeferred(Deferred request)
	{
		if(request == null)
			return this;
		
		final Deferred rq = request;
		return addCallback(new DeferredCallable()
		{
			public Object onFailure(Exception e) throws Exception 
			{
				rq.call(e);
				return rq;
			}

			public Object onSuccess(Object param) throws Exception
			{
				rq.call(param);
				return rq;
			}
		});
	}
	
	/**
	 * @return true if the Deferred was cancelled.
	 * 
	 * Thread-safe.
	 */
	public boolean isCancelled()
	{
		return cancelled_;
	}
	
	/**
	 * @return true if the Deferred has been called already.
	 * 
	 * Thread-safe.
	 */
	public boolean hasFired() {
		return fired_;
	}
	
	/**
	 * Ask for callback cancellation. This operation is asynchronous: only the next
	 * time a callback will be executed a CancelledException will be passed to it
	 * instead of the normal parameter.
	 * 
	 * Thread-safe
	 */
	public void cancel()
	{
		cancelled_ = true;
	}
	
	private void pause()
	{
		paused_++;
	}
	
	private void unpause()
	{
		paused_--;
		if(paused_ != 0)
			return;
		if(fired_)
			run(lastResult_);
	}
	
	protected void restart(Object param)
	{
		lastResult_ = param;
		unpause();
	}
	
	private void run(Object param)
	{
		lastResult_ = param;
		if(paused_ == 0)
		{
			DeferredCallable callback;
			while((callback = listeners_.poll()) != null)
			{
				if(cancelled_)
					lastResult_ = new CancelledException();
				
				try
				{
					lastResult_ = lastResult_ instanceof Exception 
						? callback.onFailure((Exception)lastResult_)
						: callback.onSuccess(lastResult_);
				}
				catch(Exception e)
				{
					lastResult_ = e;
				}
				
				if(lastResult_ instanceof Deferred)
				{
					pause();
					Deferred rq = (Deferred)lastResult_;
					rq.addCallback(new DeferredBoth() {
						@Override
						public Object onBoth(Object param) {
							restart(param);
							return null;
						}
					});
					break;
				}
			}
		}
	}	
}
