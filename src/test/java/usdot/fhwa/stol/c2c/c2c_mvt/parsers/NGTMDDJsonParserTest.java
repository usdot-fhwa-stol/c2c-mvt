package usdot.fhwa.stol.c2c.c2c_mvt.parsers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;
import java.nio.charset.StandardCharsets;
import usdot.fhwa.stol.c2c.c2c_mvt.decoders.JsonDecoder;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

class NGTMDDJsonParserTest {
    private JsonDecoder jsonDecoder;
    private NGTMDDJsonParser parser;

    @BeforeEach
    void setUp() {
        jsonDecoder = new JsonDecoder();
        jsonDecoder.setEncoding(StandardCharsets.UTF_8.name());
        parser = new NGTMDDJsonParser();
    }

    // 1. Root is not a JSON object
    @Test
    void testIdentifyMessageType_RootNotObject() {
        String jsonMessage = "[1,2,3]";
        Assertions.assertThatThrownBy(() -> {
            JsonC2CMessage message = jsonDecoder.checkSyntax(jsonMessage.getBytes(StandardCharsets.UTF_8));
            parser.identifyMessageType(message);
        }).isInstanceOf(C2CMVTException.class)
          .hasMessageContaining("ngTMDD message must be a JSON Object");
    }

    // 2. Root object missing "message" key
    @Test
    void testIdentifyMessageType_MissingMessageKey() {
        String jsonMessage = "{\"notMessage\":{}}";
        Assertions.assertThatThrownBy(() -> {
            JsonC2CMessage message = jsonDecoder.checkSyntax(jsonMessage.getBytes(StandardCharsets.UTF_8));
            parser.identifyMessageType(message);
        }).isInstanceOf(C2CMVTException.class)
          .hasMessageContaining("ngTMDD root object must contain the key \"message\"");
    }

    // 3. "message" property is not an object
    @Test
    void testIdentifyMessageType_MessageNotObject() {
        String jsonMessage = "{\"message\":42}";
        Assertions.assertThatThrownBy(() -> {
            JsonC2CMessage message = jsonDecoder.checkSyntax(jsonMessage.getBytes(StandardCharsets.UTF_8));
            parser.identifyMessageType(message);
        }).isInstanceOf(C2CMVTException.class)
          .hasMessageContaining("\"message\" property must be a JSON Object");
    }

    // 4. "message" object missing "messageType" key
    @Test
    void testIdentifyMessageType_MissingMessageTypeKey() {
        String jsonMessage = "{\"message\":{}}";
        Assertions.assertThatThrownBy(() -> {
            JsonC2CMessage message = jsonDecoder.checkSyntax(jsonMessage.getBytes(StandardCharsets.UTF_8));
            parser.identifyMessageType(message);
        }).isInstanceOf(C2CMVTException.class)
          .hasMessageContaining("ngTMDD \"message\" property must contain the key \"messageType\"");
    }

    // 5. "messageType" is not a string
    @Test
    void testIdentifyMessageType_MessageTypeNotString() {
        String jsonMessage = "{\"message\":{\"messageType\":123}}";
        Assertions.assertThatThrownBy(() -> {
            JsonC2CMessage message = jsonDecoder.checkSyntax(jsonMessage.getBytes(StandardCharsets.UTF_8));
            parser.identifyMessageType(message);
        }).isInstanceOf(C2CMVTException.class)
          .hasMessageContaining("\"messageType\" property must be a String");
    }

    // 6. Correct message type
    @Test
    void testIdentifyMessageType_Success() {
        String jsonMessage = """
        {
            "message": {
                "messageType": "ActivityLogRequest"
            }
        }
        """;
        try {
            JsonC2CMessage message = jsonDecoder.checkSyntax(jsonMessage.getBytes(StandardCharsets.UTF_8));
            String messageType = parser.identifyMessageType(message);
            Assertions.assertThat(messageType).isEqualTo("ActivityLogRequest");
        } catch (C2CMVTException exception) {
            Assertions.fail("Exception was thrown when it was not expected", exception);
        }
    }
}