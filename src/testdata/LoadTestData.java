package testdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class LoadTestData
{
	public static byte[] loadTestData() throws IOException
	{
		ByteArrayOutputStream sourceBuffer = new ByteArrayOutputStream();
		try ( InputStream in = LoadTestData.class.getResourceAsStream("book1.txt"))
		{
			byte[] buf = new byte[4096];
			for (int len; (len = in.read(buf)) > 0;)
			{
				sourceBuffer.write(buf, 0, len);
			}
		}
		return sourceBuffer.toByteArray();
	}
}
