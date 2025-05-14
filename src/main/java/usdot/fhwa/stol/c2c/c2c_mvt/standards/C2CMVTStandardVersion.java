package usdot.fhwa.stol.c2c.c2c_mvt.standards;

import java.util.ArrayList;
import java.util.List;

import com.github.erosb.jsonsKema.JsonArray;
import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;


public class C2CMVTStandardVersion 
{
	List<String> encodingsList;
	List<String> messageTypesList;
	String decoderFQN;
	String parserFQN;
	String validatorFQN;
	String schemaFile;

	C2CMVTStandardVersion(JsonObject version)
	{
		JsonArray encodingArray = (JsonArray)version.get("encodings");
		encodingsList = new ArrayList<>(encodingArray.length());
		for (JsonValue encoding : encodingArray.getElements())
		{
			encodingsList.add(((JsonString)encoding).getValue());
		}

		JsonArray messageTypeArray = (JsonArray)version.get("messageTypes");
		messageTypesList = new ArrayList<>(messageTypeArray.length());
		for (JsonValue messageType : messageTypeArray.getElements())
		{
			messageTypesList.add(((JsonString)messageType).getValue());
		}

		decoderFQN = "usdot.fhwa.stol.c2c.c2c_mvt.decoders." + ((JsonString)version.get("decoder")).getValue();
		parserFQN = "usdot.fhwa.stol.c2c.c2c_mvt.parsers." + ((JsonString)version.get("parser")).getValue();
		validatorFQN = "usdot.fhwa.stol.c2c.c2c_mvt.validators." + ((JsonString)version.get("validator")).getValue();
		schemaFile = ((JsonString)version.get("schema")).getValue();
	}
}
