package com.main;

import akka.actor.typed.ActorSystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class TCThread extends Thread {

    private static final int TC_IV_LENGTH = 12;
    private static final int SPI_LENGTH = 2;
    private static final int ARC_LENGTH = 4;
    private static final int MAC_LENGTH = 16;

    private final InputStream stream;
    private final ActorSystem<GuardianActor.Command> mainActor;

    public TCThread(InputStream stream, ActorSystem<GuardianActor.Command> mainActor) {
        this.stream = stream;
        this.mainActor = mainActor;
    }

    public void run() {
        while (true) {
            try {
                int tcData = this.stream.read();
                if (tcData != -1) {
                    byte first = (byte) tcData;
                    //TODO: header length
                    byte[] rest = this.stream.readNBytes(5);
                    byte[] frameHeader = new byte[6];
                    frameHeader[0] = first;
                    System.arraycopy(rest, 0, frameHeader, 1, 5);
                    byte firstLen = (byte) (frameHeader[2] & (byte) 0b00000011);
                    int firstLength = firstLen << 8;
                    int length = firstLength + ((int) (frameHeader[3])) + 1;
                    //length is provided in bytes
                /*int lengthByte = length / 8;
                if((length % 8) != 0) {
                    lengthByte++;
                }*/
                    //System.out.println("TC frame length:");
                    int lengthSec = length + SPI_LENGTH + ARC_LENGTH + TC_IV_LENGTH + MAC_LENGTH;
                    byte[] tc = new byte[lengthSec];
                    System.arraycopy(frameHeader, 0, tc, 0, frameHeader.length);
                    byte[] frame = this.stream.readNBytes(lengthSec - frameHeader.length);
                    System.arraycopy(frame, 0, tc, frameHeader.length, frame.length);
                    System.out.println("Received TC Frame with length: " + frame.length);
                    //System.out.println("Length: " + lengthSec);
                    System.out.println("Encrypted Frame: ");
                    System.out.println(toHex(tc));
                    /*System.out.println("Received TC");
                    System.out.println("Length: " + lengthSec);
                    System.out.println(Arrays.toString(tc));*/
                    mainActor.tell(new GuardianActor.TC(tc));
                    //assumes TC frame length includes header and trailer
                    Thread.sleep(5000);
                }
            } catch (IOException e) {
                System.out.println("Could not read from TC input stream...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
