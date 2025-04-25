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
 *
 * @author Federal Highway Administration
 */
@RestController
public class StandardValidationController
{
	private static String m_sWorkingDir;
	private final AtomicBoolean m_oValidationInProgress = new AtomicBoolean(false);
	private JsonObject STANDARDS;
	private static final Logger LOGGER = LoggerFactory.getLogger(StandardValidationController.class);
	private static final ArrayList<String> LOGS = new ArrayList();
	private static final String DATEFMT = "yyyy-MM-dd'T'HH:mm:ss:SSS";
	private final String FILEDIR = "messages";
	private static String m_sCurrentMsg = null;
	private final long RESETERRORS = 3600000;
	private long m_lLastGetStandards = System.currentTimeMillis();
	
	@PostConstruct
	public void init()
	{
		try
		{
			String sJarPath = C2CMVTApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
			if (sJarPath.startsWith("jar:nested:/"))
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
	
	
	@PostMapping("/status")
    public String getStatus(@RequestParam(name = "include_messages") boolean bIncludeMessages) 
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
					oRet.append("\"").append(sMsg.replaceAll("\\\"", "\\\\\"").replaceAll("\\\n", "\\\\n").replaceAll("\\\t", "\\\\t")).append("\",");
				}
				if (LOGS.size() > 0)
					oRet.setLength(oRet.length() -1); // remove trailing comma
			}
			oRet.append("]");
		}
		oRet.append('}');
		return oRet.toString();
	}

	
	@GetMapping("/standards")
    public String getStandards() {
        LOGGER.debug("getStandards() invoked");
		if (m_lLastGetStandards + RESETERRORS > System.currentTimeMillis())
		{
			m_lLastGetStandards = System.currentTimeMillis();
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
        return oStandards.toString();
    }
	
	@PostMapping("/versions")
	public String getVersions(@RequestParam(name = "standard") String sStandard)
	{
		LOGGER.debug(String.format("getVersions() invoked with standard = %s", sStandard));
		List<IJsonValue> oVersions = new ArrayList();
		for (IJsonString oVersion : STANDARDS.get(sStandard).requireObject().get("versions").requireObject().getProperties().keySet())
		{
			oVersions.add(oVersion);
		}
		
		return oVersions.toString();
	}
	
	
	@PostMapping("/encodings")
	public String getEncodings(@RequestParam(name = "standard") String sStandard, @RequestParam(name = "version") String sVersion)
	{
		LOGGER.debug(String.format("getEncodings() invoked with standard = %s and version = %s", sStandard, sVersion));
		IJsonValue oEncodings = STANDARDS.get(sStandard).requireObject().get("versions").requireObject().get(sVersion).requireObject().get("encodings");
		if (oEncodings == null)
			return "[\"UTF-8\"]";
		
		return oEncodings.toString();
	}
	
	
	@PostMapping("/messagetypes")
	public String getMessageTypes(@RequestParam(name = "standard") String sStandard, @RequestParam(name = "version") String sVersion)
	{
		LOGGER.debug(String.format("getMessageTypes() invoked with standard = %s and version = %s", sStandard, sVersion));
		IJsonValue oTypes = STANDARDS.get(sStandard).requireObject().get("versions").requireObject().get(sVersion).requireObject().get("messageTypes");
		if (oTypes == null)
			return "[]";

		return oTypes.toString();
	}
	
	
	@PostMapping("/upload")
	public String uploadMessages(@RequestParam(name = "uploaded_file") MultipartFile oData, @RequestParam(name = "standard") String sStandard, 
								 @RequestParam(name = "version") String sVersion, @RequestParam(name = "encoding") String sEncoding, 
								 @RequestParam(name = "message_type") String sMessageType)
	{
		try
		{
			if (m_oValidationInProgress.get())
				return "{\"msg\": \"Failed to upload. Validation in progress\"}";
			UUID oId = UUID.randomUUID();
			m_sCurrentMsg = oId.toString();
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
			new Thread(() -> validateMessages(yPayload, sStandard, sVersion, sEncoding, sMessageType)).start();
		}
		catch (IOException oEx)
		{
			logException(LOGGER, oEx, null);
			return "{\"msg\": \"Failed to upload.\"}";
		}

		return "{\"msg\": \"Received\"}";
	}
	
	
	@GetMapping("/resetLog")
	public String resetLog()
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
		catch (IOException oEx)
		{
			logException(LOGGER, oEx, null);
		}
		return "{\"msg\": \"Success\"}";
	}
	
	
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
			for (StackTraceElement oSTE : oEx.getStackTrace())
				LOGGER.error(oSTE.toString());

			return ResponseEntity.internalServerError().build();
		}
	}
	
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
			addMessage("Validation completed with no errors");
		}
		catch (C2CMVTException oEx)
		{
			addMessage("Validation completed with errors");
			logException(LOGGER, oEx.m_oOriginal, oEx.m_sAdditionalMessage);
		}
		finally
		{
			m_sCurrentMsg = null;
			m_oValidationInProgress.set(false);
		}
	}
	
	
	private static String getMessage(String sMsg)
	{
		if (m_sCurrentMsg != null)
			return String.format("%s - %s - %s", new SimpleDateFormat(DATEFMT).format(System.currentTimeMillis()), m_sCurrentMsg, sMsg);
		else
			return String.format("%s - %s", new SimpleDateFormat(DATEFMT).format(System.currentTimeMillis()), sMsg);
	}
	
	
	public static void logException(Exception oEx, String sExtra)
	{
		logException(LOGGER, oEx, sExtra);
	}

	public static void logException(Logger oLogger, Exception oEx, String sExtra)
	{
		StringBuilder sBuf = new StringBuilder();
		if (sExtra != null)
			sBuf.append(sExtra).append('\n').append('\t');
		sBuf.append(oEx.toString());
		addMessage(sBuf.toString());
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
	
	
	public static void addMessage(String sMsg)
	{
		synchronized (LOGS)
		{
			LOGS.add(getMessage(sMsg));
		}
	}
}
