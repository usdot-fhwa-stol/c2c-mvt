/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt;

import org.slf4j.Logger;

/**
 *
 * @author Federal Highway Administration
 */
public class Util 
{
	public static void LogException(Logger oLogger, Exception oEx)
	{
		StringBuilder sBuf = new StringBuilder();
		StackTraceElement[] oStackTrace = oEx.getStackTrace();
		for (StackTraceElement oSTE : oStackTrace)
		{
			sBuf.append(oSTE.toString()).append('\n');
		}
		if (oStackTrace.length > 0)
			sBuf.setLength(sBuf.length() - 1);
		
		oLogger.error(sBuf.toString());
	}
}
