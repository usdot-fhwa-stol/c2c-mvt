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

import java.util.List;


/**
 * This class represents a version of a C2C standard and includes the necessary fields for
 * message validation.
 */
public class C2CMVTStandardVersion 
{
	/*
	 * A list of encodings supported by this version of a C2C standard.
	 */
	private List<String> encodings;


	/*
	 * A list of message types supported by this version of a C2C standard. Can be empty
	 * if the messages are self defining
	 */
	private List<String> messageTypes;


	/*
	 * The fully qualified name of the decoder class for this version of a C2C standard.
	 */
	private String decoder;


	/*
	 * The fully qualified name of the parser class for this version of a C2C standard.
	 */
	private String parser;


	/*
	 * The fully qualified name of the validator class for this version of a C2C standard.
	 */
	private String validator;


	/*
	 * The name of the schema file for this version of a C2C standard.
	 */
	private String schema;


	/**
	 * Gets the encodings supported by this version of a C2C standard.
	 * @return A list of encodings supported by this version of a C2C standard.
	 */
	public List<String> getEncodings() 
	{
		return encodings;
	}


	/**
	 * Sets the encodings supported by this version of a C2C standard.
	 * @param encodings A list of encodings supported by this version of a C2C standard.
	 */
	public void setEndcodings(List<String> encodings) 
	{
		this.encodings = encodings;
	}


	/**
	 * Gets the message types supported by this version of a C2C standard.
	 * @return A list of message types supported by this version of a C2C standard.
	 */
	public List<String> getMessageTypes() 
	{
		return messageTypes;
	}


	/**
	 * Sets the message types supported by this version of a C2C standard.
	 * This can be empty if the messages are self defining.
	 * @param messageTypes A list of message types supported by this version of a C2C standard.
	 */
	public void setMessageTypes(List<String> messageTypes) 
	{
		this.messageTypes = messageTypes;
	}


	/**
	 * Gets the fully qualified name of the decoder class for this version of a C2C standard.
	 * @return The fully qualified name of the decoder class for this version of a C2C standard.
	 */
	public String getDecoder() 
	{
		return decoder;
	}


	/**
	 * Sets the fully qualified name of the decoder class for this version of a C2C standard.
	 * @param decoder The fully qualified name of the decoder class for this version of a C2C standard.
	 */
	public void setDecoder(String decoder) 
	{
		this.decoder = decoder;
	}


	/**
	 * Gets the fully qualified name of the parser class for this version of a C2C standard.
	 * @return The fully qualified name of the parser class for this version of a C2C standard.
	 */
	public String getParser() 
	{
		return parser;
	}


	/**
	 * Sets the fully qualified name of the parser class for this version of a C2C standard.
	 * @param parser The fully qualified name of the parser class for this version of a C2C standard.
	 */
	public void setParser(String parser) 
	{
		this.parser = parser;
	}
	

	/**
	 * Gets the fully qualified name of the validator class for this version of a C2C standard.
	 * @return The fully qualified name of the validator class for this version of a C2C standard.
	 */
	public String getValidator() 
	{
		return validator;
	}


	/**
	 * Sets the fully qualified name of the validator class for this version of a C2C standard.
	 * @param validator The fully qualified name of the validator class for this version of a C2C standard.
	 */
	public void setValidator(String validator) 
	{
		this.validator = validator;
	}


	/**
	 * Gets the name of the schema file for this version of a C2C standard.
	 * @return The name of the schema file for this version of a C2C standard.
	 */
	public String getSchema() 
	{
		return schema;
	}


	/**
	 * Sets the name of the schema file for this version of a C2C standard.
	 * @param schema The name of the schema file for this version of a C2C standard.
	 */
	public void setSchema(String schema) 
	{
		this.schema = schema;
	}
}
