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
 * Implementation of {@link Parser} for NGTMDD Messages
 * 
 * @author Aaron Cherney
 */
public class NGTMDDJsonParser extends JsonParser
{
	/**
	 * Call this only if the user selects "Auto Detect" for the message type. If
	 * the user selects a specific message type then the message type is already
	 * identified.
	 * 
	 * @param decodedMessage the decoded message that needs its message type identified
	 * @return the message type as a String
	 * @throws C2CMVTException
	 */
	@Override
	public String identifyMessageType(JsonC2CMessage decodedMessage) throws C2CMVTException
	{
		// TODO: Implement logic to identify the message type based on the contents of the decodedMessage
		// Need to implement this once we know more about the ngTMDD message format
		// For now, just throw an exception to indicate that this method needs to be implemented
		throw new C2CMVTException(new Exception("Failed to identify message type"), "Failed to identify message type");
	}
}
