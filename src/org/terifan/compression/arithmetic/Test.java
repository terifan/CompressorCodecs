package org.terifan.compression.arithmetic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.terifan.compression.io.BitInputStream;
import org.terifan.compression.io.BitOutputStream;


public class Test
{
	public static void main(String ... args)
	{
		try
		{
//			byte [] plainText = "0000000010000000100000000000010001000000010000000010000001000000000100000010000000100000001000000000000000101010101000000010000000001000".getBytes();
//
//			int [][] initFreq = new int[2][2];
//			initFreq[0][0] = 10;
//			initFreq[0][1] = 0;
//			initFreq[1][0] = 10;
//			initFreq[1][1] = 0;
//
			byte [] plainText = "big biggles biggeled olbig".getBytes();

			int [][] initFreq = new int[256][256];
			initFreq['b']['i'] = 100;
			initFreq['i']['g'] = 100;

//			byte [] plainText = "aaaaaaaaaaaaaaBaaaaaaaaaaaaaaBaaaaaaaaaaaaaaBaaaaaaaaaaaaaaBxxxxxaaaaaaaaaaBxxxxxxxaaaaaaaaaBxxxxxaaaaaaaaaaaaBxxxxaaaaaaaaaaaaaaaBxxxxxxaaaaaaaaaaaaaaaBxxxxaaaaaaaaaaaaaaaBxxxxxxaaaaaaaaaaaaaB".getBytes();
//
//			int [][] initFreq = new int[256][256];
//			initFreq['a']['a'] = 100;
//			initFreq['B']['x'] = 100;
//			initFreq['B']['a'] = 50;
//			initFreq['x']['x'] = 100;
//			initFreq['x']['a'] = 50;

			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			try (BitOutputStream bitOutputStream = new BitOutputStream(baos))
			{
				FrequencyTable [] table = new FrequencyTable[initFreq.length];
				for (int i = 0; i < initFreq.length; i++)
				{
					table[i] = new FrequencyTable(initFreq[i], true);
				}
				ArithmeticModel model = new ArithmeticModel();
				ArithmeticEncoder encoder = new ArithmeticEncoder(model, bitOutputStream);
				for (int i = 0, p = 0; i < plainText.length; i++)
				{
					int c = plainText[i] & 255;
//				int c = plainText[i]-'0';
					encoder.encode(table[p], 1+c);
					p = c;
				}
				encoder.encodeEnd();
			}

			byte [] compressedData = baos.toByteArray();

//			Debug.hexDump(48, compressedData);


			byte [] decompressedData;

			FrequencyTable [] table = new FrequencyTable[initFreq.length];
			baos = new ByteArrayOutputStream();
			for (int i = 0; i < initFreq.length; i++)
			{
				table[i] = new FrequencyTable(initFreq[i], true);
			}
			ArithmeticModel model = new ArithmeticModel();
			ArithmeticDecoder decoder = new ArithmeticDecoder(model, new BitInputStream(new ByteArrayInputStream(compressedData)));
			for (int i = 0, p = 0; i < plainText.length; i++)
			{
				p = decoder.decode(table[p])-1;
//				p = decoder.decode(table[p]);
				baos.write(p);
			}
			decompressedData = baos.toByteArray();

			System.out.println(Arrays.equals(plainText, decompressedData) + "\t" + plainText.length + "\t" + compressedData.length + "\t"+ (int)((1-compressedData.length/(double)(plainText.length))*1000)/10f);
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}