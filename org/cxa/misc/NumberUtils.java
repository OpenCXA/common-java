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
package org.cxa.misc;

/**
 * This class contains utility functions for working with /
 * manipulating numbers.
 * 
 * @author Christopher Armenio
 */
public class NumberUtils
{
	private static final Double MAX_CAST_DELTA = .000001;
	
	
	/**
	 * Method for intelligently converting between Number classes.
	 * This method will attempt to convert the number with NO information
	 * loss:
	 * <p>
	 * <b>Fractional Number -> Integer Number:</b>
	 * If the fractional number (Double, Float) has a fractional component
	 * this function will return null.
	 * <p>
	 * <b>Fraction Number -> Fractional Number</b>
	 * If the difference between the result and the input is greater than {@link NumberUtils#MAX_CAST_DELTA}
	 * this function will return null.
	 * <p>
	 * <b>General</b>
	 * If the conversion will cause an overflow/underflow of the target number
	 * type, this function will return null.
	 * 
	 * 
	 * @param valueIn the number to convert
	 * @param toClassIn the desired class of the number
	 * 
	 * @return the input number converted to the target class, or null
	 * 		if strict conversion is impossible
	 */
	public static Number castToClass_strict(Number valueIn, Class<? extends Number> toClassIn)
	{
		if( valueIn == null ) return null;
		
		if( valueIn.getClass() == toClassIn ) return valueIn;
		
		boolean hasFractionalComponent = (Math.floor(valueIn.doubleValue()) != valueIn.doubleValue());
		
		// we can't convert a fractional component to an integer
		if( hasFractionalComponent &&
		  (toClassIn.equals(Byte.class) || toClassIn.equals(Short.class) ||
		   toClassIn.equals(Integer.class) || toClassIn.equals(Long.class)) ) return null;
		
		// now convert to a Double as our base class (everything _should_ be representable)
		Double base = valueIn.doubleValue();
		
		// now check our ranges or delta (based up on our output type)
		if( toClassIn.equals(Byte.class) && (base < Byte.MIN_VALUE) || (base > Byte.MAX_VALUE) ) return null;
		else if( toClassIn.equals(Short.class) && (base < Short.MIN_VALUE) || (base > Short.MAX_VALUE) ) return null;
		else if( toClassIn.equals(Integer.class) && (base < Integer.MIN_VALUE) || (base > Integer.MAX_VALUE) ) return null;
		else if( toClassIn.equals(Long.class) && (base < Long.MIN_VALUE) || (base > Long.MAX_VALUE) ) return null;
		else if( toClassIn.equals(Float.class) && (Math.abs((double)base.floatValue() - base) > MAX_CAST_DELTA) ) return null;
		
		// now actually perform our conversion
		Number retVal = null;
		{
			if( toClassIn.equals(Byte.class) ) retVal = Byte.valueOf(base.byteValue());
			else if( toClassIn.equals(Short.class) ) retVal = Short.valueOf(base.shortValue());
			else if( toClassIn.equals(Integer.class) ) retVal = Integer.valueOf(base.intValue());
			else if( toClassIn.equals(Long.class) ) retVal = Long.valueOf(base.longValue());
			else if( toClassIn.equals(Float.class) ) retVal = Float.valueOf(base.floatValue());
			else if( toClassIn.equals(Double.class) ) retVal = Double.valueOf(base.doubleValue());
		}
		return retVal;
	}
}
