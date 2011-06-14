package com.tellmewhere.async;

public class DeferredCallback implements DeferredCallable
{
	public Object onFailure(Exception e) throws Exception {
		return e;
	}

	public Object onSuccess(Object param) throws Exception {
		return param;
	}
}