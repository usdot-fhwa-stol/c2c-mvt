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

package usdot.fhwa.stol.c2c.c2c_mvt.parsers;

import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonTypingException;
import com.github.erosb.jsonsKema.JsonValue;

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;


/**
 * Implementation of {@link Parser} for NGTMDD Messages
 * 
 * @author Aaron Cherney
 */
public class NGTMDDJsonParser extends JsonParser
{
	/**
	 * This method checks for the correct structure (the JSON root element is an object that
	 * contains the key "message" which is an object that contains the key "messageType" which
	 * is a String) of an ngTMDD message while parsing the message type.
	 * 
	 * @param decodedMessage the decoded message that needs its message type identified
	 * @return the message type as a String
	 * @throws C2CMVTException if the message does not conform to the ngTMDD specification
	 */
	@Override
	public String identifyMessageType(JsonC2CMessage decodedMessage) throws C2CMVTException
	{
		JsonValue messageJsonValue = decodedMessage.getJson();
		try
		{
			messageJsonValue.requireObject();
		}
		catch (JsonTypingException exception)
		{
			throw new C2CMVTException(new Exception("Failed to identify message type", exception), "ngTMDD message must be a JSON Object");
		}
			
		JsonValue messageJsonObject = ((JsonObject)messageJsonValue).get("message");
		if (messageJsonObject == null)
			throw new C2CMVTException(new Exception("Failed to identify message type"), "ngTMDD root object must contain the key \"message\"");
		
		try
		{
			messageJsonObject.requireObject();
		}
		catch (JsonTypingException exception)
		{
			throw new C2CMVTException(new Exception("Failed to identify message type", exception), "\"message\" property must be a JSON Object");
		}

		JsonValue messageType = ((JsonObject)messageJsonObject).get("messageType");
		if (messageType == null)
			throw new C2CMVTException(new Exception("Failed to identify message type"), "ngTMDD \"message\" property must contain the key \"messageType\"");

		try
		{
			messageType.requireString();
		}
		catch (JsonTypingException exception)
		{
			throw new C2CMVTException(new Exception("Failed to identify message type", exception), "\"messageType\" property must be a String");
		}
		
		return ((JsonString)messageType).getValue();
	}
}
