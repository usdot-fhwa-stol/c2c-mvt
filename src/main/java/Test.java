
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Federal Highway Administration
 */
public class Test 
{
	public static void main(String[] sArgs)
		throws Exception
	{
		try (BufferedReader oReader = new BufferedReader(new InputStreamReader(Files.newInputStream(Path.of("C:/Users/aaron.cherney/Documents/BOM.json")), Charset.forName("UTF-8"))))
		{
			int nChar;
			int nRead = 0;
			while ((nChar = oReader.read()) >= 0)
			{
				++nRead;
			}
			System.out.println(nRead);
		}
	}
}
