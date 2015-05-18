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
package org.cxa.misc.sqLiteUtils;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cxa.misc.sqLiteUtils.SqLiteDatabase.DatabaseException;
import org.cxa.misc.sqLiteUtils.SqLiteTableColumn.Modifier;
import org.cxa.misc.stringUtils.DelimitedStringBuilder;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteConstants;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * Protected class for implementing a SQLite table. Used
 * ONLY by SqLiteDatabaseManager internals.
 * 
 * @author Christopher Armenio
 */
public class SqLiteTable
{
	private final String name;
	private final Class<?> modelClass;
	private final SQLiteQueue queue;
	
	
	protected SqLiteTable(String nameIn, Class<?> modelClassIn, SQLiteQueue queueIn)
	{
		this.name = nameIn;
		this.modelClass = modelClassIn;
		this.queue = queueIn;
	}
	
	
	protected String getName()
	{
		return this.name;
	}
	
	
	protected Object[] listAllElements() throws DatabaseException
	{	
		SQLiteJob<Object[]> job = this.queue.execute(new SQLiteJob<Object[]>()
		{
			@Override
			protected Object[] job(SQLiteConnection conn) throws Throwable
			{
				List<Object> retVal = new ArrayList<Object>();
				SQLiteStatement st = null;
				try
				{
					st = conn.prepare(String.format("SELECT * FROM %s;", SqLiteTable.this.name));
					while(st.step())
					{
						Object newObject = SqLiteTable.this.createObjectFromCurrentStatement(st);
						if( newObject == null ) return null;
						
						retVal.add(newObject);
					}
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				return retVal.toArray();	
			}
		});
		Object[] retVal = job.complete();
		if( job.getError() != null ) throw new DatabaseException(job.getError());
		return retVal;
	}
	
	
	protected Object[] getElementsByColumnValues(Map<String, ?> colValsIn) throws DatabaseException
	{
		SQLiteJob<Object[]> job = this.queue.execute(new SQLiteJob<Object[]>()
		{
			@Override
			protected Object[] job(SQLiteConnection conn) throws Throwable
			{
				List<Object> retVal = new ArrayList<Object>();
				SQLiteStatement st = null;
				try
				{
					DelimitedStringBuilder dsb = new DelimitedStringBuilder(" AND ");
					for( String currFieldName : colValsIn.keySet() )
					{
						dsb.append(String.format("%s=?", currFieldName));
					}
					
					st = conn.prepare(String.format("SELECT * FROM %s WHERE %s;", SqLiteTable.this.name, dsb.toString()));
					Object[] vals = colValsIn.values().toArray();
					for( int i = 0; i < colValsIn.size(); i++ )
					{
						if( !bindForStatement(st, i+1, vals[i]) ) throw new DatabaseException(
								String.format("error binding data for sql query: '%s'::'%s'", vals[i].getClass().getSimpleName(), vals[i].toString()));
					}
					while(st.step())
					{	
						Object newObject = SqLiteTable.this.createObjectFromCurrentStatement(st);
						if( newObject == null ) return null;
						
						retVal.add(newObject);
					}
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				return retVal.toArray();
			}
		});
		Object[] retVal = job.complete();
		if( job.getError() != null ) throw new DatabaseException(job.getError());
		return retVal;
	}
	
	
	protected void insertRow(Object dataObjectIn) throws DatabaseException
	{
		SQLiteJob<Void> job = this.queue.execute(new SQLiteJob<Void>()
		{
			@Override
			protected Void job(SQLiteConnection conn) throws Throwable
			{
				SQLiteStatement st = null;
				try
				{
					DelimitedStringBuilder dsb_colNames = new DelimitedStringBuilder(",");
					Map<String, ?> cols = SqLiteTable.this.getColumnValues(dataObjectIn);
					for( String currColName : cols.keySet() )
					{
						dsb_colNames.append(currColName);
					}
					
					DelimitedStringBuilder dsb_colVals = new DelimitedStringBuilder(",");
					for( int i = 0; i < cols.size(); i++ )
					{
						dsb_colVals.append("?");
					}
					
					st = conn.prepare(String.format("INSERT INTO %s (%s) VALUES (%s);", SqLiteTable.this.name, dsb_colNames.toString(), dsb_colVals.toString()));
					Object[] colVals = cols.values().toArray();
					for( int i = 0; i < colVals.length; i++ )
					{
						if( !bindForStatement(st, i+1, colVals[i]) ) throw new DatabaseException(
								String.format("error binding data for sql query: '%s'::'%s'", colVals[i].getClass().getSimpleName(), colVals[i].toString()));
					}
			
					st.step();
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				return null;
			}
		});
		job.complete();
		if( job.getError() != null ) throw new DatabaseException(job.getError());
	}
	
	
	protected boolean updateRow(Object dataObjectIn) throws DatabaseException
	{
		SQLiteJob<Boolean> job =  this.queue.execute(new SQLiteJob<Boolean>()
		{
			@Override
			protected Boolean job(SQLiteConnection conn) throws Throwable
			{
				SQLiteStatement st = null;
				try
				{
					DelimitedStringBuilder dsb_cols = new DelimitedStringBuilder(",");
					Map<String, ?> cols = SqLiteTable.this.getColumnValues(dataObjectIn);
					for( String currColName : cols.keySet() )
					{
						dsb_cols.append(currColName + "=?");
					}
					
					Entry<String, Object> primaryKey = getPrimaryKeyValueForDataObject(dataObjectIn);
					if( primaryKey == null ) return false;
					String whereClause = String.format("%s=?", primaryKey.getKey());
					
					st = conn.prepare(String.format("UPDATE %s SET %s WHERE %s;", SqLiteTable.this.name, dsb_cols.toString(), whereClause));
					Object[] colVals = cols.values().toArray();
					int i = 0;
					for( i = 0; i < cols.size(); i++ )
					{
						if( !bindForStatement(st, i+1, colVals[i]) ) throw new DatabaseException(
								String.format("error binding data for sql query: '%s'::'%s'", colVals[i].getClass().getSimpleName(), colVals[i].toString()));
					}
					if( !bindForStatement(st, i+1, primaryKey.getValue()) ) throw new DatabaseException(
							String.format("error binding data for sql query: '%s'::'%s'", primaryKey.getValue().getClass().getSimpleName(), primaryKey.getValue().toString()));
			
					if( !st.step() ) return true;
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				return false;
			}
		});
		Boolean retVal = job.complete();
		if( job.getError() != null ) throw new DatabaseException(job.getError());
		return retVal;
	}
	
	
	protected boolean deleteRowsByColumnValues(Map<String, ?> colValsIn) throws DatabaseException
	{
		SQLiteJob<Boolean> job =  this.queue.execute(new SQLiteJob<Boolean>()
		{
			@Override
			protected Boolean job(SQLiteConnection conn) throws Throwable
			{
				SQLiteStatement st = null;
				try
				{
					DelimitedStringBuilder dsb = new DelimitedStringBuilder(" AND ");
					for( String currFieldName : colValsIn.keySet() )
					{
						dsb.append(String.format("%s=?", currFieldName));
					}
					
					st = conn.prepare(String.format("DELETE FROM %s WHERE %s;", SqLiteTable.this.name, dsb.toString()));
					Object[] vals = colValsIn.values().toArray();
					for( int i = 0; i < colValsIn.size(); i++ )
					{
						if( !bindForStatement(st, i+1, vals[i]) ) throw new DatabaseException(
								String.format("error binding data for sql query: '%s'::'%s'", vals[i].getClass().getSimpleName(), vals[i].toString()));
					}
					if( !st.step() ) return true;
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				return false;
			}
		});
		Boolean retVal = job.complete();
		if( job.getError() != null ) throw new DatabaseException(job.getError());
		return retVal;
	}
	
	
	protected boolean verifyTableStructure() throws DatabaseException
	{
		// first, we need to verify if the table exists
		if( !doesTableExist() ) return false;
		
		// table exists...now we need to check the actual structure
		if( !doesTableHaveCorrectColumns() ) return false;
		
		return true;
	}
	
	
	protected boolean doesTableExist() throws DatabaseException
	{
		return this.queue.execute(new SQLiteJob<Boolean>()
		{
			@Override
			protected Boolean job(SQLiteConnection conn) throws Throwable
			{
				Boolean retVal = false;
				SQLiteStatement st = null;
				try
				{
					st = conn.prepare("SELECT * FROM sqlite_master WHERE type='table' AND name=?;");
					st.bind(1, SqLiteTable.this.name);
					retVal = st.step();
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				return retVal;
			}
		}).complete();
	}
	
	
	protected boolean doesTableHaveCorrectColumns() throws DatabaseException
	{
		return this.queue.execute(new SQLiteJob<Boolean>()
		{
			@Override
			protected Boolean job(SQLiteConnection conn) throws Throwable
			{
				// create our columns and return values array
				Map<String, SqLiteTableColumn> cols = SqLiteTable.this.getColumns();
				Map<String, Boolean> returnValues = new HashMap<String, Boolean>();
				for( String currColName : cols.keySet() )
				{
					returnValues.put(currColName, Boolean.FALSE);
				}
				
				// perform our query
				SQLiteStatement st = null;
				try
				{
					st = conn.prepare(String.format("PRAGMA TABLE_INFO(%s);", SqLiteTable.this.name));
					int stepIndex = 0;
					while(st.step())
					{
						// make sure we don't have extra columns
						if( stepIndex >= cols.size() ) break;
						// check the column types
						if( (st.columnType(1) != SQLiteConstants.SQLITE_TEXT) || (st.columnType(2) != SQLiteConstants.SQLITE_TEXT) ) continue; 
						
						// see if the model has the column
						SqLiteTableColumn colAnn = cols.get(st.columnString(1));
						if( colAnn == null) return false;
						
						// now see if the type matches
						if( colAnn.dataType().toString().equals(st.columnString(2)) ) returnValues.put(st.columnString(1), Boolean.TRUE);
						
						stepIndex++;
					}
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				
				// iterate and determine if everything matched
				for( Boolean currRetVal : returnValues.values() )
				{
					if( !currRetVal ) return false;
				}
				return true;
			}
		}).complete();
	}
	
	
	protected void createTable() throws DatabaseException
	{
		this.queue.execute(new SQLiteJob<Void>()
		{
			@Override
			protected Void job(SQLiteConnection conn) throws Throwable
			{
				// create our columns and return values array
				Map<String, SqLiteTableColumn> cols = SqLiteTable.this.getColumns();
				
				DelimitedStringBuilder dsb = new DelimitedStringBuilder(", ");
				Iterator<Entry< String, SqLiteTableColumn>> it = cols.entrySet().iterator();
				while( it.hasNext() )
				{
					Entry<String, SqLiteTableColumn> currEntry = it.next();
					
					dsb.append(String.format("\"%s\" %s %s", currEntry.getKey(), currEntry.getValue().dataType(), getModifiersString(currEntry.getValue().modifiers())));
				}
				String query = String.format("CREATE TABLE \"%s\" (%s);", SqLiteTable.this.name, dsb.toString()); 
				
				// perform our query
				SQLiteStatement st = null;
				try
				{
					st = conn.prepare(query.toString());
					st.stepThrough();
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				return null;
			}
		}).complete();
	}
	
	
	protected void dropTable() throws DatabaseException
	{
		this.queue.execute(new SQLiteJob<Void>()
		{
			@Override
			protected Void job(SQLiteConnection conn) throws Throwable
			{
				SQLiteStatement st = null;
				try
				{
					st = conn.prepare("DROP TABLE " + SqLiteTable.this.name + ";");
					st.stepThrough();
				}
				catch (SQLiteException e) { throw new DatabaseException(e); }
				finally
				{
					if( st != null ) st.dispose();
				}
				return null;
			}
		}).complete();
	}
	
	
	private Map<String, SqLiteTableColumn> getColumns()
	{
		Map<String, SqLiteTableColumn> cols = new HashMap<String, SqLiteTableColumn>();
		for( Field currField : this.modelClass.getDeclaredFields() )
		{
			SqLiteTableColumn colAnn = currField.getAnnotation(SqLiteTableColumn.class);
			if( colAnn != null )
			{
				cols.put(currField.getName(), colAnn);
			}
		}
		return cols;
	}
	
	
	private Map<String, ?> getColumnValues(Object dataObjectIn)
	{
		HashMap<String, Object> cols = new HashMap<String, Object>();
		for( Field currField : this.modelClass.getDeclaredFields() )
		{
			SqLiteTableColumn colAnn = currField.getAnnotation(SqLiteTableColumn.class);
			if( (colAnn != null) && !isColumnPrimaryAutoIncrement(colAnn) )
			{
				try
				{
					if( !currField.isAccessible() ) currField.setAccessible(true);
					Object val = currField.get(dataObjectIn);
					if( val != null ) cols.put(currField.getName(), val);
				}
				catch (IllegalArgumentException | IllegalAccessException e) { }
			}
		}
		return cols;
	}
	
	
	private Object createObjectFromCurrentStatement(SQLiteStatement st) throws DatabaseException
	{
		Object retVal = null;
		try
		{
			retVal = this.modelClass.newInstance();
			
			for( int i = 0; i < st.columnCount(); i++ )
			{
				String fieldName = st.getColumnName(i);
				
				Field classField = this.modelClass.getDeclaredField(fieldName);
				if( classField == null ) throw new DatabaseException(String.format("unknown field name in model class: '%s'", fieldName));
				
				boolean wasAccessible = classField.isAccessible();
				try
				{
					if( !wasAccessible ) classField.setAccessible(true);
					
					// handle our special cases
					if( (classField.getType() == Boolean.class) && (st.columnType(i) == SQLiteConstants.SQLITE_INTEGER) )
					{
						classField.set(retVal, (st.columnInt(i) > 0) ? Boolean.TRUE : Boolean.FALSE);
					}
					else classField.set(retVal, st.columnValue(i));
				}
				finally
				{
					if( !wasAccessible ) classField.setAccessible(false);
				}
			}
		}
		catch( InstantiationException e )
		{
			throw new DatabaseException("Error instantiating model object, likely no public default constructor");
		}
		catch( IllegalAccessException | SQLiteException | NoSuchFieldException | SecurityException e )
		{
			throw new DatabaseException(e);
		}
		
		return retVal;
	}
	
	
	private Entry<String, Object> getPrimaryKeyValueForDataObject(Object dataObjectIn)
	{
		for( Field currField : this.modelClass.getDeclaredFields() )
		{
			SqLiteTableColumn colAnn = currField.getAnnotation(SqLiteTableColumn.class);
			if( (colAnn != null) && isColumnPrimaryAutoIncrement(colAnn) )
			{
				Entry<String, Object> retVal = null;
				boolean wasAccessible = currField.isAccessible();
				try
				{
					if( !wasAccessible ) currField.setAccessible(true);
					
					retVal =  new AbstractMap.SimpleEntry<String, Object>(currField.getName(), currField.get(dataObjectIn));
				}
				catch (IllegalArgumentException | IllegalAccessException e) { }
				finally
				{
					if( !wasAccessible ) currField.setAccessible(false);
				}
				return retVal;
			}
		}
		
		return null;
	}
	
	
	private static String getModifiersString(Modifier modifiersIn[])
	{
		DelimitedStringBuilder dsb = new DelimitedStringBuilder(" ");
		if( modifiersIn == null ) return "";
		
		for( Modifier currMod : modifiersIn )
		{
			dsb.append(currMod.toString());
		}
		
		return dsb.toString();
	}
	
	
	private static boolean isColumnPrimaryAutoIncrement(SqLiteTableColumn colAnnotationIn)
	{
		List<Modifier> modifiers = Arrays.asList(colAnnotationIn.modifiers());
		return modifiers.contains(Modifier.AUTO_INCREMENT) && modifiers.contains(Modifier.PRIMARY_KEY);
	}
	
	
	private static boolean bindForStatement(SQLiteStatement st, int indexIn, Object dataIn) throws SQLiteException
	{
		if( dataIn instanceof Double ) st.bind(indexIn, (Double)dataIn);
		else if( dataIn instanceof Integer ) st.bind(indexIn, (Integer)dataIn);
		else if( dataIn instanceof Long ) st.bind(indexIn, (Long)dataIn);
		else if( dataIn instanceof String ) st.bind(indexIn, (String)dataIn);
		else if( dataIn instanceof Boolean ) st.bind(indexIn, ((Boolean)dataIn) ? 1 : 0);
		else return false;
		
		return true;
	}
}
