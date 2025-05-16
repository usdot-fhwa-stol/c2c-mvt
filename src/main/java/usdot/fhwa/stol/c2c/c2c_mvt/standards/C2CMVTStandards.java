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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.decoders.Decoder;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CBaseMessage;
import usdot.fhwa.stol.c2c.c2c_mvt.parsers.Parser;


/**
 * This class reads the c2c-mvt.json file and stores the information in memory.
 * It provides methods to retrieve the list of C2C standards, versions, encodings, and message types.
 * It also provides methods to create instances of the Decoder, Parser, and Validator classes for the specified C2C standard and version.
 * The structure of the configuration file is a JSON Object as follows:
 * {
 * 	"standardName1": {
 * 		"versions": {
 * 			"versionName1": {
 * 				"encodings": ["encoding1", "encoding2"],
 * 				"messageTypes": ["messageType1", "messageType2"],
 * 				"decoderFQN": "DecoderClass",
 * 				"parserFQN": "ParserClass",
 * 				"validatorFQN": "ValidatorClass",
 * 			    "schemaFile": "schema1.json"
 * 			},
 * 			"versionName2": {
 * 				"encodings": ["encoding3"],
 * 				"messageTypes": [],
 * 				"decoderFQN": "DecoderClass",
 * 				"parserFQN": "ParserClass",
 * 				"validatorFQN": "ValidatorClass",
 * 			    "schemaFile": "schema2.json"
 * 			}
 * 		}
 * 	},
 * 	"standardName2": {
 * 		"versions": {
 * 			"versionName1": {
 * 				"encodings": ["encoding4"],
 * 				"messageTypes": ["messageType3"],
 * 				"decoderFQN": "DecoderClass",
 * 				"parserFQN": "ParserClass",
 * 				"validatorFQN": "ValidatorClass",
 * 			    "schemaFile": "schema3.json"
 * 			}
 * 		}
 * 	}
 * }
 * The Decoder class must be in the usdot.fhwa.stol.c2c.c2c_mvt.decoders package.
 * The Parser class must be in the usdot.fhwa.stol.c2c.c2c_mvt.parsers package.
 * The Validator class must be in the usdot.fhwa.stol.c2c.c2c_mvt.validators package.
 */
public class C2CMVTStandards 
{
	/**
	 * Map of C2C standards, where the key is the standard name and the value is the C2CStandard object.
	 */
	private final Map<String, C2CMVTStandardVersions> c2cStandardMap = new HashMap<>();


	/**
	 * Constructor that reads the C2C MVT configuration file and initializes the C2C standards.
	 *
	 * @param resource the ClassPathResource representing the C2C MVT configuration file
	 * @throws C2CMVTException if an error occurs while reading the configuration file
	 */
	public C2CMVTStandards(ClassPathResource resource)
		throws C2CMVTException
	{
		try
		{
			ObjectNode objectNode = (ObjectNode)new ObjectMapper().readTree(resource.getInputStream());
			try (InputStream inputStream = resource.getInputStream())
			{
				objectNode = (ObjectNode)new ObjectMapper().readTree(inputStream);
			}

			Iterator<Entry<String, JsonNode>> iterator = objectNode.fields();
			while (iterator.hasNext())
			{
				Entry<String, JsonNode> entry = iterator.next();
				String standardName = entry.getKey();
				c2cStandardMap.put(standardName, new C2CMVTStandardVersions(standardName, (ObjectNode)entry.getValue()));
			}
		}
		catch (Exception oEx)
		{
			throw new C2CMVTException(oEx, "Error reading C2C MVT configuration file");
		}
	}


	/**
	 * Returns a string representation of a JSON array of the C2C standards implemented in the application.
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getStandardsAsJsonArray() throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode standardsArray = objectMapper.createArrayNode();
		for (String standardName : c2cStandardMap.keySet())
		{
			standardsArray.add(standardName);
		}

		return objectMapper.writeValueAsString(standardsArray);
	}


	/**
	 * Returns a string representation of a JSON array of the implemented versions for the specified C2C standard.
	 * @param standardName the name of the C2C standard
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getVersionsAsJsonArray(String standardName) throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode versionsArray = objectMapper.createArrayNode();
		C2CMVTStandardVersions standard = c2cStandardMap.get(standardName);
		for (String versionName : standard.c2cStandardVersionMap.keySet())
		{
			versionsArray.add(versionName);
		}

		return objectMapper.writeValueAsString(versionsArray);
	}

	
	/**
	 * Returns a string representation of a JSON array of the encodings for the specified C2C standard and version.
	 * @param standardName the name of the C2C standard
	 * @param versionName the name of version of the C2C standard
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getEncodingsAsJsonArray(String standardName, String versionName) throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode encodingsArray = objectMapper.createArrayNode();
		C2CMVTStandardVersions standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		for (String encoding : version.getEncodings())
		{
			encodingsArray.add(encoding);
		}

		return objectMapper.writeValueAsString(encodingsArray);
	}


	/**
	 * Returns a string representation of a JSON array of the message types for the specified C2C standard and version.
	 * @param standardName the name of the C2C standard
	 * @param versionName the name of version of the C2C standard
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getMessageTypesAsJsonArray(String standardName, String versionName) throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode messageTypesArray = objectMapper.createArrayNode();
		C2CMVTStandardVersions standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		for (String messageType : version.getMessageTypes())
		{
			messageTypesArray.add(messageType);
		}

		return objectMapper.writeValueAsString(messageTypesArray);
	}


	/**
	 * Creates a new instance of the Decoder for the given C2C standard and version
	 * @param standardName name of the C2C standard
	 * @param versionName version of the C2C standard
	 * @return a new instance of the Decoder for the given C2C standard and version
	 * @throws C2CMVTException if an error occurs while creating the Decoder instance
	 */
	@SuppressWarnings("unchecked")
	public Decoder<C2CBaseMessage> getDecoderInstance(String standardName, String versionName) throws C2CMVTException
	{
		C2CMVTStandardVersions standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		try
		{
			return (Decoder<C2CBaseMessage>)Class.forName(version.getDecoder()).getDeclaredConstructor().newInstance();
		}
		catch (Exception ex)
		{
			throw new C2CMVTException(ex, String.format("Failed to instantiate decoder for version %s of standard %s", standardName, versionName));
		}
	}


	/**
	 * Creates a new instance of the Parser for the given C2C standard and version
	 * @param standardName name of the C2C standard
	 * @param versionName version of the C2C standard
	 * @return a new instance of the Parser for the given C2C standard and version
	 * @throws C2CMVTException if an error occurs while creating the Parser instance
	 */
	@SuppressWarnings("unchecked")
	public Parser<C2CBaseMessage> getParserInstance(String standardName, String versionName) throws C2CMVTException
	{
		C2CMVTStandardVersions standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		try
		{
			return (Parser<C2CBaseMessage>)Class.forName(version.getParser()).getDeclaredConstructor().newInstance();
		}
		catch (Exception ex)
		{
			throw new C2CMVTException(ex, String.format("Failed to instantiate parser for version %s of standard %s", versionName, standardName));
		}
	}
}
