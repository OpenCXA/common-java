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
package org.cxa.timeUtils.tests;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.cxa.timeUtils.TimeDiff;
import org.junit.Test;

/**
 * @author Christopher Armenio
 */
public class TimeDiffTests
{	
	private static final double MAX_ERROR_PCNT = 20.0;
	
	
	@Test
	public void test_isElapsed_ms100()
	{
		testTimeout(100);
	}
	
	
	@Test
	public void test_isElapsed_ms1000()
	{
		testTimeout(1000);
	}
	
	
	@Test
	public void test_isElapsed_ms10000()
	{
		testTimeout(10000);
	}
	
	
	@Test
	public void test_isElapsed_ms100000()
	{
		testTimeout(100000);
	}

	
	@Test
	public void test_isElapsed_recurring()
	{
		final int LOOP_TIMEOUT_MS = 1000;
		final int LOOP_ITERATIONS = 5;
		
		TimeDiff td_test = new TimeDiff();
		
		// use recurring wait to wait 5 times for the specified time
		int i = 0;
		long startTime_ms = System.currentTimeMillis();
		td_test.setStartTime_now();
		while(true)
		{
			if( td_test.isElapsed_recurring(LOOP_TIMEOUT_MS, TimeUnit.MILLISECONDS) )
			{
				i++;
				if( i == LOOP_ITERATIONS ) break;
			}
		}
		long endTime_ms = System.currentTimeMillis();
		long elapsedTime_ms = endTime_ms - startTime_ms;
		
		// make sure everything makes sense
		assertTrue(String.format("currTimeElapsed is out of bounds[%.1f%%]: loopTime_ms: %d, actualLoopTime_ms: %d",
				Math.abs((double)((LOOP_TIMEOUT_MS * LOOP_ITERATIONS) - elapsedTime_ms) / (double)(LOOP_TIMEOUT_MS * LOOP_ITERATIONS)) * 100.0, (LOOP_TIMEOUT_MS * LOOP_ITERATIONS), elapsedTime_ms),
				(Math.abs((double)((LOOP_TIMEOUT_MS * LOOP_ITERATIONS) - elapsedTime_ms) / (double)(LOOP_TIMEOUT_MS * LOOP_ITERATIONS) * 100.0) < MAX_ERROR_PCNT));
	}
	
	
	private void testTimeout(long timeout_msIn)
	{
		TimeDiff td_test = new TimeDiff();
		
		// set our start time
		long startTime_ns = System.nanoTime();
		long startTime_ms = System.currentTimeMillis();
		td_test.setStartTime_now();
		
		// do our wait
		while(!td_test.isElapsed(timeout_msIn, TimeUnit.MILLISECONDS)){}
		
		// get our end time
		long endTime_ns = System.nanoTime();
		long endTime_ms = System.currentTimeMillis();
		
		// calculate our elapsed times
		long nanoTimeElapsed = endTime_ns - startTime_ns;
		long currTimeElapsed = endTime_ms - startTime_ms;
		
		// compare
		assertTrue(String.format("nanoTimeElapsed is out of bounds [%.1f%%]: reqTimeout_ms: %d, actualTimeout_ms: %.2f", 
				Math.abs((timeout_msIn - (nanoTimeElapsed / 1.0E6)) / timeout_msIn) * 100.0, timeout_msIn, (nanoTimeElapsed / 1000000.0)), 
				(Math.abs((timeout_msIn - (nanoTimeElapsed / 1.0E6)) / timeout_msIn) * 100.0) < MAX_ERROR_PCNT);
		assertTrue(String.format("currTimeElapsed is out of bounds [%.1f%%]: reqTimeout_ms: %d, actualTimeout_ms: %d",
				Math.abs((timeout_msIn - currTimeElapsed) / timeout_msIn) * 100.0, timeout_msIn, currTimeElapsed),
				(Math.abs((timeout_msIn - currTimeElapsed) / timeout_msIn) * 100.0) < MAX_ERROR_PCNT);
	}
}
