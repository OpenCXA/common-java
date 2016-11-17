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
package com.ovrengineered.stateMachine;

import java.util.concurrent.TimeUnit;

import com.ovrengineered.timeUtils.TimeDiff;


/**
 * This is a class which encapsulates a state
 * of a finite state machine. This class is 
 * generally extended using an anonymous class.
 *
 * @author Christopher Armenio
 */
public abstract class State<T>
{
	protected enum StateType
	{
		STATE_TYPE_STANDARD,
		STATE_TYPE_TIMED
	}
	
	
	private final T idObject;
	private final StateType stateType;
	
	
	private final T nextState;
	private final TimeDiff td_transition;
	private final Long stateTime;
	private final TimeUnit stateTimeUnit;
	
	
	/**
	 * Creates a standard state with the given "idObject".
	 *
	 * @param idObjectIn an unique object which can
	 * 		be used to distinguish this state from other states
	 * 		in the state machine. This object is generally 
	 * 		an enum value.
	 */
	public State(T idObjectIn)
	{
		if( idObjectIn == null ) throw new NullPointerException("idObject cannot be null");
		
		// save our references
		this.idObject = idObjectIn;
	
		this.stateType = StateType.STATE_TYPE_STANDARD;
		
		this.nextState = null;
		this.td_transition = null;
		this.stateTime = null;
		this.stateTimeUnit = null;
	}
	
	
	/**
	 * Creates a timed state with the given "idObject". Timed states
	 * automatically transition to the provided "next state" after
	 * a fixed amount of time.
	 * 
	 * @param idObjectIn an unique object which can
	 * 		be used to distinguish this state from other states
	 * 		in the state machine. This object is generally 
	 * 		an enum value.
	 * @param nextState_idObjectIn the idObject of the state to which
	 * 		this state should transition after the fixed amount of time
	 * @param stateTimeIn the time that should be spent in this time, in the
	 * 		time unit specified by {@code stateTimeUnitIn}
	 * @param stateTimeUnitIn
	 */
	public State(T idObjectIn, T nextState_idObjectIn, long stateTimeIn, TimeUnit stateTimeUnitIn)
	{
		if( idObjectIn == null ) throw new NullPointerException("idObject cannot be null");
		if( nextState_idObjectIn == null ) throw new NullPointerException("nextState_idObject cannot be null");
		
		// save our references
		this.idObject = idObjectIn;
	
		this.stateType = StateType.STATE_TYPE_TIMED;
		
		this.nextState = nextState_idObjectIn;
		this.td_transition = new TimeDiff();
		this.stateTime = stateTimeIn;
		this.stateTimeUnit = stateTimeUnitIn;	
	}
	
	
	/**
	 * Returns a string representation of this state
	 *
	 * @return a string representation of this state
	 */
	public String toString()
	{
		return this.idObject.toString();
	}
	
	
	/**
	 * This function is called by the owning state machine
	 * when first entering this state. This function is called
	 * exactly once per transition to this state.
	 * 
	 * @param prevStateIn the "idObject" for the state from which we transitioned 
	 * 		(may be null for first transition of any given StateMachine instance)
	 * 
	 * @note this function should be overridden by the user (if desired)
	 */
	public void enter(T prevStateIn){};
	
	
	/**
	 * This function is called by the owning state machine
	 * continuously as long as the state machine remains in this
	 * state.
	 * 
	 * @note this function should be overridden by the user (if desired)
	 */
	public void state(){};
	
	
	/**
	 * This function is called by the owning state machine
	 * when leaving this state. This function is called
	 * exactly once per transition out of this state.
	 * 
	 * @param nextStateIn the "idObject" for the state to which we will be transitioning
	 * 
	 * @note this function should be overridden by the user (if desired)
	 */
	public void leave(T nextStateIn){};
	
	
	/**
	 * Returns the "idObject" which can be used to
	 * uniquely identify this state amongst other
	 * states in the state machine.
	 *
	 * @return the "idObject" unique to this state
	 */
	protected T getIdObject()
	{
		return this.idObject;
	}
	
	
	/**
	 * Returns this type of this state (normal or timed)
	 * 
	 * <p>
	 * Should only be used internally by {@link StateMachine}
	 * </p>
	 * 
	 * @return the type of this state
	 */
	protected StateType getStateType()
	{
		return this.stateType;
	}
	
	
	/**
	 * Resets the state transition timer (for timed states only)
	 * 
	 * <p>
	 * Should only be used internally by {@link StateMachine}
	 * </p>
	 * 
	 * @throws IllegalStateException if this state is not a timed state
	 */
	protected void resetStateTimer()
	{
		if( this.td_transition == null ) throw new IllegalStateException("state transition TimeDiff is null! is this a TimedState?");
		
		this.td_transition.setStartTime_now();
	}
	
	
	/**
	 * Determines if it is time for this timed state to
	 * transition to its stored next state.
	 * (for timed states only)
	 *
	 * <p>
	 * Should only be used internally by {@link StateMachine}
	 * </p>
	 * 
	 * @return true if it is time to transition to the next state, false if not
	 * 
	 * @throws IllegalStateException if this state is not a timed state
	 */
	protected boolean isStateTimeElapsed()
	{
		if( this.td_transition == null ) throw new IllegalStateException("state transition TimeDiff is null! is this a TimedState?");
		
		return this.td_transition.isElapsed(this.stateTime, this.stateTimeUnit);
	}
	
	
	/**
	 * Returns the target state for transition after the specified
	 * time has expired.
	 * (for timed states only)
	 * 
	 * <p>
	 * Should only be used internally by {@link StateMachine}
	 * </p>
	 * 
	 * @return the target state for timed transition
	 * 
	 * @throws IllegalStateException if this state is not a timed state
	 */
	protected T getNextState()
	{
		if( this.td_transition == null ) throw new IllegalStateException("state transition TimeDiff is null! is this a TimedState?");
		
		return this.nextState;
	}
}
