/**
 * Copyright 2015 opencxa.org
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
package org.cxa.misc.stringUtils;

/**
 * This is a convenience class for building strings with a pre-defined
 * delimiter.
 * 
 * @author Christopher Armenio
 */
public class DelimitedStringBuilder
{
	private StringBuilder sb;
	private final String delimeter;
	
	
	/**
	 * Creates a DelimitedStringBuilder which will use the
	 * specified delimiter.
	 * 
	 * @param delimIn the delimiter to use between appends
	 */
	public DelimitedStringBuilder(String delimIn)
	{
		this.sb = new StringBuilder();
		this.delimeter = delimIn;
	}
	
	
	/**
	 * Appends the provided string. The DelimitedStringBuilder
	 * will automatically insert the delimiter (provided in 
	 * the constructor) between successive calls to append.
	 * 
	 * @param stringToAddIn the string to add (should not contain
	 * 		the delimiter)
	 * 
	 * @return the same string builder (for chaining)
	 */
	public DelimitedStringBuilder append(String stringToAddIn)
	{
		this.sb.append(stringToAddIn);
		this.sb.append(this.delimeter);
		
		return this;
	}
	
	
	/**
	 * @return the complete string created by the DelimitedStringBuilder.
	 * 		This string will NEVER end with the delimiter provided in
	 * 		the constructor.
	 */
	@Override
	public String toString()
	{
		String retVal = this.sb.toString();
		if( retVal.endsWith(this.delimeter) ) retVal = retVal.substring(0, retVal.length()-this.delimeter.length());
		return retVal;
	}
}
