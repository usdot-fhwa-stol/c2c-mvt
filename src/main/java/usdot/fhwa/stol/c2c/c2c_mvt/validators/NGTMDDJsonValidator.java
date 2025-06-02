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

import com.github.erosb.jsonsKema.ValidationFailure;

import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;


/**
 * Implementation of {@link JsonValidator} for ngTMDD messages
 */
public class NGTMDDJsonValidator extends JsonValidator
{
	/**
	 * Constructor for NGTMDDJsonValidator. Sets the schema file by calling
	 * the parent constructor.
	 * @param schemaFile the schema file used for validation
	 */
	public NGTMDDJsonValidator(ClassPathResource schemaFile) 
	{
		super(schemaFile);
	}


	/**
	 * Parses the ValidationFailure to determine the error message for the message that failed
	 * to validate against the schema
	 * 
	 * @param failure validation results from {@link com.github.erosb.jsonsKema.Validator#validate(com.github.erosb.jsonsKema.IJsonValue)}
	 * @param message the C2C Message that failed to validate
	 */
	@Override
	public String getErrorMessage(ValidationFailure failure, JsonC2CMessage message) 
	{
		StringBuilder error = new StringBuilder();
		for (ValidationFailure fail : failure.getCauses())
		{
			String pointer = fail.getSchema().getLocation().getPointer().toString();
			if (pointer.startsWith("#/" + message.getMessageType()) && (pointer.compareTo("#/" + message.getMessageType()) == 0 || pointer.startsWith("#/" + message.getMessageType() + "/")))
			{
				error.append(fail.getMessage());
				appendErrorPerCause(fail, error, 2);
			}
		}

		if (error.isEmpty())
			error.append(message.getMessageType() + " is an invalid message type for ngTMDD messages");

		return error.toString();
	}


	/**
	 * Recursive function to iterate through multiple causes of ValidationFailure.
	 * 
	 * @param failure the ValidationFailure
	 * @param errorBuilder StringBuilder to append error messages to
	 * @param depth used to add the correct number of tabs for nested ValidationFailures
	 */
	private void appendErrorPerCause(ValidationFailure failure, StringBuilder errorBuilder, int depth)
	{
		if (failure.getCauses().isEmpty())
			return;

		for (ValidationFailure cause : failure.getCauses())
		{
			errorBuilder.append('\n');
			for (int i = 0; i < depth; i++)
				errorBuilder.append('\t');
			errorBuilder.append(cause.getClass().getSimpleName() + ": " + cause.getMessage().replace("instance", cause.getInstance().toString()));
			appendErrorPerCause(cause, errorBuilder, depth + 1);
		}
	}

}
