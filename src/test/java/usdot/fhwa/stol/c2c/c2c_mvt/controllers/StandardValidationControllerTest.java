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
package usdot.fhwa.stol.c2c.c2c_mvt.controllers;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;

/**
 * Unit tests for StandardValidationController
 * 
 * @author Eric Chen
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StandardValidationControllerTest {

	@LocalServerPort
	private int port;

	@Autowired
	private StandardValidationController controller;


	private final RestTemplate restTemplate = new RestTemplate();
	
	@Test
	void contextLoads() {
		assertThat(controller).isNotNull();
	}

	@Test
	void testGetStatus_IncludeValidationRecordsTrue() {
		String url = "http://localhost:" + port + "/status?include_validation_records=true";
		ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("\"validating\":");
	}

	@Test
	void testGetStandards_Success() {
		String url = "http://localhost:" + port + "/standards";
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotEmpty();
	}

	@Test
	void testGetVersions_ValidStandard() {
		String url = "http://localhost:" + port + "/versions?standard=ngTMDD";
		try {
			ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isNotEmpty();
		} catch (HttpServerErrorException e) {
			System.out.println("Body: " + e.getResponseBodyAsString());
			throw e;
		}
	}


	@Test
	void testGetEncodings_ValidStandardAndVersion() {
		String url = "http://localhost:" + port + "/encodings?standard=ngTMDD&version=1.0";
		ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
	
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("UTF-8");
	}
	

	@Test
	void testGetMessageTypes_ValidStandardAndVersion() {
		String url = "http://localhost:" + port + "/messagetypes?standard=ngTMDD&version=1.0";
		ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);
	
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("[]");
	}
	

	@Test
	void testUploadMessages_ValidFile() {
		String url = "http://localhost:" + port + "/upload";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		String validJson = """
		{
			"message":
			{
				"messageType": "ActivityLogRequest",
				"ownerOrganizationId": "org_id",
				"externalOrganizationId": "ext_org_id",
				"requestId": "request_id"
			}
		}
		""";

		ByteArrayResource fileResource = new ByteArrayResource(validJson.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public String getFilename() {
				return "ngtmdd_test.json";
			}
		};

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("uploaded_file", fileResource);
		body.add("standard", "ngTMDD");
		body.add("version", "1.0");
		body.add("encoding", "UTF-8");
		body.add("message_type", "Auto Detect");

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("Received");
	}

	

	@Test
	void testResetLog_Success() {
		String url = "http://localhost:" + port + "/resetLog";
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).contains("Success");
	}

	@Test
	void testDownloadLog_Success() {
		String url = "http://localhost:" + port + "/downloadLog";
		ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/octet-stream");
	}


	@Test
	void testValidateMessages_NoExceptionThrown() {
		// Arrange
		String jsonString =
		"""
		{
			"message":
			{
				"messageType": "ActivityLogRequest",
				"ownerOrganizationId": "org_id",
				"externalOrganizationId": "ext_org_id",
				"requestId": "request_id"
			}
		}		
		""";
		byte[] messageBytes = jsonString.getBytes(StandardCharsets.UTF_8);
		String fileExt = ".json";
		String standard = "ngTMDD";
		String version = "1.0";
		String encoding = "UTF-8";
		String selectedMessageType = "Auto Detect";

		controller.validateMessages(messageBytes, fileExt, standard, version, encoding, selectedMessageType);
		ArrayList<String> validationMessages = StandardValidationController.getValidationRecords();
		for (String validationMessage : validationMessages)
		{
			if (validationMessage.contains("Failed to validate message"))
				Assertions.fail("An Exception was thrown during validateMessage");
		}
		controller.deleteMessages();
	}


	@Test
	void testValidateMessages_InvalidJsonSyntax() {
		// Arrange
		String jsonString =
		"""
		{
			"message
		}		
		""";
		byte[] messageBytes = jsonString.getBytes(StandardCharsets.UTF_8);
		String fileExt = ".json";
		String standard = "ngTMDD";
		String version = "1.0";
		String encoding = "UTF-8";
		String selectedMessageType = "Auto Detect";

		controller.validateMessages(messageBytes, fileExt, standard, version, encoding, selectedMessageType);
		String errorRecord = null;
		for (String record : StandardValidationController.getValidationRecords())
		{
			if (record.contains("Error occured in separateMessage()"))
				errorRecord = record;
		}
		controller.deleteMessages();
		assertThat(errorRecord).isNotNull();
	}

	@Test
	void testValidateMessages_InvalidWorkingDirectory() {
		// Arrange
		String jsonString =
		"""
		{
			"message
		}		
		""";
		byte[] messageBytes = jsonString.getBytes(StandardCharsets.UTF_8);
		String fileExt = ".json";
		String standard = "ngTMDD";
		String version = "1.0";
		String encoding = "UTF-8";
		String selectedMessageType = "Auto Detect";
		try
		{
			Field field = StandardValidationController.class.getDeclaredField("workingDirectory");
			field.setAccessible(true);
			String oldWorkingDir = (String)field.get(controller);
			field.set(controller, "/invalidpath<>:\"/|?*.txt");
			controller.validateMessages(messageBytes, fileExt, standard, version, encoding, selectedMessageType);
			field.set(controller, oldWorkingDir);
			String errorRecord = null;
			for (String record : StandardValidationController.getValidationRecords())
			{
				if (record.contains("Failed to save message to disk for message"))
					errorRecord = record;
			}
			controller.deleteMessages();
			assertThat(errorRecord).isNotNull();
		}
		catch (NoSuchFieldException | IllegalAccessException ex)
		{
			Assertions.fail();
		}
	}
}