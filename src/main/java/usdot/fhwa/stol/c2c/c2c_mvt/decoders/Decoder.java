package usdot.fhwa.stol.c2c.c2c_mvt.decoders;

import java.util.ArrayList;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CMessage;

/**
 * Base class that defines the interfaces and common variables for Decoders. All
 * of the functions throw a {@link C2CMVTException} to allow all of the Exception
 * handling and logging to be taken care of by {@link StandardValidationController}
 * @author Aaron Cherney
 */
public abstract class Decoder 
{	
	/**
	 * Name of the encoding that needs to be decoded
	 */
	protected String m_sEncoding;
	
	/**
	 * Method to implement the logic for decoding a message
	 * @param <T> The type of {@link C2CMessage} the Decoder creates, for example
	 * messages in Json format would return {@Link JsonC2CMessage}s
	 * @param yBytes the message(s) to be decoded
	 * @return A list of decoded {@link C2CMessage}s 
	 * @throws C2CMVTException
	 */
	public abstract <T extends C2CMessage> ArrayList<T> decode(byte[] yBytes) throws C2CMVTException;
	
	/**
	 * Method to implement the logic to separate messages that are concatenated
	 * @param yBytes the message(s) to separate
	 * @return List of separated messages as byte arrays
	 * @throws C2CMVTException
	 */
	protected abstract ArrayList<byte[]> separateMessages(byte[] yBytes) throws C2CMVTException;
	
	/**
	 * Method to implement the logic for checking for security threats
	 * @param yBytes the message to check
	 * @return true if no security threats are detected, false if a threat is detected
	 * so the message can be ignore
	 * @throws C2CMVTException
	 */
	protected abstract boolean checkSecurity(byte[] yBytes) throws C2CMVTException;
	
	/**
	 * Ensures the message has the expected syntax and creates the appropriate
	 * {@link C2CMessage}. Throws a C2CMVTException if the syntax is not valid.
	 * @param <T> The type of {@link C2CMessage} the Decoder creates, for example
	 * messages in Json format would return a {@Link JsonC2CMessage}
	 * @param yBytes the message in bytes
	 * @return the {@link C2CMessage} wrapping the message if it has the correct
	 * syntax
	 * @throws C2CMVTException
	 */
	protected abstract <T extends C2CMessage> T checkSyntax(byte[] yBytes) throws C2CMVTException;
	
	/**
	 * @return the name of encoding used for this Decoder
	 */
	public String getEncoding()
	{
		return m_sEncoding;
	}
	
	/**
	 * Sets the encoding used for this Decoder
	 * @param sEncoding name of the encoding
	 */
	public void setEncoding(String sEncoding)
	{
		m_sEncoding = sEncoding;
	}
}
