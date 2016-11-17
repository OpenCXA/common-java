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
package com.ovrengineered.collections;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * This Fifo implementation has a fixed maximum size and will either drop
 * newly queued items when full OR dequeue older items
 * (as configured by {@link OnFullAction}
 * 
 * @author Christopher Armenio
 */
public class FixedFifo<T>
{
	
	/**
	 * Used to specified what action should be taken on
	 * {@link FixedFifo#queue(Object)} when the Fifo is
	 * already full.
	 */
	public enum OnFullAction
	{
		/**
		 * Dequeue the oldest item and add the new item
		 */
		ON_FULL_DEQUEUE,
		
		/**
		 * Drop the new item leaving the queue unmodified
		 */
		ON_FULL_DROP
	}
	
	
	protected LinkedList<T> elements = new LinkedList<T>();
	private final int maxSize;
	private final OnFullAction onFullAction;
	
	
	/**
	 * Creates a fifo with a fixed maximum element size.
	 * 
	 * @param maxSize_elemsIn the maximum number of elements that
	 * 		can be stored in this fifo
	 * @param onFullActionIn the action that should be taken when
	 * 		calling {@link #queue(Object)} on a full fifo
	 */
	public FixedFifo(int maxSize_elemsIn, OnFullAction onFullActionIn)
	{
		this.maxSize = maxSize_elemsIn;
		this.onFullAction = onFullActionIn;
	}
	
	
	/**
	 * The "oldest" element in the fifo
	 * 
	 * @return the oldest element or NULL if the fifo is empty
	 */
	public T dequeue()
	{
		return (this.elements.size() != 0) ? this.elements.removeFirst() : null;
	}
	
	
	/**
	 * Determines the current number of elements in the fifo
	 * 
	 * @return the current number of elements in the fifo
	 */
	public int getCurrSize()
	{
		return this.elements.size();
	}
	
	
	/**
	 * Returns an iterator over all elements
	 *
	 * @return an iterator that iterates over all elements
	 */
	public Iterator<T> getIterator()
	{
		return this.elements.iterator();
	}
	
	
	/**
	 * Returns the first element added to this fifo
	 * 
	 * @return the first element added
	 */
	public T peek_first()
	{
		return this.elements.getFirst();
	}
	
	
	/**
	 * Returns the last element added to this fifo
	 * 
	 * @return the last element added
	 */
	public T peek_last()
	{
		return this.elements.getLast();
	}
	
	
	/**
	 * Returns the element at the specified index of the fifo
	 * 
	 * @param indexIn the index of the desired element
	 * 
	 * @return the element or NULL if index out of bounds
	 */
	public T peek_atIndex(int indexIn)
	{	
		if( indexIn >= this.elements.size() ) return null;
		return this.elements.get(indexIn);
	}
	
	
	/**
	 * Convenience method to return the element at the
	 * specified index, from the last inserted element
	 * of the queue (eg. the last element can be retrieved
	 * using {@code peek_atIndex_fromLast(0)}, the second-to-last
	 * element can be retrieved using {@code peek_atIndex_fromLast(1)})
	 * 
	 * @param indexIn the index from the last element in the queue
	 * 
	 * @return the element or NULL if index out of bounds
	 */
	public T peek_atIndex_fromLast(int indexIn)
	{
		if( indexIn >= this.elements.size() ) return null;
		return this.elements.get((this.elements.size()-1) - indexIn); 
	}
	
	
	/**
	 * Removes all elements from the fifo
	 */
	public void clear()
	{
		this.elements.clear();
	}
	
	
	/**
	 * Determines whether the fifo is full
	 * 
	 * @return true if the fifo is full, false if not
	 */
	public boolean isFull()
	{
		return (this.getCurrSize() == maxSize);
	}
	
	
	/**
	 * Adds an element to the last position in the Fifo
	 * 
	 * @param elemIn the element to add
	 * 
	 * @return true if the specified element was successfully
	 * 		added to fifo, false if not (ex. fifo was declared
	 * 		with {@link OnFullAction#ON_FULL_DROP}) 
	 */
	public boolean queue(T elemIn)
	{
		// if we're full, figure out what we should do
		if( this.isFull() )
		{
			switch( this.onFullAction )
			{
				case ON_FULL_DEQUEUE:
					this.dequeue();
					break;
					
				case ON_FULL_DROP:
					return false;
			}
		}
		
		// if we made it here, we should add our element
		this.elements.addLast(elemIn);
		return true;
	}
}
