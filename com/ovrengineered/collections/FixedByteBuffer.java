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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;


/**
 * A utility class for manipulating arrays of bytes. It is internally
 * backed by the Java NIO ByteBuffer class.
 * 
 * @author Christopher Armenio
 */
public class FixedByteBuffer
{
	/**
	 * Defined parameter that can be passed as the
	 * {@code length_BytesIn } parameter to either
	 * {@link #init_subsetOfData(FixedByteBuffer, int, int)} or
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}
	 */
	public static final int CXA_FIXED_BYTE_BUFFER_LEN_ALL = -1;
	
	
	private final FixedByteBuffer parent;
	private final int startIndexInParent;
	
	private final ByteBuffer nioBuffer;
	
	private int size_bytes = 0;
	private int readIndex = 0;
	private int writeIndex = 0;
	
	
	/**
	 * Creates a byte buffer with the specified capacity
	 * 
	 * @param capacity_bytesIn maximum storage capacity of this
	 * 		buffer, in bytes
	 */
	public FixedByteBuffer(int capacity_bytesIn)
	{		
		this(ByteBuffer.allocate(capacity_bytesIn), null, 0, 0);
	}
	
	
	private FixedByteBuffer(ByteBuffer nioBufferIn, FixedByteBuffer parentIn, int startIndexInParentIn, int size_bytesIn)
	{
		this.nioBuffer = nioBufferIn;
		this.nioBuffer.order(ByteOrder.LITTLE_ENDIAN);
		this.parent = parentIn;
		this.startIndexInParent = startIndexInParentIn;
		
		// reset our read and write indices and size (for consistency)
		this.readIndex = 0;
		this.writeIndex = 0;
		this.size_bytes = size_bytesIn;
	}
	
	
	/**
	 * Returns a FixedByteBuffer that contains
	 * a shared subset of the data in the source FixedByteBuffer. The resulting
	 * FixedByteBuffer has maximum length and current size of {@code length_bytesIn}
	 * and is read-only
	 * 
	 * <p>
	 * This function is meant to provide an easy way to interact with a subset of
	 * fixedByteBuffer's <b>existing data</b> (eg. accessing sub-buffers of a single parent).
	 * Therefore, the following condition must be met:
	 * {@code (startIndexIn + length_BytesIn) <= {@link #getSize_bytes()}}
	 * To interact with a subset of the fixedByteBuffer's maximum capacity, including data, use
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}
	 * </p>
	 * 
	 * <p>
	 * The resulting buffer operates directly on the data within the provided
	 * source fixedByteBuffer (no copies). Any appends, removes, or clears on this
	 * resulting buffer directly affects the source buffer and the value of its respective
	 * member function calls as well.
	 * </p>
	 * 
	 * @param sourceIn the source buffer which will server as the data backing for the new buffer
	 * @param startIndexIn the index, within the source buffer, that should correspond
	 * 		to index 0 of the resulting buffer. Must be less than {@link #getSize_bytes()}
	 * 		of the source fixedByteBuffer
	 * @param length_bytesIn the desired length/size of the resulting buffer. {@code startIndexIn + length_BytesIn}
	 * 		must not extend past the number of elements currently in the source buffer. To specify ALL data
	 * 		from startIndexIn to the end of the data in the source buffer, use CXA_FIXED_BYTE_BUFFER_LEN_ALL.
	 * 
	 * @return the resulting byte buffer
	 * @throws IndexOutOfBoundsException if {@code startIndexIn > sourceIn.getCurrSize_bytes()}
	 */
	@SuppressWarnings("unused")
	public static FixedByteBuffer init_subsetOfData(FixedByteBuffer sourceIn, int startIndexIn, int length_bytesIn) throws IndexOutOfBoundsException
	{
		if( length_bytesIn != CXA_FIXED_BYTE_BUFFER_LEN_ALL )
		{
			if( (startIndexIn + length_bytesIn) > sourceIn.getSize_bytes() ) throw new IndexOutOfBoundsException(String.format("idx: %d > %d :srcSize", startIndexIn, sourceIn.getSize_bytes()));
		}
		else length_bytesIn = sourceIn.getSize_bytes() - startIndexIn;
		
		// we're good to go, setup our subset byte buffer
		FixedByteBuffer retVal = null;
		try
		{
			// set the source's limit so we create an array with the requested number of bytes
			sourceIn.nioBuffer.limit(startIndexIn + length_bytesIn);
			// set the source's position to the requested position
			sourceIn.nioBuffer.position(startIndexIn);
			
			// create our sliced buffer (read-only)
			ByteBuffer nioBuffer = sourceIn.nioBuffer.slice().asReadOnlyBuffer();
			nioBuffer.position(nioBuffer.capacity());
			nioBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			retVal = new FixedByteBuffer(nioBuffer, sourceIn, startIndexIn, length_bytesIn);
		}
		finally
		{
			// now restore the source's limit (must be first)
			sourceIn.nioBuffer.limit(sourceIn.nioBuffer.capacity());
		}
		if( retVal == null ) throw new NullPointerException("retVal is null...something weird happend");
		
		return retVal;
	}
	
	
	/**
	 * Shorthand for calling {@link #init_subsetOfData(FixedByteBuffer, int, int)} using {@link #CXA_FIXED_BYTE_BUFFER_LEN_ALL}
	 * as the third parameter
	 * 
	 * @see #init_subsetOfData(FixedByteBuffer, int, int)
	 */
	public static FixedByteBuffer init_subsetOfData(FixedByteBuffer sourceIn, int startIndexIn) throws IndexOutOfBoundsException
	{
		return FixedByteBuffer.init_subsetOfData(sourceIn, startIndexIn, CXA_FIXED_BYTE_BUFFER_LEN_ALL);
	}
	
	
	/**
	 * Initializes the pre-allocated fixedByteBuffer to operate within
	 * a subset of the data and capacity of the source fixedByteBuffer. The resulting
	 * allocated byte buffer has maximum length of {@code length_bytesIn}, a current size
	 * of {@code (srcCurrSize_bytes < startIndexIn) ? 0 : (srcCurrSize_bytes - startIndexIn)}
	 * and is read/write
	 * 
	 * <p>
	 * This function is meant to provide an easy way to interact with a subset of
	 * fixedByteBuffer's capacity (eg. building a single parent buffer with multiple sub-buffers).
	 * Therefore, the following condition must be met:
	 * {@code (startIndexIn + length_BytesIn) <= {@link #getMaxSize_bytes()} }
	 * </p>
	 *
	 * <p>
	 * The resulting buffer operates directly on the data within the provided
	 * source fixedByteBuffer (no copies). Any appends, removes, or clears on this
	 * resulting buffer directly affects the source buffer and the value of its respective
	 * member function calls as well.
	 * </p>
	 * 
	 * @param sourceIn the source buffer which will server as the data backing for the new buffer
	 * @param startIndexIn the index, within the source buffer, that should correspond
	 *		to index 0 of the resulting buffer. Must be less than {@link #getMaxSize_bytes()}
	 *		of the source fixedByteBuffer
	 * @param length_bytesIn the desired length/size of the resulting buffer. {@code startIndexIn + length_BytesIn}
	 *		must not extend past the total capacity of the source buffer. To specify ALL data
	 *		from startIndexIn to the end of the capacity of the source buffer, use CXA_FIXED_BYTE_BUFFER_LEN_ALL.
	 * 
	 * @return the resulting byte buffer
	 * @throws IllegalArgumentException if the {@code sourceIn} is a read-only buffer
	 * @throws IndexOutOfBoundsException if {@code (startIndexIn + length_bytesIn) > sourceIn.getCurrSize_bytes() }
	 */
	@SuppressWarnings("unused")
	public static FixedByteBuffer init_subsetOfCapacity(FixedByteBuffer sourceIn, int startIndexIn, int length_bytesIn) throws IndexOutOfBoundsException, IllegalArgumentException
	{
		if( sourceIn.isReadOnly() ) throw new IllegalArgumentException("sourceIn must not be read-only");
		
		int srcMaxSize_bytes = sourceIn.getMaxSize_bytes();
		
		// make sure we have enough data in the source buffer
		if( length_bytesIn != CXA_FIXED_BYTE_BUFFER_LEN_ALL )
		{
			if( (startIndexIn + length_bytesIn) > srcMaxSize_bytes) throw new IndexOutOfBoundsException(String.format("idx+len: %d > %d :srcMaxSize", startIndexIn+length_bytesIn, srcMaxSize_bytes));
		}
		else length_bytesIn = srcMaxSize_bytes - startIndexIn;
		
		
		// calculate the size of the new subset buffer
		int srcCurrSize_bytes = sourceIn.getSize_bytes();
		int subsetSize_bytes = (srcCurrSize_bytes < startIndexIn) ? 0 : (srcCurrSize_bytes - startIndexIn);
		
		// we're good to go, setup our subset byte buffer
		FixedByteBuffer retVal = null;
		try
		{
			//  set the source's limit so we create an array with the requested number of bytes
			sourceIn.nioBuffer.limit(startIndexIn + length_bytesIn);
			// set the source's position to the requested position
			sourceIn.nioBuffer.position(startIndexIn);
			
			// create our sliced buffer (read-only)
			ByteBuffer nioBuffer = sourceIn.nioBuffer.slice();
			nioBuffer.position(subsetSize_bytes);
			nioBuffer.order(ByteOrder.LITTLE_ENDIAN);
			
			retVal = new FixedByteBuffer(nioBuffer, sourceIn, startIndexIn, subsetSize_bytes);
		}
		finally
		{
			// now restore the source's limit (must be first)
			sourceIn.nioBuffer.limit(sourceIn.nioBuffer.capacity());
		}
		if( retVal == null ) throw new NullPointerException("retVal is null...something weird happend");
		
		return retVal;
	}
	
	
	/**
	 * Shorthand for calling {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)} using {@link #CXA_FIXED_BYTE_BUFFER_LEN_ALL}
	 * as the third parameter
	 * 
	 * @see #init_subsetOfCapacity(FixedByteBuffer, int, int)
	 */
	public static FixedByteBuffer init_subsetOfCapacity(FixedByteBuffer sourceIn, int startIndexIn) throws IndexOutOfBoundsException, IllegalArgumentException
	{
		return FixedByteBuffer.init_subsetOfCapacity(sourceIn, startIndexIn, CXA_FIXED_BYTE_BUFFER_LEN_ALL);
	}
	
	
	/**
	 * Determines the current number of bytes in the buffer
	 * 
	 * @return the current number of bytes in the byte buffer
	 */
	public int getSize_bytes()
	{
		return this.size_bytes;
	}
	
	
	/**
	 * Determines the maximum number of bytes that can be contained within this buffer
	 * 
	 * @return the maximum number of bytes this buffer can hold
	 */
	public int getMaxSize_bytes()
	{
		return this.nioBuffer.capacity();
	}
	
	
	/**
	 * Determines the number of unused bytes remaining in the buffer
	 * {@code ({@link #getMaxSize_bytes()} - {@link #getSize_bytes()})}
	 * 
	 * @return the number of unused/free bytes in the buffer
	 */
	public int getFreeSize_bytes()
	{
		return this.getMaxSize_bytes() - this.getSize_bytes();
	}
	
	
	/**
	 * Determines if this buffer is currently empty (no bytes)
	 * 
	 * @return true if there are no bytes in this buffer, false if there are
	 */
	public boolean isEmpty()
	{
		return (this.getSize_bytes() == 0);
	}
	
	
	/**
	 * Determines if this buffer is currently full
	 * {@code ({@link #getSize_bytes()} == {@link #getMaxSize_bytes()})}
	 * 
	 * @return true if this buffer is full, false if not
	 */
	public boolean isFull()
	{
		return (this.getSize_bytes() == this.getMaxSize_bytes());
	}
	
	
	/**
	 * Determines if this buffer is restricted to read-only access
	 *
	 * @return true if the buffer is read-only, false if read/write
	 */
	public boolean isReadOnly()
	{
		return this.nioBuffer.isReadOnly();
	}
	
	
	/**
	 * Returns the current readIndex of this buffer
	 * 
	 * @return the readIndex
	 */
	public int getReadIndex()
	{
		return this.readIndex;
	}
	
	
	/**
	 * Sets the readIndex of this buffer
	 * 
	 * @param indexIn the desired index. Must be
	 * 		{@code < {@link #getSize_bytes()}}
	 * 
	 * @throws IndexOutOfBoundsException if {@code indexIn >= {@link #getSize_bytes()}}
	 */
	public void setReadIndex(int indexIn) throws IndexOutOfBoundsException
	{
		if( indexIn >= this.getSize_bytes() ) throw new IndexOutOfBoundsException(String.format("idx: %d >= %d :size", indexIn, this.getSize_bytes()));
		
		this.readIndex = indexIn;
	}
	
	
	/**
	 * Returns the current writeIndex of this buffer
	 * 
	 * @return the writeIndex
	 */
	public int getWriteIndex()
	{
		return this.writeIndex;
	}
	
	
	/**
	 * Sets the writeIndex of this buffer
	 * 
	 * @param indexIn the desired index. Must be
	 * 		{@code < {@link #getMaxSize_bytes()}}
	 * 
	 * @throws IndexOutOfBoundsException if {@code indexIn >= {@link #getMaxSize_bytes()}}
	 */
	public void setWriteIndex(int indexIn) throws IndexOutOfBoundsException
	{
		if( indexIn >= this.getMaxSize_bytes() ) throw new IndexOutOfBoundsException(String.format("idx: %d >= %d :maxSize", indexIn, this.getMaxSize_bytes()));
		
		this.writeIndex = indexIn;
	}
	
	
	/**
	 * Puts a byte into the buffer at the current writeIndex overwriting
	 * any data at that current position. The writeIndex is then incremented by 1.
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param byteIn the byte
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()}+1) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_byte(byte byteIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		this.nioBuffer.put(this.writeIndex, byteIn);
		this.writeIndex += 1;
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.size_bytes), this.parent.size_bytes);
		}
	}
	
	
	/**
	 * Puts a boolean into the buffer at the current writeIndex overwriting
	 * any data at that current position. The writeIndex is then incremented by 1.
	 * booleans are treated as a single byte of value 0 or 1
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param boolIn the boolean value
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()}+1) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_boolean(Boolean boolIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		this.nioBuffer.put(this.writeIndex, (boolIn ? (byte)1 : (byte)0));
		this.writeIndex += 1;
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.getSize_bytes()), this.parent.getSize_bytes());
		}
	}
	
	
	/**
	 * Puts an unsigned 16-bit integer into the buffer at the current writeIndex overwriting
	 * any data at that current position. The writeIndex is then incremented by 2.
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param uint16_leIn the unsigned 16-bit integer
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()}+2) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_uint16LE(short uint16_leIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		this.nioBuffer.putShort(this.writeIndex, uint16_leIn);
		this.writeIndex += 2;
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.getSize_bytes()), this.parent.getSize_bytes());
		}
	}
	
	
	/**
	 * Puts an unsigned 32-bit integer into the buffer at the current writeIndex overwriting
	 * any data at that current position. The writeIndex is then incremented by 4.
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param uint32_leIn the unsigned 32-bit integer
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()}+4) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_uint32LE(int uint32_leIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		this.nioBuffer.putInt(this.writeIndex, uint32_leIn);
		this.writeIndex += 4;
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.getSize_bytes()), this.parent.getSize_bytes());
		}
	}
	
	
	/**
	 * Puts an IEEE-754 value into the buffer at the current writeIndex overwriting
	 * any data at that current position. The writeIndex is then incremented by 4.
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param floatIn the floating point value
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()}+4) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_float(Float floatIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		this.nioBuffer.putFloat(this.writeIndex, floatIn);
		this.writeIndex += 4;
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.getSize_bytes()), this.parent.getSize_bytes());
		}
	}
	
	
	/**
	 * Puts a string into the buffer at the current writeIndex overwriting
	 * any data at that current position. All strings are internally prepended
	 * by a 2-byte length field and terminated with a NULL character.
	 * The writeIndex is then incremented by {@code {@link String#length()} + 3}
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param stringIn the string
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()} + {@link String#length()} + 3) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_string(String stringIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		if( (this.writeIndex + (stringIn.length() + 3)) > this.getMaxSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + %d + 3) > %d", this.writeIndex, stringIn.length(), this.getMaxSize_bytes())); 
		
		this.nioBuffer.putShort(this.writeIndex, (short)((stringIn.length()+1) & 0x0000FFFF)); 
		this.writeIndex += 2;
		for( byte currByte : stringIn.getBytes() )
		{
			this.nioBuffer.put(this.writeIndex, currByte);
			this.writeIndex += 1;
		}
		this.nioBuffer.put(this.writeIndex, (byte)0);
		this.writeIndex += 1;
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.getSize_bytes()), this.parent.getSize_bytes());
		}
	}
	
	
	/**
	 * Puts the source buffer into the current buffer at the current writeIndex overwriting
	 * any data at that current position. The writeIndex is then incremented by {@code {@link #getSize_bytes()}}
	 * of the source buffer.
	 * 
	 * <p>
	 * The buffer's resulting {@link #getSize_bytes()} will equal
	 * {@code MAX({@link #getSize_bytes()}, {@link #getWriteIndex()})}
	 * where {@link #getWriteIndex()} is the incremented writeIndex. If this operation
	 * grows the size of the buffer by more than the size of the input datatype, the
	 * content of the indices that have not had their value explicitly set will be undefined.
	 * </p>
	 * 
	 * <p>
	 * If this buffer was initialized using {@link #init_subsetOfCapacity(FixedByteBuffer, int)} OR
	 * {@link #init_subsetOfCapacity(FixedByteBuffer, int, int)}, the parent buffer's size will
	 * be be set to
	 * {@code MAX(this.startIndexInParent + {@link #getSize_bytes()}), parent.{@link #getSize_bytes()})}
	 * </p>
	 * 
	 * @param bufferIn the source buffer
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getWriteIndex()} + source{@link #getSize_bytes()}) > {@link #getMaxSize_bytes()}}
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 */
	public void put_rel_fbb(FixedByteBuffer bufferIn) throws IndexOutOfBoundsException, ReadOnlyBufferException
	{
		if( (this.writeIndex + bufferIn.getSize_bytes()) > this.getMaxSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + %d) > %d", this.writeIndex, bufferIn.getSize_bytes(), this.getMaxSize_bytes())); 
		
		for( int i = 0; i < bufferIn.getSize_bytes(); i++ )
		{
			this.nioBuffer.put(this.writeIndex, bufferIn.nioBuffer.get(i));
			this.writeIndex += 1;
		}
		
		// adjust our size
		this.size_bytes = Math.max(this.size_bytes, this.writeIndex);
		
		// see if we need to adjust our parent's size to match our new addition
		if( this.parent != null )
		{
			this.parent.size_bytes = Math.max((this.startIndexInParent + this.getSize_bytes()), this.parent.getSize_bytes());
		}
	}
	
	
	/**
	 * Returns the byte at the current readIndex.
	 * The readIndex is then incremented by 1.
	 * 
	 * @return the byte at the current readIndex
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getReadIndex()} + 1) > {@link #getSize_bytes()}} 
	 */
	public byte get_rel_byte() throws IndexOutOfBoundsException
	{
		byte retVal = this.get_abs_byte(this.readIndex);
		this.readIndex += 1;
		return retVal;
	}
	
	
	/**
	 * Returns the unsigned 16-bit integer starting at the current readIndex.
	 * The readIndex is then incremented by 2.
	 * 
	 * @return the unsigned 16-bit integer starting at the current readIndex
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getReadIndex()} + 2) > {@link #getSize_bytes()}} 
	 */
	public short get_rel_uint16LE() throws IndexOutOfBoundsException
	{
		short retVal = this.get_abs_uint16LE(this.readIndex);
		this.readIndex += 2;
		return retVal;		
	}
	

	/**
	 * Returns the unsigned 32-bit integer starting at the current readIndex.
	 * The readIndex is then incremented by 4.
	 * 
	 * @return the unsigned 32-bit integer starting at the current readIndex
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getReadIndex()} + 4) > {@link #getSize_bytes()}} 
	 */
	public int get_rel_uint32LE() throws IndexOutOfBoundsException
	{
		int retVal = this.get_abs_uint32LE(this.readIndex);
		this.readIndex += 4;
		return retVal;
	}
	

	/**
	 * Returns the IEEE-754 value starting at the current readIndex.
	 * The readIndex is then incremented by 4.
	 * 
	 * @return the floating point value starting at the current readIndex
	 * 
	 * @throws IndexOutOfBoundsException if initially {@code ({@link #getReadIndex()} + 4) > {@link #getSize_bytes()}} 
	 */
	public float get_rel_float() throws IndexOutOfBoundsException
	{
		float retVal = this.get_abs_float(this.readIndex);
		this.readIndex += 4;
		return retVal;
	}
	
	
	/**
	 * Returns the String starting at the current readIndex.
	 * All strings are internally prepended by a 2-byte length field and terminated with a NULL character.
	 * The readIndex is then incremented by {@code {@link String#length()} + 3}
	 * 
	 * @return the String starting at the current readIndex (length and NULL terminator are checked, but not returned)
	 * 		OR NULL if the string is not properly formatted (in which case readIndex remains unmodified)
	 * 
	 * @throws IndexOutOfBoundsException if initially
	 * 		{@code ({@link #getReadIndex()} + 3 + indicatedStringLength) > {@link #getSize_bytes()}}
	 */
	public String get_rel_string() throws IndexOutOfBoundsException
	{
		String retVal = this.get_abs_string(this.readIndex);
		this.readIndex += retVal.length() + 3;
		return retVal;
	}
	
	
	/**
	 * Returns a byte starting at the given index of the buffer.
	 * This function does not affect the readIndex. 
	 * 
	 * @param indexIn the starting index of the desired element
	 * 		within the buffer
	 * 
	 * @return the byte starting at the specified index
	 * 
	 * @throws IndexOutOfBoundsException if {@code (indexIn + 1) > {@link #getSize_bytes()}}
	 */
	public byte get_abs_byte(int indexIn) throws IndexOutOfBoundsException
	{
		if( (indexIn + 1) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + 1) > %d", indexIn, this.getSize_bytes()));
		
		return this.nioBuffer.get(indexIn);
	}
	
	
	/**
	 * Returns a 16-bit unsigned integer, in little-endian format
	 * starting at the given index of the buffer.
	 * This function does not affect the readIndex. 
	 * 
	 * @param indexIn the starting index of the desired element
	 * 		within the buffer
	 * 
	 * @return the 16-bit unsigned integer starting at the specified index
	 * 
	 * @throws IndexOutOfBoundsException if {@code (indexIn + 2) > {@link #getSize_bytes()}}
	 */
	public short get_abs_uint16LE(int indexIn) throws IndexOutOfBoundsException
	{
		if( (indexIn + 2) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + 2) > %d", indexIn, this.getSize_bytes()));
		
		return this.nioBuffer.getShort(indexIn);
	}
	
	
	/**
	 * Returns a 32-bit unsigned integer, in little-endian format
	 * starting at the given index of the buffer.
	 * This function does not affect the readIndex. 
	 * 
	 * @param indexIn the starting index of the desired element
	 * 		within the buffer
	 * 
	 * @return the 32-bit unsigned integer starting at the specified index
	 * 
	 * @throws IndexOutOfBoundsException if {@code (indexIn + 4) > {@link #getSize_bytes()}}
	 */
	public int get_abs_uint32LE(int indexIn) throws IndexOutOfBoundsException
	{
		if( (indexIn + 4) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + 4) > %d", indexIn, this.getSize_bytes()));
		
		return this.nioBuffer.getInt(indexIn);
	}
	
	
	/**
	 * Returns an IEEE-754 value (floating point)
	 * starting at the given index of the buffer.
	 * This function does not affect the readIndex. 
	 * 
	 * @param indexIn the starting index of the desired element
	 * 		within the buffer
	 * 
	 * @return the floating point value starting at the specified index
	 * 
	 * @throws IndexOutOfBoundsException if {@code (indexIn + 4) > {@link #getSize_bytes()}}
	 */
	public float get_abs_float(int indexIn) throws IndexOutOfBoundsException
	{
		if( (indexIn + 4) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + 4) > %d", indexIn, this.getSize_bytes()));
		
		return this.nioBuffer.getFloat(indexIn);
	}
	
	
	/**
	 * Returns a string which starts at the given index of the buffer.
	 * All strings are internally prepended by a 2-byte length field and terminated with a NULL character.
	 * 
	 * @param indexIn the starting index of the desired element
	 * 		within the buffer
	 * 
	 * @return the String starting at the specified index (length and NULL terminator are checked, but not returned)
	 *		OR NULL if the string is not properly formatted (in which case readIndex remains unmodified)
	 * 
	 * @throws IndexOutOfBoundsException if initially
	 * 		{@code (indexIn + 3 + indicatedStringLength) > {@link #getSize_bytes()}}
	 */
	public String get_abs_string(int indexIn) throws IndexOutOfBoundsException
	{
		// make sure we have enough for an empty string at least
		if( (indexIn + 3) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + 3) > %d", indexIn, this.getSize_bytes()));
		
		// get our length bytes
		int length_bytes = (((int)this.nioBuffer.getShort(indexIn)) & 0x0000FFFF) - 1;
		if( (indexIn + length_bytes + 1) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + %d + 1) > %d", indexIn, length_bytes, this.getSize_bytes()));
		indexIn += 2;
		
		// get our bytes
		byte[] bytes = new byte[length_bytes];
		for( int i = 0; i < length_bytes; i++ )
		{
			bytes[i] = this.nioBuffer.get(indexIn);
			indexIn += 1;
		}
		
		// now make sure we have our NULL terminator
		byte nullTerm = this.nioBuffer.get(indexIn);
		indexIn += 1;
		if( nullTerm != 0 ) return null;
		
		return new String(bytes);
	}
	
	
	/**
	 * Removes the byte at the specified location, copying all following
	 * bytes down to fill the empty spot
	 * 
	 * <p>
	 * This method can only be called on a buffer which was instantiated used
	 * {@link #FixedByteBuffer(int)}, any other instantiation type will yield
	 * an IllegalStateException
	 * </p>
	 * 
	 * @param indexIn the index of the byte to remove
	 * 
	 * @return the byte that was removed
	 * 
	 * @throws IndexOutOfBoundsException if {@code (indexIn + 1) > {@link #getSize_bytes()}} 
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 * @throws IllegalStateException if this buffer was not allocated using {@link #FixedByteBuffer(int)}
	 */
	public byte remove_abs_byte(int indexIn) throws IndexOutOfBoundsException, ReadOnlyBufferException, IllegalStateException
	{
		if( (indexIn + 1) > this.getSize_bytes() )
			throw new IndexOutOfBoundsException(String.format("(%d + 1) > %d", indexIn, this.getSize_bytes()));
		if( this.nioBuffer.isReadOnly() ) throw new ReadOnlyBufferException();
		// make sure that we don't have a parent (no great way of handling this case)
		if( this.parent != null ) throw new IllegalStateException("this buffer must NOT have a parent");
		
		// store our return value
		byte retVal = this.nioBuffer.get(indexIn);
		
		// copy the remaining values down
		for( int i = 0; i < (this.getSize_bytes()-1); i++ )
		{
			this.nioBuffer.put(i, this.nioBuffer.get(i+1));
		}
		this.size_bytes -= 1;
		
		return retVal;
	}
	
	
	/**
	 * Clears the buffer, discarding any elements current contained
	 * within it. Both read and write indices will be reset to 0.
	 * This function will result in an empty buffer, but the
	 * underlying memory may still contain the original data.
	 * 
	 * <p>
	 * This method can only be called on a buffer which was instantiated used
	 * {@link #FixedByteBuffer(int)}, any other instantiate type will yield
	 * an IllegalStateException
	 * </p>
	 * 
	 * @throws ReadOnlyBufferException if this buffer is read-only
	 * @throws IllegalStateException if this buffer was not allocated using {@link #FixedByteBuffer(int)}
	 */
	public void clear() throws ReadOnlyBufferException
	{
		if( this.nioBuffer.isReadOnly() ) throw new ReadOnlyBufferException();
		// make sure that we don't have a parent (no great way of handling this case)
		if( this.parent != null ) throw new IllegalStateException("this buffer must NOT have a parent");
		
		this.readIndex = 0;
		this.writeIndex = 0;
		this.size_bytes = 0;
	}
	
	
	/**
	 * Creates a trimmed copy of the underlying bytes of this array
	 * and returns it (discarding any free, unused bytes)
	 * 
	 * @return a trimmed copy of the underlying byte array
	 */
	public byte[] toByteArray()
	{
		byte[] retVal = new byte[this.getSize_bytes()];
		
		this.nioBuffer.position(0);
		this.nioBuffer.get(retVal);
		
		return retVal;
	}
	
	
	/**
	 * Creates and returns a string representation of the contents
	 * of the array, formatted as hexadecimal characters.
	 * Example:
	 * <code>
	 * { 0x80, 0x81, 0x82, 0x00 }
	 * </code>
	 * @return a string representation of the contents of the array 
	 */
	public String toHexString()
	{
		StringBuilder builder = new StringBuilder();
		
		builder.append("{ ");
		for( int i = 0; i < this.getSize_bytes(); i++ )
		{
			builder.append(String.format("0x%02X ", this.get_abs_byte(i)));
		}
		builder.append("}");
		
		return builder.toString();
	}
	
	
	/**
	 * Compares the contents of this buffer to the specified
	 * byte array.
	 * 
	 * @param bufferIn the byte array that should be compared
	 * 
	 * @return true if the byte array length is the same length
	 * 		as this byte buffer AND if all of the bytes in buffers
	 * 		are identical.
	 */
	public boolean equals(byte[] bufferIn)
	{
		// make sure the sizes match
		if( this.getSize_bytes() != bufferIn.length ) return false;
		
		// sizes match, now compare the bytes
		for( int i = 0; i < this.getSize_bytes(); i++ )
		{
			if( this.nioBuffer.get(i) != bufferIn[i] ) return false;
		}
		
		// if we made it here, we were successful
		return true;
	}
	
	
	/**
	 * Compares the contents of this buffer to the specified
	 * buffer
	 * 
	 * @param bufferIn the ByteBuffer that should be compared
	 * 
	 * @return true if the buffer length is the same length
	 * 		as this byte buffer AND if all of the bytes in buffers
	 * 		are identical.
	 */
	public boolean equals_fbb(FixedByteBuffer bufferIn)
	{
		// record our current positions
		int currPos_me = this.nioBuffer.position();
		int currPos_target = bufferIn.nioBuffer.position();
		
		// rewind so we can use #compareTo(ByteBuffer)
		this.nioBuffer.rewind();
		bufferIn.nioBuffer.rewind();
		
		int retVal = this.nioBuffer.compareTo(bufferIn.nioBuffer);
		
		// reset our positions
		this.nioBuffer.position(currPos_me);
		bufferIn.nioBuffer.position(currPos_target);
		
		// return the value
		return (retVal == 0);
	}
}
