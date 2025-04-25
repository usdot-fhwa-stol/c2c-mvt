/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.decoders;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.C2CMessage;

/**
 *
 * @author Federal Highway Administration
 */
public abstract class Decoder 
{
	protected static final String[] SQLKEYWORDS = new String[]{"select", "insert", "update", "delete", "drop", "union", "create", "alter", "truncate", "exec", "execute", "having"};
	protected static final Logger LOGGER = LoggerFactory.getLogger(Decoder.class);
	
	protected String m_sEncoding;
	
	public abstract <T extends C2CMessage> ArrayList<T> decode(byte[] yBytes) throws C2CMVTException;
	
	protected abstract ArrayList<byte[]> separateMessages(byte[] yBytes) throws C2CMVTException;
	
	protected abstract boolean checkSecurity(byte[] yBytes) throws C2CMVTException;
	
	protected abstract <T extends C2CMessage> T checkSyntax(byte[] yBytes) throws C2CMVTException;
	
	public String getEncoding()
	{
		return m_sEncoding;
	}
	
	public void setEncoding(String sEncoding)
	{
		m_sEncoding = sEncoding;
	}
}
