/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package usdot.fhwa.stol.c2c.c2c_mvt.decoders;

import com.github.erosb.jsonsKema.JsonParseException;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.controllers.StandardValidationController;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

/**
 *
 * @author Federal Highway Administration
 */
public class JsonDecoder extends Decoder
{
	public JsonDecoder()
	{
	}
	
	@Override
	public ArrayList<JsonC2CMessage> decode(byte[] yBytes)
		throws C2CMVTException
	{
		if (!checkSecurity(yBytes))
			throw new C2CMVTException(new Exception("Found possible security threat. Did not attempt validation."), null);
		
		ArrayList<byte[]> yMessages = separateMessages(yBytes);
		
		int nMsgCount = 0;
		try
		{
			ArrayList<JsonC2CMessage> oMessages = new ArrayList(yMessages.size());
			for (byte[] yMsg : yMessages)
			{
				oMessages.add(checkSyntax(yMsg));
				++nMsgCount;
			}
			for (JsonC2CMessage oMsg : oMessages)
				StandardValidationController.addMessage(oMsg.getMessage());
			return oMessages;
		}
		catch (JsonParseException oEx)
		{
			throw new C2CMVTException(oEx, String.format("Error occured while checking syntax of message %d of %d", nMsgCount + 1, yMessages.size()));
		}
	}

	@Override
	protected ArrayList<byte[]> separateMessages(byte[] yBytes)
		throws C2CMVTException
	{
		ArrayList<byte[]> yMessages = new ArrayList();
		try (ByteArrayInputStream oBaos = new ByteArrayInputStream(yBytes);
			BufferedReader oReader = new BufferedReader(new InputStreamReader(oBaos, Charset.forName(m_sEncoding))))
		{
			ByteArrayOutputStream oOut = new ByteArrayOutputStream();
			BufferedWriter oWriter = new BufferedWriter(new OutputStreamWriter(oOut, Charset.forName(m_sEncoding)));
			if (yBytes.length > 2 && yBytes[0] == (byte)0xEF && yBytes[1] == (byte)0xBB && yBytes[2] == (byte)0xBF) // skip BOM, only allowing UTF-8 right now
			{
				for (int n = 0; n < 3; n++)
					oBaos.read();
			}
			int nChar;
			int nBraceCount = 1;
			char cStartingBrace = (char)oReader.read();
			if (cStartingBrace != '[' && cStartingBrace != '{')
				throw new Exception("JSON data must start with [ or {");
			oWriter.write(cStartingBrace);
			char cClosingBrace = cStartingBrace == '[' ? ']' : '}';
			while ((nChar = oReader.read()) >= 0)
			{
				char cCur = (char)nChar;
				oWriter.write(nChar);
				if (cCur == cStartingBrace)
					++nBraceCount;
				else if (cCur == cClosingBrace)
					--nBraceCount;
				if (nBraceCount == 0)
				{
					oWriter.close();
					yMessages.add(oOut.toByteArray());
					oOut.reset();
					oWriter = new BufferedWriter(new OutputStreamWriter(oOut, Charset.forName(m_sEncoding)));
					int nCommaCount = 0;
					while ((nChar = oReader.read()) >= 0)
					{
						cCur = (char)nChar;
						if (Character.isWhitespace(cCur)); // do nothing for whitespace
						else if (cCur == ',' && nCommaCount++ == 0); // do nothing for first comma
						else if (cCur == '[' || cCur == '{')
						{
							oWriter.write(cCur);
							cStartingBrace = cCur;
							cClosingBrace = cStartingBrace == '[' ? ']' : '}';
							nBraceCount = 1;
							break;
						}
						else
						{
							throw new Exception("JSON Messages must be separated by an empty string, a single comma, or whitespace characters. Found unexpected character '" + cCur + "'");
						}
					}
				}
			}
			oWriter.flush();
			if (oOut.size() != 0)
				throw new Exception("JSON data must end with ] or }");
		}
		catch (Exception oEx)
		{
			throw new C2CMVTException(oEx, "Error occured in separateMessage()");
		}

		return yMessages;
	}

	@Override
	protected boolean checkSecurity(byte[] yBytes)
		throws C2CMVTException
	{
		// don't need to implement any security checks since there are no SQL databases or script execution
		return true;
	}

	@Override
	protected JsonC2CMessage checkSyntax(byte[] yBytes)
		throws C2CMVTException
	{
		try
		{
			JsonValue oJson = new JsonParser(new BufferedInputStream(new ByteArrayInputStream(yBytes))).parse();
			return new JsonC2CMessage(yBytes, oJson);
		}
		catch (Exception oEx)
		{
			throw new C2CMVTException(oEx, "Invalid JSON Syntax");
		}
	}
}
