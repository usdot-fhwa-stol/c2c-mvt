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
import usdot.fhwa.stol.c2c.c2c_mvt.controllers.StandardValidationController;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CBaseMessage;

/**
 * Base class that defines the interfaces and common variables for Parsers.All
 * of the functions throw a {@link C2CMVTException} to allow all of the Exception
 * handling and logging to be taken care of by {@link StandardValidationController}
 * 
 * @param <T> A child class of {@link C2CBaseMessage} specific to the data format of the
 * C2C Standard that is being testing against.
 * 
 * @author Aaron Cherney
 */
public abstract class Parser<T extends C2CBaseMessage>
{
	/**
	 * Parses the decoded message (if necessary) to read the information contained
	 * in the message into memory
	 * @param decodedMessage the decoded message to parse
	 * @throws C2CMVTException 
	 */
	public abstract void parseMessage(T decodedMessage) throws C2CMVTException;
	
	
	/**
	 * Call this only if the user selects "Auto Detect" for the message type. If
	 * the user selects a specific message type then the message type is already
	 * identified.
	 * 
	 * @param decodedMessage the decoded message that needs its message type identified
	 * @return the message type as a String
	 * @throws C2CMVTException 
	 */
	public abstract String identifyMessageType(T decodedMessage) throws C2CMVTException;
}
