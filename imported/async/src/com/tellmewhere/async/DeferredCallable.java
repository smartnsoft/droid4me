package com.tellmewhere.async;

public interface DeferredCallable
{
	public Object onSuccess(Object param) throws Exception;
	public Object onFailure(Exception e) throws Exception;
}