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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTApplication;
import usdot.fhwa.stol.c2c.c2c_mvt.decoders.Decoder;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CBaseMessage;
import usdot.fhwa.stol.c2c.c2c_mvt.parsers.Parser;
import usdot.fhwa.stol.c2c.c2c_mvt.standards.C2CMVTStandards;
import usdot.fhwa.stol.c2c.c2c_mvt.validators.Validator;

/**
 * This is the main class/controller of the application. It handles all of the user
 * requests for validation. It uses the c2c-mvt.json configuration file to know
 * what standards are implemented and what classes (decoder, parser, validator)
 * to instantiate for those standards.
 * 
 * @author Aaron Cherney
 */
@RestController
public class StandardValidationController
{
	/**
	 * Path of the working directory of the application. Gets set to the directory
	 * the .jar is located
	 */
	private String workingDirectory;

	
	/**
	 * Flag indicating if a message is currently being validated
	 */
	private final AtomicBoolean validationInProgress = new AtomicBoolean(false);

	
	/**
	 * This object contains all of the configuration items for the implemented standards
	 */
	private C2CMVTStandards standards;

	
	/**
	 * Logger instance
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(StandardValidationController.class);

	
	/**
	 * In memory log used for storing messages related to validation that will 
	 * be sent to the client
	 */
	private static final ArrayList<String> VALIDATION_RECORDS = new ArrayList<>();

	
	/**
	 * Format string used for dates
	 */
	private static final String DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss:SSS";

	
	/**
	 * Directory where messages that are validated are saved
	 */
	private static final String FILE_DIR = "messages";

	/**
	 * Timeout in milliseconds used to reset {@link MaxUploadSizeExceededExceptionHandler#requestTimes}
	 */
	private static final long RESETERRORS = 3600000;

	
	/**
	 * Time in milliseconds that {@link MaxUploadSizeExceededExceptionHandler#requestTimes}
	 * was last reset
	 */
	private long lastReset = System.currentTimeMillis();
	
	
	/**
	 * Initializes the controller. This method sets {@link #workingDirectory} 
	 * and reads src/main/resources/c2c-mvt.json to read the configuration of
	 * C2C standards that are implemented into {@link #c2CStandards}
	 */
	@PostConstruct
	public void init()
	{
		try
		{
			Path workingDirPath = determineWorkingDirectory();
			workingDirectory = workingDirPath.toString();
			Files.createDirectories(workingDirPath);
			Files.createDirectories(Path.of(workingDirectory, FILE_DIR));
			LOGGER.info("Working directory set to " + workingDirectory);
		}
		catch (URISyntaxException | IOException ex)
		{
			logException(LOGGER, ex, "Failed to set a working directory", null);
		}
		deleteMessages();
		
		try
		{
			standards = new C2CMVTStandards(new ClassPathResource("c2c-mvt.json"));
		}
		catch (C2CMVTException ex)
		{
			logException(LOGGER, ex.originalException, ex.additionalMessage, null);
		}
	}
	
	
	private Path determineWorkingDirectory()
		throws URISyntaxException
	{
		String jarPath = C2CMVTApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
		if (jarPath.startsWith("jar:nested:/")) // check if the path needs to modified to be a valid file system path
			jarPath = jarPath.substring("jar:nested:/".length());
		if (jarPath.startsWith("file:/"))
			jarPath = jarPath.substring("file:/".length());

		Path dir = Path.of(jarPath);
		if (dir.toString().contains("c2c-mvt.jar"))
		{
			while (dir.getFileName().toString().compareTo("c2c-mvt.jar") != 0)
				dir = dir.getParent();
		}

		dir = dir.getParent();
		dir = Path.of(dir.toString(), "c2c-mvt");
		return dir;
	}
	
	
	/**
	 * Creates a String representing a JSON Object which contains the status of the controller 
	 * (whether or not validation is currently happening) and if bIncludeMessages 
	 * is true all of the messages related to validation that are in memory
	 * @param includeValidationRecords flag to include messages in the response
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Object as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/status")
    public ResponseEntity<String> getStatus(@RequestParam(name = "include_validation_records") boolean includeValidationRecords) 
	{
		try
		{
			LOGGER.debug(String.format("getStatus() invoked with include_validation_records = %b", includeValidationRecords));
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode jsonObject = objectMapper.createObjectNode();
			jsonObject.put("validating", validationInProgress.get());
			if (includeValidationRecords)
			{
				ArrayNode msgArray = jsonObject.putArray("messages");
				synchronized (VALIDATION_RECORDS)
				{
					for (String msg : VALIDATION_RECORDS)
					{
						msgArray.add(msg);
					}
				}
			}
			return ResponseEntity.ok(objectMapper.writeValueAsString(jsonObject));
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, null, null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get status\"}");
		}
	}

	
	/**
	 * Reads the configuration object {@link #c2CStandards} to create a String 
	 * representing a JSON Array containing all of the implemented C2C standards.
	 * It also checks if enough time has elapsed to reset 
	 * {@link MaxUploadSizeExceededExceptionHandler#requestTimes} and reset
	 * it when necessary
	 * 
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@GetMapping("/standards")
    public ResponseEntity<String> getStandards() 
	{
		try
		{
			LOGGER.debug("getStandards() invoked");
			if (lastReset + RESETERRORS > System.currentTimeMillis())
			{
				lastReset = System.currentTimeMillis();
				synchronized (MaxUploadSizeExceededExceptionHandler.requestTimes)
				{
					MaxUploadSizeExceededExceptionHandler.requestTimes.clear();
				}
			}

			return ResponseEntity.ok(standards.getStandardsAsJsonArray());
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, "Failed to get standards", null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get standards\"}");
		}
        
    }
	
	
	/**
	 * Reads the configuration object {@link #c2CStandards} to create a String 
	 * representing a JSON Array containing all of the implemented versions
	 * for the given C2C standards
	 * @param standard name of the C2C standard
	 * 
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/versions")
	public ResponseEntity<String> getVersions(@RequestParam(name = "standard") String standard)
	{
		try
		{
			LOGGER.debug(String.format("getVersions() invoked with standard = %s", standard));


			return ResponseEntity.ok(standards.getVersionsAsJsonArray(standard));
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, "Failed to get versions", null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get versions\"}");
		}
	}
	
	
	/**
	 * Reads the configuration object {@link #c2CStandards} to create a String 
	 * representing a JSON Array containing all of the implemented encodings 
	 * for the given C2C standard and version.
	 * @param standard name of the C2C standard
	 * @param version version of the C2C standard
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/encodings")
	public ResponseEntity<String> getEncodings(@RequestParam(name = "standard") String standard, @RequestParam(name = "version") String version)
	{
		try
		{
			LOGGER.debug(String.format("getEncodings() invoked with standard = %s and version = %s", standard, version));

			return ResponseEntity.ok(standards.getEncodingsAsJsonArray(standard, version));
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, "Failed to get encodings", null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get encodings\"}");
		}
	}
	
	
	/**
	 * Reads the configuration object {@link #c2CStandards} to create a String 
	 * representing a JSON Array containing all of the implemented message types
	 * for the given C2C standard and version.
	 * standard sStandard name of the C2C standard
	 * @param version version of the C2C standard
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/messagetypes")
	public ResponseEntity<String> getMessageTypes(@RequestParam(name = "standard") String standard, @RequestParam(name = "version") String version)
	{
		try
		{
			LOGGER.debug(String.format("getMessageTypes() invoked with standard = %s and version = %s", standard, version));

			return ResponseEntity.ok(standards.getMessageTypesAsJsonArray(standard, version));
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, "Failed to get message types", null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get message types\"}");
		}
		
	}
	
	
	/**
	 * If there isn't a message currently being validated, this method assigns
	 * a UUID to the file that is uploaded, saves it to disk, and creates a new
	 * thread to asynchronously validate the message.
	 * @param data a file containing the message to validate
	 * @param standard name of the C2C standard
	 * @param version version of the C2C standard
	 * @param encoding encoding used for the message
	 * @param messageType the type of message being validated, if known and necessary
	 * to validate
	 * @return {@link ResponseEntity} with status code 200 and a String 
	 * representing a JSON Object saying the message was received as the body 
	 * if no exceptions occur, otherwise the status code is 500 and the body 
	 * contains an error message
	 */
	@PostMapping("/upload")
	public ResponseEntity<String> uploadMessages(@RequestParam(name = "uploaded_file") MultipartFile data, @RequestParam(name = "standard") String standard, 
								 @RequestParam(name = "version") String version, @RequestParam(name = "encoding") String encoding, 
								 @RequestParam(name = "message_type") String messageType)
	{
		try
		{
			if (validationInProgress.get())
				return ResponseEntity.internalServerError().body("{\"error\": \"Failed to upload. Validation in progress\"}");
			
			String originalFilename = data.getOriginalFilename();
			if (originalFilename == null)
				throw new IOException("Failed to determine file type");
			int lastPeriodPos = originalFilename.lastIndexOf(".");
			String fileExtension = lastPeriodPos >= 0 ? originalFilename.substring(lastPeriodPos) : "";
			
			LOGGER.debug(String.format("uploadMessages() invoked with file name %s and length %d", originalFilename, data.getSize()));
			byte[] messageBytes = data.getBytes();
			validationInProgress.set(true);
			new Thread(() -> validateMessages(messageBytes, fileExtension, standard, version, encoding, messageType)).start();
			return ResponseEntity.ok("{\"msg\": \"Received\"}");
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, null, null);
			return ResponseEntity.badRequest().body("{\"error\": \"Failed to upload.\"}");
		}
	}
	
	
	/**
	 * Deletes all files that have been uploaded for validation and resets the
	 * in memory list of messages.
	 * @return {@link ResponseEntity} with status code 200 and a String 
	 * representing a JSON Object saying the reset was a success as the body 
	 * if no exceptions occur, otherwise the status code is 500 and the body 
	 * contains an error message
	 */
	@GetMapping("/resetLog")
	public ResponseEntity<String> resetLog()
	{
		try
		{
			LOGGER.debug("resetLog() invoked");
			synchronized (VALIDATION_RECORDS)
			{
				VALIDATION_RECORDS.clear();
			}
			deleteMessages();
			return ResponseEntity.ok("{\"msg\": \"Success\"}");
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, null, null);
			return ResponseEntity.badRequest().body("{\"error\": \"Failed to upload.\"}");
		}
	}


	/**
	 * Deletes stored messages that have been submitted for validation.
	 */
	public void deleteMessages()
	{
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of(workingDirectory, FILE_DIR)))
		{
			for (Path file : directoryStream)
			{
				Files.deleteIfExists(file);
			}
		}
		catch (Exception ex)
		{
			logException(LOGGER, ex, null, null);
		}
	}
	
	
	/**
	 * Creates a zip file containing all of the messages related to validation
	 * and each file that has been uploaded for validation.
	 * @return {@link ResponseEntity} with status code 200 and the zipped file as the body 
	 * if no exceptions occur, otherwise the status code is 500
	 */
	@GetMapping("/downloadLog")
	public ResponseEntity<Resource> downloadLog()
	{
		LOGGER.debug("downloadLog() invoked");
		try 
		{
			byte[] zipFile = createLogBundle();
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename("c2c-mvt-logs.zip").build());
			return ResponseEntity.ok().headers(httpHeaders).contentLength(zipFile.length).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new ByteArrayResource(zipFile));
		}
		catch (IOException ex)
		{
			logException(LOGGER, ex, null, null);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	
	/**
	 * Creates a .zip file that contains all of the messages and validation log
	 * records.
	 * @return a byte array that is a .zip file
	 * @throws IOException 
	 */
	private byte[] createLogBundle()
		throws IOException
	{
		byte[] newlineBytes = "\n".getBytes(StandardCharsets.UTF_8);
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
		{
			try (ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(outputStream)))
			{
				zipStream.putNextEntry(new ZipEntry("validation_results.txt"));
				synchronized (VALIDATION_RECORDS)
				{
					for (String msg : VALIDATION_RECORDS)
					{
						zipStream.write(msg.getBytes(StandardCharsets.UTF_8));
						zipStream.write(newlineBytes);
					}
				}
				zipStream.closeEntry();

				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of(workingDirectory, FILE_DIR)))
				{
					for (Path file : directoryStream)
					{
						zipStream.putNextEntry(new ZipEntry(file.getFileName().toString()));
						zipStream.write(Files.readAllBytes(file));
						zipStream.closeEntry();
					}
				}
			}
			return outputStream.toByteArray();
		}
	}
	
	/**
	 * Validates the given payload by instantiating the configured Decoder, Parser,
	 * and Validator for the given C2C standard, version, encoding, and message
	 * type. The method is package private so that it can be called from the test classes.
	 * @param messageBytes messages to validate
	 * @param fileExt file extension of the uploaded file
	 * @param standard name of the C2C standard
	 * @param version version of the C2C standard
	 * @param encoding encoding used for the message
	 * @param selectedMessageType the type of message being validated, if known and necessary
	 * to validate
	 */
	void validateMessages(byte[] messageBytes, String fileExt, String standard, String version, String encoding, String selectedMessageType)
	{
		String uuidAsString = null;
		try
		{
			Decoder<C2CBaseMessage> decoder = standards.getDecoderInstance(standard, version);
			decoder.setEncoding(encoding);
			if (!decoder.checkSecurity(messageBytes))
				throw new C2CMVTException(new Exception("Found possible security threat. Did not attempt validation."), null);
			ArrayList<byte[]> separatedMessages = decoder.separateMessages(messageBytes);
			int msgTotal = separatedMessages.size();
			int msgNum = 1;
			for (byte[] msgBytes : separatedMessages)
			{
				try
				{
					UUID uuid = UUID.randomUUID();
					uuidAsString = uuid.toString();
					String filename = uuid.toString() + fileExt;
					try (BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Path.of(workingDirectory, FILE_DIR, filename))))
					{
						outputStream.write(msgBytes);
					}
					catch (IOException ex)
					{
						throw new C2CMVTException(ex, String.format("Failed to save message to disk for message %d of %d", msgNum, msgTotal));
					}
					C2CBaseMessage message = decoder.checkSyntax(msgBytes);
					Parser<C2CBaseMessage> parser = standards.getParserInstance(standard, version);
					message.setMessageType(selectedMessageType);
					if (selectedMessageType.toLowerCase().compareTo("auto detect") == 0)
						message.setMessageType(parser.identifyMessageType(message));

					parser.parseMessage(message);
					Validator<C2CBaseMessage> validator = standards.getValidatorInstance(standard, version);
					validator.validateMessage(message);
					addLogRecord(String.format("Validation completed with no errors for message %d of %d", msgNum, msgTotal), uuidAsString);
				}
				catch (C2CMVTException ex)
				{
					addLogRecord(String.format("Validation completed with errors for message %d of %d", msgNum, msgTotal), uuidAsString);
					logException(LOGGER, ex.originalException, ex.additionalMessage, uuidAsString);
				}
				finally
				{
					++msgNum;
				}
			}
		}
		catch (C2CMVTException ex)
		{
			addLogRecord("Validation failed to complete", uuidAsString);
			logException(LOGGER, ex.originalException, ex.additionalMessage, uuidAsString);
		}
		finally
		{
			validationInProgress.set(false);
		}
	}

	
	/**
	 * Formats the given message with a timestamp and the UUID of the message
	 * currently being validated if a message is being validated
	 * @param msg message to format
	 * @param msgUuid null if a message isn't being validated, otherwise the
	 * uuid of the message being validated
	 * @return formatted message String
	 */
	private static String formatMessage(String msg, String msgUuid)
	{
		if (msgUuid != null)
			return String.format("%s - %s - %s", new SimpleDateFormat(DATE_FMT).format(System.currentTimeMillis()), msgUuid, msg);
		else
			return String.format("%s - %s", new SimpleDateFormat(DATE_FMT).format(System.currentTimeMillis()), msg);
	}
	
	/**
	 * Wrapper for {@link #logException(org.slf4j.Logger, java.lang.Exception, java.lang.String) 
	 * using {@link #LOGGER} as the Logger. Logs an exception to the log4j2 file
	 * and calls {@link #addLogRecord} with the extra message and {@link Exception#toString()}
	 * @param ex Exception to log
	 * @param extraMsg Extra message to log
	 * @param messageUuid null if a message isn't being validated, otherwise the
	 * uuid of the message being validated
	 */
	public static void logException(Exception ex, String extraMsg, String messageUuid)
	{
		logException(LOGGER, ex, extraMsg, messageUuid);
	}

	/**
	 * Logs an exception to the log4j2 file and calls {@link #addLogRecord} with 
	 * the extra message and {@link Exception#toString()}
	 * @param logger Logger to use
	 * @param ex Exception to log
	 * @param extra Extra message to log
	 * @param messageUuid null if a message isn't being validated, otherwise the
	 * uuid of the message being validated
	 */
	public static void logException(Logger logger, Exception ex, String extra, String messageUuid)
	{
		StringBuilder buffer = new StringBuilder();
		if (extra != null)
			buffer.append(extra).append('\n').append('\t');
		buffer.append(ex.toString());
		addLogRecord(buffer.toString(), messageUuid);
		buffer.append('\n').append('\t');
		StackTraceElement[] stackTrace = ex.getStackTrace();
		for (StackTraceElement sTE : stackTrace)
		{
			buffer.append(sTE.toString()).append('\n').append('\t');
		}
		if (stackTrace.length > 0)
			buffer.setLength(buffer.length() - 1);
		
		String logMsg = buffer.toString();
		logger.error(logMsg);
	}
	
	/**
	 * Adds the message to the in memory list of log messages {@link #message}
	 * @param message The message to log
	 * @param messageUuid null if a message isn't being validated, otherwise the
	 * uuid of the message being validated
	 */
	public static void addLogRecord(String message, String messageUuid)
	{
		synchronized (VALIDATION_RECORDS)
		{
			VALIDATION_RECORDS.add(formatMessage(message, messageUuid));
		}
	}


	/**
	 * Gets a copy of the String contained in {@link #VALIDATION_RECORDS}. Needed
	 * for one of the unit tests.
	 * @return ArrayList containing all of the strings in {@link #VALIDATION_RECORDS}
	 */
	public static ArrayList<String> getValidationRecords()
	{
		ArrayList<String> copy;
		synchronized (VALIDATION_RECORDS)
		{
			copy = new ArrayList<>(VALIDATION_RECORDS);
		}

		return copy;
	}
}
