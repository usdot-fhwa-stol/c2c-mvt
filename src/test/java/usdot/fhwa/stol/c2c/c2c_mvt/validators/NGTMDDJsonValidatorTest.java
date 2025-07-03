package usdot.fhwa.stol.c2c.c2c_mvt.validators;

import com.github.erosb.jsonsKema.FormatValidationPolicy;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.Schema;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.github.erosb.jsonsKema.ValidationFailure;
import com.github.erosb.jsonsKema.Validator;
import com.github.erosb.jsonsKema.ValidatorConfig;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

class NGTMDDJsonValidatorTest {

    private static final String SCHEMA = """
    {
      "type": "object",
      "properties": {
        "message": {
          "oneOf": [
            { "$ref": "#/DMSControlRequest" },
            { "$ref": "#/DMSControlResponse" }
          ]
        }
      },
      "required": ["message"],
      "DMSControlRequest": {
        "type": "object",
        "properties": {
          "messageType": { "const": "DMSControlRequest" },
          "mode": { "type": "string", "enum": ["AUTO", "MANUAL"] }
        },
        "required": ["messageType", "mode"]
      },
      "DMSControlResponse": {
        "type": "object",
        "properties": {
          "messageType": { "const": "DMSControlResponse" },
          "status": { "type": "string" }
        },
        "required": ["messageType", "status"]
      }
    }
    """;

    @Test
    void testGetErrorMessage_InvalidMessageType() {
        String instanceStr = """
        {
          "message": {
            "messageType": "InvalidType"
          }
        }
        """;

        JsonValue schemaJson = new JsonParser(SCHEMA).parse();
        Schema schema = new SchemaLoader(schemaJson).load();
        JsonValue instanceJson = new JsonParser(instanceStr).parse();

        Validator validator = Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
        ValidationFailure failure = validator.validate(instanceJson);

        NGTMDDJsonValidator ngValidator = new NGTMDDJsonValidator(new ClassPathResource("dummy-schema.json"));
        JsonC2CMessage message = new JsonC2CMessage(instanceStr.getBytes(StandardCharsets.UTF_8), instanceJson);
        message.setMessageType("InvalidType");

        String errorMsg = ngValidator.getErrorMessage(failure, message);
        assertThat(errorMsg).contains("InvalidType is an invalid message type for ngTMDD messages");
    }

    @Test
    void testGetErrorMessage_InvalidEnumValue() {
        String instanceStr = """
        {
          "message": {
            "messageType": "DMSControlRequest",
            "mode": "BAD_ENUM"
          }
        }
        """;

        JsonValue schemaJson = new JsonParser(SCHEMA).parse();
        Schema schema = new SchemaLoader(schemaJson).load();
        JsonValue instanceJson = new JsonParser(instanceStr).parse();

        Validator validator = Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
        ValidationFailure failure = validator.validate(instanceJson);

        NGTMDDJsonValidator ngValidator = new NGTMDDJsonValidator(new ClassPathResource("dummy-schema.json"));
        JsonC2CMessage message = new JsonC2CMessage(instanceStr.getBytes(StandardCharsets.UTF_8), instanceJson);
        message.setMessageType("DMSControlRequest");

        String errorMsg = ngValidator.getErrorMessage(failure, message);
        assertThat(errorMsg).contains("the \"BAD_ENUM\" is not equal to any enum values");
    }

    @Test
    void testGetErrorMessage_MissingRequiredField() {
        String instanceStr = """
        {
          "message": {
            "messageType": "DMSControlRequest"
          }
        }
        """;

        JsonValue schemaJson = new JsonParser(SCHEMA).parse();
        Schema schema = new SchemaLoader(schemaJson).load();
        JsonValue instanceJson = new JsonParser(instanceStr).parse();

        Validator validator = Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
        ValidationFailure failure = validator.validate(instanceJson);

        NGTMDDJsonValidator ngValidator = new NGTMDDJsonValidator(new ClassPathResource("dummy-schema.json"));
        JsonC2CMessage message = new JsonC2CMessage(instanceStr.getBytes(StandardCharsets.UTF_8), instanceJson);
        message.setMessageType("DMSControlRequest");

        String errorMsg = ngValidator.getErrorMessage(failure, message);
        assertThat(errorMsg).contains("required properties are missing: mode");
    }

    @Test
    void testAppendErrorPerCause_AppendsNestedErrors() throws Exception {
        String instanceStr = """
        {
          "message":
          {
            "messageType": "CCTVImageLinkRequest",
            "deviceInformationRequest":
            {
              "ownerOrganization": 
              {
                "organizationId": "myorg"
              },
              "deviceType": "cctv camera",
              "deviceInformationType": "image link"
            },
            "imageType": "suppressed"
          }
        }
        """;
		NGTMDDJsonValidator ngValidator = new NGTMDDJsonValidator(new ClassPathResource("ngTMDD/ngTMDD_Schema_v1.0.json"));
		byte[] bomCheck = new byte[3];
		boolean includesBom = false;
		try (InputStream inStream = ngValidator.schemaFile.getInputStream())
		{
			int bytesRead = inStream.read(bomCheck);
			if (bytesRead > 2)
				includesBom = bomCheck[0] == (byte)0xEF && bomCheck[1] == (byte)0xBB && bomCheck[2] == (byte)0xBF;
		}
		
        JsonValue schemaJson;
		try (BufferedInputStream inStream = new BufferedInputStream(ngValidator.schemaFile.getInputStream()))
		{
			if (includesBom)
			{
				long bytesSkipped = inStream.skip(3);
				if (bytesSkipped != 3)
					throw new IOException("Failed to read the JSON Schema file.");
			}
			schemaJson = new JsonParser(inStream).parse();
		}
		Schema schema = new SchemaLoader(schemaJson).load();
        JsonValue instanceJson = new JsonParser(instanceStr).parse();
        Validator validator = Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
        ValidationFailure failure = validator.validate(instanceJson);


        StringBuilder errorBuilder = new StringBuilder();
        // Use reflection to call the private method
        Method method = NGTMDDJsonValidator.class.getDeclaredMethod("appendErrorPerCause", ValidationFailure.class, StringBuilder.class, int.class);
        method.setAccessible(true);
		for (ValidationFailure fail : failure.getCauses())
		{
			String pointer = fail.getSchema().getLocation().getPointer().toString();
			if (pointer.startsWith("#/" + "CCTVImageLinkRequest") && (pointer.compareTo("#/" + "CCTVImageLinkRequest") == 0 || pointer.startsWith("#/" + "CCTVImageLinkRequest" + "/")))
			{
				errorBuilder.append(fail.getMessage().replace("instance", fail.getInstance().toString()));
				method.invoke(ngValidator, fail, errorBuilder, 2);
			}
		}

        String errorText = errorBuilder.toString();
        assertThat(errorText).contains("required properties are missing: cctvId");
        assertThat(errorText).contains("the \"suppressed\" is not equal to any enum values");
    }
}