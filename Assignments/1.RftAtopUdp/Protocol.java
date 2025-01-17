/*
 * Replace the following string of 0s with your student number
 * 240653709
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Protocol {

    static final String NORMAL_MODE = "nm"; // normal transfer mode: (for Part 1 and 2)
    static final String TIMEOUT_MODE = "wt"; // timeout transfer mode: (for Part 3)
    static final String GBN_MODE = "gbn"; // GBN transfer mode: (for Part 4)
    static final int DEFAULT_TIMEOUT = 10; // default timeout in seconds (for Part 3)
    static final int DEFAULT_RETRIES = 4; // default number of consecutive retries (for Part 3)

    /*
     * The following attributes control the execution of a transfer protocol and
     * provide access to the
     * resources needed for a file transfer (such as the file to transfer, etc.)
     * 
     */

    private InetAddress ipAddress; // the address of the server to transfer the file to. This should be a
                                   // well-formed IP address.
    private int portNumber; // the port the server is listening on
    private DatagramSocket socket; // The socket that the client bind to
    private String mode; // mode of transfer normal/with timeout/GBN

    private File inputFile; // The client-side input file to transfer
    private String inputFileName; // the name of the client-side input file for transfer to the server
    private String outputFileName; // the name of the output file to create on the server as a result of the file
                                   // transfer
    private long fileSize; // the size of the client-side input file

    private Segment dataSeg; // the protocol data segment for sending segments with payload read from the
                             // input file to the server
    private Segment ackSeg; // the protocol ack segment for receiving ACKs from the server
    private int maxPayload; // The max payload size of the data segment
    private long remainingBytes; // the number of bytes remaining to be transferred during execution of a
                                 // transfer. This is set to the input file size at the start

    private int timeout; // the timeout in seconds to use for the protocol with timeout (for Part 3)
    private int maxRetries; // the maximum number of consecutive retries (retransmissions) to allow before
                            // exiting the client (for Part 3)(This is per segment)

    private int sentBytes; // the accumulated total bytes transferred to the server as the result of a file
                           // transfer
    private float lossProb; // the probability of corruption of a data segment during the transfer (for Part
                            // 3)
    private int currRetry; // the current number of consecutive retries (retransmissions) following a
                           // segment corruption (for Part 3)(This is per segment)
    private int totalSegments; // the accumulated total number of ALL data segments transferred to the server
                               // as the result of a file transfer
    private int resentSegments; // the accumulated total number of data segments resent to the server as a
                                // result of timeouts during a file transfer (for Part 3)

    /**************************************************************************************************************************************
     **************************************************************************************************************************************
     * For this assignment, you have to implement the following methods:
     * sendMetadata()
     * readData()
     * sendData()
     * receiveAck()
     * sendDataWithError()
     * sendFileWithTimeout()
     * sendFileWithGBN()
     * Do not change any method signatures and do not change any other methods or
     * code provided.
     ***************************************************************************************************************************************
     **************************************************************************************************************************************/

    /* PHASE 1 */

    /*
     * This method sends protocol metadata to the server.
     * Sending metadata starts a transfer by sending the following information to
     * the server in the metadata object (defined in MetaData.java):
     * size - the size of the file to send
     * name - the name of the file to create on the server
     * maxSegSize - The size of the payload of the data segment
     * deal with error in sending
     * output relevant information messages for the user to follow progress of the
     * file transfer.
     * This method does not set any of the attributes of the protocol.
     */
    public void sendMetadata() {
        try {
            // Create metadata object
            MetaData metaData = new MetaData();
            metaData.setName(outputFileName);
            metaData.setSize(fileSize);
            metaData.setMaxSegSize(maxPayload);

            // Serialize and send metadata
            DatagramPacket packet = createPacket(metaData);
            socket.send(packet);

            // Print success message per specification
            System.out.printf("SENDER: meta data is sent (file name, size, payload size): (%s, %d, %d)%n",
                    outputFileName, fileSize, maxPayload);

        } catch (IOException e) {
            System.err.printf("ERROR: Failed to send metadata: %s%n", e.getMessage());
            closeSocket();
            System.exit(1);
        }
    }

    /* PHASE 2 */

    /*
     * This method:
     * read the next chunk of data from the file into the data segment (dataSeg)
     * payload.
     * set the correct type of the data segment
     * set the correct sequence number of the data segment.
     * set the data segment's size field to the number of bytes read from the file
     * This method DOES NOT:
     * set the checksum of the data segment.
     * The method returns -1 if this is the last data segment (no more data to be
     * read) and 0 otherwise.
     */
    public int readData() throws IOException {
        try (FileReader fileReader = new FileReader(inputFile)) {
            char[] data = new char[maxPayload];

            // Skip to current position
            if (sentBytes > 0) {
                fileReader.skip(sentBytes);
            }

            // Read next chunk
            int bytesReadCount = fileReader.read(data, 0, maxPayload);

            // Handle end of file
            if (bytesReadCount == -1) {
                return -1;
            }

            // Set segment properties
            dataSeg.setType(SegmentType.Data);
            dataSeg.setSize(bytesReadCount);
            dataSeg.setPayLoad(new String(data, 0, bytesReadCount));
            dataSeg.setSq(dataSeg.getSq() == 0 ? 1 : 0); // Toggle sequence number

            // Update tracking variables
            remainingBytes -= bytesReadCount;
            sentBytes += bytesReadCount;
        }
        return remainingBytes == 0 ? -1 : 0;
    }

    /*
     * This method sends the current data segment (dataSeg) to the server
     * This method:
     * computes a checksum of the data and sets the data segment's checksum prior to
     * sending.
     * output relevant information messages for the user to follow progress of the
     * file transfer.
     */
    public void sendData() throws IOException {
        try {
            // Calculate and set checksum
            int checksum = Protocol.checksum(dataSeg.getPayLoad(), false);
            dataSeg.setChecksum(checksum);

            // Send segment
            DatagramPacket packet = createPacket(dataSeg);
            socket.send(packet);

            // Update counter and print progress
            totalSegments++;
            printSegmentInfo();
        } catch (IOException e) {
            closeSocket();
            throw e;
        }
    }

    // Decide on the right place to :
    // * update the remaining bytes so that it records the remaining bytes to be
    // read from the file after this segment is transferred. When all file bytes
    // have been read, the remaining bytes will be zero
    // * update the number of total sent segments
    // * update the number of sent bytes

    /*
     * This method receives the current Ack segment (ackSeg) from the server
     * This method:
     * needs to check whether the ack is as expected
     * exit of the client on detection of an error in the received Ack
     * return true if no error
     * output relevant information messages for the user to follow progress of the
     * file transfer.
     */
    public boolean receiveAck(int expectedDataSq) {
        // Create buffer for receiving ACK
        byte[] data = new byte[1024];
        DatagramPacket packet = new DatagramPacket(data, data.length);

        try {
            // Receive ACK packet
            socket.receive(packet);

            // Deserialize ACK segment
            Segment receivedAck;
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
                receivedAck = (Segment) objectInputStream.readObject();
            }

            // Verify sequence number
            if (receivedAck.getSq() != expectedDataSq) {
                System.err.println("ERROR: Received ACK with unexpected sequence number");
                System.err.printf("Expected: %d, Received: %d%n", expectedDataSq, receivedAck.getSq());
                System.exit(1);
            }

            // Update protocol's ackSeg and print success
            ackSeg = receivedAck;
            System.out.printf("SENDER: ACK sq= %d RECEIVED.%n", ackSeg.getSq());
            System.out.println("----------------------------------------");

        } catch (SocketTimeoutException e) {
            return false;
        } catch (IOException e) {
            System.err.printf("ERROR: Network error receiving ACK: %s%n", e.getMessage());
            closeSocket();
            System.exit(1);
        } catch (ClassNotFoundException e) {
            System.err.printf("ERROR: Failed to deserialize ACK segment: %s%n", e.getMessage());
            closeSocket();
            System.exit(1);
        }
        return true;
    }

    /* PHASE 3 */

    /*
     * This method sends the current data segment (dataSeg) to the server with
     * errors
     * This method:
     * may corrupt the checksum according to the loss probability specified if the
     * transfer mode is with timeout (wt)
     * If the count of consecutive retries/retransmissions exceeds the maximum
     * number of allowed retries, the method exits the client with an
     * appropriate error message.
     * This method does not receive any segment from the server
     * output relevant information messages for the user to follow progress of the
     * file transfer.
     */
    public void sendDataWithError() throws IOException {
        try {
            // Handle timeout
            if (currRetry > maxRetries) {
                System.err.printf("ERROR: Maximum number of retries (%d) exceeded%n", maxRetries);
                System.exit(1);
            }

            // Handle retransmission
            if (currRetry > 0) {
                System.out.printf(
                        "SENDER: TIMEOUT ALERT: Re-sending the same segment again, current retry: %d%n", currRetry);
            }

            // Determine if segment should be corrupted
            boolean isCorrupted = Protocol.isCorrupted(lossProb);

            // Calculate and set checksum (corrupted if indicated)
            int checksum = Protocol.checksum(dataSeg.getPayLoad(), isCorrupted);
            dataSeg.setChecksum(checksum);

            // Send segment
            DatagramPacket packet = createPacket(dataSeg);
            socket.send(packet);

            // Update counters and print progress
            totalSegments++;
            if (isCorrupted) {
                resentSegments++;
            }
            printSegmentInfo();
        } catch (IOException e) {
            closeSocket();
            throw e;
        }
    }

    /*
     * This method transfers the given file using the resources provided by the
     * protocol structure.
     *
     * This method is similar to the sendFileNormal method except that it resends
     * data segments if no ACK for a segment is received from the server.
     * This method:
     * simulates network corruption of some data segments by injecting corruption
     * into segment checksums (using sendDataWithError() method).
     * will timeout waiting for an ACK for a corrupted segment and will resend the
     * same data segment.
     * updates attributes that record the progress of a file transfer. This includes
     * the number of consecutive retries for each segment.
     *
     * output relevant information messages for the user to follow progress of the
     * file transfer.
     * after completing the file transfer, display total segments transferred and
     * the total number of resent segments
     * 
     * relevant methods that need to be used include: readData(),
     * sendDataWithError(), receiveAck().
     */
    void sendFileWithTimeout() throws IOException {
        try {

            // Set socket timeout
            setSocketTimeout(timeout * 1000);

            while (remainingBytes != 0) {
                readData();
                handleSegmentTransmission();
            }

            System.out.printf("Total Segments %d%n", totalSegments);
            System.out.printf("Re-sent Segments %d%n", resentSegments);

        } finally {
            // Reset socket timeout
            setSocketTimeout(0);
        }
    }

    /**
     * Sets the socket timeout to the specified value.
     * 
     * @param timeoutMs - Timeout value in milliseconds
     */
    private void setSocketTimeout(int timeoutMs) {
        try {
            socket.setSoTimeout(timeoutMs);
        } catch (SocketException e) {
            System.err.printf("ERROR: Failed to set socket timeout: %s%n", e.getMessage());
            closeSocket();
            System.exit(1);
        }
    }

    /**
     * Transmits the current segment and handles retries.
     * 
     * @throws IOException
     */
    private void handleSegmentTransmission() throws IOException {
        // Reset retry counter for new segment
        currRetry = 0;

        do {
            sendDataWithError();

            currRetry++;
        } while (!receiveAck(dataSeg.getSq()));
    }

    /* PHASE 4 */

    /*
     * transfer the given file using the resources provided by the protocol
     * structure using GoBackN.
     */
    void sendFileNormalGBN(int window) throws IOException {
        int expectedSegments = (int) Math.ceil((double) fileSize / maxPayload);
        int seqNumCount = window + 1;
        int baseSeqNum = 0;
        int nextSeqNum = 0;
        int acksReceived = 0;

        // Send initial window
        System.out.println("---------------Sending the segments in the initial window --------------------------");
        while (nextSeqNum < window && nextSeqNum < expectedSegments) {
            sendNextSegment(nextSeqNum, seqNumCount);
            nextSeqNum++;
        }

        // Process remaining segments
        System.out.println("-----------------------------------------------------------");
        System.out.println("SENDER: Waiting for an ack and slide the window if the ack number is correct");
        System.out.println("-----------------------------------------------------------");

        while (acksReceived < expectedSegments) {
            printOutstandingAcks(seqNumCount, baseSeqNum, nextSeqNum);

            if (receiveAck(baseSeqNum % seqNumCount)) {
                acksReceived++;
                baseSeqNum++;

                if (nextSeqNum < expectedSegments) {
                    System.out.println("SENDER: slide the window and send the next segment");
                    sendNextSegment(nextSeqNum, seqNumCount);
                    nextSeqNum++;
                    System.out.println("----------------------------------------");
                }
            }
        }

        System.out.println("total segments " + totalSegments);
    }

    /**
     * Sends the next segment in the sequence.
     * 
     * @param seqNum       - Sequence number of the segment
     * @param totalSeqNums - Total number of sequence numbers
     */
    private void sendNextSegment(int seqNum, int totalSeqNums) throws IOException {
        readData();
        dataSeg.setSq(seqNum % totalSeqNums);
        sendData();
    }

    /**
     * Prints the outstanding ACKs for the current window.
     * 
     * @param seqNumCount - Total number of sequence numbers
     * @param baseSeqNum  - Base sequence number of the window
     * @param nextSeqNum  - Next sequence number to be sent
     */
    private void printOutstandingAcks(int seqNumCount, int baseSeqNum, int nextSeqNum) {
        StringBuilder sb = new StringBuilder("SENDER: current outstanding Acks [ ");
        for (int i = baseSeqNum; i < nextSeqNum; i++) {
            sb.append(i % seqNumCount).append(" ");
        }
        sb.append("]");
        System.out.println(sb.toString());
    }

    /* Helper methods */

    /**
     * Creates a DatagramPacket from an object.
     * 
     * @param obj - Object to serialize
     * @return DatagramPacket containing serialized object
     * @throws IOException
     */
    private DatagramPacket createPacket(Object obj) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(obj);
            byte[] data = byteArrayOutputStream.toByteArray();

            return new DatagramPacket(data, data.length, ipAddress, portNumber);
        }
    }

    /**
     * Closes the socket if it is open.
     */
    private void closeSocket() {
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * Prints the information of the current segment being sent.
     */
    private void printSegmentInfo() {
        System.out.printf("SENDER: Sending segment: sq: %d, size: %d, checksum: %d, content: (%s)%n",
                dataSeg.getSq(), dataSeg.getSize(), dataSeg.getChecksum(), dataSeg.getPayLoad());
    }

    /*************************************************************************************************************************************
     **************************************************************************************************************************************
     **************************************************************************************************************************************
     * These methods are implemented for you .. Do NOT Change them
     **************************************************************************************************************************************
     **************************************************************************************************************************************
     **************************************************************************************************************************************/
    /*
     * This method initialises ALL the 19 attributes needed to allow the Protocol
     * methods to work properly
     */
    public void initProtocol(String hostName, String portNumber, String fileName, String outputFileName,
            String payloadSize, String mode) throws UnknownHostException, SocketException {
        this.portNumber = Integer.parseInt(portNumber);
        this.ipAddress = InetAddress.getByName(hostName);
        this.socket = new DatagramSocket();
        this.inputFile = checkFile(fileName);
        this.inputFileName = fileName;
        this.outputFileName = outputFileName;
        this.fileSize = this.inputFile.length();

        this.remainingBytes = this.fileSize;
        this.maxPayload = Integer.parseInt(payloadSize);
        this.mode = mode;
        this.dataSeg = new Segment();
        this.ackSeg = new Segment();

        this.timeout = DEFAULT_TIMEOUT;
        this.maxRetries = DEFAULT_RETRIES;

        this.sentBytes = 0;
        this.lossProb = 0;
        this.totalSegments = 0;
        this.resentSegments = 0;
        this.currRetry = 0;
    }

    /*
     * transfer the given file using the resources provided by the protocol
     * attributes, according to the normal file transfer without timeout
     * or retransmission (for part 2).
     */
    public void sendFileNormal() throws IOException {
        while (this.remainingBytes != 0) {
            readData();
            sendData();
            if (!receiveAck(this.dataSeg.getSq()))
                System.exit(0);
        }
        System.out.println("Total Segments " + this.totalSegments);
    }

    /*
     * calculate the segment checksum by adding the payload
     * Parameters:
     * payload - the payload string
     * corrupted - a boolean to indicate whether the checksum should be corrupted
     * to simulate a network error
     *
     * Return:
     * An integer value calculated from the payload of a segment
     */
    public static int checksum(String payload, Boolean corrupted) {
        if (!corrupted) {
            int i;

            int sum = 0;
            for (i = 0; i < payload.length(); i++)
                sum += (int) payload.charAt(i);
            return sum;
        }
        return 0;
    }

    /* used by Client.java to set the loss probability (for part 3) */
    public void setLossProb(float loss) {
        this.lossProb = loss;
    }

    /*
     * returns true with the given probability
     * 
     * The result can be passed to the checksum function to "corrupt" a
     * checksum with the given probability to simulate network errors in
     * file transfer.
     *
     */
    private static Boolean isCorrupted(float prob) {

        double randomValue = Math.random(); // 0.0 to 99.9
        return randomValue <= prob;
    }

    /* check if the input file does exist before sending it */
    private static File checkFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("SENDER: File does not exists");
            System.out.println("SENDER: Exit ..");
            System.exit(0);
        }
        return file;
    }
}
