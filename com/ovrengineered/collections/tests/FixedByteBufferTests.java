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
package com.ovrengineered.collections.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ReadOnlyBufferException;

import com.ovrengineered.collections.FixedByteBuffer;
import org.junit.Test;

/**
 * @author Christopher Armenio
 */
public class FixedByteBufferTests
{
	protected static final int MAX_BUFFER_SIZE = 2048;
	
	
	@Test
	public void test_getSize()
	{
		FixedByteBuffer fbb = new FixedByteBuffer(randomBufferSize());
		
		for( int i = 0; i < fbb.getMaxSize_bytes(); i++ )
		{
			fbb.put_rel_byte(genRandomByte());
			
			assertEquals("Buffer size is incorrect", i+1, fbb.getSize_bytes());
		}
	}
	
	
	@Test
	public void test_maxSize()
	{
		for( int i = 0; i < randomBufferSize(); i++ )
		{
			FixedByteBuffer fbb = new FixedByteBuffer(i);
			assertEquals("Buffer max size is incorrect", i, fbb.getMaxSize_bytes());
		}
	}
	
	
	@Test
	public void test_isEmpty()
	{
		FixedByteBuffer fbb = new FixedByteBuffer(randomBufferSize());
		
		// make sure it is initially empty
		assertTrue("Buffer is not initially empty", fbb.isEmpty());
		
		// run it through some paces
		for( int i = 0; i < fbb.getMaxSize_bytes(); i++ )
		{
			// add items
			for( int j = 0; j < i; j++ )
			{
				fbb.put_rel_byte(genRandomByte());
			}
			
			// now remove just as many
			for( int j = 0; j < i; j++ )
			{
				fbb.remove_abs_byte(0);
			}
			
			// now make sure it's empty
			boolean foo = fbb.isEmpty();
			assertTrue(String.format("Buffer is not empty after insert/remove  %d/%d", i, fbb.getMaxSize_bytes()), foo);
			
			// reset our writeIndex
			fbb.setWriteIndex(0);
		}
		
		// fill'r up
		for( int i = 0; i < randomInt(1, fbb.getMaxSize_bytes()); i++ )
		{
			fbb.put_rel_byte(genRandomByte());
		}
		fbb.clear();
		assertTrue("Buffer is not empty after reset", fbb.isEmpty());
	}
	
	
	@Test
	public void test_isFull()
	{
		for( int i = 0; i < randomBufferSize(); i++ )
		{
			FixedByteBuffer fbb = new FixedByteBuffer(i);
			
			// fill'r up
			for( int j = 0; j < i; j++ )
			{
				fbb.put_rel_byte(genRandomByte());
			}
			
			assertTrue("Buffer is not full after appends", fbb.isFull());
		}
	}
	
	
	@Test
	public void test_append_byte()
	{
		FixedByteBuffer fbb = new FixedByteBuffer(randomBufferSize());
		
		// create our test bytes and add to our buffer
		byte[] testBytes = new byte[fbb.getMaxSize_bytes()];
		for( int i = 0; i < testBytes.length; i++ )
		{
			testBytes[i] = genRandomByte();
			fbb.put_rel_byte(testBytes[i]);
		}
		
		// now check them
		for( int i = 0; i < testBytes.length; i++ )
		{
			assertEquals("byte values are not equal", testBytes[i], fbb.get_abs_byte(i));
		}
	}
	
	
	@Test
	public void test_append_byte_tooMany()
	{
		for( int i = 0; i < randomBufferSize(); i++ )
		{
			FixedByteBuffer fbb = new FixedByteBuffer(i);
			
			// fill'r up
			for( int j = 0; j < i; j++ )
			{
				fbb.put_rel_byte(genRandomByte());
			}
			
			// now try to add more
			try
			{
				fbb.put_rel_byte(genRandomByte());
				
				// if we made it here, we didn't get an exception
				fail("did not catch a buffer overflow");
			}
			catch(IndexOutOfBoundsException e){}
		}
	}
	
	
	@Test
	public void test_append_byteBuffer()
	{
		FixedByteBuffer fbb_target = new FixedByteBuffer(randomBufferSize());
		
		// create source buffers of random size and try adding them
		for( int i = 0; i < fbb_target.getMaxSize_bytes(); i++ )
		{
			FixedByteBuffer fbb_source = new FixedByteBuffer(i);
		
			// add random bytes to the source buffer
			for( int j = 0; j < i; j++ )
			{
				fbb_source.put_rel_byte(genRandomByte());
			}
			
			// now add our source to our target
			fbb_target.put_rel_fbb(fbb_source);
			
			// verify their sizes are appropriate
			assertEquals("Source and target sizes do not match", fbb_source.getSize_bytes(), fbb_target.getSize_bytes());
			
			// now compare their contents
			for( int j = 0; j < i; j++ )
			{
				assertEquals("Contents of source and target do not match", fbb_source.get_abs_byte(j), fbb_target.get_abs_byte(j));
			}
			
			// reset our target for next time
			fbb_target.clear();
		}
	}
	
	
	@Test
	public void test_append_byteBuffer_tooBig()
	{
		FixedByteBuffer fbb_target = new FixedByteBuffer(randomBufferSize());
		
		FixedByteBuffer fbb_source = new FixedByteBuffer(fbb_target.getMaxSize_bytes()+1);
		
		// fill up our source buffer
		for( int i = 0; i < fbb_source.getMaxSize_bytes(); i++ )
		{
			fbb_source.put_rel_byte(genRandomByte());
		}
		
		// now try to add the bytes
		try
		{
			fbb_target.put_rel_fbb(fbb_source);
			
			// if we made it here, we didn't catch an exception
			fail("did not catch abuffer overflow");
		}
		catch(IndexOutOfBoundsException e){}
	}
	
	
	@Test
	public void test_initSubsetOfData_remaining()
	{
		final int PARENT_SIZE_BYTES = 10;
		final int SLICE_INDEX = 4;
		
		FixedByteBuffer fbb_parent = new FixedByteBuffer(PARENT_SIZE_BYTES);
		
		// fill up our source buffer
		for( int i = 0; i < fbb_parent.getMaxSize_bytes(); i++ )
		{
			fbb_parent.put_rel_byte(genRandomByte());
		}
		
		// now create our sliced buffer
		FixedByteBuffer fbb_sliced = FixedByteBuffer.init_subsetOfData(fbb_parent, SLICE_INDEX);
		
		// make sure the parent size is still the same
		assertEquals("parent size changed", PARENT_SIZE_BYTES, fbb_parent.getSize_bytes());
		
		// now make sure our sliced buffer size is appropriate
		assertEquals("sliced buffer size is incorrect", PARENT_SIZE_BYTES-SLICE_INDEX, fbb_sliced.getSize_bytes());
		
		// now make sure the data matches
		for( int i = 0; i < fbb_sliced.getSize_bytes(); i++ )
		{
			assertEquals("byte values are not equal", fbb_parent.get_abs_byte(SLICE_INDEX+i), fbb_sliced.get_abs_byte(i));
		}
	}
	
	
	@Test
	public void test_initSubsetOfData_remaining_readOnly()
	{
		final int PARENT_SIZE_BYTES = 10;
		final int SLICE_INDEX = 4;
		
		FixedByteBuffer fbb_parent = new FixedByteBuffer(PARENT_SIZE_BYTES);
		
		// fill up our source buffer
		for( int i = 0; i < fbb_parent.getMaxSize_bytes(); i++ )
		{
			fbb_parent.put_rel_byte(genRandomByte());
		}
		
		// now create our sliced buffer
		FixedByteBuffer fbb_sliced = FixedByteBuffer.init_subsetOfData(fbb_parent, SLICE_INDEX);
		
		// make sure the parent size is still the same
		assertEquals("parent size changed", PARENT_SIZE_BYTES, fbb_parent.getSize_bytes());
		
		// now make sure our sliced buffer size is appropriate
		assertEquals("sliced buffer size is incorrect", PARENT_SIZE_BYTES-SLICE_INDEX, fbb_sliced.getSize_bytes());
		
		try
		{
			fbb_sliced.put_rel_byte((byte)0);
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
		
		try
		{
			fbb_sliced.put_rel_boolean(true);
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
		
		try
		{
			fbb_sliced.put_rel_uint16LE((short)0);
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
		
		try
		{
			fbb_sliced.put_rel_uint32LE(0);
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
		
		try
		{
			fbb_sliced.put_rel_float(0F);
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
		
		try
		{
			fbb_sliced.remove_abs_byte(0);
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
		
		try
		{
			fbb_sliced.clear();
			fail("didn't throw ReadOnlyException");
		}
		catch(ReadOnlyBufferException e){ }
	}
	
	
	@Test
	public void test_initSubsetData_fixedLen()
	{
		final int PARENT_SIZE_BYTES = 10;
		final int SLICE_INDEX = 4;
		final int SLICE_LENGTH_BYTES = 4;
		
		FixedByteBuffer fbb_parent = new FixedByteBuffer(PARENT_SIZE_BYTES);
		
		// fill up our source buffer
		for( int i = 0; i < fbb_parent.getMaxSize_bytes(); i++ )
		{
			fbb_parent.put_rel_byte(genRandomByte());
		}
		
		// now create our sliced buffer
		FixedByteBuffer fbb_sliced = FixedByteBuffer.init_subsetOfData(fbb_parent, SLICE_INDEX, SLICE_LENGTH_BYTES);
		
		// make sure the parent size is still the same
		assertEquals("parent size changed", PARENT_SIZE_BYTES, fbb_parent.getSize_bytes());
		
		// now make sure our sliced buffer size is appropriate
		assertEquals("sliced buffer size is incorrect", SLICE_LENGTH_BYTES, fbb_sliced.getSize_bytes());
		
		// now make sure the data matches
		for( int i = 0; i < fbb_sliced.getSize_bytes(); i++ )
		{
			assertEquals("byte values are not equal", fbb_parent.get_abs_byte(SLICE_INDEX+i), fbb_sliced.get_abs_byte(i));
		}
	}
	
	
	@Test
	public void test_initSubsetCapacity_remaining()
	{
		final int PARENT_SIZE_BYTES = 10;
		final int SLICE_INDEX = 4;
		
		FixedByteBuffer fbb_parent = new FixedByteBuffer(PARENT_SIZE_BYTES);
		
		// fill up our source buffer with a few bytes
		for( int i = 0; i < SLICE_INDEX; i++ )
		{
			fbb_parent.put_rel_byte(genRandomByte());
		}
		
		// now create our sliced buffer
		FixedByteBuffer fbb_sliced = FixedByteBuffer.init_subsetOfCapacity(fbb_parent, SLICE_INDEX);
		
		// make sure the parent size is still the same
		assertEquals("parent size changed", SLICE_INDEX, fbb_parent.getSize_bytes());
		
		// now make sure our sliced buffer size is appropriate
		assertEquals("sliced buffer size is incorrect", 0, fbb_sliced.getSize_bytes());
		
		// now try to fill up the sliced buffer (and the parent buffer at the same time)
		for( int i = SLICE_INDEX; i < PARENT_SIZE_BYTES; i++ )
		{
			fbb_sliced.put_rel_byte(genRandomByte());
			
			// now make sure the sizes are right
			assertEquals("parent size is incorrect", i+1, fbb_parent.getSize_bytes());
			assertEquals("sliced buffer size is incorrect", i+1-SLICE_INDEX, fbb_sliced.getSize_bytes());
			
			// now make sure the data matches
			assertEquals("byte values are not equal", fbb_parent.get_abs_byte(i), fbb_sliced.get_abs_byte(i-SLICE_INDEX));
		}
		
		// now, make sure both the parent and the sliced buffer are full
		assertTrue("parent is not full", fbb_parent.isFull());
		assertTrue("sliced buffer is not full", fbb_sliced.isFull());
	}
	
	
	@Test
	public void test_initSubsetCapacity_fixedLen()
	{
		final int PARENT_SIZE_BYTES = 10;
		final int SLICE_INDEX = 4;
		final int SLICE_LENGTH_BYTES = 4;
		
		FixedByteBuffer fbb_parent = new FixedByteBuffer(PARENT_SIZE_BYTES);
		
		// fill up our source buffer with a few bytes
		for( int i = 0; i < SLICE_INDEX; i++ )
		{
			fbb_parent.put_rel_byte(genRandomByte());
		}
		
		// now create our sliced buffer
		FixedByteBuffer fbb_sliced = FixedByteBuffer.init_subsetOfCapacity(fbb_parent, SLICE_INDEX, SLICE_LENGTH_BYTES);
		
		// make sure the parent size is still the same
		assertEquals("parent size changed", SLICE_INDEX, fbb_parent.getSize_bytes());
		
		// now make sure our sliced buffer size is appropriate
		assertEquals("sliced buffer size is incorrect", 0, fbb_sliced.getSize_bytes());
		
		// now try to fill up the sliced buffer (and the parent buffer at the same time)
		for( int i = SLICE_INDEX; i < SLICE_INDEX+SLICE_LENGTH_BYTES; i++ )
		{
			fbb_sliced.put_rel_byte(genRandomByte());
			
			// now make sure the sizes are right
			assertEquals("parent size is incorrect", i+1, fbb_parent.getSize_bytes());
			assertEquals("sliced buffer size is incorrect", i+1-SLICE_INDEX, fbb_sliced.getSize_bytes());
			
			// now make sure the data matches
			assertEquals("byte values are not equal", fbb_parent.get_abs_byte(i), fbb_sliced.get_abs_byte(i-SLICE_INDEX));
		}
		
		// now, make sure the sliced buffer is full, but parent isn't
		assertFalse("parent is full", fbb_parent.isFull());
		assertTrue("sliced buffer is not full", fbb_sliced.isFull());
	}
	
	
	@Test
	public void test_get_uint32LE()
	{	
		FixedByteBuffer fbb = new FixedByteBuffer(randomInt(4, MAX_BUFFER_SIZE));
		
		for( int tryNum = 0; tryNum < randomInt(256, 1024); tryNum++ )
		{
			// fill the buffer with random bytes
			int numFillBytes = randomInt(0, fbb.getMaxSize_bytes()-4);
			for( int i = 0; i < numFillBytes; i++ )
			{
				fbb.put_rel_byte(genRandomByte());
			}

			// now generate a random number to insert
			int insertNumber = randomInt(Integer.MIN_VALUE, Integer.MAX_VALUE);
			int insertIndex = fbb.getSize_bytes();
			
			// insert that number manually
			fbb.put_rel_byte( (byte)((insertNumber >>>   0) & 0xFF) );
			fbb.put_rel_byte( (byte)((insertNumber >>>   8) & 0xFF) );
			fbb.put_rel_byte( (byte)((insertNumber >>>  16) & 0xFF) );
			fbb.put_rel_byte( (byte)((insertNumber >>>  24) & 0xFF) );
			
			// now get the number out
			int extractedNumber = (int)fbb.get_abs_uint32LE(insertIndex);
			assertEquals(String.format("Inserted and extracted number do not match (e:0x%X, a: 0x%X)", insertNumber, extractedNumber), insertNumber, extractedNumber);
			
			// reset our buffer
			fbb.clear();
		}
	}
	
	
	@Test
	public void test_get_string()
	{
		final String TEST_STRING = "abcdefghijklmnopqrstuvwxyz0123456789";
		
		FixedByteBuffer fbb = new FixedByteBuffer(TEST_STRING.length()+3);
		
		fbb.put_rel_string(TEST_STRING);
		String retString = fbb.get_abs_string(0);
		
		assertTrue("strings do not match", retString.equals(TEST_STRING));
	}
	
	
	@Test
	public void test_toByteArray()
	{
		FixedByteBuffer fbb = new FixedByteBuffer(randomBufferSize());
		
		for( int i = 0; i < fbb.getMaxSize_bytes(); i++ )
		{
			// generate our source bytes and add them to the buffer
			byte[] sourceBytes = new byte[i];
			for( int j = 0; j < i; j++ )
			{
				sourceBytes[j] = genRandomByte();
				fbb.put_rel_byte(sourceBytes[j]);
			}
			
			// now get our byte buffer out
			byte[] extractedBytes = fbb.toByteArray();
			
			// make sure the sizes match
			assertEquals("Source and extracted arrays are of different length", sourceBytes.length, extractedBytes.length);

			// iterate and compare
			for( int j = 0; j < i; j++ )
			{
				assertEquals("Source and extracted arrays contain different values", sourceBytes[j], extractedBytes[j]);
			}
			
			// reset for next run
			fbb.clear();
		}
	}

	
	@Test
	public void test_reset()
	{
		for( int i = 0; i < randomBufferSize(); i++ )
		{
			FixedByteBuffer fbb = new FixedByteBuffer(i);
			
			// fill'r up with some bytes
			int numFillBytes = randomInt(0, fbb.getMaxSize_bytes());
			for( int j = 0; j < numFillBytes; j++ )
			{
				fbb.put_rel_byte(genRandomByte());
			}
			
			// now reset
			fbb.clear();
			
			// make sure
			assertTrue("buffer is not marked empty", fbb.isEmpty());
			assertEquals("buffer does not show 0 size", 0, fbb.getSize_bytes());
		}
	}
	
	
	@Test
	public void test_equals()
	{	
		for( int i = 0; i < randomBufferSize(); i++ )
		{
			FixedByteBuffer fbb = new FixedByteBuffer(i);
			
			// create our source bytes and add to our buffer
			byte[] sourceBytes = new byte[i];
			for( int j = 0; j < i; j++ )
			{
				sourceBytes[j] = genRandomByte();
				fbb.put_rel_byte(sourceBytes[j]);
			}
			
			// now make sure they're equal
			assertTrue("Source and buffer are not equal", fbb.equals(sourceBytes));
		}
	}
	
	
	protected static byte genRandomByte()
	{
		return (byte)(Math.random()*255);
	}
	
	
	protected static int randomBufferSize()
	{	
		return (int)(Math.random() * ((double)MAX_BUFFER_SIZE-1)) + 1;
	}
	
	
	protected static int randomInt(int minValIn, int maxValIn)
	{
		return (int)(Math.random() * ((double)maxValIn-(double)minValIn)) + minValIn;
	}
}
