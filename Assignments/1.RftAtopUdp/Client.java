import java.io.IOException;

import java.util.Scanner;

/*
 * This file contains the main method for the client.
 * Do NOT change anything in this file.
 * 
 * start client as:
 *
 *      java Client <host name> <port number> <input file name> <output file name> <payload size> <nm|wt|gpn>  
 *
 * Where:
 *      host name is the address of the server
 * 		port number is the port the server is listening on
 *		input file name is the name of the file to send
 *      output file name is name for the file on the server
 *      payload size is the size of the segment payload
 *      nm selects normal transfer mode 
 *      wt selects transfer with time out and a probability of  
 *          corruption of segments. The probability must be between 0.0 and 1.0,
 *          inclusive.
 * 	    gpn selects GoBackN mode with window size
 *
 * Only specify one transfer mode. That is, either nm, wt or gpn      
 */

public class Client {
	static Protocol proto = new Protocol();

	/* the main method */
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 6) {
			System.err.println(
					"Usage: java Client <host name> <port number> <input file name> <output file name> <payload Size> <nm|wt|gbn>");
			System.err.println("host name: is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.err.println("input file name: is the file to send");
			System.err.println("output file name: is the name of the output file");
			System.err.println("Payload size: is the size of the payload");
			System.err.println("nm selects normal transfer|wt selects transfer with time out");
			System.exit(1);
		}

		// initialise the Protocol attributes
		proto.initProtocol(args[0], args[1], args[2], args[3], args[4], args[5]);

		System.out.println("----------------------------------------------------");
		System.out.println("SENDER: File " + args[2] + " exists  ");
		System.out.println("----------------------------------------------------");
		System.out.println("----------------------------------------------------");
		String choice = args[5];
		float loss = 0;
		int window = 1;
		Scanner sc = new Scanner(System.in);

		/* Send meta data to the server */
		System.out.println("SENDER: Sending meta data");
		proto.sendMetadata();

		if (choice.equalsIgnoreCase(Protocol.TIMEOUT_MODE)) {
			System.out.println("Enter the probability of a corrupted checksum (between 0 and 1): ");
			loss = sc.nextFloat();
			proto.setLossProb(loss);
		}

		if (choice.equalsIgnoreCase(Protocol.GBN_MODE)) {
			System.out.println("Enter the size of the window (an int): ");
			window = sc.nextInt();
		}

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		switch (choice) {
			case Protocol.NORMAL_MODE:
				proto.sendFileNormal();
				break;

			case Protocol.TIMEOUT_MODE:
				proto.sendFileWithTimeout();
				break;

			case Protocol.GBN_MODE:
				proto.sendFileNormalGBN(window);
				break;

			default:
				System.out.println("Error! mode is not recognised");
		}

		System.out.println("SENDER: File is sent\n");
		sc.close();
	}
}
