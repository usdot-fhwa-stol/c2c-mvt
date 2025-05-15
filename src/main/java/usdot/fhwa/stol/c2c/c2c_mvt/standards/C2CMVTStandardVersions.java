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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * This class represents a C2C standard and all of its versions.
 */
public class C2CMVTStandardVersions 
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
	C2CMVTStandardVersions(String standardName, ObjectNode standard) throws JsonMappingException, JsonProcessingException
	{
		ObjectNode versions = (ObjectNode)standard.get("versions");
		Iterator<Entry<String, JsonNode>> versionIterator = versions.fields();
		ObjectMapper objectMapper = new ObjectMapper();
		while (versionIterator.hasNext())
		{
			Entry<String, JsonNode> versionEntry = versionIterator.next();
			String versionName = versionEntry.getKey();
			C2CMVTStandardVersion version = objectMapper.readValue(versionEntry.getValue().toString(), C2CMVTStandardVersion.class);
			version.setDecoder("usdot.fhwa.stol.c2c.c2c_mvt.decoders." + version.getDecoder());
			version.setParser("usdot.fhwa.stol.c2c.c2c_mvt.parsers." + version.getParser());
			version.setValidator("usdot.fhwa.stol.c2c.c2c_mvt.validators." + version.getValidator());
			c2cStandardVersionMap.put(versionName, version);
		}
	}
}
