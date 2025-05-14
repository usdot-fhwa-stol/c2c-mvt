/*
 * Copyright (C) 2025 LEIDOS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package usdot.fhwa.stol.c2c.c2c_mvt.standards;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;

/**
 * This class represents a C2C standard and all of its versions.
 */
public class C2CMVTStandard 
{
	/**
	 * A map of all the versions of this C2C standard. The key is the version name
	 * and the value is the C2CMVTStandardVersion object.
	 */
	Map<String, C2CMVTStandardVersion> c2cStandardVersionMap = new HashMap<>();


	/**
	 * Creates a C2CMVTStandard object from the given JSON object which is detailed in the class
	 * description of {@link C2CMVTStandards}.
	 * @param standardName the name of the standard
	 * @param standard the JSON object representing the standard
	 */
	C2CMVTStandard(String standardName, JsonObject standard)
	{
		JsonObject versions = (JsonObject)standard.get("versions");
		for (Entry<JsonString, JsonValue> versionEntry : versions.getProperties().entrySet())
		{
			String versionName = versionEntry.getKey().getValue();
			c2cStandardVersionMap.put(versionName, new C2CMVTStandardVersion((JsonObject)versionEntry.getValue()));
		}
	}
}
