package com.nio.SonarGnss;

import java.io.ByteArrayOutputStream;

/**
 * This is the UbxParser class and parses binary UBX protocol packets received from GNSS/RTK devices.
 */
public class UbxParser {

    // This is the rs field and creates an instance of RtkString helper for formatting data
    RtkString rs = new RtkString();

    // This is the OnTimeReceivedListener interface and defines callback methods when valid navigation data arrives
    public interface OnTimeReceivedListener {
        // This is the onTimeReceived callback method and delivers parsed navigation values to listeners
        void onTimeReceived(String date,
                            String time,
                            String latitude,
                            String latitudeDi,
                            String longitude,
                            String longitudeDi,
                            String speed,
                            String cog,
                            String altitude);
    }

    // This is the SYNC_CHAR_1 constant and represents the first UBX sync byte (0xB5)
    private static final byte SYNC_CHAR_1 = (byte) 0xB5;
    // This is the SYNC_CHAR_2 constant and represents the second UBX sync byte (0x62)
    private static final byte SYNC_CHAR_2 = (byte) 0x62;

    // This is the buffer field and accumulates incoming raw byte streams
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    // This is the listener field and holds reference to the time/position data callback listener
    private final OnTimeReceivedListener listener;

    // This is the UbxParser constructor and sets up the listener callback reference
    public UbxParser(OnTimeReceivedListener listener) {
        // This line assigns the listener taking insights from listener parameter
        this.listener = listener;
    }

    /**
     * Call this method whenever Bluetooth data is received.
     */
    // This is the addBytes function and processes raw incoming Bluetooth data chunks into UBX packets
    public synchronized void addBytes(byte[] data, int length) {
        // This line writes new incoming bytes into our accumulator buffer taking insights from data and length
        buffer.write(data, 0, length);

        // This line converts buffer contents into a byte array taking insights from buffer
        byte[] bytes = buffer.toByteArray();

        // This line initializes index pointer to traverse incoming bytes taking insights from initial zero position
        int index = 0;

        // This line loops through bytes to extract valid packets taking insights from bytes length
        while (index <= bytes.length - 8) {
            // This line checks for UBX sync characters taking insights from bytes at index
            if (bytes[index] != SYNC_CHAR_1 || bytes[index + 1] != SYNC_CHAR_2) {
                // This line advances pointer forward taking insights from mismatched sync bytes
                index++;
                continue;
            }

            // This line calculates payload length taking insights from bytes at index 4 and 5
            int payloadLength = (bytes[index + 4] & 0xFF) | ((bytes[index + 5] & 0xFF) << 8);

            // This line calculates total packet length taking insights from header, payload, and checksum
            int packetLength = 6 + payloadLength + 2;

            // This line waits for the complete packet to arrive taking insights from bytes length
            if (index + packetLength > bytes.length)
                break;

            // This line verifies packet checksum integrity taking insights from verifyChecksum function
            if (!verifyChecksum(bytes, index, packetLength)) {
                // This line skips forward taking insights from failed checksum validation
                index++;
                continue;
            }

            // This line extracts message class byte taking insights from byte at index 2
            int msgClass = bytes[index + 2] & 0xFF;
            // This line extracts message ID byte taking insights from byte at index 3
            int msgId = bytes[index + 3] & 0xFF;

            // This line checks if message is NAV-PVT position/time packet taking insights from class and ID
            if (msgClass == 0x01 && msgId == 0x07) {
                // This line calculates the payload start index taking insights from index plus 6 header bytes
                int payloadStart = index + 6;
                // This line parses navigation PVT data taking insights from bytes and payloadStart
                parseNavPVT(bytes, payloadStart);
            }

            // This line advances the index pointer by packet length taking insights from packetLength
            index += packetLength;
        }

        // This line creates a temporary stream to hold unprocessed bytes taking insights from remaining data
        ByteArrayOutputStream remaining = new ByteArrayOutputStream();

        // This line checks if unparsed bytes remain taking insights from index and bytes length
        if (index < bytes.length) {
            // This line saves remaining bytes taking insights from index and remaining length
            remaining.write(bytes, index, bytes.length - index);
        }

        // This line resets the main buffer taking insights from completed processing
        buffer.reset();

        try {
            // This line writes unparsed bytes back into main buffer taking insights from remaining stream
            remaining.writeTo(buffer);
        } catch (Exception ignored) {
            // This line safely catches any stream transfer exceptions taking insights from try block
        }
    }

    // This is the verifyChecksum function and validates packet data against UBX checksum bytes
    private boolean verifyChecksum(byte[] packet, int start, int packetLength) {
        // This line initializes first checksum accumulator taking insights from initial zero value
        int ckA = 0;
        // This line initializes second checksum accumulator taking insights from initial zero value
        int ckB = 0;

        // This line calculates checksum over payload taking insights from packet bytes
        for (int i = start + 2; i < start + packetLength - 2; i++) {
            // This line updates first checksum byte taking insights from packet byte at index i
            ckA = (ckA + (packet[i] & 0xFF)) & 0xFF;
            // This line updates second checksum byte taking insights from updated ckA
            ckB = (ckB + ckA) & 0xFF;
        }

        // This line returns true if calculated checksum matches packet checksum taking insights from ckA and ckB
        return ckA == (packet[start + packetLength - 2] & 0xFF)
                && ckB == (packet[start + packetLength - 1] & 0xFF);
    }

    // This is the parseNavPVT function and extracts position, velocity, and time values from NAV-PVT payload
    private void parseNavPVT(byte[] packet, int payloadStart) {
        // This line extracts the GNSS fix type taking insights from payload offset 20
        int fixType = packet[payloadStart + 20] & 0xFF;

        // This line extracts the year combining two bytes in little-endian format taking insights from offsets 4 and 5
        int year = (packet[payloadStart + 4] & 0xFF) | ((packet[payloadStart + 5] & 0xFF) << 8);
        // This line extracts the month taking insights from payload offset 6
        int month = packet[payloadStart + 6] & 0xFF;
        // This line extracts the day of the month taking insights from payload offset 7
        int day = packet[payloadStart + 7] & 0xFF;

        // This line extracts the hour taking insights from payload offset 8
        int hour = packet[payloadStart + 8] & 0xFF;
        // This line extracts the minute taking insights from payload offset 9
        int minute = packet[payloadStart + 9] & 0xFF;
        // This line extracts the second taking insights from payload offset 10
        int second = packet[payloadStart + 10] & 0xFF;

        // This line extracts the validity flags taking insights from payload offset 11
        int valid = packet[payloadStart + 11] & 0xFF;

        // This line extracts 4-byte raw latitude integer taking insights from payload offsets 28 to 31
        int latRaw = (packet[payloadStart + 28] & 0xFF) |
                        ((packet[payloadStart + 29] & 0xFF) << 8) |
                        ((packet[payloadStart + 30] & 0xFF) << 16) |
                        (packet[payloadStart + 31] << 24);

        // This line extracts 4-byte raw longitude integer taking insights from payload offsets 24 to 27
        int lonRaw = (packet[payloadStart + 24] & 0xFF) |
                        ((packet[payloadStart + 25] & 0xFF) << 8) |
                        ((packet[payloadStart + 26] & 0xFF) << 16) |
                        (packet[payloadStart + 27] << 24);

        // This line extracts 4-byte raw height integer taking insights from payload offsets 32 to 35
        int heightRaw = (packet[payloadStart + 32] & 0xFF) |
                        ((packet[payloadStart + 33] & 0xFF) << 8) |
                        ((packet[payloadStart + 34] & 0xFF) << 16) |
                        (packet[payloadStart + 35] << 24);

        // This line extracts 4-byte raw ground speed integer taking insights from payload offsets 60 to 63
        int gSpeedRaw = (packet[payloadStart + 60] & 0xFF) |
                        ((packet[payloadStart + 61] & 0xFF) << 8) |
                        ((packet[payloadStart + 62] & 0xFF) << 16) |
                        (packet[payloadStart + 63] << 24);

        // This line extracts 4-byte raw heading/course integer taking insights from payload offsets 64 to 67
        int headingRaw = (packet[payloadStart + 64] & 0xFF) |
                        ((packet[payloadStart + 65] & 0xFF) << 8) |
                        ((packet[payloadStart + 66] & 0xFF) << 16) |
                        (packet[payloadStart + 67] << 24);

        // This line converts raw latitude to decimal degrees dividing by 10^7 taking insights from latRaw
        double latitude = latRaw / 1e7;
        // This line converts raw longitude to decimal degrees dividing by 10^7 taking insights from lonRaw
        double longitude = lonRaw / 1e7;
        // This line converts height in millimeters to meters dividing by 1000 taking insights from heightRaw
        double altitude = heightRaw / 1000.0;
        // This line converts ground speed in mm/s to m/s dividing by 1000 taking insights from gSpeedRaw
        double speedMps = gSpeedRaw / 1000.0;
        // This line converts heading scaling by 10^5 to degrees taking insights from headingRaw
        double cog = headingRaw / 100000.0;

        // This line checks if date/time is valid and fix type is active taking insights from valid and fixType
        if ((valid & 0x03) == 0x03 && fixType != 0) {
            // This line triggers the callback listener taking insights from parsed and formatted navigation data
            listener.onTimeReceived(
                    rs.DATE(year, month, day),
                    rs.TIME(second, minute, hour),
                    rs.LAT(latitude),
                    rs.LATS(latitude),
                    rs.LON(longitude),
                    rs.LONS(longitude),
                    rs.SPEED(speedMps),
                    rs.COG(cog),
                    rs.ALTITUDE(altitude)
            );
        }
    }
}
