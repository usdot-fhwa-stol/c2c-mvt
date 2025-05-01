package usdot.fhwa.stol.c2c.c2c_mvt.messages;

/**
 * Base class used for messages in the application.
 * @author Aaron Cherney
 */
public class C2CMessage 
{
	/**
	 * Representation of the message as bytes
	 */
	protected byte[] m_yPayload;
	
	/**
	 * Constructor. Sets {@link #m_yPayload}
	 * @param bytes The message in bytes
	 */
	public C2CMessage(byte[] yPayload)
	{
		m_yPayload = yPayload;
	}
}
