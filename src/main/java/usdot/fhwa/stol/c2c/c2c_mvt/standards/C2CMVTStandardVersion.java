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

import java.util.ArrayList;
import java.util.List;

import com.github.erosb.jsonsKema.JsonArray;
import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;

/**
 * This class represents a version of a C2C standard and includes the necessary fields for
 * message validation.
 */
public class C2CMVTStandardVersion 
{
	/*
	 * A list of encodings supported by this version of a C2C standard.
	 */
	List<String> encodingsList;


	/*
	 * A list of message types supported by this version of a C2C standard. Can be empty
	 * if the messages are self defining
	 */
	List<String> messageTypesList;


	/*
	 * The fully qualified name of the decoder class for this version of a C2C standard.
	 */
	String decoderFQN;


	/*
	 * The fully qualified name of the parser class for this version of a C2C standard.
	 */
	String parserFQN;


	/*
	 * The fully qualified name of the validator class for this version of a C2C standard.
	 */
	String validatorFQN;


	/*
	 * The name of the schema file for this version of a C2C standard.
	 */
	String schemaFile;

	/**
	 * 
	 * @param version
	 */
	C2CMVTStandardVersion(JsonObject version)
	{
		JsonArray encodingArray = (JsonArray)version.get("encodings");
		encodingsList = new ArrayList<>(encodingArray.length());
		for (JsonValue encoding : encodingArray.getElements())
		{
			encodingsList.add(((JsonString)encoding).getValue());
		}

		JsonArray messageTypeArray = (JsonArray)version.get("messageTypes");
		messageTypesList = new ArrayList<>(messageTypeArray.length());
		for (JsonValue messageType : messageTypeArray.getElements())
		{
			messageTypesList.add(((JsonString)messageType).getValue());
		}

		decoderFQN = "usdot.fhwa.stol.c2c.c2c_mvt.decoders." + ((JsonString)version.get("decoder")).getValue();
		parserFQN = "usdot.fhwa.stol.c2c.c2c_mvt.parsers." + ((JsonString)version.get("parser")).getValue();
		validatorFQN = "usdot.fhwa.stol.c2c.c2c_mvt.validators." + ((JsonString)version.get("validator")).getValue();
		schemaFile = ((JsonString)version.get("schema")).getValue();
	}
}
