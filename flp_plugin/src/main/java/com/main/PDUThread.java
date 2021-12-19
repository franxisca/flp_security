package com.main;

import akka.actor.typed.ActorSystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class PDUThread extends Thread{
    private final InputStream stream;
    private final ActorSystem<GuardianActor.Command> mainActor;

    public PDUThread(InputStream stream, ActorSystem<GuardianActor.Command> mainActor) {
        this.stream = stream;
        this.mainActor = mainActor;
    }

    public void run() {
        while (true) {
            try {
                int pduData = this.stream.read();
                if (pduData != -1) {
                    byte tag = (byte) pduData;
                    byte[] length = this.stream.readNBytes(2);
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    bb.put(length[0]);
                    bb.put(length[1]);
                    //length is provided in bits
                    short len = bb.getShort(0);
                    short lengthInByte = (short) (len / 8);
                    if ((len % 8) != 0) {
                        lengthInByte++;
                    }
                    byte[] value = this.stream.readNBytes(lengthInByte);
                    byte[] pdu = new byte[lengthInByte + 3];
                    pdu[0] = tag;
                    pdu[1] = (byte) ((lengthInByte >> 8) & 0xff);
                    pdu[2] = (byte) (lengthInByte & 0xff);
                    System.arraycopy(value, 0, pdu, 3, value.length);
                    System.out.println("Received PDU with length: " + (len + 3));
                    //System.out.println("Length: " + (len + 3));
                    //System.out.println(Arrays.toString(pdu));
                    System.out.println("PDU: " + toHex(pdu));
                    mainActor.tell(new GuardianActor.PDU(pdu));
                }
                Thread.sleep(5000);

            } catch (IOException e) {
                System.out.println("Could not read from PDU input stream...");
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
