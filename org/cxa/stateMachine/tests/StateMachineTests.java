/**
 * Copyright 2014 opencxa.org
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
package org.cxa.stateMachine.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cxa.exec.updater.Updatable;
import org.cxa.exec.updater.Updater;
import org.cxa.stateMachine.State;
import org.cxa.stateMachine.StateMachine;
import org.cxa.timeUtils.TimeDiff;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christopher Armenio
 *
 */
public class StateMachineTests
{
	private enum TestState
	{
		STATE_1,
		STATE_2
	}
	
	private enum InternalState
	{
		INIT,
		ENTER,
		STATE,
		LEAVE
	}
	
	
	private class TransitionerUpdatable implements Updatable 
	{
		private final int targetNumTransitions;
		private int currNumTransitions = 0;
		
		private TransitionerUpdatable(int numTransitionsIn)
		{
			this.targetNumTransitions = numTransitionsIn;
		}
		
		private boolean isDone()
		{
			return (this.currNumTransitions >= this.targetNumTransitions);
		}
		
		@Override
		public boolean start() { return true; }

		@Override
		public void update()
		{
			if( this.currNumTransitions >= this.targetNumTransitions ) return;
			
			TestState targetState = null;
			switch( stateMachine.getCurrentState() )
			{
				case STATE_1:
					targetState = TestState.STATE_2;
					break;
					
				case STATE_2:
					targetState = TestState.STATE_1;
					break;
			}
			
			// now that we have our target state, transition
			stateMachine.transition(targetState);
			
			this.currNumTransitions++;
		}

		@Override
		public void stop() { }
	}
	
	
	private class TransitionCheckerState extends State<TestState>
	{
		private InternalState internalState = InternalState.INIT;
		
		public TransitionCheckerState(TestState idObjectIn) { super(idObjectIn); }
		
		@Override
		public void enter(TestState prevStateIn)
		{
			if( (this.internalState != InternalState.INIT) && (this.internalState != InternalState.LEAVE) )
			{
				failFromThread(String.format("incorrect internal state for .enter(): %s",this.internalState.toString()));
				return;
			}
			this.internalState = InternalState.ENTER;
			
			// see if we should randomly transition while in state (only for certain tests)
			if( enableInStateTransitions && (Math.random() >= 0.75) )
			{
				logger.trace("in-thread .enter() transition");
				TestState targetState = null;
				switch( stateMachine.getCurrentState() )
				{
					case STATE_1:
						targetState = TestState.STATE_2;
						break;
						
					case STATE_2:
						targetState = TestState.STATE_1;
						break;
				}
				
				// now that we have our target state, transition
				stateMachine.transition(targetState);
				stateMachine.update();
			}
		}
		
		@Override
		public void state()
		{
			if( (this.internalState != InternalState.ENTER) && (this.internalState != InternalState.STATE) )
			{
				failFromThread(String.format("incorrect internal state for .state(): %s", this.internalState.toString()));
				return;
			}
			if( this.internalState != InternalState.STATE ) this.internalState = InternalState.STATE;
			
			// see if we should randomly transition while in state (only for certain tests)
			if( enableInStateTransitions && (Math.random() >= 0.75) )
			{
				logger.trace("in-thread .state() transition");
				TestState targetState = null;
				switch( stateMachine.getCurrentState() )
				{
					case STATE_1:
						targetState = TestState.STATE_2;
						break;
						
					case STATE_2:
						targetState = TestState.STATE_1;
						break;
				}
				
				// now that we have our target state, transition
				stateMachine.transition(targetState);
				stateMachine.update();
			}
		}
		
		@Override
		public void leave(TestState nextStateIn)
		{
			if( (this.internalState != InternalState.ENTER) && (this.internalState != InternalState.STATE) )
			{
				failFromThread(String.format("incorrect internal state for .leave(): %s", this.internalState.toString()));
				return;
			}
			this.internalState = InternalState.LEAVE;
		}
	}
	
	
	private StateMachine<TestState> stateMachine = null;
	private Updater updater_stateMachine;
	private List<Updater> otherUpdaters;
	private Logger logger = LogManager.getLogger("stateMachineTester");
	
	private Semaphore sem_fail = null;
	private Throwable failureReason = null;
	
	private boolean enableInStateTransitions = false;
	
	
	@Before
	public void setup()
	{
		this.sem_fail = new Semaphore(0);
		this.failureReason = null;
		this.enableInStateTransitions = false;
		
		// setup our state machine
		this.stateMachine = new StateMachine<TestState>("dut");
		this.stateMachine.addState(new TransitionCheckerState(TestState.STATE_1));
		this.stateMachine.addState(new TransitionCheckerState(TestState.STATE_2));
		this.stateMachine.transition(TestState.STATE_1);
		this.stateMachine.update();
		
		this.updater_stateMachine = new Updater(this.stateMachine);
		this.otherUpdaters = new ArrayList<Updater>();
	}
	
	
	@After
	public void tearDown()
	{
		TimeDiff td_updaterStopper = new TimeDiff();
		
		// stop our stateMachine updater
		td_updaterStopper.setStartTime_now();
		if( this.updater_stateMachine.isRunning() )
		{
			this.updater_stateMachine.stop();
			while( this.updater_stateMachine.isRunning() )
			{
				if(td_updaterStopper.isElapsed(10000, TimeUnit.MILLISECONDS) ) fail("failed to stop state machine updater");
			}
		}
		
		// stop any other updater we created
		for( Updater currUpdater : this.otherUpdaters )
		{
			currUpdater.stop();
			td_updaterStopper.setStartTime_now();
			while( currUpdater.isRunning() )
			{
				if(td_updaterStopper.isElapsed(10000, TimeUnit.MILLISECONDS) ) fail("failed to stop other updater");
			}
		}
		
		try{ Thread.sleep(1000); } catch(InterruptedException e){ fail("interrupted while waiting for stateMachine updater to stop"); }
		assertFalse("stateMachine updater failed to stop", this.updater_stateMachine.isRunning());
		
		this.stateMachine = null;
		this.updater_stateMachine = null;
		this.sem_fail = null;
		this.failureReason = null;
	}
	
	
	@Test
	public void test_singleThreadUpdateAndTransition()
	{		
		this.stateMachine.transition(TestState.STATE_2);
		this.stateMachine.update();
		
		assertEquals("not in correct state", this.stateMachine.getCurrentState(), TestState.STATE_2);
	}
	
	
	@Test
	public void test_singleThreadUpdateAndTransition_timedState()
	{
		final int STATE_TIME_MS = 5000;
		
		this.stateMachine = new StateMachine<TestState>("dut");
		this.stateMachine.addState(new State<TestState>(TestState.STATE_1, TestState.STATE_2, STATE_TIME_MS, TimeUnit.MILLISECONDS){});
		this.stateMachine.addState(new State<TestState>(TestState.STATE_2){});
		
		// transition into our first state
		this.stateMachine.transition(TestState.STATE_2);
		this.stateMachine.update();
		
		// update our state for up to 1.25 * STATE_TIME_MS
		TimeDiff td_testTransition = new TimeDiff();
		while( !td_testTransition.isElapsed((long)(1.25 * STATE_TIME_MS), TimeUnit.MILLISECONDS) )
		{
			Thread.yield();
		}
		
		assertEquals("not in correct state", this.stateMachine.getCurrentState(), TestState.STATE_2);
	}
	
	
	@Test
	public void test_singleThreadUpdate_singleThreadTransition()
	{
		assertTrue("failed to start stateMachine updater", this.updater_stateMachine.start());
		while( !this.updater_stateMachine.isRunning() );
		
		// switch states a bunch
		Updater updater_trans = new Updater(new TransitionerUpdatable(10000));
		this.otherUpdaters.add(updater_trans);
		updater_trans.start();
		
		// wait for finish
		TimeDiff td_finish = new TimeDiff();
		while( !((TransitionerUpdatable)updater_trans.getUpdatable()).isDone() )
		{
			this.checkFailure();
			
			if( td_finish.isElapsed(60000, TimeUnit.MILLISECONDS) ) fail("failed to complete in a timely fashion");
			
			Thread.yield();
		}
		
		updater_trans.stop();
	}
	
	
	@Test
	public void test_singleThreadUpdate_dualSeperateThreadTransition()
	{
		assertTrue("failed to start stateMachine updater", this.updater_stateMachine.start());
		while( !this.updater_stateMachine.isRunning() );
		
		// setup two threads to transition the stateMachine
		Updater updater_trans1 = new Updater(new TransitionerUpdatable(10000));
		Updater updater_trans2 = new Updater(new TransitionerUpdatable(10000));
		this.otherUpdaters.add(updater_trans1);
		this.otherUpdaters.add(updater_trans2);
		updater_trans1.start();
		updater_trans2.start();
		
		// wait for them to finish
		TimeDiff td_finish = new TimeDiff();
		while(  !((TransitionerUpdatable)updater_trans1.getUpdatable()).isDone() &&
				!((TransitionerUpdatable)updater_trans2.getUpdatable()).isDone() )
		{
			if( td_finish.isElapsed(60000, TimeUnit.MILLISECONDS) ) fail("failed to complete in a timely fashion");
			
			Thread.yield();
		}
		
		updater_trans1.stop();
		updater_trans1.stop();
	}
	
	
	@Test
	public void test_singleThreadUpdate_singleThreadUpdateAndTransition()
	{
		assertTrue("failed to start stateMachine updater", this.updater_stateMachine.start());
		while( !this.updater_stateMachine.isRunning() );
		
		// switch states a bunch (and enable random in-state transitions)
		this.enableInStateTransitions = true;
		Updater updater_trans = new Updater(new TransitionerUpdatable(100000));
		this.otherUpdaters.add(updater_trans);
		updater_trans.start();
		
		// wait for finish
		TimeDiff td_finish = new TimeDiff();
		while( !((TransitionerUpdatable)updater_trans.getUpdatable()).isDone() )
		{
			this.checkFailure();
			
			if( td_finish.isElapsed(60000, TimeUnit.MILLISECONDS) ) fail("failed to complete in a timely fashion");
			
			Thread.yield();
		}
		
		updater_trans.stop();	
	}
	
	
	private void failFromThread(String reasonIn)
	{
		this.failureReason = new RuntimeException(reasonIn);
		this.sem_fail.release();
	}
	
	
	private void checkFailure()
	{
		if( this.sem_fail.tryAcquire() ) fail(this.failureReason.getMessage());
	}
}
