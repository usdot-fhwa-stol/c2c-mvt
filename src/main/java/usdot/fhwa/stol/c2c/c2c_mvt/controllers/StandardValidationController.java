/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.controllers;

import com.github.erosb.jsonsKema.JsonArray;
import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
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
import usdot.fhwa.stol.c2c.c2c_mvt.Util;
import usdot.fhwa.stol.c2c.c2c_mvt.c2cMvtApplication;

/**
 *
 * @author Federal Highway Administration
 */
@RestController
public class StandardValidationController 
{
	private static String m_sWorkingDir;
	private final AtomicBoolean m_oValidationInProgress = new AtomicBoolean(false);
	private final JsonObject STANDARDS = (JsonObject)new JsonParser(
	"""
	{
		"ngTMDD":
		{
			"versions":
			{
				"1.0" :
				{
					"encodings": ["text", "Hex", "Base64", "ASN.1"],
					"messageTypes": ["DMSControlRequest"]
				},
				"1.01" :
 				{
 				},
				"1.1" :
 				{
 				},
				"2.0" :
 				{
  				}
			}
		},
		"TMDD":
		{
			"versions":
			{
				"3.03c" :
				{
					"encodings": ["text"]
				},
				"3.03d" :
 				{
					"encodings": ["text", "Base64"]
 				},
				"3.1" :
 				{
					"encodings": ["text", "Hex", "Base64"]
 				} 
			}
		}
	}
    """).parse();
	private static final Logger LOGGER = LoggerFactory.getLogger(StandardValidationController.class);
	private final ArrayList<String> LOGS = new ArrayList();
	private final String DATEFMT = "yyyy-MM-dd'T'HH:mm:ss:SSS";
	private final String FILEDIR = "messages";
	
	@PostConstruct
	public void init()
	{
		try
		{
			String sJarPath = c2cMvtApplication.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
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
			Files.createDirectory(oDir);
			Files.createDirectory(Path.of(m_sWorkingDir, FILEDIR));
			LOGGER.info("Working directory set to " + m_sWorkingDir);
		}
		catch (URISyntaxException | IOException oEx)
		{
			LOGGER.error("Failed to set a working directory");
			Util.LogException(LOGGER, oEx);
			
		}
	}
	
	
	@GetMapping("/status")
    public String getStatus() 
	{
		LOGGER.debug("getStatus() invoked");
		StringBuilder oRet = new StringBuilder();
		oRet.append('{');
		oRet.append("\"validating\":").append(m_oValidationInProgress.get()).append(',');
		oRet.append("\"messages\":[");
		synchronized (LOGS)
		{
			for (String sMsg : LOGS)
			{
				oRet.append("\"").append(sMsg).append("\",");
			}
			if (LOGS.size() > 0)
				oRet.setLength(oRet.length() -1); // remove trailing comma
		}
		oRet.append("]");
		oRet.append('}');
		return oRet.toString();
	}
	
	@GetMapping("/standards")
    public String getStandards() {
        LOGGER.debug("getStandards() invoked");
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
		JsonObject oStandard = (JsonObject)STANDARDS.get(sStandard);
		List<JsonValue> oVersions = new ArrayList();
		for (JsonString oVersion : ((JsonObject)oStandard.get("versions")).getProperties().keySet())
		{
			oVersions.add(oVersion);
		}
		
		return oVersions.toString();
	}
	
	
	@PostMapping("/encodings")
	public String getEncodings(@RequestParam(name = "standard") String sStandard, @RequestParam(name = "version") String sVersion)
	{
		LOGGER.debug(String.format("getEncodings() invoked with standard = %s and version = %s", sStandard, sVersion));
		JsonObject oStandard = (JsonObject)STANDARDS.get(sStandard);
		JsonObject oVersion = (JsonObject)((JsonObject)oStandard.get("versions")).get(sVersion);
		JsonArray oEncodings = (JsonArray)oVersion.get("encodings");
		if (oEncodings == null)
			return "[\"plain text\"]";
		
		return oEncodings.toString();
	}
	
	
	@PostMapping("/messagetypes")
	public String getMessageTypes(@RequestParam(name = "standard") String sStandard, @RequestParam(name = "version") String sVersion)
	{
		LOGGER.debug(String.format("getMessageTypes() invoked with standard = %s and version = %s", sStandard, sVersion));
		JsonObject oStandard = (JsonObject)STANDARDS.get(sStandard);
		JsonObject oVersion = (JsonObject)((JsonObject)oStandard.get("versions")).get(sVersion);
		JsonArray oTypes = (JsonArray)oVersion.get("messageTypes");
		if (oTypes == null)
			return "[]";

		return oTypes.toString();
	}
	
	
	@PostMapping("/upload")
	public String uploadMessages(@RequestParam(name = "uploaded_file") MultipartFile oData)
	{
		try
		{
			UUID oId = UUID.randomUUID();
			int nLastPeriod = oData.getOriginalFilename().lastIndexOf(".");
			String sExt = nLastPeriod >= 0 ? oData.getOriginalFilename().substring(nLastPeriod) : "";
			String sFilename = oId.toString() + sExt;
			LOGGER.debug(String.format("uploadMessages() invoked with file name %s and length %d saving as %s", oData.getOriginalFilename(), oData.getSize(), sFilename));
			
			try (BufferedOutputStream oOut = new BufferedOutputStream(Files.newOutputStream(Path.of(m_sWorkingDir, FILEDIR, sFilename))))
			{
				oOut.write(oData.getBytes());
			}
			m_oValidationInProgress.set(true);
			new Thread(() -> validateMessages()).start();
		}
		catch (IOException oEx)
		{
			for (StackTraceElement oSTE : oEx.getStackTrace())
				LOGGER.error(oSTE.toString());
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
			Util.LogException(LOGGER, oEx);
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
	
	private void validateMessages()
	{
		try
		{
			for (int n = 0; n < 5; n++)
			{
				Thread.sleep(1000);
				synchronized (LOGS)
				{
					LOGS.add(getMessage("Validation step " + n));
				}
			}
		}
		catch (InterruptedException oEx)
		{
			LOGGER.error(oEx.getMessage());
		}
		m_oValidationInProgress.set(false);
	}
	
	
	private String getMessage(String sMsg)
	{
		return String.format("%s - %s", new SimpleDateFormat(DATEFMT).format(System.currentTimeMillis()), sMsg);
	}
}
