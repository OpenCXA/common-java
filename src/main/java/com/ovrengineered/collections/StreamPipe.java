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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * This is a convenience class for creating an output stream
 * that is directly linked to another input stream.
 * (ie. writes to the output stream are copied to and available
 * through the input stream)
 *
 * @author Christopher Armenio
 */
public class StreamPipe
{
	private PipedInputStream pis = new PipedInputStream();
	private PipedOutputStream pos = new PipedOutputStream();

	
	/**
	 * Creates the stream pipe object
	 *
	 * @throws IOException if an error occurs with the underlying streams
	 */
	public StreamPipe() throws IOException
	{
		this.pis.connect(this.pos);
	}
	
	
	/**
	 * @return the input stream directly linked to this output stream
	 */
	public InputStream getInputStream()
	{
		return this.pis;
	}
	
	
	/**
	 * @return the output stream directly linked to the input stream
	 */
	public OutputStream getOutputStream()
	{
		return this.pos;
	}
	
	
	/**
	 * Closes both the input and output stream
	 *
	 * @throws IOException if an error occurs with the underlying streams
	 */
	public void close() throws IOException
	{
		this.pis.close();
		this.pos.close();
	}
}
