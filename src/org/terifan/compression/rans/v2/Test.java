package org.terifan.compression.rans.v2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import testdata.LoadTestData.Source;
import static testdata.LoadTestData.loadTestData;



public class Test
{
	public static void main(String... aArgs)
	{
		try
		{
			test1();
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}


	private static void test2() throws IOException
	{
		byte[] sourceBuffer = loadTestData(Source.BOOK);

		SymbolStats stats = new SymbolStats(14);
		stats.count_freqs(sourceBuffer);
		stats.normalize_freqs();
		stats.make_alias_table();

		int[] cum2sym = new int[1<<14];
		for (int s=0; s < 256; s++)
			for (int i=stats.cum_freqs[s]; i < stats.cum_freqs[s+1]; i++)
				cum2sym[i] = s;

		RansEncSymbol[] esyms = new RansEncSymbol[256];
		RansDecSymbol[] dsyms = new RansDecSymbol[256];

		int prob_bits = 14;
		for (int i = 0; i < 256; i++)
		{
			esyms[i] = new RansEncSymbol();
			dsyms[i] = new RansDecSymbol();

			RANSCodec.RansEncSymbolInit(esyms[i], stats.cum_freqs[i], stats.freqs[i], prob_bits);
			RANSCodec.RansDecSymbolInit(dsyms[i], stats.cum_freqs[i], stats.freqs[i]);
		}

		byte[] compressedData;

		{
			ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

			RANSCodec codec = new RANSCodec();
			codec.RansEncInit();

			for (int i = sourceBuffer.length; --i >= 0; )
			{
				int s = 0xff & sourceBuffer[i];
				codec.RansEncPutSymbol(outputBuffer, esyms[s]);
			}
			codec.RansEncFlush(outputBuffer);

			compressedData = reverse(outputBuffer.toByteArray());

			System.out.printf("rANS encode: %s\n", compressedData.length + " / " + sourceBuffer.length);
		}

		{
			ByteArrayInputStream inputBuffer = new ByteArrayInputStream(compressedData);
			ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

			RANSCodec codec = new RANSCodec();
			codec.RansDecInit(inputBuffer);

			for (int i = 0; i < sourceBuffer.length; i++)
			{
				int s = cum2sym[codec.RansDecGet(prob_bits)];
				outputBuffer.write(s);
				codec.RansDecAdvanceSymbol(inputBuffer, dsyms[s], prob_bits);
			}

			System.out.println(outputBuffer.toString());

			System.out.printf("RESULT: %s%n", Arrays.equals(sourceBuffer, outputBuffer.toByteArray()));
		}
	}


	private static void test1() throws IOException
	{
		byte[] sourceBuffer = loadTestData(Source.BOOK);

		SymbolStats stats = new SymbolStats(16);
		stats.count_freqs(sourceBuffer);
		stats.normalize_freqs();
		stats.make_alias_table();

		byte[] compressedData;

		{
			ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();

			RANSCodec codec = new RANSCodec();
			codec.RansEncInit();

			for (int i = sourceBuffer.length; --i >= 0; )
			{
				int s = 0xff & sourceBuffer[i];
				codec.RansEncPutAlias(outputBuffer, stats, s);
			}
			codec.RansEncFlush(outputBuffer);

			compressedData = reverse(outputBuffer.toByteArray());

			System.out.printf("rANS encode: %s\n", compressedData.length + " / " + sourceBuffer.length);
		}

		{
			ByteArrayInputStream inputBuffer = new ByteArrayInputStream(compressedData);
			byte[] outputBuffer = new byte[sourceBuffer.length];

			RANSCodec codec = new RANSCodec();
			codec.RansDecInit(inputBuffer);

			for (int i = 0; i < sourceBuffer.length; i++)
			{
				int s = codec.RansDecGetAlias(stats);
				outputBuffer[i] = (byte)s;
				codec.RansDecRenorm(inputBuffer);
			}

			System.out.printf("RESULT: %s%n", Arrays.equals(sourceBuffer, outputBuffer));
		}
	}


	private static byte[] reverse(byte[] aBuffer)
	{
		for (int i = 0, n = aBuffer.length, j = aBuffer.length - 1, end = n / 2; i < end; i++, j--)
		{
			byte tmp = aBuffer[i];
			aBuffer[i] = aBuffer[j];
			aBuffer[j] = tmp;
		}
		return aBuffer;
	}
}
