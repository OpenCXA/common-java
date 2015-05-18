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
package org.apache.logging.log4j;

/**
 * This is a simple class for re-routing Apache logging messages
 * to LogCat on Android. It's not very fancy, and it only supports
 * basic logging, but it works!
 * 
 * @author Christopher Armenio
 */
public class LogManager
{
	public static Logger getLogger(String loggerNameIn)
	{
		return new Logger(loggerNameIn);
	}
}
