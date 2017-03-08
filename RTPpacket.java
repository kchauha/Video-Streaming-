public class RTPpacket {
	static int HeaderSize = 12;
	public int ver;
	public int payloadType;
	public int seqNum;
	public int ts;
	public int Ssrc;
	public byte[] header;
	public int payload_size;
	public byte[] payload;
	public int padd;
	public int ext;
	public int CC;
	public int Marker;
	

	public RTPpacket(int PType, int Framenb, int Time, byte[] data,	int data_length) {
		ver = 2;
		padd = 0;
		ext = 0;
		CC = 0;
		Marker = 0;
		Ssrc = 15;
		seqNum = Framenb;
		ts = Time;
		payloadType = PType;
		header = new byte[HeaderSize];
		header[0] = (byte) ((ver << 7) >> 7);
		header[1] = (byte) (header[1] | (payloadType << 7 - 7));
		header[2] = (byte) (seqNum >> 8);
		header[3] = (byte) (seqNum & 0xFF);
		header[4] = (byte) (ts >> 24);
		header[5] = (byte) ((ts << 8) >> 24);
		header[6] = (byte) ((ts << 16) >> 24);
		header[7] = (byte) ((ts << 24) >> 24);
		header[8] = (byte) (Ssrc >> 24);
		header[9] = (byte) ((Ssrc << 8) >> 24);
		header[10] = (byte) ((Ssrc << 16) >> 24);
		header[11] = (byte) ((Ssrc << 24) >> 24);
		payload_size = data_length;
		payload = new byte[data_length];
		for (int i = 0; i < data_length; i++) {
			payload[i] = data[i];
		}
	}

	public RTPpacket(byte[] pack, int pack_size) {
		ver = 2;
		padd = 0;
		ext = 0;
		CC = 0;
		Marker = 0;
		Ssrc = 0;
		if (pack_size >= HeaderSize) {
			header = new byte[HeaderSize];
			for (int i = 0; i < HeaderSize; i++)
				header[i] = pack[i];
			payload_size = pack_size - HeaderSize;
			payload = new byte[payload_size];
			for (int i = HeaderSize; i < pack_size; i++)
				payload[i - HeaderSize] = pack[i];
			payloadType = header[1] & 127;
			seqNum = unsigned_int(header[3]) + 256
					* unsigned_int(header[2]);
			ts = unsigned_int(header[7]) + 256 * unsigned_int(header[6])
					+ 65536 * unsigned_int(header[5]) + 16777216
					* unsigned_int(header[4]);
		}
	}

	public int getpayload(byte[] data) {
		for (int i = 0; i < payload_size; i++)
			data[i] = payload[i];
		return (payload_size);
	}

	public int getpayload_length() {
		return (payload_size);
	}

	public int getlength() {
		return (payload_size + HeaderSize);
	}

	public int getpacket(byte[] packet) {
		for (int i = 0; i < HeaderSize; i++)
			packet[i] = header[i];
		for (int i = 0; i < payload_size; i++)
			packet[i + HeaderSize] = payload[i];
		return (payload_size + HeaderSize);
	}

	public int gettimestamp() {
		return (ts);
	}

	public int getsequencenumber() {
		return (seqNum);
	}

	public int getpayloadtype() {
		return (payloadType);
	}

	public void printheader() {
		for (int i = 0; i < (HeaderSize - 4); i++) {
			for (int j = 7; j >= 0; j--)
				if (((1 << j) & header[i]) != 0)
					System.out.print("1");
				else
					System.out.print("0");
			System.out.print(" ");
		}
		System.out.println();
	}

	static int unsigned_int(int nb) {
		if (nb >= 0)
			return (nb);
		else
			return (256 + nb);
	}
}