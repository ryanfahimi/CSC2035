import java.io.*;
import java.net.*;

public class EchoClient {
	public static void main(String[] args) throws IOException {

		// Check the arguments
		if (args.length != 2) {
			System.err.println("Usage: java EchoClient <host name> <port number>");
			System.err.println("host name is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port is a positive number in the range 1025 to 65535\n");
			System.exit(1);
		}

		// Convert the arguments to ensure that they are valid
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		InetAddress ip = InetAddress.getByName(hostName);

		DatagramSocket clientSocket = null;
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

		try {
			// Creates a datagramSocket and binds it to any available port on local machine
			clientSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " +
					portNumber);
			System.exit(1);
		}

		String userInput = null;
		byte buffer[] = null;

		while (userInput == null || !(userInput.trim().equals("exit"))) {
			System.out.print("please enter the message: ");
			userInput = stdIn.readLine();

			// convert the string message into a byte array
			buffer = userInput.getBytes();
			System.out.println(new String(buffer).trim());

			// Constructs a DatagramPacket for sending data at specified address and
			// specified port
			// The data must be in the form of an array of bytes
			DatagramPacket DpSend = new DatagramPacket(buffer, buffer.length, ip, portNumber);

			clientSocket.send(DpSend);

			System.out.println("waiting ...");

			// clear the buffer
			buffer = new byte[256];

			DatagramPacket DpReceive = new DatagramPacket(buffer, buffer.length);

			clientSocket.receive(DpReceive);

			String received = new String(DpReceive.getData()).trim();

			System.out.println("client received " + received);
			System.out.println("---------------------------------");
		}
		System.out.println("exiting ...");
		clientSocket.close();
	}
}