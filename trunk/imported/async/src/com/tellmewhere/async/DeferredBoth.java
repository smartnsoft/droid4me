package com.tellmewhere.async;

/**
 * DeferredBoth implements DeferredCallback where success and failures 
 * are handled the same way. Useful for try/finally like blocks. 
 */
public abstract class DeferredBoth implements DeferredCallable
{
	public Object onSuccess(Object param) throws Exception
	{
		return onBoth(param);
	}
	
	public Object onFailure(Exception e) throws Exception
	{
		return onBoth(e);
	}
	
	public abstract Object onBoth(Object param);
}