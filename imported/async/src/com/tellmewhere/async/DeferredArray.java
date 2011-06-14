package com.tellmewhere.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An ArrayList of result values as Object is returned if all call succeed.
 * A DeferredArrayException is thrown otherwise.
 */
public class DeferredArray extends Deferred
{
	private ArrayList<Deferred> pendings_ = new ArrayList<Deferred>();
	private ArrayList<Object> results_ = new ArrayList<Object>();
	private boolean failFast_ = false;
	private int failures_ = 0;
	
	public DeferredArray(boolean failFast, Deferred d1, Deferred d2) {
		this(failFast, Arrays.asList(new Deferred[] { d1, d2 }));
	}
	
	/**
	 * If failFast is false, the DeferredArray is run when all input Deferreds
	 * have been run, successfully or not. If failFast is true, it is run either
	 * when all input Deferreds have been successfully run, or when one of them
	 * fail, cancelling the remaining ones.
	 */
	public DeferredArray(boolean failFast, List<Deferred> inputs)
	{
		failFast_ = failFast;
		if(inputs.size() <= 0)
		{
			// Work as a DeferredRequest.success()
			call(null);
			return;
		}
		
		// The input deferred cannot be plugged at the same time we initialize
		// results_ and pendings_ because the can be run immediately and try
		// to read or modify the latter.
		for(Deferred d: inputs) {
			results_.add(null);
			pendings_.add(d);
		}
		
		for(int i=0; i != inputs.size(); ++i) {
			final int index = i;
			final Deferred request = inputs.get(i);
			request.addCallback(new DeferredCallback()
			{
				public Object onFailure(Exception e) throws Exception
				{
					return notifyResult(request, index, e);
				}

				public Object onSuccess(Object param) throws Exception 
				{
					return notifyResult(request, index, param);
				}	
			});	
		}
	}
	
	public List<Object> getResults()
	{
		return results_;
	}
	
	protected Object notifyResult(Deferred rq, int index, Object o)
	{
		if(hasMorePending())
		{
			pendings_.remove(rq);
			results_.set(index, o);			
			if(o instanceof Exception) {
				failures_++;
				if(failFast_) {
					for(Deferred d: pendings_)
						d.cancel();
					pendings_.clear();
					call(new DeferredArrayException(this));
					return o;
				}
			}
			
			if(!hasMorePending())
			{
				call(failures_ > 0 ? (Object)new DeferredArrayException(this) : results_);
			}
		}
		return o;
	}
	
	private boolean hasMorePending()
	{
		return pendings_.size() > 0;
	}
}