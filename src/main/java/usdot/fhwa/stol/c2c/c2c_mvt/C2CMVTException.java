/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt;

/**
 *
 * @author Federal Highway Administration
 */
public class C2CMVTException extends Exception
{
	public Exception m_oOriginal;
	public String m_sAdditionalMessage;
	
	public C2CMVTException(Exception oEx, String sMsg)
	{
		m_oOriginal = oEx;
		m_sAdditionalMessage = sMsg;
	}
}
