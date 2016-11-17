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
package com.ovrengineered.misc;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This is a convenience class for exception-related functions
 * 
 * @author Christopher Armenio
 */
public class ExceptionUtils
{
	private static int MAX_NUM_STACK_TRACE_ENTRIES = 5;
	
	
	/**
	 * Standard logging message for indicating that, although an exception occurred,
	 * the exception has been caught and execution will continue as normal.
	 */
	public static final String MSG_OPERATION_WILL_CONTINUE = "Operation will continue as normal";
	
	
	/**
	 * This is a convenience function for generating a uniform logging message containing the
	 * following information about an exception:
	 * <ul>
	 *    <li>exception class</li>
	 *    <li>location of the exception (className::methodName)</li>
	 *    <li>stack trace</li>
	 * </ul>
	 * It is equivalent to calling {@link #getExceptionLogString(Throwable, String)}
	 * with null for {@code customMessageIn}
	 *    
	 * @param eIn the exception
	 * 
	 * @return a string which can be used to log the exception
	 */
	public static String getExceptionLogString(Throwable eIn)
	{
		return getExceptionLogString(eIn, null);
	}
	
	
	/**
	 * This is a convenience function for generating a uniform logging message containing the
	 * following information about an exception:
	 * <ul>
	 *    <li>exception class</li>
	 *    <li>location of the exception (className::methodName)</li>
	 *    <li>a user-supplied message</li>
	 *    <li>stack trace</li>
	 * </ul>
	 *    
	 * @param eIn the exception
	 * @param customMessageIn a custom message that will be included in the message
	 * 
	 * @return a string which can be used to log the exception
	 */
	public static String getExceptionLogString(Throwable eIn, String customMessageIn)
	{
		Throwable rootCause = eIn;
		while(rootCause.getCause() != null &&  rootCause.getCause() != rootCause) rootCause = rootCause.getCause();
		StackTraceElement[] rootCauseStackTrace = rootCause.getStackTrace();
		StackTraceElement rootCauseElement = ((rootCauseStackTrace != null) && (rootCauseStackTrace.length > 0)) ? rootCauseStackTrace[0] : null;
		
		return String.format("%s occurred while calling '%s::%s'.\r\n%s%s<stacktrace>\r\n%s</stacktrace>",
				eIn.getClass().getSimpleName(),
				((rootCauseElement != null) ? rootCauseElement.getClassName() : "<unknown>"),
				((rootCauseElement != null) ? rootCauseElement.getMethodName() : "<unknown>"),
				(((eIn.getMessage() != null) && !eIn.getMessage().isEmpty()) ? String.format("message reads: %s\r\n", eIn.getMessage()) : ""), 
				((customMessageIn != null) ? (customMessageIn + ".\r\n") : ""),
				ExceptionUtils.stackTraceToString(eIn));
	}
	
	
	/**
	 * This is a convenience function for printing the content of
	 * a stack track to a String
	 * 
	 * @param eIn the exception producing the stack trace
	 * 
	 * @return a string containing the stack trace (similar to {@link Exception#printStackTrace()})
	 */
	public static String stackTraceToString(Throwable eIn)
	{
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		for( int i = 0; i < MAX_NUM_STACK_TRACE_ENTRIES; i++ )
		{
			StackTraceElement currElem = eIn.getStackTrace()[i];
			pw.println(currElem.toString());
		}
		
		return sw.toString();
	}
}
