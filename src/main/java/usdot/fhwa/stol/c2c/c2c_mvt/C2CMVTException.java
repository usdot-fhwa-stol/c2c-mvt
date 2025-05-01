/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt;

/**
 * Exception object to be used throughout the application. Wraps an {@link Exception}
 * object and contains a String for an additional message
 * @author Aaron Cherney
 */
public class C2CMVTException extends Exception
{

	/**
	 * The origianal Exception
	 */
	public Exception m_oOriginal;

	/**
	 * An additional message to give more details about the exception
	 */
	public String m_sAdditionalMessage;
	
	/**
	 * Constructor that sets {@link #m_oOriginal} and {@link #m_sAdditionalMessage}
	 * @param excptn original exception
	 * @param string additional message giving details about the exception
	 */
	public C2CMVTException(Exception oEx, String sMsg)
	{
		m_oOriginal = oEx;
		m_sAdditionalMessage = sMsg;
	}
}
