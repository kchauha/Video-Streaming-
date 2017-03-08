import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.Timer;

public class Server implements Runnable, ActionListener {

	InetAddress CliIP;
	int destPort = 0;
	Socket RTSPsock;
	static BufferedReader readBuff;
	static BufferedWriter writeBuff;
	static String VideoFileName;
	static int ID = 123456;
	int seqNum = 0;
	Socket client;
	final static String CRLF = "\r\n";
	Server theServer1;
	DatagramSocket RTPsock;
	JLabel label;
	int in = 0;
	VideoStream video;
	static int MJPEG_TYPE = 26;
	static int FRAME_PERIOD = 100;
	static int len = 500;
	final static int START = 0;
	final static int READY = 1;
	final static int PLAYING = 2;
	final static int SETUP = 3;
	final static int PLAY = 4;
	final static int PAUSE = 5;
	final static int STOP = 6;
	Timer timer;
	byte[] buf;

	public Server() {
		JFrame jme = new JFrame("Server");
		timer = new Timer(FRAME_PERIOD, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);
		buf = new byte[15000];
		jme.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				timer.stop();
				System.exit(0);
			}
		});
		label = new JLabel("Frame: ", JLabel.CENTER);
		jme.getContentPane().add(label, BorderLayout.CENTER);
	}

	public Server(Socket client, Server theServer) {
		// TODO Auto-generated constructor stub
		this.client = client;
		theServer1 = theServer;
	}

	public static void main(String argv[]) throws Exception {
		JFrame j2m = new JFrame();
		j2m.pack();
		j2m.setVisible(true);
		Server theServer = new Server();
		int RTSPport = 5555;
		@SuppressWarnings("resource")
		ServerSocket listenSocket = new ServerSocket(RTSPport);
		while (true) {
			Socket RTSPsock = listenSocket.accept();
			new Thread(new Server(RTSPsock, theServer)).start();
		}
	}

	// Run
	// method-------------------------------------------------------------------------
	synchronized public void run() {
		int state;
		try {
			System.out.println("Server Started");
			PrintStream pstream = new PrintStream(client.getOutputStream());
			theServer1.CliIP = client.getInetAddress();
			state = START;
			readBuff = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			writeBuff = new BufferedWriter(new OutputStreamWriter(
					client.getOutputStream()));
			int request_type;
			boolean done = false;
			while (!done) {
				request_type = theServer1.RTSPrequest(); // blocking
				if (request_type == SETUP) {
					done = true;
					state = READY;
					System.out.println("state: SETUP");
					theServer1.RTSPresponse();
					theServer1.video = new VideoStream(VideoFileName);
					theServer1.RTPsock = new DatagramSocket();
				}
			}
			while (true) {
				request_type = theServer1.RTSPrequest(); // blocking
				if ((request_type == PLAY) && (state == READY)) {
					theServer1.RTSPresponse();
					theServer1.timer.start();
					state = PLAYING;
					System.out.println("state: PLAYING");
				} else if ((request_type == PAUSE) && (state == PLAYING)) {
					theServer1.RTSPresponse();
					theServer1.timer.stop();
					state = READY;
					System.out.println("state: SETUP");
				} else if (request_type == STOP) {
					theServer1.RTSPresponse();
					theServer1.timer.stop();
					pstream.close();
					System.exit(0);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	synchronized public void actionPerformed(ActionEvent e) {
		DatagramPacket senddp;
		DatagramSocket RTPsocka;
		InetAddress cliIPa;
		int destPorta;
		RTPsocka = RTPsock;
		cliIPa = CliIP;
		destPorta = destPort;
		if (in < len) {
			in++;
			try {
				int frameLen = video.getnextframe(buf);
				RTPpacket pack = new RTPpacket(MJPEG_TYPE, in, in
						* FRAME_PERIOD, buf, frameLen);
				int pLen = pack.getlength();
				byte[] packet_bits = new byte[pLen];
				pack.getpacket(packet_bits);
				senddp = new DatagramPacket(packet_bits, pLen, cliIPa,
						destPorta);
				RTPsocka.send(senddp);
				pack.printheader();
				label.setText("Send frame: " + in);
			} catch (Exception ex) {
				System.out.println("Exception: " + ex);
				System.exit(0);
			}
		} else {
			timer.stop();
		}
	}

	synchronized protected void RTSPresponse() {
		try {
			writeBuff.write("RTSP/1.0 200 OK" + CRLF);
			writeBuff.write("CSeq: " + seqNum + CRLF);
			writeBuff.write("Session: " + ID + CRLF);
			writeBuff.flush();
			System.out.println("Response sent");
		} catch (Exception ex) {
			System.out.println("Exception: " + ex);
			System.exit(0);
		}
	}

	protected int RTSPrequest() {
		int request_type = -1;
		try {
			String req = readBuff.readLine();
			System.out.println(req);
			StringTokenizer tokens = new StringTokenizer(req);
			String reqa = tokens.nextToken();
			if ((new String(reqa)).compareTo("SETUP") == 0)
				request_type = SETUP;
			else if ((new String(reqa)).compareTo("PLAY") == 0)
				request_type = PLAY;
			else if ((new String(reqa)).compareTo("PAUSE") == 0)
				request_type = PAUSE;
			else if ((new String(reqa)).compareTo("STOP") == 0)
				request_type = STOP;
			if (request_type == SETUP) {
				VideoFileName = tokens.nextToken();
			}
			String SeqNumLine = readBuff.readLine();
			System.out.println(SeqNumLine);
			tokens = new StringTokenizer(SeqNumLine);
			tokens.nextToken();
			seqNum = Integer.parseInt(tokens.nextToken());
			String LastLine = readBuff.readLine();
			System.out.println(LastLine);
			if (request_type == SETUP) {
				tokens = new StringTokenizer(LastLine);
				for (int i = 0; i < 3; i++)
					tokens.nextToken(); // skip unused stuff
				destPort = Integer.parseInt(tokens.nextToken());
			}
		} catch (Exception ex) {
			System.out.println("Exception: " + ex);
			System.exit(0);
		}
		return (request_type);
	}
}