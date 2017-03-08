import java.io.*;

public class VideoStream {
	FileInputStream f;
	int frameNum;

	public VideoStream(String filename) throws Exception {
		f = new FileInputStream(filename);
		frameNum = 0;
	}

	public int getnextframe(byte[] frame) throws Exception {
		int len = 0;
		String frameLen;
		byte[] frame_length = new byte[5];
		f.read(frame_length, 0, 5);
		frameLen = new String(frame_length);
		len = Integer.parseInt(frameLen);
		return (f.read(frame, 0, len));
	}
}