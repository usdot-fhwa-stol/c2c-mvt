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

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;

import com.github.erosb.jsonsKema.FormatValidationPolicy;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.Schema;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.github.erosb.jsonsKema.ValidationFailure;
import com.github.erosb.jsonsKema.ValidatorConfig;

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

/**
 * Implementation of {@link Validator} for Json Messages
 */
public abstract class JsonValidator extends Validator<JsonC2CMessage>
{
	/**
	 * Constructor for JsonValidator. Sets the schema file by calling
	 * the parent constructor.
	 * @param schemaFile the schema file used for validation
	 */
	public JsonValidator(ClassPathResource schemaFile)
	{
		super(schemaFile);
	}



	/**
	 * Validates the message against the schema. This method reads the schema file, checking 
	 * for a Byte Order Mark (BOM) at the start of the file since the library used for validation
	 * does not handle BOMs correctly.
	 * @param message the message to validate
	 * @throws C2CMVTException when the message is not valid according to the schema
	 */
	@Override
	public void validateMessage(JsonC2CMessage message) throws C2CMVTException 
	{
		try
		{
			byte[] bomCheck = new byte[3];
			boolean includesBom = false;
			try (InputStream inStream = schemaFile.getInputStream())
			{
				int bytesRead = inStream.read(bomCheck);
				if (bytesRead > 2)
					includesBom = bomCheck[0] == (byte)0xEF && bomCheck[1] == (byte)0xBB && bomCheck[2] == (byte)0xBF;
			}

			JsonValue schemaJson;
			try (BufferedInputStream inStream = new BufferedInputStream(schemaFile.getInputStream()))
			{
				if (includesBom)
					inStream.skip(3);
				schemaJson = new JsonParser(inStream).parse();
			}
			Schema schema = new SchemaLoader(schemaJson).load();
			com.github.erosb.jsonsKema.Validator validator = com.github.erosb.jsonsKema.Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
			ValidationFailure failure = validator.validate(message.getJson());
			if (failure != null)
			{
				throw new Exception(getErrorMessage(failure, message));
			}
		}
		catch (Exception oEx)
		{
			throw new C2CMVTException(oEx, "Failed to validate message");
		}
	}


	/**
	 * Determines and returns an error message from the {@link ValidationFailure}
	 * object. Children classes need to implement this method since having knowledge
	 * of the specific JSON Schema helps determine what the message should be.
	 * 
	 * @param failure validation results from {@link com.github.erosb.jsonsKema.Validator#validate(com.github.erosb.jsonsKema.IJsonValue)}
	 * @param message the message that validation was attempted on
	 * @return error message related to a failed validation attempt
	 */
	public abstract String getErrorMessage(ValidationFailure failure, JsonC2CMessage message);
}
