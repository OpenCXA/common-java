/**
 * Copyright 2013 opencxa.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cxa.exec.simpleThread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cxa.misc.ExceptionUtils;


/**
 * A simple class for simplifying thread creation/termination
 * 
 * @author Christopher Armenio
 */
public abstract class SimpleThread
{
	private static final int THREAD_STOP_TIMEOUT_MS = 5000;
	
	protected final Logger logger;
	
	private Thread thread = null;
	private volatile boolean keepRunning = false;
	
	
	/**
	 * Creates the SimpleThread object (but does not yet start it)
	 */
	public SimpleThread()
	{
		this.logger = LogManager.getLogger(this.toString());
	}
	
	
	/**
	 * Starts the thread
	 * 
	 * @return if the thread was successfully started
	 */
	public boolean start()
	{
		if( this.isRunning() )
		{
			this.logger.warn("attempted to start an already running thread");
			return false;
		}
		
		// setup our thread
		this.thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				logger.trace("thread started");
				
				try { SimpleThread.this.run(); }
				catch(Exception e)
				{
					logger.warn(ExceptionUtils.getExceptionLogString(e, "thread will now stop"));
					keepRunning = false;
				}
				 
				logger.trace("thread stopped");
			}
		}, this.logger.getName());
	
		
		// if we made it here, everything looks good for a thread start
		this.keepRunning = true;
		this.thread.start();
		
		return true;
	}
	
	
	/**
	 * Stops the currently running thread (if any) and
	 * waits for it to join.
	 * 
	 * @throws RuntimeException if the thread could not be joined
	 * in a reasonable amount of time
	 */
	public void stop() throws RuntimeException
	{
		// we're purposefully silent here
		if( !this.keepRunning ) return;
		
		// signal that we should stop (nicely)
		this.logger.trace("stop requested, asking nicely");
		this.keepRunning = false;
		
		// if we are calling from a different thread, make sure our worker thread joins
		if( !this.thread.equals(Thread.currentThread()) )
		{
			// wait for a bit to see if it stops
			try { this.thread.join(THREAD_STOP_TIMEOUT_MS); }
			catch(InterruptedException e)
			{
				this.logger.warn("interrupted while waiting for thread to stop (nicely)...getting rough");
			}
		
			// see if we were successful
			if( !this.thread.isAlive() ) return;	
			
			// if we made it here, we need to get rough
			this.logger.trace("thread didn't respond, getting rough");
			this.thread.interrupt();
			
			// wait for a bit to see if it stops
			try{ Thread.sleep(THREAD_STOP_TIMEOUT_MS); } catch(InterruptedException e) { }
			
			// if it's still alive when we get here, we pretty much failed
			if( this.thread.isAlive() )
			{
				this.logger.error("zombie thread!");
				throw new RuntimeException("zombie thread!");
			}
		}
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return String.format("%s.%s", this.getClass().getSimpleName(), System.identityHashCode(this));
	}
	
	
	/**
	 * @return true if this state machine is currently running, false if not
	 */
	public boolean isRunning()
	{
		return (this.thread != null ) ? this.thread.isAlive() : false;
	}
	
	
	/**
	 * Can be used by subclasses in their respected {@link #run()} function to
	 * determine when to return (based upon requests to stop execution)
	 * 
	 * @return true if the thread should stop execution
	 */
	protected boolean shouldKeepRunning()
	{
		return this.keepRunning;
	}
	
	
	/**
	 * Must be implemented by the subclass. This method is expected to not return
	 * until all execution is complete OR {@link #shouldKeepRunning()} becomes false.
	 */
	public abstract void run();
}
