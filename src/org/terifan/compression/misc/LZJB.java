package org.terifan.compression.misc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;


public class LZJB
{
	private final static int MATCH_BITS = 6;
	private final static int MATCH_MIN = 3;
	private final static int MATCH_MAX = (1 << MATCH_BITS) + (MATCH_MIN - 1);
	private final static int WINDOW_SIZE = 1 << (16 - MATCH_BITS);
	private final static int OFFSET_MASK = WINDOW_SIZE - 1;
	private final static int REFS_COUNT = 1 << 16;
	private final static int REFS_DEPTH = 8;

	private byte[] mWindow = new byte[0];
	private int[][] mRefs = new int[REFS_COUNT][REFS_DEPTH];
	private int mWindowOffset;


	public LZJB()
	{
		for (int[] refs : mRefs)
		{
			Arrays.fill(refs, -1);
		}
	}


	public byte[] compress(byte[] aSrcBuffer) throws IOException
	{
		return compress(new ByteArrayInputStream(aSrcBuffer));
	}


	public byte[] compress(ByteArrayInputStream aInput) throws IOException
	{
		int copymap = 0;
		int copymask = 128;

		int futureSize = 0;
		byte[] future = new byte[MATCH_MAX];

		for (int c; futureSize < MATCH_MAX && (c = aInput.read()) != -1; futureSize++)
		{
			future[futureSize] = (byte)c;
		}

		String log = "";

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ByteArrayOutputStream work = new ByteArrayOutputStream();

		boolean first = true;

		while (futureSize > 0)
		{
			copymask <<= 1;
			if (copymask == 256)
			{
				if (work.size() > 0)
				{
					output.write(copymap);
					work.writeTo(output);
					work.reset();
				}

				copymask = 1;
				copymap = 0;

				if (first)
				{
					copymask <<= 1;
					first = false;
				}
			}

			int bestLength = 0;

			if (futureSize >= MATCH_MIN)
			{
				int hash = ((0xff & future[0]) << 16) + ((0xff & future[1]) << 8) + (0xff & future[2]);
				hash += hash >> 9;
				hash += hash >> 5;
				hash &= REFS_COUNT - 1;
				int[] refs = mRefs[hash];

				int bestDist = 0;

				for (int i = 0; i < REFS_DEPTH && refs[i] > -1; i++)
				{
					int dist = mWindowOffset - refs[i];
					int mlen = 0;

					if (dist < WINDOW_SIZE)
					{
						for (int cpy = refs[i]; mlen < futureSize && cpy < mWindowOffset; mlen++, cpy++)
						{
							if (future[mlen] != mWindow[cpy])
							{
								break;
							}
						}

						if (mlen > bestLength)
						{
							bestLength = mlen;
							bestDist = dist;
						}
					}
				}

				System.arraycopy(refs, 0, refs, 1, REFS_DEPTH - 1);
				refs[0] = mWindowOffset;

				if (bestLength >= MATCH_MIN)
				{
					copymap |= copymask;
					work.write(0xff & ((((bestLength - MATCH_MIN) << (8 - MATCH_BITS)) | (bestDist >> 8))));
					work.write(0xff & bestDist);
					log+="["+bestDist+","+bestLength+"]";
				}
				else
				{
					work.write(0xff & future[0]);
					bestLength = 1;
					log+="+";
				}
			}
			else
			{
				work.write(0xff & future[0]);
				bestLength = 1;
				log+="^";
			}

			if (mWindowOffset + bestLength > mWindow.length)
			{
				mWindow = Arrays.copyOfRange(mWindow, 0, (mWindowOffset + bestLength) * 3 / 2);
			}

			System.arraycopy(future, 0, mWindow, mWindowOffset, bestLength);
			mWindowOffset += bestLength;
			futureSize -= bestLength;

			System.arraycopy(future, bestLength, future, 0, futureSize);
			for (int c; futureSize < MATCH_MAX && (c = aInput.read()) != -1; futureSize++)
			{
				future[futureSize] = (byte)c;
			}
		}

//		System.out.println(log);
//		Log.hexDump(mWindow);

		output.write(copymap);
		work.writeTo(output);

		byte[] tmp = output.toByteArray();

		if (tmp.length == 3 && tmp[0] == 2 && (tmp[1] & 0x80) == 0)
		{
			tmp = new byte[]
			{
				(byte)((0xff & tmp[1] << 1) | 0x01),
				tmp[2]
			};
		}
		else
		{
			tmp = Arrays.copyOfRange(tmp, 0, tmp.length + 1);
			System.arraycopy(tmp, 0, tmp, 1, tmp.length - 1);
			tmp[0] = (byte)((tmp.length - 1) << 1);
		}

		return tmp;
	}


	public byte[] decompress(InputStream aInput) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		decompress(aInput, output);
		return output.toByteArray();
	}


	public void decompress(InputStream aInput, ByteArrayOutputStream aOutput) throws IOException
	{
		int end = 0;
		int copymap = 0;
		int copymask = 128;

		boolean first = true;

		int c = aInput.read();

		if ((c & 0x01) == 0x01)
		{
			aInput = new ByteArrayInputStream(new byte[]
			{
				(byte)0x02,
				(byte)(c >> 1),
				(byte)aInput.read()
			});

			end = 2;
		}
		else
		{
			end = c >> 1;
		}

		while (end > 0)
		{
			copymask <<= 1;
			if (copymask == 256)
			{
				copymask = 1;
				copymap = aInput.read();
				end--;

				if (first)
				{
					copymask <<= 1;
					first = false;
				}
			}
			if ((copymap & copymask) != 0)
			{
				int a = aInput.read();
				int b = aInput.read();
				end--;
				end--;

				int mlen = (a >> (8 - MATCH_BITS)) + MATCH_MIN;
				int dist = ((a << 8) | b) & OFFSET_MASK;

				int cpy = mWindowOffset - dist;
				if (cpy < 0)
				{
					throw new RuntimeException();
				}
				while (--mlen >= 0)
				{
					mWindow = Arrays.copyOfRange(mWindow, 0, mWindowOffset + 1);
					mWindow[mWindowOffset++] = mWindow[cpy];
					aOutput.write(0xff & mWindow[cpy++]);
				}
			}
			else
			{
				c = aInput.read();
				end--;

				mWindow = Arrays.copyOfRange(mWindow, 0, mWindowOffset + 1);
				mWindow[mWindowOffset++] = (byte)c;
				aOutput.write(c);
			}
		}
	}


	public static void main(String... args)
	{
		try
		{
//			String text = "For us, it's a really exciting outcome, because this novel litigation approach worked and would get us a resolution really quickly, and it gave us a way to get our client's data deleted. We were prepared for much more pushback. It's incredibly useful to have this tool in our toolkit for when phones are taken in the future. I can't see any reason why this couldn't be done whenever another traveler is facing this sort of phone seizure.";

			String[] strings = new String[]{"TransmissionHeader","ApiKey","DevKey","ApiKey","Version","Version","TransmissionType","TransmissionType","TransmissionType","TransmissionCreateDateTime","TransmissionCreateDateTime","TransactionCount","TransactionCount","SenderName","SenderName","SenderName","ReceiverName","ReceiverName","ReceiverName","SenderTransmissionNo","SenderTransmissionNo","SenderTransmissionNo","SuppressTransmissionAck","SuppressTransmissionAck","StopProcessOnError","StopProcessOnError","ProcessGrouping","ProcessGroup","ProcessGroup","ProcessGroup","ProcessGroupOwner","ProcessGroupOwner","ProcessGroupOwner","InSequence","InSequence","StopProcessOnError","StopProcessOnError","ProcessGrouping","TransmissionHeader","TransmissionBody","TransactionElement","TransactionHeader","TransactionType","TransactionType","TransactionType","SenderTransactionId","SenderTransactionId","SenderTransactionId","ProcessInfo","ProcessGroup","ProcessGroup","ProcessGroup","ProcessSequence","ProcessSequence","ProcessSequence","ProcessInfo","SendReason","Remark","RemarkText","RemarkText","RemarkText","Remark","Identifier","IdentifierText","IdentifierText","IdentifierText","Identifier","ObjectType","ObjectType","ObjectType","SendReason","Reference","Reference","Reference","TransactionHeader","RegisterShipmentOrder","ShipmentOrderSubmittedEvent","ShipmentOrder","OrderDateTime","OrderDateTime","TransportInformation","ServiceLevel","Handle","standard","Handle","ServiceLevel","TermsOfTransport","IncoTerm","Handle","Handle","IncoTerm","LocationRefSummary","LocationIdentifiers","LocationIdentifier","DomainIdentifier","Location","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","IdentifierAuthority","LocationIdentifier","LocationIdentifiers","PartyRole","Handle","consignor","Handle","PartyRole","TimeSpan","From","Date","Date","HasTime","HasTime","From","To","Date","Date","HasTime","HasTime","To","TimeSpan","LocationRefSummary","TermsDateTime","TermsDateTime","TermsOfTransport","TransportMode","Handle","sea","Handle","TransportMode","TransportProduct","Handle","bulk","Handle","TransportProduct","TransportInformation","Contacts","ShipmentOrderContact","PartyRole","Handle","consignor","Handle","PartyRole","CommunicationMethod","Handle","email","Handle","CommunicationMethod","PhoneNumber","PhoneNumber","Name","Name","Addresses","PhoneAddress","PhoneNumber","PhoneNumber","PhysicalType","PhoneAddress","Addresses","ShipmentOrderContact","ShipmentOrderContact","PartyRole","Handle","consignee","Handle","PartyRole","CommunicationMethod","Handle","email","Handle","CommunicationMethod","PhoneNumber","Name","Intern Kontakt","Name","Addresses","PhoneAddress","PhoneNumber","PhysicalType","PhoneAddress","Addresses","ShipmentOrderContact","Contacts","Locations","LocationRefSummary","LocationIdentifiers","LocationIdentifier","DomainIdentifier","Location","DomainIdentifier","Identifier","33937","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","LocationIdentifier","LocationIdentifiers","PartyRole","Handle","consignor","Handle","PartyRole","TimeSpan","From","Date","Date","HasTime","HasTime","From","To","Date","Date","HasTime","HasTime","To","TimeSpan","LocationRefSummary","LocationRefSummary","LocationIdentifiers","LocationIdentifier","DomainIdentifier","Location","DomainIdentifier","Identifier","LYS","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","LocationIdentifier","LocationIdentifiers","PartyRole","Handle","consignee","Handle","PartyRole","TimeSpan","From","Date","Date","HasTime","HasTime","From","To","Date","Date","HasTime","HasTime","To","TimeSpan","LocationRefSummary","Locations","Remark","Remark","ShipmentOrderIdentifiers","ShipmentOrderIdentifier","DomainIdentifier","OrderNumber","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderIdentifier","ShipmentOrderIdentifier","DomainIdentifier","OrderType","DomainIdentifier","Identifier","SubOrder","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderIdentifier","ShipmentOrderIdentifiers","ShipmentOrderOrderLines","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1111","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","40000","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1106","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","1000000","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1103","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1115","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1117","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1118","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1119","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLine","Article","ContractArticleIdentifiers","ContractArticleIdentifier","DomainIdentifier","Article","DomainIdentifier","Identifier","1120","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ContractArticleIdentifier","ContractArticleIdentifiers","Article","Identifiers","ShipmentOrderOrderLineIdentifier","DomainIdentifier","OrderLine","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentOrderOrderLineIdentifier","Identifiers","Quantity","Quantity","Description","ShipmentOrderOrderLine","ShipmentOrderOrderLines","Measurements","ShipmentMeasurement","MeasurementType","Handle","loading_duration","Handle","MeasurementType","Value","Value","ShipmentMeasurement","ShipmentMeasurement","MeasurementType","Handle","unloading_duration","Handle","MeasurementType","Value","0.25","Value","ShipmentMeasurement","Measurements","OrderNumber","OrderNumber","ShipmentOrder","RegisterShipmentRef","Identifiers","ShipmentIdentifier","DomainIdentifier","OrderNumber","DomainIdentifier","Identifier","Identifier","IdentifierAuthority","LXIR","IdentifierAuthority","ShipmentIdentifier","Identifiers","RegisterShipmentRef","ShipmentOrderSubmittedEvent","RegisterShipmentOrder","TransactionElement","TransmissionBody","Transmission"};

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			LZJB compressor = new LZJB();

			LZJB decompressor2 = new LZJB();

			int sumPacked = 0;
			int sumUnpacked = 0;

			for (String s : strings)
			{
				byte[] src = s.getBytes();

				byte[] packed = compressor.compress(src);

				System.out.println(packed.length + " / " + src.length);

				byte[] unpacked = decompressor2.decompress(new ByteArrayInputStream(packed));
				if (!new String(unpacked).equals(s))
				{
					System.out.println(s);
					System.out.println(new String(unpacked));

					throw new IllegalStateException();
				}

				baos.write(packed);

				sumPacked += packed.length;
				sumUnpacked += src.length;
			}

//			Log.hexDump(baos.toByteArray());

			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			LZJB decompressor = new LZJB();

			for (String s : strings)
			{
				byte[] unpacked = decompressor.decompress(bais);

				if (!new String(unpacked).equals(s))
				{
					System.out.println(s);
					System.out.println(new String(unpacked));

					throw new IllegalStateException();
				}
			}

			int zip = 0;
			baos = new ByteArrayOutputStream();
			for (String s : strings)
			{
				baos.write(s.getBytes().length);
				baos.write(s.getBytes());
			}
			byte[] data = baos.toByteArray();
			baos.reset();
			DeflaterOutputStream def = new DeflaterOutputStream(baos);
			def.write(data);
			def.close();
			zip = baos.size();

			System.out.println("----------------");
			System.out.println(sumPacked + " / " + sumUnpacked + " (" + zip + ")");
		}
		catch (Exception e)
		{
			e.printStackTrace(System.out);
		}
	}
}
