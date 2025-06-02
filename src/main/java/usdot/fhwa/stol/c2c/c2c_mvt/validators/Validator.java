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
package usdot.fhwa.stol.c2c.c2c_mvt.validators;

import org.springframework.core.io.ClassPathResource;

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CBaseMessage;

/**
 * Validator is an abstract class that defines the structure for validating C2C messages.
 * It uses a schema file specific to the C2C standard and version to validate messages.
 * @param <T> A child class of {@link C2CBaseMessage} specific to the data format of the
 * C2C Standard that is being testing against.
 */
public abstract class Validator<T extends C2CBaseMessage> 
{
	/**
	 * The schema file used for validation.
	 */
	ClassPathResource schemaFile;


	/**
	 * Constructor for Validator. Sets the schema file.
	 * @param schemaFile
	 */
	public Validator(ClassPathResource schemaFile)
	{
		this.schemaFile = schemaFile;
	}


	/**
	 * Validates the message against the schema. If the message is not valid,
	 * a C2CMVTException is thrown and details of the error should be included in the
	 * message of the exception.
	 * 
	 * @param message the message to validate
	 * @throws C2CMVTException when the message is not valid according to the schema
	 */
	public abstract void validateMessage(T message) throws C2CMVTException;
}
