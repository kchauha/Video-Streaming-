import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;

public class Client {
	JFrame f = new JFrame("Client");
	JButton setupButton = new JButton("Setup");
	JButton playButton = new JButton("Play");
	JButton pauseButton = new JButton("Pause");
	JButton stopButton = new JButton("Stop");
	JPanel mainPanel = new JPanel();
	JPanel buttonPanel = new JPanel();
	JLabel iconLabel = new JLabel();
	ImageIcon icon;
	static BufferedReader readBuff;
	static BufferedWriter writeBuff;
	static String VideoFileName;
	int seqNum = 0;
	int pid = 0;
	final static String CRLF = "\r\n";
	static int MJPEG_TYPE = 26;
	DatagramPacket rcv;
	DatagramSocket RTPsock;
	static int rcevPort = 25000;
	Timer timer;
	byte[] buf;
	final static int START = 0;
	final static int READY = 1;
	final static int PLAYING = 2;
	static int state;
	Socket RTSPsock;
	

	public Client() {
		f.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		buttonPanel.setLayout(new GridLayout(1, 0));
		buttonPanel.add(setupButton);
		buttonPanel.add(playButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(stopButton);
		setupButton.addActionListener(new setupButtonListener());
		playButton.addActionListener(new playButtonListener());
		pauseButton.addActionListener(new pauseButtonListener());
		stopButton.addActionListener(new stopButtonListener());
		iconLabel.setIcon(null);
		mainPanel.setLayout(null);
		mainPanel.add(iconLabel);
		mainPanel.add(buttonPanel);
		iconLabel.setBounds(0, 0, 380, 280);
		buttonPanel.setBounds(0, 280, 380, 50);
		f.getContentPane().add(mainPanel, BorderLayout.CENTER);
		f.setSize(new Dimension(390, 370));
		f.setVisible(true);
		timer = new Timer(20, new timerListener());
		timer.setInitialDelay(0);
		timer.setCoalesce(true);
		buf = new byte[15000];
	}

	public static void main(String argv[]) throws Exception {
		System.out.println("Client - 1 Started");
		Client theClient = new Client();
		int RTSPport = 5555;
		String ServerHost = "localhost";
		InetAddress servIP = InetAddress.getByName(ServerHost);
		VideoFileName = "movie.mjpeg";
		theClient.RTSPsock = new Socket(servIP, RTSPport);
		readBuff = new BufferedReader(new InputStreamReader(theClient.RTSPsock.getInputStream()));
		writeBuff = new BufferedWriter(new OutputStreamWriter(theClient.RTSPsock.getOutputStream()));
		state = START;
	}

	class setupButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (state == START) {
				try {
					RTPsock = new DatagramSocket(rcevPort);
					RTPsock.setSoTimeout(5);
				} catch (SocketException se) {
					System.out.println("Socket exception: " + se);
					System.exit(0);
				}
				seqNum = 1;
				RTSPreq("SETUP");
				if (servRes() != 200)
					System.out.println("Something went wrong");
				else {
					state = READY;
				}
			}
		}
	}

	class playButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (state == READY) {
				seqNum = seqNum++;
				RTSPreq("PLAY");
				if (servRes() != 200)
					System.out.println("Something went wrong");
				else {
					state = PLAYING;
					timer.start();
				}
			}
		}
	}

	class pauseButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (state == PLAYING) {
				seqNum = seqNum++;
				RTSPreq("PAUSE");
				if (servRes() != 200)
					System.out.println("Something went wrong");
				else {
					state = READY;
					timer.stop();
				}
			}
		}
	}

	class stopButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			seqNum = seqNum++;
			RTSPreq("STOP");
			if (servRes() != 200)
				System.out.println("Something went wrong");
			else {
				state = START;
				timer.stop();
				System.exit(0);
			}
		}
	}

	class timerListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			rcv = new DatagramPacket(buf, buf.length);
			try {
				RTPsock.receive(rcv);
				RTPpacket r = new RTPpacket(rcv.getData(),
						rcv.getLength());
				System.out.println("Received RTP packet SeqNum: "
						+ r.getsequencenumber() + " TimeStamp "
						+ r.gettimestamp() + " ms, of type "
						+ r.getpayloadtype());
				r.printheader();
				int payload_length = r.getpayload_length();
				byte[] payload = new byte[payload_length];
				r.getpayload(payload);
				Toolkit toolkit = Toolkit.getDefaultToolkit();
				Image image = toolkit.createImage(payload, 0, payload_length);
				icon = new ImageIcon(image);
				iconLabel.setIcon(icon);
			} catch (InterruptedIOException iioe) {
			} catch (IOException ioe) {
				System.out.println("Exception: " + ioe);
			}
		}
	}
	private void RTSPreq(String reqType) {
		try {
			writeBuff.write(reqType + " " + VideoFileName+ " RTSP/1.0" + CRLF);
			System.out.print(reqType + " " + VideoFileName + " RTSP/1.0"+ CRLF);
			writeBuff.write("CSeq " + seqNum + CRLF);
			System.out.print("CSeq " + seqNum + CRLF);
			if (reqType.equals("SETUP")) {
				writeBuff.write("Transport: RTP/UDP; client_port= "+ rcevPort + CRLF);
				System.out.print("Transport: RTP/UDP; client_port= "+ rcevPort + CRLF);
			} else {
				writeBuff.write("Session: " + pid + CRLF);
				System.out.print("Session: " + pid + CRLF);
			}
			try {
				writeBuff.flush();
			} catch (IOException e) {
				System.out.println("Exception: " + e.toString());
			}
		} catch (Exception ex) {
			System.out.println("Exception: " + ex);
			System.exit(0);
		}
	}
	private int servRes() {
		int reply_code = 0;
		try {
			String StatusLine = readBuff.readLine();
			System.out.println(StatusLine);
			StringTokenizer tokens = new StringTokenizer(StatusLine);
			tokens.nextToken(); // skip over the RTSP version
			reply_code = Integer.parseInt(tokens.nextToken());
			if (reply_code == 200) {
				String SeqNumLine = readBuff.readLine();
				System.out.println(SeqNumLine);
				String SessionLine = readBuff.readLine();
				System.out.println(SessionLine);
				tokens = new StringTokenizer(SessionLine);
				tokens.nextToken(); // skip over the Session:
				pid = Integer.parseInt(tokens.nextToken());
			}
		} catch (Exception ex) {
			System.out.println("Exception: " + ex);
			System.exit(0);
		}
		return (reply_code);
	}

	
}