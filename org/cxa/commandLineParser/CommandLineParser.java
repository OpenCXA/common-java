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
package org.cxa.commandLineParser;

import java.util.ArrayList;
import java.util.List;

import org.cxa.commandLineParser.Option.Argument;
import org.cxa.commandLineParser.optionListener.OptionNoArgumentListener;
import org.cxa.commandLineParser.optionListener.OptionWithArgumentListener;

/**
 * This is a utility class for parsing CommandLine options/arguments
 * 
 * @author Christopher Armenio
 */
public class CommandLineParser
{
	private List<Option> options = new ArrayList<Option>();
	private final String progName;
	private final String progDescription;
	
	
	/**
	 * Creates a command line parser object.
	 * 
	 * <p>
	 * The new CommandLineParser object is created with three default options
	 * "--help", "-h", "-?" all of which default to printing the usage/help
	 * screen and exiting
	 * </p> 
	 * 
	 * @param progNameIn the name of the executable program
	 */
	public CommandLineParser(String progNameIn)
	{
		this(progNameIn, null);
	}
	
	
	/**
	 * Creates a command line parser object
	 * 
	 * <p>
	 * the new CommandLineParser object is created with three default options
	 * "--help", "-h", "-?" all of which default to printing the usage/help
	 * screen and exiting
	 * </p>
	 * 
	 * @param progNameIn the name of the executable program
	 * @param progDescIn a user-friendly description printed during usage screens
	 */
	public CommandLineParser(String progNameIn, String progDescIn)
	{
		this.progName = progNameIn;
		this.progDescription = progDescIn;
		
		OptionNoArgumentListener optList_help = new OptionNoArgumentListener()
		{
			@Override
			public void optionIsPresent()
			{
				printUsage();
				System.exit(0);
			}
		};
		
		this.addOption("h", "help", "prints command help and exits", false, optList_help);
		this.addOption("?", null, "prints command help and exits", false, optList_help);
	}
	
	
	/**
	 * Adds an option to the command line parser
	 * 
	 * @param shortOptIn a "short" representation of this option (usually one to two characters)
	 * @param longOptIn a "long" representation of this option (usually multiple characters/words)
	 * @param descIn a user-friendly description of this option
	 * @param isRequiredIn true if this option is required to be present on the command line
	 * @param olIn a listener that will be called when this option is successfully parsed
	 */
	public void addOption(String shortOptIn, String longOptIn, String descIn, boolean isRequiredIn, OptionNoArgumentListener olIn)
	{
		this.options.add(new Option(shortOptIn, longOptIn, descIn, isRequiredIn, olIn));
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
	public <T> void addOption(String shortOptIn, String longOptIn, String descIn, boolean isOptionRequiredIn, boolean isArgumentRequiredIn, Class<T> expectedArgumentTypeIn, OptionWithArgumentListener<T> owalIn)
	{
		this.options.add(new Option(shortOptIn, longOptIn, descIn, isOptionRequiredIn, isArgumentRequiredIn, expectedArgumentTypeIn, owalIn));
	}
	
	
	/**
	 * Prints the usage/help screen for the configured options
	 */
	public void printUsage()
	{
		System.out.println("Usage:  " + this.progName + " <options>\r\n");
		System.out.println(this.progDescription);
		System.out.println("\r\n");
		
		for( Option currOpt : this.options )
		{
			System.out.printf("%-30s   %s\r\n", currOpt.getUsageString(), currOpt.getDescription());
		}
	}
	
	
	/**
	 * Parses the given array of strings for the command line options and arguments
	 * 
	 * @param optsIn the array of strings containing the command line options 
	 * 
	 * @return true on successful parsing and false if an error is encountered
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public boolean parseOptions(String optsIn[])
	{
		List<Option> presentOptions = new ArrayList<Option>();
		
		// iterate through our CL arguments
		for( int i = 0; i < optsIn.length; i++ )
		{
			String passedOpt = optsIn[i];
			
			Option expectedOpt = null;
			if( passedOpt.startsWith("--") ) expectedOpt = this.findOptionByLongOpt(passedOpt);
			else if( passedOpt.startsWith("-") ) expectedOpt = this.findOptionByShortOpt(passedOpt);
			else
			{
				System.err.println(String.format("Option [%s] not specified with preceding '--' or '-'", passedOpt));
				return false;
			}
			
			// get our option
			if( expectedOpt == null )
			{
				System.err.println(String.format("Error: Unknown option [%s]", passedOpt));
				return false;
			}
			presentOptions.add(expectedOpt);
			
			// now see if we need to parse an argument
			Argument expectedArg = expectedOpt.getArgument();
			if( expectedArg == null )
			{
				// no argument...call our callback and move on to the next item
				OptionNoArgumentListener ol = (OptionNoArgumentListener)expectedOpt.getOptionListener();
				if( ol != null ) ol.optionIsPresent();
				continue;
			}
			
			// if we made it here, we _may_ need to parse an argument
			if( expectedArg.isRequired() )
			{
				// make sure we actually have another element
				if( ++i >= optsIn.length )
				{
					System.err.println(String.format("Error: Required argument not specified for option [%s]", passedOpt));
					return false;
				}
				// get the argument string value
				String argVal_str = optsIn[i];
				Object argVal = parseArgumentFromString(argVal_str, expectedArg.getExpectedType());
				if( argVal == null )
				{
					System.err.println(String.format("Error: Cannot parse expected %s argument from string [%s]",
							expectedArg.getExpectedType().getSimpleName(), argVal_str));
					return false;
				}
				
				// if we made it here, we have a valid argument
				OptionWithArgumentListener ol = (OptionWithArgumentListener)expectedOpt.getOptionListener();
				if( ol != null ) ol.optionIsPresent(argVal);
				continue;
			}
			else
			{
				// check to see if the next item is another option or our argument
				if( ++i >= optsIn.length )
				{
					// no argument
					OptionWithArgumentListener ol = (OptionWithArgumentListener)expectedOpt.getOptionListener();
					if( ol != null ) ol.optionIsPresent(null);
					continue;
				}
				
				// if we made it here, things get a little tricky...
				String nextStr = optsIn[i];
				if( nextStr.startsWith("--") || nextStr.startsWith("-") )
				{
					// the next string is an option...therefore we have no argument
					i--;
					OptionWithArgumentListener ol = (OptionWithArgumentListener)expectedOpt.getOptionListener();
					if( ol != null ) ol.optionIsPresent(null);
					continue;
				}
				else
				{
					// the next string is not an option...therefore it should be an argument
					Object argVal = parseArgumentFromString(nextStr, expectedArg.getExpectedType());
					if( argVal == null )
					{
						System.err.println(String.format("Error: Cannot parse expected %s argument from string [%s]",
								expectedArg.getExpectedType().getSimpleName(), nextStr));
						return false;
					}
					
					// if we made it here, we have a valid argument
					OptionWithArgumentListener ol = (OptionWithArgumentListener)expectedOpt.getOptionListener();
					if( ol != null ) ol.optionIsPresent(argVal);
					continue;
				}
			}
		}
		
		// if we made it here, we parsed everything successfully
		// now make sure we have all of our required options
		for( Option currReqOpt : this.listRequiredOptions() )
		{
			if( !presentOptions.contains(currReqOpt) )
			{
				System.err.println(String.format("Error: required argument not present [%s]", currReqOpt.getLongOpt()));
				return false;
			}
		}
		
		// if we made it here, we're good to go!
		return true;
	}
	
	
	private Option findOptionByShortOpt(String shortOptionIn)
	{
		shortOptionIn = shortOptionIn.replaceFirst("-{1,2}", "");
		
		// iterate through our stored options looking for the specified string
		for( Option currOption : this.options )
		{
			if( (currOption.getShortOpt() != null) && currOption.getShortOpt().equals(shortOptionIn) )
			{
				return currOption;
			}
		}
		
		// if we made it here, we couldn't find the option
		return null;
	}
	
	
	private Option findOptionByLongOpt(String longOptionIn)
	{
		longOptionIn = longOptionIn.replaceFirst("-{1,2}", "");
		
		// iterate through our stored options looking for the specified string
		for( Option currOption : this.options )
		{
			if( (currOption.getLongOpt() != null) && currOption.getLongOpt().equals(longOptionIn) )
			{
				return currOption;
			}
		}
		
		// if we made it here, we couldn't find the option
		return null;
	}
	
	
	private List<Option> listRequiredOptions()
	{
		List<Option> retVal = new ArrayList<Option>();
		
		for( Option currOpt : this.options )
		{
			if( currOpt.isRequired() ) retVal.add(currOpt);
		}
		
		return retVal;
	}
	
	
	@SuppressWarnings("rawtypes")
	private static Object parseArgumentFromString(String stringIn, Class expectedClassIn)
	{
		try
		{
			if( expectedClassIn == Integer.class )
			{
				return Integer.parseInt(stringIn);
			}
			else if( expectedClassIn == Long.class )
			{
				return Long.parseLong(stringIn);
			}
			else if( expectedClassIn == Float.class )
			{
				return Float.parseFloat(stringIn);
			}
			else if( expectedClassIn == Double.class )
			{
				return Double.parseDouble(stringIn);
			}
			else if( expectedClassIn == String.class )
			{
				return stringIn;
			}
			else if( expectedClassIn == Boolean.class )
			{
				return Boolean.parseBoolean(stringIn);
			}
		}
		catch(Exception e)
		{
			return null;
		}
		
		return null;
	}
}
