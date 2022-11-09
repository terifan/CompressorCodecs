package testdata;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
//			byte [] input = "The quick brown fox jumped over the lazy dog.".getBytes();


public class LoadTestData
{
	public static enum Source
	{
		BOOK,
		LENNA_COLOR,
		LENNA_GRAY,
	}

	public static int[] loadTestDataInt(Source aSource) throws IOException
	{
		byte[] data = loadTestData(aSource);
		int[] out = new int[data.length];
		for (int i = 0; i < data.length; i++)
		{
			out[i] = 0xff & data[i];
		}
		return out;
	}

	public static byte[] loadTestData(Source aSource) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (aSource == Source.BOOK)
		{
			try ( InputStream in = LoadTestData.class.getResourceAsStream("book1.txt"))
			{
				byte[] buf = new byte[4096];
				for (int len; (len = in.read(buf)) > 0;)
				{
					output.write(buf, 0, len);
				}
			}
		}
		else if (aSource == Source.LENNA_GRAY)
		{
			BufferedImage image = ImageIO.read(LoadTestData.class.getResourceAsStream("lenna_gray.png"));
			for (int y = 0; y < image.getHeight(); y++)
			{
				for (int x = 0; x < image.getWidth(); x++)
				{
					output.write(0xff & image.getRGB(x, y));
				}
			}
		}
		else if (aSource == Source.LENNA_COLOR)
		{
			BufferedImage image = ImageIO.read(LoadTestData.class.getResourceAsStream("lenna.png"));
			for (int c = 0; c < 3; c++)
			{
				for (int y = 0; y < image.getHeight(); y++)
				{
					for (int x = 0; x < image.getWidth(); x++)
					{
						output.write(0xff & (image.getRGB(x, y) >> (8 * c)));
					}
				}
			}
		}
		return output.toByteArray();
	}
}
