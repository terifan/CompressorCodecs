package org.terifan.compression.lzw;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;


public class Test
{
	public static void main(String... args)
	{
		try
		{
//			File f = new File("d:/jvm-app-0.log");
//			byte[] input = new byte[(int) f.length()];
//			try (FileInputStream fin = new FileInputStream(f))
//			{
//				fin.read(input);
//			}

			String text = "ImageWidth,ImageHeight,ImageDescription,Orientation,Make,Model,XResolution,YResolution,ResolutionUnit,Software,DateTime,WhitePoint,PrimaryChromaticities,YCbCrCoefficients,YCbCrPositioning,ReferenceBlackWhite,Copyright,ExifOffset,ExposureTime,FNumber,ExposureProgram,ISOSpeedRatings,ExifVersion,DateTimeOriginal,DateTimeDigitized,ComponentConfiguration,CompressedBitsPerPixel,ShutterSpeedValue,ApertureValue,BrightnessValue,ExposureBiasValue,MaxApertureValue,SubjectDistance,MeteringMode,LightSource,Flash,FocalLength,MakerNote,UserComment,FlashPixVersion,ColorSpace,ExifImageWidth,ExifImageHeight,RelatedSoundFile,ExifInteroperabilityOffset,FocalPlaneXResolution,FocalPlaneYResolution,FocalPlaneResolutionUnit,SensingMethod,FileSource,SceneType,ThumbWidth,ThumbHeight,ThumbBitsPerSample,ThumbCompression,ThumbPhotometricInterpretation,ThumbStripOffsets,ThumbSamplesPerPixel,ThumbRowsPerStrip,ThumbStripByteConunts,ThumbXResolution,ThumbYResolution,ThumbPlanarConfiguration,ThumbResolutionUnit,ThumbJpegIFOffset,ThumbJpegIFByteCount,ThumbYCbCrCoefficients,ThumbYCbCrSubSampling,ThumbYCbCrPositioning,ThumbReferenceBlackWhite,RatingNumber,RatingPercent,ImageNumber,Title,ImageUniqueID,Comment,Author,Tags,Subject";
//			String text = "Changed,Count,Created,Create Time,Coordinate,Color,Client,Closed,Clear,Colorful";

			{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (DeflaterOutputStream dos = new DeflaterOutputStream(baos))
			{
				dos.write(text.getBytes());
			}
			System.out.println(baos.size());
			}

//			String name = text.split(",");
//			for (int i = 0; i < names.length; i++)
//			{
//				String s = "";
//				for (int j = names[i].length(); --j >= 0;)
//				{
//					s += names[i].charAt(j);
//				}
//				names[i] = s;
//			}
//
//			Arrays.sort(names);
//
//			byte [] input = "The quick brown fox jumped over the lazy dog.".getBytes();
			byte [] input = text.getBytes();

			int[] alpha = new int[256];
			int j = 0;
			for (int i = 'A'; i <= 'Z'; i++)
			{
				alpha[i] = j++;
			}
			for (int i = 'a'; i <= 'z'; i++)
			{
				alpha[i] = j++;
			}
			alpha[','] = j++;

			long t = System.nanoTime();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (LZWOutputStream out = new LZWOutputStream(baos, 6, 12))
			{
				for (byte c : input)
				{
					out.write(alpha[c&255]);
				}
			}
			t = System.nanoTime() - t;

			System.out.println("Compress: " + t / 1000000 + "ms");
			System.out.println("Size: " + baos.size() + " / " + input.length);

			t = System.nanoTime();
			try (LZWInputStream in = new LZWInputStream(new ByteArrayInputStream(baos.toByteArray()), 6, 12))
			{
				baos = new ByteArrayOutputStream();
				for (int c; (c = in.read()) != -1;)
				{
					System.out.print((char)c);
					baos.write(c);
				}
			}
			t = System.nanoTime() - t;

			System.out.println("Decompress: " + t / 1000000 + "ms");
			System.out.println("Match: " + Arrays.equals(input, baos.toByteArray()));
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
