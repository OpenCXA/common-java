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
package com.ovrengineered.exec.updater;

import com.ovrengineered.exec.simpleThread.SimpleThread;
import com.ovrengineered.misc.ExceptionUtils;


/**
 * This is a convenience class for running an updatable
 * object in its own, separate thread.
 *
 * @author Christopher Armenio
 */
public class Updater extends SimpleThread
{	
	private final Updatable updatable;
	
	
	/**
	 * Creates a Updater which will stimulate
	 * the provided Updatable
	 *
	 * @param updatableIn the updatable to update
	 */
	public Updater(Updatable updatableIn)
	{
		this.updatable = updatableIn;
		this.logger.debug("owns " + this.updatable.toString() );
	}
	
	
	/**
	 * Starts a thread which will continuously call the
	 * {@link Updatable#update} method.
	 * After each call, the thread will yield.
	 *
	 * @return true if the updatable and thread were started successfully
	 * 		false if the updatable was not ready to start or the 
	 * 		updatable was already running
	 */
	@Override
	public boolean start()
	{
		if( this.isRunning() )
		{
			this.logger.warn("attempted to start an already running updater");
			return false;
		}
		
		
		// call our start function
		this.logger.trace("start requested, calling '.start()' function");
		try
		{
			if( !this.updatable.start() )
			{
				this.logger.warn("updatable reported unable to start, thread start aborted");
				return false; 
			}
		}
		catch(Exception e)
		{
			this.logger.warn(ExceptionUtils.getExceptionLogString(e, "thread start aborted"));
			return false;
		}
	
		
		// if we made it here, everything looks good for a thread start
		this.logger.trace("'.start()' reports good-to-go, starting");
		return super.start();
	}
	
	
	/**
	 * @return the Updatable that is associated with this updater
	 */
	public Updatable getUpdatable()
	{
		return this.updatable;
	}


	/*
	 * (non-Javadoc)
	 * @see com.ovrengineered.exec.simpleThread.SimpleThread#run()
	 */
	@Override
	public void run()
	{	
		while(this.shouldKeepRunning() && !Thread.interrupted() )
		{
			try { updatable.update(); }
			catch(Exception e)
			{
				logger.warn(ExceptionUtils.getExceptionLogString(e, "updater will now stop"));
				break;
			}
			
			Thread.yield();
		}
		
		logger.trace("calling '.stop()' function");
		try{ updatable.stop(); } catch(Exception e) { logger.warn(ExceptionUtils.getExceptionLogString(e)); } 
	}
}
