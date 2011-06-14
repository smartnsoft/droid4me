package com.tellmewhere.async;

import java.util.Iterator;
import java.util.List;

/**
 * DeferredArrayException is thrown when a DeferredArray fails.
 * It contains a reference on the original array to sort success from
 * failures.
 */
public class DeferredArrayException extends Exception
{
	private static final long serialVersionUID = -1646374329395349797L;
	private DeferredArray deferred_ = null;
	
	public DeferredArrayException(DeferredArray d)
	{
		deferred_ = d;
	}
	
	public DeferredArray getDeferredArray()
	{
		return deferred_;
	}
	
	public Exception getFirstError() {
		List<Object> results = deferred_.getResults();
		return (Exception)results.get(getFirstErrorIndex());
	}
	
	public int getFirstErrorIndex() {
		List<Object> results = deferred_.getResults();
		for(int i = 0; i != results.size(); ++i) {
			if(results.get(i) instanceof Exception)
				return i;
		}
		return 0;
	}
	
	public String getMessage()
	{
		String msg = "";
		if(deferred_ == null)
			return msg;
		
		int result = 0;
		for(Iterator<Object> i = deferred_.getResults().iterator(); i.hasNext(); ++result)
		{
			Object o = i.next();
			if(!(o instanceof Throwable))
				continue;
			msg += "Result " + result + ": " + ((Throwable)o).toString() + "\n";
		}
		return msg;
	}
}