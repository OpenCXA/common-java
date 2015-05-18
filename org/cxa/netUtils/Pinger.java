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
package org.cxa.netUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for performing ICMP echo requests to a given host
 * 
 * @author Christopher Armenio
 */
public class Pinger
{	
	private static final int TIMEOUT_MS = 10000;
	
	
	/**
	 * Pings a host using the OS' command-line ping mechanism (since Java requires
	 * root/admin access to ping natively). This function may take some time to return.
	 * 
	 * @param hostNameIn the hostname to ping
	 * @param numPingsIn the number of pings to send to the host
	 * @return the number of pings received from the host OR -1 on error
	 */
	public static int pingHost(String hostNameIn, int numPingsIn)
	{
		int retVal = -1;
		
		if( System.getProperty("os.name").contains("Windows") )
		{
			try
			{
				Process pingProc = Runtime.getRuntime().exec(String.format("ping -n %d -w %d %s", numPingsIn, TIMEOUT_MS, hostNameIn));
				try{ pingProc.waitFor(); } catch(InterruptedException e)
				{
					pingProc.destroy();
					return -1;
				}
				
				// iterate through our output
				BufferedReader outputReader = new BufferedReader(new InputStreamReader(pingProc.getInputStream()));
				while( outputReader.ready() )
				{	
					// looking for:
					// '    Packets: Sent = 4, Received = 4, Lost = 0 (0% loss)'
					Pattern p = Pattern.compile("^\\s*Packets: Sent = (\\d*), Received = (\\d*), Lost = (\\d*)");
					Matcher m = p.matcher(outputReader.readLine());
					if( m.find() )
					{
						// our pattern matched...extract the number received
						//Integer numSent = Integer.parseInt(m.group(1));
						Integer numReceived = Integer.parseInt(m.group(2));
						//Integer numLost = Integer.parseInt(m.group(3));
						
						retVal = numReceived;
						break;
					}
				}
				
				outputReader.close();
				pingProc.destroy();
			}
			catch(IOException e) { }
		}
		else throw new RuntimeException(String.format("unsupported operation system: '%s'", System.getProperty("os.name")));
		
		return retVal;
	}
}
