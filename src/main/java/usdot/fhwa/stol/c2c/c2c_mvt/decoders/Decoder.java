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
package usdot.fhwa.stol.c2c.c2c_mvt.decoders;

import java.util.ArrayList;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.controllers.StandardValidationController;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CMessage;

/**
 * Base class that defines the interfaces and common variables for Decoders. All
 * of the functions throw a {@link C2CMVTException} to allow all of the Exception
 * handling and logging to be taken care of by {@link StandardValidationController}
 * 
 * @param <T> A child class of C2CMessage specific to the data format of the
 * C2C Standard that is being testing against.
 * 
 * @author Aaron Cherney
 */
public abstract class Decoder<T extends C2CMessage>
{	
	/**
	 * Name of the encoding that needs to be decoded
	 */
	protected String encoding;
	
	/**
	 * Method to implement the logic to separate messages that are concatenated
	 * @param messageBytes the message(s) to separate
	 * @return List of separated messages as byte arrays
	 * @throws C2CMVTException
	 */
	public abstract ArrayList<byte[]> separateMessages(byte[] messageBytes) throws C2CMVTException;
	
	/**
	 * Method to implement the logic for checking for security threats
	 * @param messageBytes the message to check
	 * @return true if no security threats are detected, false if a threat is detected
	 * so the message can be ignore
	 * @throws C2CMVTException
	 */
	public abstract boolean checkSecurity(byte[] messageBytes) throws C2CMVTException;
	
	/**
	 * Ensures the message has the expected syntax and creates the appropriate
	 * {@link C2CMessage}. Throws a C2CMVTException if the syntax is not valid.
	 * @param <T> The type of {@link C2CMessage} the Decoder creates, for example
	 * messages in Json format would return a {@Link JsonC2CMessage}
	 * @param messageBytes the message in bytes
	 * @return the {@link C2CMessage} wrapping the message if it has the correct
	 * syntax
	 * @throws C2CMVTException
	 */
	public abstract T checkSyntax(byte[] messageBytes) throws C2CMVTException;
	
	/**
	 * @return the name of encoding used for this Decoder
	 */
	public String getEncoding()
	{
		return encoding;
	}
	
	/**
	 * Sets the encoding used for this Decoder
	 * @param encoding name of the encoding
	 */
	public void setEncoding(String encoding)
	{
		this.encoding = encoding;
	}
}
