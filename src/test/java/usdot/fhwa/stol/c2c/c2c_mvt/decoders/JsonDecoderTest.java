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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for JsonDecoder
 * 
 * @author Eric Chen
 */
class JsonDecoderTest {

    private JsonDecoder jsonDecoder;

    @BeforeEach
    void setUp() {
        jsonDecoder = new JsonDecoder();
        jsonDecoder.setEncoding(StandardCharsets.UTF_8.name());
    }

    @Test
    void testSeparateMessages_ValidJsonArray() throws Exception {
        String json = "{\"key1\":\"value1\"},{\"key2\":\"value2\"}";
        ArrayList<byte[]> messages = jsonDecoder.separateMessages(json.getBytes(StandardCharsets.UTF_8));

        assertThat(messages).hasSize(2);
        assertThat(new String(messages.get(0), StandardCharsets.UTF_8)).isEqualTo("{\"key1\":\"value1\"}");
        assertThat(new String(messages.get(1), StandardCharsets.UTF_8)).isEqualTo("{\"key2\":\"value2\"}");
    }

    @Test
    void testSeparateMessages_ValidJsonObject() throws Exception {
        String json = "{\"key\":\"value\"}";
        ArrayList<byte[]> messages = jsonDecoder.separateMessages(json.getBytes(StandardCharsets.UTF_8));

        assertThat(messages).hasSize(1);
        assertThat(new String(messages.get(0), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @Test
    void testSeparateMessages_InvalidJsonThrowsException() {
        String invalidJson = "{\"key\":\"value\""; // Missing closing brace

        C2CMVTException exception = assertThrows(C2CMVTException.class, () -> {
            jsonDecoder.separateMessages(invalidJson.getBytes(StandardCharsets.UTF_8));
        });

        assertThat(exception.getMessage()).contains("Error occured in separateMessage()");
    }

    @Test
    void testSeparateMessages_EmptyInput() throws Exception {
        String emptyJson = "";
        ArrayList<byte[]> messages = jsonDecoder.separateMessages(emptyJson.getBytes(StandardCharsets.UTF_8));

        assertThat(messages).isEmpty();
    }

    @Test
    void testCheckSecurity_AlwaysReturnsTrue() throws Exception {
        String json = "{\"key\":\"value\"}";
        boolean result = jsonDecoder.checkSecurity(json.getBytes(StandardCharsets.UTF_8));

        assertThat(result).isTrue();
    }

    @Test
    void testCheckSyntax_ValidJson() throws Exception {
        String json = "{\"key\":\"value\"}";
        JsonC2CMessage message = jsonDecoder.checkSyntax(json.getBytes(StandardCharsets.UTF_8));

        assertThat(message).isNotNull();
        assertThat(new String(message.getBytes(), StandardCharsets.UTF_8)).isEqualTo(json);
    }

    @Test
    void testCheckSyntax_InvalidJsonThrowsException() {
        String invalidJson = "{\"key\":\"value\""; // Missing closing brace

        C2CMVTException exception = assertThrows(C2CMVTException.class, () -> {
            jsonDecoder.checkSyntax(invalidJson.getBytes(StandardCharsets.UTF_8));
        });

        assertThat(exception.getMessage()).contains("Invalid JSON Syntax");
    }

    @Test
    void testCheckSyntax_EmptyInputThrowsException() {
        String emptyJson = "";

        C2CMVTException exception = assertThrows(C2CMVTException.class, () -> {
            jsonDecoder.checkSyntax(emptyJson.getBytes(StandardCharsets.UTF_8));
        });

        assertThat(exception.getMessage()).contains("Invalid JSON Syntax");
    }

    @Test
    void testSeparateMessages_WithBOM() throws Exception {
        byte[] jsonWithBOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '{', '"', 'k', 'e', 'y', '"', ':', '"', 'v', 'a', 'l', 'u', 'e', '"', '}'};
        ArrayList<byte[]> messages = jsonDecoder.separateMessages(jsonWithBOM);

        assertThat(messages).hasSize(1);
        assertThat(new String(messages.get(0), StandardCharsets.UTF_8)).isEqualTo("{\"key\":\"value\"}");
    }
}