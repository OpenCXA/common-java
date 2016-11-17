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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used to define which fields (member variables) 
 * of a datatype object will have columns within a @see SqLiteDatabase 
 * 
 * @author Christopher Armenio
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SqLiteTableColumn
{
	/**
	 * Defines what kind of SQLite datatype will be used
	 * to represent the field.
	 */
	public enum DataType
	{
		/**
		 * Used for all integer types (byte, short, integer, long)
		 */
		INTEGER("INTEGER"),
		
		/**
		 * Used for Strings
		 */
		TEXT("TEXT"),
		
		/**
		 * Used for boolean values
		 */
		BOOLEAN("BOOL");
		
		
		final String str;
		private DataType(String stringRepIn) { this.str = stringRepIn; }
		
		@Override public String toString() { return this.str; }
	}
	
	
	/**
	 * Used to define which SQLite modifiers should be included in the
	 * column definition for the target field / member variable.
	 */
	public enum Modifier
	{
		/**
		 * This column is a primary key. Should only have one per table.
		 */
		PRIMARY_KEY("PRIMARY KEY"),
		
		/**
		 * Auto-increments the value of this field each time a row is inserted.
		 * Should only be used with {@link DataType#INTEGER} types. 
		 */
		AUTO_INCREMENT("AUTOINCREMENT"),
		
		/**
		 * Specifies that this field cannot be NULL
		 */
		NOT_NULL("NOT NULL");
		
		final String str;
		private Modifier(String stringRepIn) { this.str = stringRepIn; }
		
		@Override public String toString() { return this.str; }
	}
	
	/**
	 * Specifies that SQLite datatype that will be used to represent the
	 * contents of this field.
	 * @return the datatype
	 */
	DataType dataType();
	
	/**
	 * Specifies any special modifiers for the column definition.
	 * @return any modifiers
	 */
	Modifier[] modifiers();
}
