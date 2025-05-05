/*
 * Copyright (C) 2025 LEIDOS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package usdot.fhwa.stol.c2c.c2c_mvt.decoders;

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
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;

/**
 * Implementation of {@link Decoder} for Json Messages
 * 
 * @author Aaron Cherney
 */
public class JsonDecoder extends Decoder
{
	private static enum STATE
	{
		BEFORE_ROOT,
		READ,
		IN_QUOTE,
		ESCAPE,
		BETWEEN_MSGS
	};
	/**
	 * Checks for multiple valid messages defined by the bytes. Tests for
	 * and ignores Byte order marking and ensures that the bytes could define
	 * a JSON Object or Array by testing for matching {} or [] respectively
	 * 
	 * 
	 * @param messageBytes the message(s) in bytes
	 * @return list of separate messages as byte[]
	 * @throws C2CMVTException
	 */
	@Override
	public ArrayList<byte[]> separateMessages(byte[] messageBytes)
		throws C2CMVTException
	{
		ArrayList<byte[]> messageList = new ArrayList();
		try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(messageBytes);
			BufferedReader reader = new BufferedReader(new InputStreamReader(byteInputStream, Charset.forName(encoding))))
		{
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName(encoding)));
			if (messageBytes.length > 2 && messageBytes[0] == (byte)0xEF && messageBytes[1] == (byte)0xBB && messageBytes[2] == (byte)0xBF) // skip BOM, only allowing UTF-8 right now
			{
				for (int n = 0; n < 3; n++)
					byteInputStream.read();
			}
			
			char startingBrace = '{';
			char closingBrace = '}';
			int braceCount = 0;
			STATE curState = STATE.BEFORE_ROOT;
			int charsRead = 0;
			int startQuote = -1;
			int charRead;
			while ((charRead = reader.read()) >= 0)
			{
				++charsRead;
				char curChar = (char)charRead;
				if (curState != STATE.BETWEEN_MSGS)
					writer.write(charRead);
				switch (curState)
				{
					case BEFORE_ROOT:
					{
						if (curChar != '[' && curChar != '{')
							throw new Exception("JSON data must start with [ or {");
						startingBrace = curChar;
						closingBrace = startingBrace == '[' ? ']' : '}';
						++braceCount;
						curState = STATE.READ;
						break;
					}
					case READ:
					{
						if (curChar == startingBrace)
							++braceCount;
						else if (curChar == closingBrace)
							--braceCount;
						
						if (curChar == '"')
						{
							curState = STATE.IN_QUOTE;
							startQuote = charsRead;
						}
						if (braceCount == 0)
						{
							writer.close();
							messageList.add(outputStream.toByteArray());
							outputStream.reset();
							writer = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName(encoding)));
							curState = STATE.BETWEEN_MSGS;
						}
						break;
					}
					case IN_QUOTE:
					{
						if (curChar == '\\')
							curState = STATE.ESCAPE;
						else if (curChar == '"')
							curState = STATE.READ;
						break;
					}
					case ESCAPE:
					{
						curState = STATE.IN_QUOTE;
						break;
					}
					case BETWEEN_MSGS:
					{
						if (curChar == '[' || curChar == '{')
						{
							writer.write(charRead);
							startingBrace = curChar;
							closingBrace = startingBrace == '[' ? ']' : '}';
							++braceCount;
							curState = STATE.READ;
						}
						break;
					}
				}
			}

			writer.flush();
			writer.close();
			if (outputStream.size() != 0)
			{
				if (curState == STATE.IN_QUOTE)
					throw new Exception(String.format("Reached end of file without finding closing double quote starting from character %d", startQuote));
				else
					throw new Exception("JSON data must end with ] or }");
			}
		}
		catch (Exception ex)
		{
			throw new C2CMVTException(ex, "Error occured in separateMessage()");
		}

		return messageList;
	}

	/**
	 * 
	 * @param messageBytes message to check security on
	 * @return always true for now, since there are no threats we need to test 
	 * for in json messages
	 * @throws C2CMVTException
	 */
	@Override
	public boolean checkSecurity(byte[] messageBytes)
		throws C2CMVTException
	{
		// don't need to implement any security checks since there are no SQL databases or script execution
		return true;
	}

	/**
	 * Attempts to create a {@link JsonValue} which can be a Json Object or Array.
	 * If the syntax is incorrect the JsonParser will throw an Exception.
	 * @param messageBytes message in bytes to check
	 * @return {@link JsonC2CMessage} that wrap yBytes and contains the created
	 * Json Object/Array
	 * @throws C2CMVTException
	 */
	@Override
	public JsonC2CMessage checkSyntax(byte[] messageBytes)
		throws C2CMVTException
	{
		try
		{
			JsonValue jsonValue = new JsonParser(new BufferedInputStream(new ByteArrayInputStream(messageBytes))).parse();
			return new JsonC2CMessage(messageBytes, jsonValue);
		}
		catch (Exception oEx)
		{
			throw new C2CMVTException(oEx, "Invalid JSON Syntax");
		}
	}
}
