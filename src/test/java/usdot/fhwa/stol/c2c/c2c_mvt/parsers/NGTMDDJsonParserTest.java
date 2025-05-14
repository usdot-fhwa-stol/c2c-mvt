package usdot.fhwa.stol.c2c.c2c_mvt.parsers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import java.nio.charset.StandardCharsets;

import usdot.fhwa.stol.c2c.c2c_mvt.decoders.JsonDecoder;
import usdot.fhwa.stol.c2c.c2c_mvt.C2CMVTException;
import usdot.fhwa.stol.c2c.c2c_mvt.messages.JsonC2CMessage;



class NGTMDDJsonParserTest 
{
	private JsonDecoder jsonDecoder;
	private NGTMDDJsonParser parser;
	
	@BeforeEach
	void setUp() 
	{
		jsonDecoder = new JsonDecoder();
		jsonDecoder.setEncoding(StandardCharsets.UTF_8.name());

		parser = new NGTMDDJsonParser();
	}


	@Test
	void testIdentifyMessageType_ThrowsC2CMVTException() 
	{
		try 
		{
			JsonC2CMessage message = jsonDecoder.checkSyntax("{\"test\":\"message\"}".getBytes(StandardCharsets.UTF_8));
			Assertions.assertThatThrownBy(() -> parser.identifyMessageType(message))
			.isInstanceOf(C2CMVTException.class)
			.hasMessageContaining("Failed to identify message type");
		} 
		catch (Exception e) 
		{
			Assertions.fail("Exception should not be thrown", e);
		}	   
	}
}