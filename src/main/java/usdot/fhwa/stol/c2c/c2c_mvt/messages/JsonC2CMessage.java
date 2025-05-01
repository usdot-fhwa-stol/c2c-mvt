package usdot.fhwa.stol.c2c.c2c_mvt.messages;

import com.github.erosb.jsonsKema.JsonValue;

/**
 * Implementation of {@link C2CMessage} for Json messages.
 * @author Aaron Cherney
 */
public class JsonC2CMessage extends C2CMessage
{
	/**
	 * The message represented as a JsonValue, which can be a JsonObject or
	 * JsonArray
	 */
	protected JsonValue m_oMessage;

	
	/**
	 * Constructor. Calls {@link C2CMessage#C2CMessage(byte[])}.
	 * @param yPayload the message in bytes
	 */
	public JsonC2CMessage(byte[] yPayload)
	{
		super(yPayload);
	}
	
	/**
	 * Constructor. Calls {@link JsonC2CMessage#JsonC2CMessage(byte[])} and
	 * sets {@link #m_oMessage}
	 * @param yPayload the message in bytes
	 * @param oMessage the message as a Json Object or Array
	 */
	public JsonC2CMessage(byte[] yPayload, JsonValue oMessage)
	{
		this(yPayload);
		m_oMessage = oMessage;
	}
	
	/**
	 * Returns a String representation of the Json message
	 * @return String representation of the Json message
	 */
	public String getMessage()
	{
		return m_oMessage.toString();
	}
}
