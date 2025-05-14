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
package usdot.fhwa.stol.c2c.c2c_mvt.messages;

import com.github.erosb.jsonsKema.JsonValue;

/**
 * Implementation of {@link C2CBaseMessage} for Json messages.
 * 
 * @author Aaron Cherney
 */
public class JsonC2CMessage extends C2CBaseMessage
{
	/**
	 * The message represented as a JsonValue, which can be a JsonObject or
	 * JsonArray
	 */
	protected JsonValue messageAsJson;

	
	/**
	 * Constructor. Calls {@link C2CBaseMessage#C2CMessage(byte[])}.
	 * @param messageBytes the message in bytes
	 */
	public JsonC2CMessage(byte[] messageBytes)
	{
		super(messageBytes);
	}
	
	
	/**
	 * Constructor. Calls {@link JsonC2CMessage#JsonC2CMessage(byte[])} and
	 * sets {@link #messageAsJson}
	 * @param messageBytes the message in bytes
	 * @param messageAsJson the message as a Json Object or Array
	 */
	public JsonC2CMessage(byte[] messageBytes, JsonValue messageAsJson)
	{
		this(messageBytes);
		this.messageAsJson = messageAsJson;
	}
	
	
	/**
	 * Returns a String representation of the Json message
	 * @return String representation of the Json message
	 */
	@Override
	public String toString()
	{
		return messageAsJson.toString();
	}
}
