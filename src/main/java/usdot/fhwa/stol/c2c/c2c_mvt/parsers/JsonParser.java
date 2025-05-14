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

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

/**
 * Implementation of {@link Parser} for Json Messages
 * @author Aaron Cherney
 */
public class JsonParser extends Parser<JsonC2CMessage>
{
	/**
	 * The generic JsonParser doesn't need to do anything in the parseMessage
	 * function since the Json Object or Array stored in {@link JsonC2CMessage#messageAsJson}
	 * gets created in the decode process
	 * @param decodedMessage the decoded message to parse
	 * @throws C2CMVTException 
	 */
	@Override
	public void parseMessage(JsonC2CMessage decodedMessage) throws C2CMVTException
	{
	}

	/**
	 * Call this only if the user selects "Auto Detect" for the message type. If
	 * the user selects a specific message type then the message type is already
	 * identified. This method needs to be implemented by child class for a 
	 * specific C2C Standard since there isn't a generic way to identify the message type
	 * 
	 * @param decodedMessage the decoded message that needs its message type identified
	 * @return Always throws an Exception. Must implement this method in a child class for a specific
	 * C2C Standard
	 * @throws C2CMVTException 
	 */
	@Override
	public String identifyMessageType(JsonC2CMessage decodedMessage) throws C2CMVTException
	{
		throw new C2CMVTException(new Exception("Failed to identify message type"), "Failed to identify message type");
	}

}
