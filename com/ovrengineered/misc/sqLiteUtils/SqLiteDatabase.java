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
package com.ovrengineered.misc.sqLiteUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.almworks.sqlite4java.SQLiteQueue;

/**
 * This is a class designed for working with SQLite databases
 * in a generic fashion (similar to GSON). This class is
 * thread-safe as it uses the SQLite4Java-provided JobQueues.
 * 
 * @author Christopher Armenio
 */
public class SqLiteDatabase
{
	static
	{
		java.util.logging.Logger.getLogger("com.almworks.sqlite4java").setLevel(java.util.logging.Level.OFF);
	}
	
	
	/**
	 * Wrapper class for exceptions occurring during database manipulation
	 * @author Christopher Armenio
	 */
	public static class DatabaseException extends Exception
	{
		private static final long serialVersionUID = 7664177931634716366L;
		
		/**
		 * Creates a DatabaseException with the given throwable as the cause
		 * 
		 * @param eIn the cause of the exception
		 */
		public DatabaseException(Throwable eIn)
		{
			super(eIn);
		}
		
		/**
		 * Creates a DatabaseException with the provided message
		 * 
		 * @param messageIn the message describing the exception
		 */
		public DatabaseException(String messageIn)
		{
			super(messageIn);
		}
	}
	
	
	private final List<SqLiteTable> tables;
	private final SQLiteQueue queue;
	
	
	/**
	 * Creates a SQLiteDatabase object using the given database file as
	 * the underlying data source. Once opened, the structure of the database
	 * tables will be checked against the structure provided in the parameters.
	 * 
	 * @param dbPathIn path the SQLite database file
	 * @param tablesIn a map which maps table names to the underlying class of the
	 * 		data object stored in each row. Class fields should be annotated using
	 * 		@see SQLiteTableColumn.
	 * @param restructureTableIfNeededIn if true, the database file will be restructured
	 * 		as needed to match the structure provided in 'tablesIn'. Note: Restructuring
	 * 		MAY be destructive.
	 * 
	 * @throws DatabaseException if an error occurs during opening/verification of the database
	 */
	public SqLiteDatabase(File dbPathIn, Map<String, Class<?>> tablesIn, boolean restructureTableIfNeededIn) throws DatabaseException
	{	
		this.queue = new SQLiteQueue(dbPathIn);
		this.queue.start();
		
		// create our tables
		List<SqLiteTable> tmpTables = new ArrayList<SqLiteTable>();
		Iterator<Entry<String, Class<?>>> it = tablesIn.entrySet().iterator();
		while( it.hasNext() )
		{
			Entry<String, Class<?>> currEntry = it.next();
			tmpTables.add(new SqLiteTable(currEntry.getKey(), currEntry.getValue(), this.queue));
		}
		this.tables = tmpTables;
		
		// now verify their structure
		for( SqLiteTable currTable : this.tables )
		{
			if( !currTable.verifyTableStructure() )
			{
				// table structure isn't right
				if( !restructureTableIfNeededIn ) throw new DatabaseException(String.format("Invalid table structure for '%s' and no authority to fix it!", currTable.getName()));
				
				// if we made it here, we have authority to fix it!
				if( !currTable.doesTableExist() ) currTable.createTable();
				else if( !currTable.doesTableHaveCorrectColumns() )
				{
					// TODO: do something more appropriate here
					currTable.dropTable();
					currTable.createTable();
				}
			}
		}
	}
	
	
	/**
	 * @return true if the database is open, false if not
	 */
	public boolean isOpen()
	{
		return (this.queue != null) && !this.queue.isStopped();
	}
	
	
	/**
	 * Returns an array of all objects in the provided table.
	 * Returned objects will be of the proper type for 
	 * the provided table (as defined during instantiation).
	 * 
	 * @param tableNameIn the name of the table
	 * @return an array of objects contained within the table
	 * 		Note: be sure to check for a zero-length object
	 * 		array as it cannot be cast to an array of the
	 * 		desired object class. 
	 * 
	 * @throws DatabaseException if an error occurs
	 */
	public Object[] listAllElementsInTable(String tableNameIn) throws DatabaseException
	{
		SqLiteTable table = this.getTableByName(tableNameIn);
		if( table == null ) throw new DatabaseException(String.format("uknown table: '%s'", tableNameIn));
		return table.listAllElements();
	}
	
	
	/**
	 * Returns an array of all elements matching the provided
	 * column/field values for the provided table.
	 * 
	 * @param tableNameIn the name of the table
	 * @param colValsIn a mapping of column/field name to desired value. If the
	 * 		values match, the row/object will be included in the results.
	 * 
	 * @return an array of objects with matching values for the provided columns/fields
	 * 		Note: be sure to check for a zero-length object array as it cannot be cast
	 * 		to an array of the desired object class.
	 * 
	 * @throws DatabaseException if an error occurs
	 */
	public Object[] getElementsByColumnValues(String tableNameIn, Map<String, ?> colValsIn) throws DatabaseException
	{
		SqLiteTable table = this.getTableByName(tableNameIn);
		if( table == null ) throw new DatabaseException(String.format("uknown table: '%s'", tableNameIn));
		return table.getElementsByColumnValues(colValsIn);
	}
	
	
	/**
	 * Inserts a given row into table using the dataobject as the source
	 * 
	 * @param tableNameIn the name of the table
	 * @param dataObjectIn a data object representing the row to add. MUST
	 * 		be of the same datatype provided for the table in the instantiation
	 * 		of the SqLiteDatabase class
	 * 
	 * @throws DatabaseException if an error occurs
	 */
	public void insertRow(String tableNameIn, Object dataObjectIn) throws DatabaseException
	{
		SqLiteTable table = this.getTableByName(tableNameIn);
		if( table == null ) throw new DatabaseException(String.format("uknown table: '%s'", tableNameIn));
		table.insertRow(dataObjectIn);
	}
	
	
	/**
	 * Updates a given row in the table with the new data object
	 * 
	 * @param tableNameIn the name of the table
	 * @param dataObjectIn a data object representing the row to add. MUST
	 * 		be of the same datatype provided for the table in the instantiation
	 * 		of the SqLiteDatabase class
	 *  
	 * @return true if a row has been successfully updated, false if the
	 * 		primary key field/member (as defined in the instantiation of the
	 * 		SqLiteDatabase class) does not match any entries in the database
	 * 
	 * @throws DatabaseException if an error occurs
	 */
	public boolean updateRow(String tableNameIn, Object dataObjectIn) throws DatabaseException
	{
		SqLiteTable table = this.getTableByName(tableNameIn);
		if( table == null ) throw new DatabaseException(String.format("uknown table: '%s'", tableNameIn));
		return table.updateRow(dataObjectIn);
	}
	
	
	/**
	 * Deletes all rows in the database where all elements matching the provided
	 * column/field values for the provided table.
	 * 
	 * @param tableNameIn the name of the table
	 * @param colValsIn a mapping of column/field name to desired value. If the
	 * 		values match, the row/object will be included in the results.
	 * 
	 * @return true if one or more rows have been deleted, false if 
	 * 		no rows were deleted
	 * 
	 * @throws DatabaseException if an error occurs
	 */
	public boolean deleteRowsByColumnValues(String tableNameIn, Map<String, ?> colValsIn ) throws DatabaseException
	{
		SqLiteTable table = this.getTableByName(tableNameIn);
		if( table == null ) throw new DatabaseException(String.format("uknown table: '%s'", tableNameIn));
		return table.deleteRowsByColumnValues(colValsIn);
	}
	
	
	private SqLiteTable getTableByName(String nameIn)
	{
		for( SqLiteTable currTable : this.tables )
		{
			if( currTable.getName().equals(nameIn) ) return currTable;
		}
		
		return null;
	}
}
