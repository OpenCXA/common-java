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
package org.cxa.misc;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;


/**
 * @author Christopher Armenio
 *
 */
public class ClassUtils
{
	private static final Gson GSON = new Gson();
	
	
	// converts a map containing field names to a class
	public static Object classFromPojoMap(Object pojoIn, Class<?> expectedClassIn)
	{
		if( pojoIn == null ) throw new IllegalArgumentException("pojo cannot be null");
		if( expectedClassIn == null ) throw new IllegalArgumentException("expected class cannot be null");
		return GSON.fromJson(GSON.toJson(pojoIn), expectedClassIn);
	}
	
	
	public static <T extends Object> T coerceToType(Object objectIn, Class<T> clazz)
	{
		return coerceToType(objectIn, clazz, null);
	}
	
	
	@SuppressWarnings("unchecked")
	public static <T extends Object> T coerceToType(Object objectIn, Class<T> clazz, T defaultValIn)
	{
		return ((objectIn != null) && clazz.isAssignableFrom(objectIn.getClass())) ? (T)objectIn : defaultValIn;
	}
	
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> objectArrayToList(Object[] objectsIn, Class<T> clazz)
	{
		if( objectsIn == null ) return null;
		
		List<T> retVal = new ArrayList<T>();
		
		for( Object currObj : objectsIn )
		{
			if( clazz.isAssignableFrom(currObj.getClass()) ) retVal.add((T) currObj); 
		}
		
		return retVal;
	}
}
