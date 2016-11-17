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
package com.ovrengineered.commandLineParser;

import com.ovrengineered.commandLineParser.optionListener.OptionListener;
import com.ovrengineered.commandLineParser.optionListener.OptionNoArgumentListener;
import com.ovrengineered.commandLineParser.optionListener.OptionWithArgumentListener;

/**
 * This is a class which encapsulates functions for Options and Arguments
 * 
 * @author Christopher Armenio
 */
public class Option
{
	public class Argument
	{
		private boolean isRequired;
		@SuppressWarnings("rawtypes")
		private Class expectedType;
		
		
		private Argument(boolean isRequiredIn, @SuppressWarnings("rawtypes") Class expectedTypeIn)
		{
			this.isRequired = isRequiredIn;
			this.expectedType = expectedTypeIn;
		}
		
		
		public boolean isRequired()
		{
			return this.isRequired;
		}
		
		
		@SuppressWarnings("rawtypes")
		public Class getExpectedType()
		{
			return this.expectedType;
		}
	}
	
	
	private String shortOpt;
	private String longOpt;
	private String desc;
	private boolean isRequired;
	private OptionListener ol = null;
	private Argument arg = null;
	
	
	/**
	 * Creates an option that takes no arguments
	 * 
	 * @param shortOptIn a "short" representation of this option (usually one to two characters)
	 * @param longOptIn a "long" representation of this option (usually multiple characters/words)
	 * @param descIn a user-friendly description of this option
	 * @param isRequiredIn true if this option is required to be present on the command line
	 * @param olIn a listener that will be called when this option is successfully parsed
	 */
	public Option(String shortOptIn, String longOptIn, String descIn, boolean isRequiredIn, OptionNoArgumentListener olIn)
	{
		this.shortOpt = shortOptIn;
		this.longOpt = longOptIn;
		this.desc = descIn;
		this.isRequired = isRequiredIn;
		this.ol = olIn;
	}
	
	
	/**
	 * Adds an option to the command line parser that has a optional argument
	 * 
	 * @param shortOptIn a "short" representation of this option (usually one to two characters)
	 * @param longOptIn a "long" representation of this option (usually multiple characters/words)
	 * @param descIn a user-friendly description of this option
	 * @param isOptionRequiredIn true if this option is required to be present on the command line
	 * @param isArgumentRequiredIn true if the argument is required, false if it is optional
	 * @param expectedArgumentTypeIn a class representing the type of the parameter that should be parsed
	 * 		and passed to the listener
	 * @param owalIn a listener that will be called when this option is successfully parsed
	 */
	public Option(String shortOptIn, String longOptIn, String descIn, boolean isOptionRequiredIn, boolean isArgumentRequiredIn, @SuppressWarnings("rawtypes") Class expectedArgumentTypeIn, OptionWithArgumentListener<?> owalIn)
	{
		this.shortOpt = shortOptIn;
		this.longOpt = longOptIn;
		this.desc = descIn;
		this.isRequired = isOptionRequiredIn;
		this.ol = owalIn;
		
		this.arg = new Argument(isArgumentRequiredIn, expectedArgumentTypeIn);
	}
	
	
	/**
	 * Returns the usage screen for this option (no description)
	 * @return the usage string for this option
	 */
	public String getUsageString()
	{
		String retVal = null;
		if( !this.isRequired )
		{
			retVal = String.format("[<-%s | --%s>%s]", this.shortOpt, this.longOpt, ((this.arg != null) ? " <arg>]" : ""));
		}
		else
		{
			retVal = String.format("<-%s | --%s>", this.shortOpt, this.longOpt);
			if( this.arg != null ) retVal += " <arg>";
		}
		
		return retVal;
	}
	
	
	/**
	 * @return the user-friendly description of this option
	 */
	public String getDescription()
	{
		return this.desc;
	}
	
	
	/**
	 * @return the "short" string representation of this option
	 */
	public String getShortOpt()
	{
		return this.shortOpt;
	}
	
	
	/**
	 * @return the "long" string representation of this option
	 */
	public String getLongOpt()
	{
		return this.longOpt;
	}
	
	
	/**
	 * @return true if this option is required
	 */
	public boolean isRequired()
	{
		return this.isRequired;
	}
	
	
	/**
	 * @return the argument associated with this option (or null)
	 * 		if this option has no arguments
	 */
	public Argument getArgument()
	{
		return this.arg;
	}
	
	
	/**
	 * @return the option listener associated with this option
	 */
	public OptionListener getOptionListener()
	{
		return this.ol;
	}
}
