/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.messages;

/**
 *
 * @author Federal Highway Administration
 */
public class C2CMessage 
{
	protected byte[] m_yPayload;
	
	public C2CMessage(byte[] yPayload)
	{
		m_yPayload = yPayload;
	}
}
