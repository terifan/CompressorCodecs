package org.terifan.compression.dic;

import java.io.IOException;
import java.util.Arrays;


class UTF8
{
	/**
	 * Write fixed length String
	 */
//	public static byte [] encodeChars(String aInput) throws IOException
//	{
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		try (VLCOutputStream vlc = new VLCOutputStream(baos))
//		{
//			for (int i = 0, len = aInput.length(), p = 'A'; i < len; i++)
//			{
//				vlc.writeVar32S(aInput.charAt(i) - p);
//				p = aInput.charAt(i);
//			}
//		}
//		return baos.toByteArray();
//	}

	/**
	 * Write fixed length String
	 */
	public static byte [] encodeUTF8(String aInput)
	{
		byte [] array = new byte[aInput.length()];
		int ptr = 0;

		for (int i = 0, len = aInput.length(); i < len; i++)
		{
			if (ptr + 3 > array.length)
			{
				array = Arrays.copyOf(array, (ptr + 3) * 3 / 2);
			}

			char c = aInput.charAt(i);
		    if ((c >= 0x0000) && (c <= 0x007F))
		    {
				array[ptr++] = (byte)c;
		    }
		    else if (c > 0x07FF)
		    {
				array[ptr++] = (byte)(0xE0 | ((c >> 12) & 0x0F));
				array[ptr++] = (byte)(0x80 | ((c >>  6) & 0x3F));
				array[ptr++] = (byte)(0x80 | ((c      ) & 0x3F));
		    }
		    else
		    {
				array[ptr++] = (byte)(0xC0 | ((c >>  6) & 0x1F));
				array[ptr++] = (byte)(0x80 | ((c      ) & 0x3F));
		    }
		}

		return Arrays.copyOf(array, ptr);
	}


	/**
	 * Read fixed length String
	 */
	public static String decodeUTF8(byte [] aInput)
	{
		char [] array = new char[aInput.length];
		int bufOffset = 0;

		for (int i = 0, sz = aInput.length; i < sz;)
		{
			int c = aInput[i++] & 255;

			if (c < 128) // 0xxxxxxx
			{
				array[bufOffset++] = (char)c;
			}
			else if ((c & 0xE0) == 0xC0) // 110xxxxx
			{
				array[bufOffset++] = (char)(((c & 0x1F) << 6) | (aInput[i++] & 0x3F));
			}
			else if ((c & 0xF0) == 0xE0) // 1110xxxx
			{
				array[bufOffset++] = (char)(((c & 0x0F) << 12) | ((aInput[i++] & 0x3F) << 6) | (aInput[i++] & 0x3F));
			}
			else
			{
				throw new IllegalStateException("This decoder only handles 16-bit characters: c = " + c);
			}
		}

		return new String(array, 0, bufOffset);
	}


	/**
	 * Write zero terminated String
	 */
	public static void encodeUTF8Z(String aInput, BitOutputStream aOutput) throws IOException
	{
		for (int i = 0, len = aInput.length(); i < len; i++)
		{
			char c = aInput.charAt(i);
			if (c == 0)
			{
				throw new IOException("ASCII zero charachers not allowed.");
			}
		    if ((c >= 0x0000) && (c <= 0x007F))
		    {
				aOutput.writeInt8(c & 0xff);
		    }
		    else if (c > 0x07FF)
		    {
				aOutput.writeInt8(0xE0 | ((c >> 12) & 0x0F));
				aOutput.writeInt8(0x80 | ((c >>  6) & 0x3F));
				aOutput.writeInt8(0x80 | ((c      ) & 0x3F));
		    }
		    else
		    {
				aOutput.writeInt8(0xC0 | ((c >>  6) & 0x1F));
				aOutput.writeInt8(0x80 | ((c      ) & 0x3F));
		    }
		}

		aOutput.writeInt8(0); // terminator
	}


	/**
	 * Read zero terminated String
	 */
	public static String decodeUTF8Z(BitInputStream aInput) throws IOException
	{
		StringBuilder output = new StringBuilder();

		for (;;)
		{
			int c = aInput.readInt8();

			if (c == 0)
			{
				break;
			}
			if (c < 128) // 0xxxxxxx
			{
				output.append((char)c);
			}
			else if ((c & 0xE0) == 0xC0) // 110xxxxx
			{
				output.append((char)(((c & 0x1F) << 6) | (aInput.readInt8() & 0x3F)));
			}
			else if ((c & 0xF0) == 0xE0) // 1110xxxx
			{
				output.append((char)(((c & 0x0F) << 12) | ((aInput.readInt8() & 0x3F) << 6) | (aInput.readInt8() & 0x3F)));
			}
			else
			{
				throw new IllegalStateException("This decoder only handles 16-bit characters: c = " + c);
			}
		}

		return output.toString();
	}
}
