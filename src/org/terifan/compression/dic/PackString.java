package org.terifan.compression.dic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import static org.terifan.compression.util.Log.hexDump;


public class PackString
{
	private final static boolean DISABLED = true;

	private ArrayList<String> mDictionary;


	public PackString()
	{
		mDictionary = new ArrayList<>();

		for (String input : new String[]
		{
			"follow|ed", "node|s", "email|s", "admin", "address|es", "audit|ed", "change|s|d", "client|s", "cluster|s", "cost", "country",
			"subscription|s", "create|d", "customer|s", "date|s", "datetime", "default|s", "delivery", "description|s", "disable|s|d",
			"dispatch|ed", "division|s", "document|s", "enable|d", "event|s", "export|ed", "filename|s", "finish|ed", "id", "identity",
			"index", "issue|s", "large", "location|s", "mandatory", "name|s|d", "number|s|ed", "operator|s", "optional", "organization|s",
			"owner|s", "parameter|s", "priority", "protocol|s", "reference|s", "row|s", "rule|s", "selection|s", "small", "stacktrace",
			"start|ed", "status|es", "storage", "street|s", "subject", "timezone|s", "type", "uri", "user|s", "uuid", "valid|ated",
			"validation", "version|s", "worker|s", "sender|s", "filter|s", "group|ed|s", "set|s", "to", "form|ed|s", "from", "layout|s",
			"source|s", "zone|s", "color|s", "text|s", "label|s", "title|d|s", "application|s", "cloud|s", "config|s", "username|s",
			"password|s", "fail|s|ed", "fast|er|est", "retry", "min|imum", "max|imum", "attempt|ed|s", "interval|s", "multiply", "multiplier|s",
			"log", "logging", "file|d|s", "profile|d|s", "carrier", "key|s", "allow|ed", "marketplace|s", "account|s", "direct", "link|s",
			"state|s", "code|s", "license|s", "insurance|s|d", "limit|s|ed", "accept|ed", "publish|ed", "true", "false", "addition|s", "zip",
			"city", "first", "last", "language|s", "phone|s", "mobile|s", "rate|d|s", "rating|s", "round|ed", "classification|s",
			"remark|s|ed", "expiry", "prediction|s", "employee|s", "stamp", "info", "schema|s", "item|s", "request|s", "method|s",
			"header|s", "value|s", "body", "mode|s", "path|s", "response|s", "portal|s", "port|ed|s", "qualification", "parent|s", "artifact|s",
			"relative|d", "model", "package", "packaging", "properties", "property", "element|s", "dependencies", "dependency",
			"exclusion|s", "build|s", "lifecycle|s", "metadata", "mapping", "plugin|s", "execution|s", "ignore|s|d", "goal|s", "range|s",
			"connector|s", "idle|d", "stop", "second|s", "minute|s", "hour|s", "day|s", "year|s", "manual", "system|s", "extra", "phase",
			"authorization", "collection|s", "interface", "service", "reply", "query", "public", "protect|ed", "private", "logic", "list|s",
			"menu", "receive|d|r", "agent|s", "blame|d", "raw", "init", "initiate|d", "initialize|d", "end|ed", "final", "security", "insight", "action|s",
			"pull|ed", "push|ed", "import|ed", "send", "submit", "information", "job", "safe", "secret", "destination", "visual", "studio",
			"image", "photo", "ycbcr", "xml", "resolution", "aberration", "acceleration", "active", "alias", "altitude", "analog", "angle",
			"anti", "aperture", "applied", "area", "areas", "artist", "author", "backward", "balance|d", "baseline|d", "battery", "bayer",
			"bearing", "best", "bit|s", "black", "blur|ed", "brightness", "byte", "calibration", "camera", "capture", "cell", "chroma",
			"chromatic", "chromaticities", "clip", "coefficient|s", "colorimetric", "comment|s", "component|s", "composite|s", "compress|ed",
			"computer|s", "configuration|s", "contrast", "copyright|s", "corr", "count|s", "crop", "current", "curve|d", "custom", "data", "delta|s",
			"depth|s", "device|s", "differential", "digest|ed", "digital", "digitize|d", "distance", "elevation|s", "embed|ed", "encode|d",
			"encoding", "energy", "enhance|d", "exposure|s", "factor|ed|s", "flash", "format|s", "forward|ed", "frame|s", "frequency",
			"function|s", "gain|s", "gamma", "green", "halftone", "hint|s", "history", "host|s", "humidity", "illuminant", "instance|s",
			"inter", "interchange|d", "interlace|d", "interleave|d", "interoperability", "interpretation|s", "iso", "jpeg", "keyword|s",
			"latitude", "length|s", "level|s", "light|s", "linearization|s", "localize|d", "longitude|s", "look", "make|s", "maker|s",
			"map|s", "mask|ed", "match|ed|ing", "matrix", "measure|s", "meter|ing", "near", "neutral", "noise|s", "note|d", "offset|s",
			"option|s", "orientation|s", "original|s", "output|s|ed", "pack|ed|et", "param|s", "pattern|s", "percent", "photometric",
			"pixel|s", "planar", "plane|s", "policy", "policies", "position|ing", "predictor|s", "pressure|s", "preview|s", "primary",
			"print|ed|er", "process|ed|ing", "quality", "radius|es", "ratio|s", "recommend|ed", "reduction|s", "reel", "related",
			"render|ed", "repeat|s|ed", "restart|s|ed", "safety", "sample|s", "sampling", "satellite|s", "saturation|s", "scale|s",
			"scene|s", "self", "semantic|s", "sensitivity", "serial|s", "setting|s", "shadow|s", "sharpness", "shot|s", "shutter|s",
			"signature|s", "size|s", "software", "space|s", "spatial", "spectral", "split", "standard|s", "strength|s", "strip|s", "sub",
			"table|s", "target|ed", "temperature|s", "threshold|ing", "tile|s", "timer|s", "time|s", "tone|s", "track|s", "transfer|s",
			"unique", "unit|s", "water", "white", "width|s", "vignetting", "zoom", "rgb", "hex", "inv", "dec", "add", "for", "next", "move",
			"left", "right", "up", "down", "competitive", "turnover", "null", "float", "double", "long", "integer", "boolean", "short"
		})
		{
			String[] parts = input.split("\\|");
			for (int i = 0; i < parts.length; i++)
			{
				String word = parts[0] + (i == 0 ? "" : parts[i]);

				if (mDictionary.contains(word))
				{
					throw new IllegalArgumentException(word);
				}

				mDictionary.add(word);
				mDictionary.add(word.substring(0, 1).toUpperCase() + word.substring(1));
				mDictionary.add(word.toUpperCase());
			}
		}

		// ensure words are sorted shortest words first
		mDictionary.sort((s, t) -> s.length() == t.length() ? s.compareTo(t) : s.length() - t.length());

//		System.out.println(mDictionary.size());
	}


	public void pack(BitOutputStream aOutputStream, String aText) throws IOException
	{
		if (DISABLED)
		{
			byte[] buf = UTF8.encodeUTF8(aText);
			aOutputStream.writeVar32(buf.length);
			aOutputStream.write(buf);
			return;
		}

		aOutputStream.writeExpGolomb(aText.length(), 7);

		for (int offset = 0; offset < aText.length();)
		{
			int bestIndex = -1;
			int bestLength = -1;
			for (int i = mDictionary.size(); --i >= 0;) // find longest word
			{
				String word = mDictionary.get(i);
				if (aText.startsWith(word, offset))
				{
					bestIndex = i;
					bestLength = word.length();
					break;
				}
			}

			if (bestLength == -1)
			{
				char c = aText.charAt(offset);
				if (c >= 'a' && c <= 'z')
				{
					aOutputStream.writeBits(0b01, 2);
					aOutputStream.writeBits(c - 'a', 5);
				}
				else if (c >= 'A' && c <= 'Z')
				{
					aOutputStream.writeBits(0b001, 3);
					aOutputStream.writeBits(c - 'A', 5);
				}
				else if (c == ',')
				{
					aOutputStream.writeBits(0b0001, 4);
					aOutputStream.writeBits(0, 4);
				}
				else
				{
					aOutputStream.writeBits(0b0000, 4);
					aOutputStream.writeBits(c, 8);
				}
				bestLength = 1;
			}
			else
			{
				aOutputStream.writeBit(1);
				aOutputStream.writeExpGolomb(bestIndex, 7);
			}

			offset += bestLength;
		}

		aOutputStream.align();
	}


	public String unpack(BitInputStream aInputStream) throws IOException
	{
		StringBuilder sb = new StringBuilder();

		if (DISABLED)
		{
			byte[] buf = new byte[aInputStream.readVar32()];
			aInputStream.read(buf);
			sb.append(UTF8.decodeUTF8(buf));
			return sb.toString();
		}

		int len = (int)aInputStream.readExpGolomb(7);

		while (sb.length() < len)
		{
			if (aInputStream.readBit() == 0)
			{
				if (aInputStream.readBit() == 1)
				{
					sb.append((char)('a' + aInputStream.readBits(5)));
				}
				else if (aInputStream.readBit() == 1)
				{
					sb.append((char)('A' + aInputStream.readBits(5)));
				}
				else if (aInputStream.readBit() == 1)
				{
					sb.append((char)(',' + aInputStream.readBits(4)));
				}
				else
				{
					sb.append((char)aInputStream.readBits(8));
				}
			}
			else
			{
				sb.append(mDictionary.get((int)aInputStream.readExpGolomb(7)));
			}
		}

		aInputStream.align();

		return sb.toString();
	}


	public static void main(String... args)
	{
		try
		{
			String input = "reference,documentType,client,fromDate,toDate,groupSets,sender,filter,apple";

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (BitOutputStream bos = new BitOutputStream(baos))
			{
				new PackString().pack(bos, input);
			}
			byte[] data = baos.toByteArray();

			hexDump(data);
			System.out.println(input.length() + " => " + data.length);

			String decoded = new PackString().unpack(new BitInputStream(new ByteArrayInputStream(data)));

			System.out.println(decoded.equals(input));
		}
		catch (Throwable e)
		{
			e.printStackTrace(System.out);
		}
	}
}
