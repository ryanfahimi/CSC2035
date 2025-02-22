import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/*
 * This file contains the main method for the server.
 * Do NOT change anything in this file.
 * 
 * start server as:
 *      
 *      java Server <port number>
 *
 * where port number is a port for the server to listen on in the range 1025 to 65535
 */

public class Server {
	public static final String ANSI_PURPLE = "\u001B[35m";
	public static final String RESET = "\033[0m"; // Text Reset

	DatagramSocket socket = null;
	long totalBytes = 0;
	String outputFileName;

	/* the main method */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 1) {
			System.err.println("Usage: java Server <port number>");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.exit(1);
		}

		Server server = new Server();
		int portNumber = Integer.parseInt(args[0]);
		// create a socket
		server.socket = new DatagramSocket(portNumber);

		System.out.println("SERVER: binding ... Ready to receive meta info from the client ");
		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		server.ReceiveMetaData();

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		System.out.println("SERVER: Waiting for the actual file ..");
		System.out.println("------------------------------------------------------------------");
		server.receiveFile();
	}

	/* Receive the file in chuncks on the given socket from the client */
	public void receiveFile() throws IOException {
		FileWriter myWriter = new FileWriter(outputFileName);
		int currentTotal = 0;
		byte[] incomingData = new byte[1024];
		Segment dataSeg = new Segment();

		// while still receiving segments
		while (currentTotal < totalBytes) {
			DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

			// receive from the client
			socket.receive(incomingPacket);

			byte[] data = incomingPacket.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(in);

			// read and print the content of the segment
			try {
				dataSeg = (Segment) is.readObject();
				System.out.println("SERVER: A Segment with sq " + dataSeg.getSq() + " is received: ");
				System.out.println("\tINFO: size " + dataSeg.getSize() + ", checksum " + dataSeg.getChecksum()
						+ ", content (" + dataSeg.getPayLoad() + ")");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			// extract the client IP address and port number
			InetAddress IPAddress = incomingPacket.getAddress();
			int port = incomingPacket.getPort();

			// calculate the checksum, the checksum is not corrupted
			int x = Protocol.checksum(dataSeg.getPayLoad(), false);

			// if the calculated checksum is same as that of received checksum then send
			// corresponding ack
			if (x == dataSeg.getChecksum()) {
				System.out.println("SERVER: Calculated checksum is " + x + "  VALID");
				Segment ackSeg = new Segment();

				// prepare the Ack segment
				ackSeg.setSq(dataSeg.getSq());
				ackSeg.setType(SegmentType.Ack);
				System.out.println("SERVER: Sending an ACK with sq " + ackSeg.getSq());

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(outputStream);
				os.writeObject(ackSeg);
				byte[] dataAck = outputStream.toByteArray();
				DatagramPacket replyPacket = new DatagramPacket(dataAck, dataAck.length, IPAddress, port);

				// send the Ack segment
				socket.send(replyPacket);

				// write the payload of the data segment to output file
				myWriter.write(dataSeg.getPayLoad());
				currentTotal = currentTotal + dataSeg.getSize();

				System.out.println("\t\t>>>>>>> NETWORK: ACK is sent successfully <<<<<<<<<");
				System.out.println("------------------------------------------------");
				System.out.println("------------------------------------------------");
			} else {
				System.out.println("SERVER: Calculated checksum is " + x + "  INVALID");
				System.out.println("SERVER: Not sending any ACK ");
				System.out.println("*************************** ");
			}
		}
		System.out.println("SERVER: File copying complete\n");
		myWriter.close();
	}

	/* Received meta data from the client */
	public void ReceiveMetaData() throws IOException, InterruptedException {
		MetaData metaData = new MetaData();

		byte[] receive = new byte[65535];
		DatagramPacket receiveMetaData = new DatagramPacket(receive, receive.length);

		// receive from the client
		socket.receive(receiveMetaData);

		byte[] data = receiveMetaData.getData();
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);

		try {
			metaData = (MetaData) is.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// extract the size of the file, the name of the output file and the size of the
		// payload
		totalBytes = metaData.getSize();
		outputFileName = metaData.getName();
		int maxSegSize = metaData.getMaxSegSize();

		// print the expected number of segments
		System.out.println(
				"SERVER: Meta info are received successfully: (file name, size, expected number of Segments): ("
						+ metaData.getName() + ", " + metaData.getSize() + ", "
						+ (int) Math.ceil((float) totalBytes / maxSegSize) + ")");
	}
}