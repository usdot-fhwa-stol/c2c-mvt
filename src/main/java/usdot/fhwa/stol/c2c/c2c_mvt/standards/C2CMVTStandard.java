package usdot.fhwa.stol.c2c.c2c_mvt.standards;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.github.erosb.jsonsKema.JsonObject;
import com.github.erosb.jsonsKema.JsonString;
import com.github.erosb.jsonsKema.JsonValue;

public class C2CMVTStandard 
{
	Map<String, C2CMVTStandardVersion> c2cStandardVersionMap = new HashMap<>();


	C2CMVTStandard(String standardName, JsonObject standard)
	{
		JsonObject versions = (JsonObject)standard.get("versions");
		for (Entry<JsonString, JsonValue> versionEntry : versions.getProperties().entrySet())
		{
			String versionName = versionEntry.getKey().getValue();
			c2cStandardVersionMap.put(versionName, new C2CMVTStandardVersion((JsonObject)versionEntry.getValue()));
		}
	}
}
