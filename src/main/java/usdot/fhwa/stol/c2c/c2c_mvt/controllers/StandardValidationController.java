/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.controllers;

import com.github.erosb.jsonsKema.IJsonString;
import com.github.erosb.jsonsKema.IJsonValue;
import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;
import jakarta.annotation.PostConstruct;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CMessage;

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
	private static String m_sWorkingDir;

	/**
	 * Flag indicating if a message is currently being validated
	 */
	private final AtomicBoolean m_oValidationInProgress = new AtomicBoolean(false);

	/**
	 * This object contains all of the configuration items for the implemented standards
	 */
	private JsonObject STANDARDS;

	/**
	 * Logger instance
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(StandardValidationController.class);

	/**
	 * In memory log used for storing messages related to validation that will 
	 * be sent to the client
	 */
	private static final ArrayList<String> LOGS = new ArrayList();

	/**
	 * Format string used for dates
	 */
	private static final String DATEFMT = "yyyy-MM-dd'T'HH:mm:ss:SSS";

	/**
	 * Directory where messages that are validated are saved
	 */
	private final String FILEDIR = "messages";

	/**
	 * UUID of the message currently being validated
	 */
	private static String m_sCurrentMsg = null;

	/**
	 * Timeout in milliseconds used to reset {@link MaxUploadSizeExceededExceptionHandler#m_oRequestTimes}
	 */
	private final long RESETERRORS = 3600000;

	/**
	 * Time in milliseconds that {@link MaxUploadSizeExceededExceptionHandler#m_oRequestTimes}
	 * was last reset
	 */
	private long m_lLastReset = System.currentTimeMillis();
	
	/**
	 * Initializes the controller. This method sets {@link #m_sWorkingDir} 
	 * and reads src/main/resources/c2c-mvt.json to read the configuration of
	 * C2C standards that are implemented into {@link #STANDARDS}
	 */
	@PostConstruct
	public void init()
	{
		try
		{
			String sJarPath = C2CMVTApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
			if (sJarPath.startsWith("jar:nested:/")) // check if the path needs to modified to be a valid file system path
				sJarPath = sJarPath.substring("jar:nested:/".length());
			if (sJarPath.startsWith("file:/"))
				sJarPath = sJarPath.substring("file:/".length());
			
			Path oDir = Path.of(sJarPath);
			if (oDir.toString().contains("c2c-mvt.jar"))
			{
				while (oDir.getFileName().toString().compareTo("c2c-mvt.jar") != 0)
					oDir = oDir.getParent();
			}
			
			oDir = oDir.getParent();
			oDir = Path.of(oDir.toString(), "c2c-mvt");
			m_sWorkingDir = oDir.toString();
			Files.createDirectories(oDir);
			Files.createDirectories(Path.of(m_sWorkingDir, FILEDIR));
			LOGGER.info("Working directory set to " + m_sWorkingDir);
			
			ClassPathResource oCPR = new ClassPathResource("c2c-mvt.json");
			try (InputStream oIn = oCPR.getInputStream())
			{
				STANDARDS = (JsonObject)new JsonParser(oIn).parse();
			}
		}
		catch (URISyntaxException | IOException oEx)
		{
			logException(LOGGER, oEx, "Failed to set a working directory");
		}
	}
	
	/**
	 * Creates a String representing a JSON Object which contains the status of the controller 
	 * (whether or not validation is currently happening) and if bIncludeMessages 
	 * is true all of the messages related to validation that are in memory
	 * @param bIncludeMessages flag to include messages in the response
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Object as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/status")
    public ResponseEntity<String> getStatus(@RequestParam(name = "include_messages") boolean bIncludeMessages) 
	{
		try
		{
			LOGGER.debug("getStatus() invoked");
			StringBuilder oRet = new StringBuilder();
			oRet.append('{');
			oRet.append("\"validating\":").append(m_oValidationInProgress.get());
			if (bIncludeMessages)
			{
				oRet.append(',');
				oRet.append("\"messages\":[");
				synchronized (LOGS)
				{
					for (String sMsg : LOGS)
					{
						oRet.append("\"").append(sMsg.replaceAll("\\\"", "\\\\\"").replaceAll("\\\n", "\\\\n").replaceAll("\\\t", "\\\\t")).append("\","); // escape characters to have valid JSON
					}
					if (LOGS.size() > 0)
						oRet.setLength(oRet.length() -1); // remove trailing comma
				}
				oRet.append("]");
			}
			oRet.append('}');
			return ResponseEntity.ok(oRet.toString());
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get status\"}");
		}
	}

	/**
	 * Reads the configuration object {@link #STANDARDS} to create a String 
	 * representing a JSON Array containing all of the implemented C2C standards.
	 * It also checks if enough time has elapsed to reset 
	 * {@link MaxUploadSizeExceededExceptionHandler#m_oRequestTimes} and reset
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
			if (m_lLastReset + RESETERRORS > System.currentTimeMillis())
			{
				m_lLastReset = System.currentTimeMillis();
				synchronized (MaxUploadSizeExceededExceptionHandler.m_oRequestTimes)
				{
					MaxUploadSizeExceededExceptionHandler.m_oRequestTimes.clear();
				}
			}

			List<JsonValue> oStandards = new ArrayList();
			for (JsonString oStandard : STANDARDS.getProperties().keySet())
			{
				oStandards.add(oStandard);
			}
			return ResponseEntity.ok(oStandards.toString());
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get standards\"}");
		}
        
    }
	
	/**
	 * Reads the configuration object {@link #STANDARDS} to create a String 
	 * representing a JSON Array containing all of the implemented versions
	 * for the given C2C standards
	 * @param sStandard name of the C2C standard
	 * 
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/versions")
	public ResponseEntity<String> getVersions(@RequestParam(name = "standard") String sStandard)
	{
		try
		{
			LOGGER.debug(String.format("getVersions() invoked with standard = %s", sStandard));
			List<IJsonValue> oVersions = new ArrayList();
			for (IJsonString oVersion : STANDARDS.get(sStandard).requireObject().get("versions").requireObject().getProperties().keySet())
			{
				oVersions.add(oVersion);
			}

			return ResponseEntity.ok(oVersions.toString());
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get versions\"}");
		}
	}
	
	/**
	 * Reads the configuration object {@link #STANDARDS} to create a String 
	 * representing a JSON Array containing all of the implemented encodings 
	 * for the given C2C standard and version.
	 * @param sStandard name of the C2C standard
	 * @param sVersion version of the C2C standard
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/encodings")
	public ResponseEntity<String> getEncodings(@RequestParam(name = "standard") String sStandard, @RequestParam(name = "version") String sVersion)
	{
		try
		{
			LOGGER.debug(String.format("getEncodings() invoked with standard = %s and version = %s", sStandard, sVersion));
			IJsonValue oEncodings = STANDARDS.get(sStandard).requireObject().get("versions").requireObject().get(sVersion).requireObject().get("encodings");
			if (oEncodings == null)
				return ResponseEntity.ok("[\"UTF-8\"]");

			return ResponseEntity.ok(oEncodings.toString());
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get encodings\"}");
		}
	}
	
	/**
	 * Reads the configuration object {@link #STANDARDS} to create a String 
	 * representing a JSON Array containing all of the implemented message types
	 * for the given C2C standard and version.
	 * @param sStandard name of the C2C standard
	 * @param sVersion version of the C2C standard
	 * @return {@link ResponseEntity} with status code 200 and the String 
	 * representing a JSON Array as the body if no exceptions occur, otherwise 
	 * the status code is 500 and the body contains an error message
	 */
	@PostMapping("/messagetypes")
	public ResponseEntity<String> getMessageTypes(@RequestParam(name = "standard") String sStandard, @RequestParam(name = "version") String sVersion)
	{
		try
		{
			LOGGER.debug(String.format("getMessageTypes() invoked with standard = %s and version = %s", sStandard, sVersion));
			IJsonValue oTypes = STANDARDS.get(sStandard).requireObject().get("versions").requireObject().get(sVersion).requireObject().get("messageTypes");
			if (oTypes == null)
				return ResponseEntity.ok("[]");

			return ResponseEntity.ok(oTypes.toString());
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.internalServerError().body("{\"error\": \"Failed to get message types\"}");
		}
		
	}
	
	/**
	 * If there isn't a message currently being validated, this method assigns
	 * a UUID to the file that is uploaded, saves it to disk, and creates a new
	 * thread to asynchronously validate the message.
	 * @param oData a file containing the message to validate
	 * @param sStandard name of the C2C standard
	 * @param sVersion version of the C2C standard
	 * @param sEncoding encoding used for the message
	 * @param sMessageType the type of message being validated, if known and necessary
	 * to validate
	 * @return {@link ResponseEntity} with status code 200 and a String 
	 * representing a JSON Object saying the message was received as the body 
	 * if no exceptions occur, otherwise the status code is 500 and the body 
	 * contains an error message
	 */
	@PostMapping("/upload")
	public ResponseEntity<String> uploadMessages(@RequestParam(name = "uploaded_file") MultipartFile oData, @RequestParam(name = "standard") String sStandard, 
								 @RequestParam(name = "version") String sVersion, @RequestParam(name = "encoding") String sEncoding, 
								 @RequestParam(name = "message_type") String sMessageType)
	{
		try
		{
			if (m_oValidationInProgress.get())
				return ResponseEntity.internalServerError().body("{\"error\": \"Failed to upload. Validation in progress\"}");
			UUID oId = UUID.randomUUID();
			String sOriginalFilename = oData.getOriginalFilename();
			if (sOriginalFilename == null)
				throw new IOException("Failed to determine file type");
			int nLastPeriod = sOriginalFilename.lastIndexOf(".");
			String sExt = nLastPeriod >= 0 ? sOriginalFilename.substring(nLastPeriod) : "";
			String sFilename = oId.toString() + sExt;
			LOGGER.debug(String.format("uploadMessages() invoked with file name %s and length %d saving as %s", sOriginalFilename, oData.getSize(), sFilename));
			byte[] yPayload = oData.getBytes();
			try (BufferedOutputStream oOut = new BufferedOutputStream(Files.newOutputStream(Path.of(m_sWorkingDir, FILEDIR, sFilename))))
			{
				oOut.write(yPayload);
			}
			m_oValidationInProgress.set(true);
			m_sCurrentMsg = oId.toString();
			new Thread(() -> validateMessages(yPayload, sStandard, sVersion, sEncoding, sMessageType)).start();
			return ResponseEntity.ok("{\"msg\": \"Received\"}");
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
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
			synchronized (LOGS)
			{
				LOGS.clear();
			}
			try (DirectoryStream<Path> oDir = Files.newDirectoryStream(Path.of(m_sWorkingDir, FILEDIR)))
			{
				for (Path oFile : oDir)
				{
					Files.deleteIfExists(oFile);
				}
			}
			return ResponseEntity.ok("{\"msg\": \"Success\"}");
		}
		catch (Exception oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.badRequest().body("{\"error\": \"Failed to upload.\"}");
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
		byte[] yNewline = "\n".getBytes(StandardCharsets.UTF_8);
		try (ByteArrayOutputStream oOut = new ByteArrayOutputStream())
		{
			try (ZipOutputStream oZOS = new ZipOutputStream(new BufferedOutputStream(oOut)))
			{
				oZOS.putNextEntry(new ZipEntry("logs.txt"));
				synchronized (LOGS)
				{
					for (String sMsg : LOGS)
					{
						oZOS.write(sMsg.getBytes(StandardCharsets.UTF_8));
						oZOS.write(yNewline);
					}
				}
				oZOS.closeEntry();

				try (DirectoryStream<Path> oDir = Files.newDirectoryStream(Path.of(m_sWorkingDir, FILEDIR)))
				{
					for (Path oFile : oDir)
					{
						oZOS.putNextEntry(new ZipEntry(oFile.getFileName().toString()));
						oZOS.write(Files.readAllBytes(oFile));
						oZOS.closeEntry();
					}
				}
			}
			HttpHeaders oHeaders = new HttpHeaders();
			oHeaders.setContentDisposition(ContentDisposition.builder("attachment").filename("c2c-mvt-logs.zip").build());
			return ResponseEntity.ok().headers(oHeaders).contentLength(oOut.size()).contentType(MediaType.APPLICATION_OCTET_STREAM).body(new ByteArrayResource(oOut.toByteArray()));
		}
		catch (IOException oEx)
		{
			logException(LOGGER, oEx, null);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	/**
	 * Validates the given payload by instantiating the configured Decoder, Parser,
	 * and Validator for the given C2C standard, version, encoding, and message
	 * type.
	 * @param yPayload messages to validate
	 * @param sStandard name of the C2C standard
	 * @param sVersion version of the C2C standard
	 * @param sEncoding encoding used for the message
	 * @param sMessageType the type of message being validated, if known and necessary
	 * to validate
	 */
	private void validateMessages(byte[] yPayload, String sStandard, String sVersion, String sEncoding, String sMessageType)
	{
		Decoder oDecoder = null;
		try
		{
			try
			{
				String sDecoderClass = STANDARDS.get(sStandard).requireObject().get("versions").requireObject().get(sVersion).requireObject().get("decoder").requireString().getValue();
				oDecoder = (Decoder)Class.forName(sDecoderClass).getDeclaredConstructor().newInstance();
			}
			catch (Exception oEx)
			{
				throw new C2CMVTException(oEx, String.format("Failed to instantiate decoder for version %s of standard %s", sVersion, sStandard));
			}
			if (oDecoder != null)
			{
				oDecoder.setEncoding(sEncoding);
				ArrayList<C2CMessage> oMessages = oDecoder.decode(yPayload);
			}
			addLogRecord("Validation completed with no errors");
		}
		catch (C2CMVTException oEx)
		{
			addLogRecord("Validation completed with errors");
			logException(LOGGER, oEx.m_oOriginal, oEx.m_sAdditionalMessage);
		}
		finally
		{
			m_sCurrentMsg = null;
			m_oValidationInProgress.set(false);
		}
	}
	
	/**
	 * Formats the given message with a timestamp and the UUID of the message
	 * currently being validated if a message is being validated
	 * @param sMsg message to format
	 * @return formatted message String
	 */
	private static String formatMessage(String sMsg)
	{
		if (m_sCurrentMsg != null)
			return String.format("%s - %s - %s", new SimpleDateFormat(DATEFMT).format(System.currentTimeMillis()), m_sCurrentMsg, sMsg);
		else
			return String.format("%s - %s", new SimpleDateFormat(DATEFMT).format(System.currentTimeMillis()), sMsg);
	}
	
	/**
	 * Wrapper for {@link #logException(org.slf4j.Logger, java.lang.Exception, java.lang.String) 
	 * using {@link #LOGGER} as the Logger. Logs an exception to the log4j2 file
	 * and calls {@link #addLogRecord} with the extra message and {@link Exception#toString()}
	 * @param oEx Exception to log
	 * @param sExtra Extra message to log
	 */
	public static void logException(Exception oEx, String sExtra)
	{
		logException(LOGGER, oEx, sExtra);
	}

	/**
	 * Logs an exception to the log4j2 file and calls {@link #addLogRecord} with 
	 * the extra message and {@link Exception#toString()}
	 * @param oLogger Logger to use
	 * @param oEx Exception to log
	 * @param sExtra Extra message to log
	 */
	public static void logException(Logger oLogger, Exception oEx, String sExtra)
	{
		StringBuilder sBuf = new StringBuilder();
		if (sExtra != null)
			sBuf.append(sExtra).append('\n').append('\t');
		sBuf.append(oEx.toString());
		addLogRecord(sBuf.toString());
		sBuf.append('\n').append('\t');
		StackTraceElement[] oStackTrace = oEx.getStackTrace();
		for (StackTraceElement oSTE : oStackTrace)
		{
			sBuf.append(oSTE.toString()).append('\n').append('\t');
		}
		if (oStackTrace.length > 0)
			sBuf.setLength(sBuf.length() - 1);
		
		String sLog = sBuf.toString();
		oLogger.error(sLog);
	}
	
	/**
	 * Adds the message to the in memory list of log messages {@link #LOGS}
	 * @param sMsg The message to log
	 */
	public static void addLogRecord(String sMsg)
	{
		synchronized (LOGS)
		{
			LOGS.add(formatMessage(sMsg));
		}
	}
}
