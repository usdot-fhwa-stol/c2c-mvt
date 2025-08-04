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

/**
 * Base class used for messages in the application.
 * 
 * @author Aaron Cherney
 */
public class C2CBaseMessage 
{
	/**
	 * Representation of the message as bytes
	 */
	protected byte[] messageBytes;


	/**
	 * The message type of the message
	 */
	protected String messageType;
	
	
	/**
	 * Constructor. Sets {@link #messageBytes}
	 * @param messageBytes The message in bytes
	 */
	public C2CBaseMessage(byte[] messageBytes)
	{
		this.messageBytes = messageBytes;
	}
	
	
	/**
	 * Gets the byte array representation of the message
	 * @return the byte array represenetation of the message
	 */
	public byte[] getBytes()
	{
		return messageBytes;
	}


	/**
	 * Setter for {@link C2CBaseMessage#messageType}
	 * @param messageType the message type
	 */
	public void setMessageType(String messageType)
	{
		this.messageType = messageType;
	}


	/**
	 * Getter for {@link C2CBaseMessage#messageType}
	 */
	public String getMessageType()
	{
		return messageType;
	}
}
