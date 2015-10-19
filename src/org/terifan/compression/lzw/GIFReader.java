package org.terifan.compression.lzw;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.imageio.ImageTypeSpecifier;


public class GIFReader
{
	public static BufferedImage readImage(File aFile) throws IOException
	{
		BufferedImage image = null;

		DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(aFile)));
		try
		{
			if (in.readInt() != 0x47494638)
			{
				throw new IOException("Not a GIF image");
			}
			int version = in.readShort();
			if (version != 0x3961 && version != 0x3761)
			{
				throw new IOException("Not a GIF image");
			}

			int width = Short.reverseBytes(in.readShort());
			int height = Short.reverseBytes(in.readShort());

			byte[][] palette = new byte[3][256];

			for (int code = 0; code != 0x3B;)
			{
				code = in.read();
//				System.out.println("code=" + code);

				switch (code)
				{
					case 0xF7: // global palette
					{
						in.read(); // background
						in.read(); // aspect
						for (int i = 0; i < 256; i++)
						{
							palette[0][i] = in.readByte();
							palette[1][i] = in.readByte();
							palette[2][i] = in.readByte();
						}
						break;
					}
					case 0x21: // extension
					{
						in.read(); // extension type
						int len = Integer.reverse(in.readInt());
						in.skip(len);
						in.read(); // 0
						break;
					}
					case 0x2C: // image
					{
						in.readShort(); // offset x
						in.readShort(); // offset y
						width = Short.reverseBytes(in.readShort());
						height = Short.reverseBytes(in.readShort());
						in.read(); // 0

						int initialCodeSize = in.read();

						byte [] alpha = new byte[256];
						Arrays.fill(alpha, (byte)255);

						image = ImageTypeSpecifier.createIndexed(palette[0], palette[1], palette[2], alpha, initialCodeSize, DataBuffer.TYPE_BYTE).createBufferedImage(width, height);
						byte [] buffer = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();

						try (LZWInputStream lzw = new LZWInputStream(new PacketInputStream(in), 12, initialCodeSize))
						{
							lzw.read(buffer);
						}

						in.read(); // 0

						break;
					}
					case 0x3B: // EOF
					{
						break;
					}
					default:
					{
						throw new IOException("Unexpected header code: " + code);
					}
				}
			}

			return image;
		}
		finally
		{
			in.close();
		}
	}


	private static class PacketInputStream extends InputStream
	{
		private byte[] mBuffer = new byte[256];
		private int mOffset, mLength;
		private InputStream mInputStream;

		PacketInputStream(InputStream aInputStream)
		{
			mInputStream = aInputStream;
		}

		@Override
		public int read() throws IOException
		{
			if (mOffset == mLength)
			{
				mOffset = 0;
				mLength = mInputStream.read();
				if (mLength == -1)
				{
					return -1;
				}
				if (mLength == 0)
				{
					throw new EOFException();
				}
				for (int off = 0, rem = mLength; rem > 0;)
				{
					int read = mInputStream.read(mBuffer, off, rem);
					if (read == -1)
					{
						throw new EOFException();
					}
					rem -= read;
					off += read;
				}
			}

			return mBuffer[mOffset++] & 255;
		}
	}
}