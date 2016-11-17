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
package com.ovrengineered.timeUtils;


import java.util.concurrent.TimeUnit;

/**
 * <p>
 * This is a utility class for non-blocking waiting using
 * configurable time periods. This class utilizes System.nanoTime(),
 * thus resolution should be reasonably accurate to nanosecond levels.
 * </p>
 *
 * <STRONG>Example Usage:</STRONG>
 * <pre>
 * {@code
 * TimeDiff td_break = new TimeDiff();
 * ...
 * td_break.setStartTime_now();
 * while(true)
 * {
 *    // do something here
 *    
 *    if( td_break.isElapsed(1000, TimeUnit.MILLISECONDS) ) break;
 * }
 * }
 * </pre>
 *
 * @author Christopher Armenio
 */
public class TimeDiff
{
	private long startTime_ns = System.nanoTime();
	
	
	/**
	 * Sets the start time for this TimeDiff
	 */
	public void setStartTime_now()
	{
		this.startTime_ns = System.nanoTime();
	}
	
	
	/**
	 * Returns the time elapsed since instantiation OR a call to
	 * {@link #setStartTime_now()}
	 *
	 * @return the elapsed time in milliseconds
	 */
	public long getElapsedTime_ms()
	{
		return (System.nanoTime() - this.startTime_ns) / 1000000;
	}
	
	
	/**
	 * Determines if the provided time period has elapsed since a
	 * call to {@link #setStartTime_now()}
	 *
	 * @param valueIn the number of seconds/milliseconds/etc
	 * @param tuIn the time unit associated with valueIn
	 *
	 * @return true if the provided time period has elapsed
	 */
	public boolean isElapsed(long valueIn, TimeUnit tuIn)
	{
		return ((System.nanoTime() - this.startTime_ns) >= TimeUnit.NANOSECONDS.convert(valueIn, tuIn)); 
	}
	
	
	/**
	 * This is a convenience method for situations where
	 * {@link #isElapsed} is called on a regular basis. Upon
	 * an elapsed time period, this function will automatically reset 
	 * the start time of the TimeDiff using {@link #setStartTime_now}.
	 * 
	 * @param valueIn the number of seconds/milliseconds/etc
	 * @param tuIn the time unit associated with valueIn
	 *
	 * @return true if the provided time period has elapsed
	 */
	public boolean isElapsed_recurring(long valueIn, TimeUnit tuIn)
	{
		boolean retVal = isElapsed(valueIn, tuIn);
		
		if( retVal ) this.setStartTime_now();
		return retVal;
	}
}
