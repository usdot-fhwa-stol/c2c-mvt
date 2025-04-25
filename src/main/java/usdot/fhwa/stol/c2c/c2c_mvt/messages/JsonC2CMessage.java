/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.messages;

import com.github.erosb.jsonsKema.JsonValue;

/**
 *
 * @author Federal Highway Administration
 */
public class JsonC2CMessage extends C2CMessage
{
	protected JsonValue m_oMessage;

	public JsonC2CMessage(byte[] yPayload)
	{
		super(yPayload);
	}
	
	
	public JsonC2CMessage(byte[] yPayload, JsonValue oMessage)
	{
		this(yPayload);
		m_oMessage = oMessage;
	}
	
	
	public String getMessage()
	{
		return m_oMessage.toString();
	}
}
