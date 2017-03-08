all: Server.class Client.class Client1.class RTPpacket.class VideoStream.class RTCPpacket.class
Server.class: Server.java
	javac -d . -classpath . Server.java
Client.class: Client.java
	javac -d . -classpath . Client.java
Client1.class: Client1.java
	javac -d . -classpath . Client1.java
RTPpacket.class: RTPpacket.java
	javac -d . -classpath . RTPpacket.java
VideoStream.class: VideoStream.java
	javac -d . -classpath . VideoStream.java
RTCPpacket.class: RTCPpacket.java
	javac -d . -classpath . RTCPpacket.java
clean:
	rm -f *.class
