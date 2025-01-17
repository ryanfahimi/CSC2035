import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        // Check the arguments
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.err.println("port should be a number between 1025 and 65535\n");
            System.exit(1);
        }

        int portNumber = Integer.parseInt(args[0]);
        byte[] incomingData = new byte[1024];
        DatagramPacket incomingPacket = null;
        DatagramSocket serverSocket = null;

        try {
            serverSocket = new DatagramSocket(portNumber);
        } catch (SocketException e) {
            System.out.println(
                    "Socket could not be opened, or the socket could not bind to the specified port " + portNumber);
            System.out.println(e.getMessage());
        }

        while (true) {
            incomingPacket = new DatagramPacket(incomingData, incomingData.length);
            serverSocket.receive(incomingPacket);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(incomingData);
            ObjectInputStream objectStream = new ObjectInputStream(inputStream);
            try {
                Student student = (Student) objectStream.readObject();
                System.out.println("Student information received: " + student);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            InetAddress IPAddress = incomingPacket.getAddress();
            int port = incomingPacket.getPort();
            String reply = "message is received";
            byte[] replyByte = reply.getBytes();
            DatagramPacket replyPacket = new DatagramPacket(replyByte, replyByte.length, IPAddress, port);
            serverSocket.send(replyPacket);

            serverSocket.close();
            System.exit(0);
        }
    }
}