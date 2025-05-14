package usdot.fhwa.stol.c2c.c2c_mvt.standards;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;

import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.decoders.Decoder;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CBaseMessage;
import usdot.fhwa.stol.c2c.c2c_mvt.parsers.Parser;


/**
 * This class reads the c2c-mvt.json file and stores the information in memory.
 * It provides methods to retrieve the list of C2C standards, versions, encodings, and message types.
 * It also provides methods to create instances of the Decoder, Parser, and Validator classes for the specified C2C standard and version.
 */
public class C2CMVTStandards 
{
	/**
	 * Map of C2C standards, where the key is the standard name and the value is the C2CStandard object.
	 */
	private final Map<String, C2CMVTStandard> c2cStandardMap = new HashMap<>();


	/**
	 * Constructor that reads the C2C MVT configuration file and initializes the C2C standards.
	 * @param resource the ClassPathResource representing the C2C MVT configuration file
	 * @throws C2CMVTException if an error occurs while reading the configuration file
	 */
	public C2CMVTStandards(ClassPathResource resource)
		throws C2CMVTException
	{
		try
		{
			JsonObject c2CStandards;
			try (InputStream inputStream = resource.getInputStream())
			{
				c2CStandards = (JsonObject)new JsonParser(inputStream).parse();
			}

			for (Entry<JsonString, JsonValue> standardEntry : c2CStandards.getProperties().entrySet())
			{
				String standardName = standardEntry.getKey().getValue();
				c2cStandardMap.put(standardName, new C2CMVTStandard(standardName, (JsonObject)standardEntry.getValue()));
			}
		}
		catch (Exception oEx)
		{
			throw new C2CMVTException(oEx, "Error reading C2C MVT configuration file");
		}
	}


	/**
	 * Returns a string representation of a JSON array of the C2C standards implemented in the application.
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getStandardsAsJsonArray() throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode standardsArray = objectMapper.createArrayNode();
		for (String standardName : c2cStandardMap.keySet())
		{
			standardsArray.add(standardName);
		}

		return objectMapper.writeValueAsString(standardsArray);
	}


	/**
	 * Returns a string representation of a JSON array of the implemented versions for the specified C2C standard.
	 * @param standardName the name of the C2C standard
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getVersionsAsJsonArray(String standardName) throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode versionsArray = objectMapper.createArrayNode();
		C2CMVTStandard standard = c2cStandardMap.get(standardName);
		for (String versionName : standard.c2cStandardVersionMap.keySet())
		{
			versionsArray.add(versionName);
		}

		return objectMapper.writeValueAsString(versionsArray);
	}

	/**
	 * Returns a string representation of a JSON array of the encodings for the specified C2C standard and version.
	 * @param standardName the name of the C2C standard
	 * @param versionName the name of version of the C2C standard
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getEncodingsAsJsonArray(String standardName, String versionName) throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode encodingsArray = objectMapper.createArrayNode();
		C2CMVTStandard standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		for (String encoding : version.encodingsList)
		{
			encodingsArray.add(encoding);
		}

		return objectMapper.writeValueAsString(encodingsArray);
	}


	/**
	 * Returns a string representation of a JSON array of the message types for the specified C2C standard and version.
	 * @param standardName the name of the C2C standard
	 * @param versionName the name of version of the C2C standard
	 * @return a string representation of a JSON array
	 * @throws JsonProcessingException
	 */
	public String getMessageTypesAsJsonArray(String standardName, String versionName) throws JsonProcessingException
	{
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode messageTypesArray = objectMapper.createArrayNode();
		C2CMVTStandard standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		for (String messageType : version.messageTypesList)
		{
			messageTypesArray.add(messageType);
		}

		return objectMapper.writeValueAsString(messageTypesArray);
	}


	/**
	 * Creates a new instance of the Decoder for the given C2C standard and version
	 * @param standardName name of the C2C standard
	 * @param versionName version of the C2C standard
	 * @return a new instance of the Decoder for the given C2C standard and version
	 * @throws C2CMVTException if an error occurs while creating the Decoder instance
	 */
	@SuppressWarnings("unchecked")
	public Decoder<C2CBaseMessage> getDecoderInstance(String standardName, String versionName) throws C2CMVTException
	{
		C2CMVTStandard standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		try
		{
			return (Decoder<C2CBaseMessage>)Class.forName(version.decoderFQN).getDeclaredConstructor().newInstance();
		}
		catch (Exception ex)
		{
			throw new C2CMVTException(ex, String.format("Failed to instantiate decoder for version %s of standard %s", standardName, versionName));
		}
	}


	/**
	 * Creates a new instance of the Parser for the given C2C standard and version
	 * @param standardName name of the C2C standard
	 * @param versionName version of the C2C standard
	 * @return a new instance of the Parser for the given C2C standard and version
	 * @throws C2CMVTException if an error occurs while creating the Parser instance
	 */
	@SuppressWarnings("unchecked")
	public Parser<C2CBaseMessage> getParserInstance(String standardName, String versionName) throws C2CMVTException
	{
		C2CMVTStandard standard = c2cStandardMap.get(standardName);
		C2CMVTStandardVersion version = standard.c2cStandardVersionMap.get(versionName);
		try
		{
			return (Parser<C2CBaseMessage>)Class.forName(version.parserFQN).getDeclaredConstructor().newInstance();
		}
		catch (Exception ex)
		{
			throw new C2CMVTException(ex, String.format("Failed to instantiate parser for version %s of standard %s", versionName, standardName));
		}
	}
}
