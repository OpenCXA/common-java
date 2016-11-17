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

/**
 * This is an interface for an execution object which is similar to an asynchronous
 * thread. None of the functions below should be infinitely blocking, however there is
 * no contract that all functions must be 100% non-blocking. It should be assumed that
 * none of the below functions will return immediately.
 * 
 * <p>
 * Execution should usually follow this order:
 * <ol>
 *    <li>{@link #start()}</li>
 *    <li>{@link #update()} -- 0 or more times</li>
 *    <li>{@link #stop()}</li>
 * <ol>
 * Multiple cycles of the above execution order are allowable
 * 
 * @author Christopher Armenio
 */
public interface Updatable
{
	/**
	 * This function notifies the Updatable that execution is about to begin
	 * and gives the Updatable an option to report its desire to abort execution
	 * (by returning false).
	 * 
	 * <p>
	 * Should be called before any calls to {@link #update()} or {@link #stop()}.
	 * </p>
	 * 
	 * @return true if execution should continue, false if the Updatable is not in
	 * proper state to handle calls to {@link #update()} or {@link #stop()}
	 */
	public abstract boolean start();
	
	
	/**
	 * This is the primary work function for the Updatable. It should be
	 * designed to be repeatedly called on a regular basis (eg. not indefinitely
	 * blocking).
	 */
	public abstract void update();
	
	
	/**
	 * This function notifies the Updatable that execution is stopping and
	 * that it should clean up any required resources. After this function
	 * returns, it should be assumed that this Updatable will receive
	 * no further function calls.
	 * 
	 * <p>
	 * Note on the above assumption: that assumption is 100% correct. However,
	 * it is generally good practice to design your Updatable such that, after
	 * a call to {@link #stop()}, a call to {@link #start()} will return the
	 * Updatable to proper running state.
	 * </p>
	 */
	public abstract void stop();
}
