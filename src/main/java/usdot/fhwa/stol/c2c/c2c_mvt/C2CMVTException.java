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
	 * The original Exception
	 */
	public Exception originalException;

	
	/**
	 * An additional message to give more details about the exception
	 */
	public String additionalMessage;
	
	
	/**
	 * Constructor that sets {@link #originalException} and {@link #additionalMessage}
	 * @param ex original exception
	 * @param msg additional message giving details about the exception
	 */
	public C2CMVTException(Exception ex, String msg)
	{
		super(msg, ex);
		originalException = ex;
		additionalMessage = msg;
	}
}
