import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

public class Client {
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

        try {

            clientSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.err.println("the socket could not be opened, or the socket could not bind to the specified port " +
                    portNumber);
            System.exit(1);
        }
        Student student = new Student(1, "John", "Smith");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
        objectStream.writeObject(student);

        byte[] data = outputStream.toByteArray();
        DatagramPacket sentPacket = new DatagramPacket(data, data.length, ip, portNumber);
        clientSocket.send(sentPacket);

        System.out.println("Student information is sent");

        byte[] incomingData = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(incomingData, incomingData.length);
        clientSocket.receive(receivedPacket);
        String response = new String(receivedPacket.getData()).trim();
        System.out.println("Response from server: " + response);

        clientSocket.close();
        System.exit(0);
    }
}