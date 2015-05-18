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
package org.cxa.collections;

import java.util.Iterator;


/**
 * This is a fixed-size fifo (declared with {@link FixedFifo.OnFullAction#ON_FULL_DEQUEUE})
 * which has the ability to perform statistics on the numbers contained within
 * the fifo (eg. averaging, etc) 
 * 
 * @author Christopher Armenio
 */
public class StatisticalWindowFifo<T extends Number> extends FixedFifo<T>
{
	/**
	 * Creates a fifo with a fixed maximum element size.
	 * 
	 * @param maxSize_elemsIn the maximum number of elements that
	 * 		can be stored in this fifo
	 */
	public StatisticalWindowFifo(int maxSize_elemsIn)
	{
		super(maxSize_elemsIn, OnFullAction.ON_FULL_DEQUEUE);
	}
	
	
	/**
	 * Calculates the average of the numerical elements in this fifo.
	 * Average calculation is done with an intermediary {@link Double}
	 * value and converted to the return type using the appropriate
	 * {@link Number#byteValue()} {@link Number#floatValue()}, etc
	 * function call
	 * 
	 * @return the numerical average of the elements contained within
	 * 		this fifo or NULL if no elements are present
	 */
	@SuppressWarnings("unchecked")
	public T getAverage()
	{
		Double retVal_dbl = Double.valueOf(0);
		
		Iterator<T> it = this.elements.iterator();
		int numElements = 0;
		Class<T> retClass = null;
		while( it.hasNext() )
		{
			T currElem = it.next();
			
			retVal_dbl += currElem.doubleValue();
			numElements++;
			
			if( retClass == null ) retClass = (Class<T>)currElem.getClass();
		}
		
		// if there are no elements, we cannot average
		if( numElements == 0 ) return null;
		
		// complete our average
		retVal_dbl /= numElements;
		
		// now setup our return class
		T retVal = null;

		if( retClass.equals(Byte.class) ) retVal = (T)Byte.valueOf(retVal_dbl.byteValue());
		else if( retClass.equals(Double.class) ) retVal = (T)retVal_dbl;
		else if( retClass.equals(Float.class) ) retVal = (T)Float.valueOf(retVal_dbl.floatValue());
		else if( retClass.equals(Integer.class) ) retVal = (T)Integer.valueOf(retVal_dbl.intValue());
		else if( retClass.equals(Long.class) ) retVal = (T)Long.valueOf(retVal_dbl.longValue());
		else if( retClass.equals(Short.class) ) retVal = (T)Short.valueOf(retVal_dbl.shortValue());
		else throw new RuntimeException("Unsupported parameterized class type...must be a standard java.Number");
		
		return retVal;
	}

}
