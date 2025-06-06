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
}