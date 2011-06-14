package com.tellmewhere.async;

import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.Looper;

import com.tellmewhere.sdk.log.Logger;

public class Reactor {
	private static final int THREAD_COUNT = 3;
	private static int freeCount_ = THREAD_COUNT;
	private static final Logger log = new Logger(Reactor.class);
	private static final HashMap<Thread, Handler> handler_ = new HashMap<Thread, Handler>();
	private static final ExecutorService executor_ = Executors.newFixedThreadPool(THREAD_COUNT);
	private static final ScheduledExecutorService timer_ = Executors.newSingleThreadScheduledExecutor();
	private static final PriorityQueue<Action> queue_ = new PriorityQueue<Action>(50, new Comparator<Action>() {
	    public int compare(Action a, Action b) {
		    	return b.priority - a.priority;
			}
		});
	
	/**
	 * @return a Handler for the current calling thread.
	 */
	static synchronized Handler getHandler() {
		Thread t = Thread.currentThread();
		if(!handler_.containsKey(t)) {
			Looper looper = Looper.myLooper();
			if(looper == null && log.isErrorEnabled())
				log.error("Caller thread has no Looper");
			handler_.put(t, new Handler());
		}
		return handler_.get(t);
	} 

	/**
	 * Execute passed Reactor.Action in another thread,
	 * then call back return Deferred in calling thread. Caller must have a Looper.
	 * Reactor.Action *must not* call callLater() recursively, it will fail since
	 * pooled threads do not have loopers. Instead, sequence the asynchronous
	 * actions from the calling side.
	 */
	public static Deferred call(Action action) {
		if(action == null)
			throw new NullPointerException("action");
		log.debug("Queuing '" + action.name);
		queue_.add(action);
		executeNext();
		return action.deferred;
	}
	
	private static void executeNext() {
		while(true) {
			Action a = queue_.peek();
			if(a == null)
				break;
			if(a.deferred.isCancelled()) {
				queue_.poll().deferred.call(null);
				continue;
			}
			if(freeCount_ <= 0 || (a.priority < NORMAL && freeCount_ <= 1))
				break;
			a.deferred.addCallback(new DeferredBoth() {
				public Object onBoth(Object param) {
					freeCount_++;
					executeNext();
					return param;
				}
			});
			freeCount_--;
			executor_.submit(queue_.poll());
		}
	}
	
	/**
	 * Call back in calling thread after the given delay
	 * @param delay ms
	 */
	public static Deferred schedule(long delay) {
		final Action action = new Action.Empty();
		timer_.schedule(action, delay, TimeUnit.MILLISECONDS);
		return action.deferred;
	}
	
	public static final char HIGH = 30;
	public static final char NORMAL = 20;
	public static final char LOW = 10;
	
	public static abstract class Action implements Runnable{
		public final char priority;
		public final String name;
		private final Deferred deferred;
		
		public Action(String name) {
			this(name, NORMAL);
		}
		
		public Action(String name, char priority) {
			this.name = name;
			deferred = new Deferred(true, name);
			this.priority = priority;
		}
		
		/***
		 * Can be called from another thread
		 */
		public final void run() {
			try {
				deferred.call(call());
			}catch(Exception e) {
				deferred.call(e);
			}
		}
		
		public abstract Object call() throws Exception;
		
		public static class Empty extends Action{
			public Empty() {super(null);}
			public Object call() {return null;}
		}
	}
}
