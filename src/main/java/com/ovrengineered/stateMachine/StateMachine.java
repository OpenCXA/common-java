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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ovrengineered.exec.updater.Updatable;
import com.ovrengineered.misc.ExceptionUtils;
import com.ovrengineered.stateMachine.State.StateType;


/**
 * <p>
 * This is a utility class which provides basic finite-state-machine
 * functionality.
 * </p>
 *
 * <STRONG>Example Usage:</STRONG>
 * <pre>
 * {@code
 * private enum ExState {STATE_IDLE, STATE_AWAKE};
 * ...
 * StateMachine fsm = new StateMachine("exampleStateMachine");
 * ...
 * stateMachine.addState(new State<ExState>(ExState.STATE_IDLE)
 * {
 *    public void enter() { System.out.println("enter"); }
 *    
 *    public void state() { System.out.println("state"); }
 *    
 *    public void leave() { System.out.println("leave"); }
 * });
 * ...
 * while(1)
 * {
 *    stateMachine.update();
 *    Thread.yield();
 * }
 * }
 * </pre>
 *
 * @author Christopher Armenio
 */
public class StateMachine<T> implements Updatable
{
	private static final int MAX_NESTED_TRANSITIONS = 5;
	
	private Logger logger;
	private Map<T, State<T>> states = new HashMap<T, State<T>>();
	
	private volatile State<T> currState = null;
	private volatile State<T> nextState = null;
	
	
	private volatile boolean isInUpdate = false;
	private volatile int nestedTransitions = 0;
	
	
	/**
	 * Creates a state machine with the given name.
	 *
	 * @param nameIn the name of the state machine (used for logging)
	 */
	public StateMachine(String nameIn)
	{
		this.logger = LogManager.getLogger("fsm." + nameIn);
	}
	
	
	/**
	 * Adds the given state to the state machine. If the state machine
	 * is newly created, it will automatically transition into the first
	 * state added upon a call to {@link #update}. To override this
	 * initial state call {@link #transition} with your desired initial
	 * state before any calls to {@link #update}.
	 *
	 * @param stateIn the state to add to the state machine. The
	 *		state should have an IdObject which is unique WRT all states
	 *		in the state machine.
	 *
	 * @throws IllegalArgumentException if the provided state's IdObject has
	 *		already been added to this state machine.
	 */
	public void addState(State<T> stateIn) throws IllegalArgumentException
	{	
		if( stateIn == null ) throw new IllegalArgumentException("state must NOT be null");
		
		// make sure we don't add duplicate states
		if( this.states.containsKey(stateIn.getIdObject()) )
		{
			throw new IllegalArgumentException(String.format("trying to add a duplicate state: '%s'", stateIn.toString()));
		}
		
		// add the state to our map
		this.states.put(stateIn.getIdObject(), stateIn);
		
		// if we're currently not in a known state, enter this state (when update is called)
		if( this.currState == null ) this.transition(stateIn.getIdObject());
	}
	
	
	/**
	 * @return the "idObject" of the current state of the state machine
	 * 		(as provided to {@link State#State(Object)} or NULL if
	 * 		the state machine is newly created and has not transitioned
	 *		into any new state
	 */
	public T getCurrentState()
	{
		return (this.currState != null) ? this.currState.getIdObject() : null;
	}
	
	
	/**
	 * Instructs the state machine to transition into the
	 * provided state upon the next call to {@link #update()}.
	 * Note: this function does nothing but flag ths state machine
	 * to transition upon the next call to {@link #update()}. Until
	 * {@link #update()} is called, the state machine will not actually
	 * enter the new state. Therefore, multiple calls to this function
	 * before a call to {@link #update()} will result in the state machine
	 * transitioning exactly once into the state indicated by the last
	 * call to this function.
	 *
	 * @param newStateObjectIn the IdObject of the target state
	 *
	 * @throws IllegalArgumentException if the provided IdObject does
	 *		not correspond to a known state
	 */
	public synchronized void transition(T newStateObjectIn) throws IllegalArgumentException
	{
		State<T> newNextState = this.states.get(newStateObjectIn);
		if( newNextState == null ) throw new IllegalArgumentException("unknown target state");
		
		// we have a valid new state...mark for transition
		this.nextState = newNextState;
	}
	
	
	/**
	 * Instructs the state machine to transition into the provided state
	 * immediately. This function will call any applicable {@link State#leave(Object)}
	 * and {@link State#enter(Object)} methods before this method returns.
	 * 
	 * Therefore, extreme care should be taken when calling within a state callback
	 * ({@link State#enter(Object)}, {@link State#state()}, {@link State#leave(Object)}).
	 * Specifically, ensure that:
	 * <ul>
	 *    <li>this method is the last action taken before leaving the callback (via return)</li>
	 *    <li>
	 *       calls to this method within a state callback do not lead to state-loops
	 *       (eg. a set of states that immediately transition amongst each other in a closed cycle)
	 *    </li>
	 * </ul>
	 * 
	 * @param newStateObjectIn the IdObject of the target state
	 * 
	 * @throws IllegalArgumentException if the provided IdObject does
	 *		not correspond to a known state
	 * @throws IllegalStateException if a state loop is detected (eg. number of nested transitions
	 * 		exceeds {@link #STATE_LOOP_MAX_NESTED_UPDATES})
	 */
	public synchronized void transition_now(T newStateObjectIn) throws IllegalArgumentException, IllegalStateException
	{
		this.nestedTransitions++;
		if( this.nestedTransitions > MAX_NESTED_TRANSITIONS )
			throw new IllegalStateException("too many nested calls to StateMachine.transition_now()...likely state loop detected");
		
		this.transition(newStateObjectIn);
		this.update_priv();
		
		this.nestedTransitions--;
	}
	
	
	/**
	 * This function stimulates the state machine and causes any changes to
	 * the state machine state. This function should be called on a regular
	 * basis, usually in a background loop.
	 * 
	 * <p>
	 * This function must <b>NOT</b> be called from within a state callback
	 * as it can lead to state loops. If you absolutely MUST transition within
	 * a state callback, use {@link #transition_now(Object)} as it has built-in
	 * protections for state loops.
	 * </p>
	 */
	@Override
	public synchronized void update() throws IllegalStateException
	{
		if( this.isInUpdate || (this.nestedTransitions > 0) ) throw new IllegalStateException("update must not be called from within a state callback...consider using StateMachine.transition_now()");
		
		this.isInUpdate = true;
		this.update_priv();
		this.isInUpdate = false;
	}
	

	/*
	 * (non-Javadoc)
	 * @see com.ovrengineered.misc.updater.Updatable#start()
	 */
	@Override
	public boolean start() { return true; }


	/*
	 * (non-Javadoc)
	 * @see com.ovrengineered.misc.updater.Updatable#stop()
	 */
	@Override
	public void stop() { }
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this.logger.getName();
	}
	
	
	private synchronized void update_priv()
	{
		if(this.nextState != null)
		{	
			// call the leave function of our old state
			try
			{
				if(this.currState != null) this.currState.leave(nextState.getIdObject());
			}
			catch(Exception e)
			{
				this.logger.warn(ExceptionUtils.getExceptionLogString(e, ExceptionUtils.MSG_OPERATION_WILL_CONTINUE));
			}
			
			// actually do the transition
			State<T> prevState = this.currState;
			this.currState = this.nextState;
			this.nextState = null;
			if( prevState != null ) this.logger.info(String.format("new state: '%s'", this.currState.toString()));
			else this.logger.info(String.format("initial state: '%s'", this.currState.toString()));
		
			// call the enter function of our new state
			try
			{
				// reset our state timer (if we are a timed state)
				if( this.currState.getStateType() == StateType.STATE_TYPE_TIMED ) this.currState.resetStateTimer();
				
				this.currState.enter((prevState != null) ? prevState.getIdObject() : null);
			}
			catch(Exception e)
			{
				this.logger.warn(ExceptionUtils.getExceptionLogString(e, ExceptionUtils.MSG_OPERATION_WILL_CONTINUE));
			}
		}
		else if( this.currState != null )
		{
			// we're just sitting in a state somewhere...
			try
			{
				// if it's a timed state, make sure we only stay here for the configured time period...
				// otherwise, call our state callback as needed
				if( this.currState.getStateType() == StateType.STATE_TYPE_TIMED )
				{
					if( this.currState.isStateTimeElapsed() )
					{
						this.transition(this.currState.getNextState());
						return;
					}
				}
				
				// even if it's a timed state, it still needs to be updated...
				this.currState.state();
			}
			catch(Exception e)
			{
				this.logger.warn(ExceptionUtils.getExceptionLogString(e, ExceptionUtils.MSG_OPERATION_WILL_CONTINUE));
			}
		}
	}
}
