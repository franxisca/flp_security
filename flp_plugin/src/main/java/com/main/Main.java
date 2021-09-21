package com.main;

/*import akka.actor.ActorRef;
import akka.actor.ActorSystem;*/
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

public class Main {

    public static final int portReceiveProcetedTM = 8080;
    public static final int portReceiveUnprotectedTC = 8081;
    public static final int portReceiveUserCommand = 8082;
    public static final int portReceiveEPReply = 8083;

    public static final int portSendProtectedTC = 8084;
    public static final int portSendUnprotectedTM = 8085;
    public static final int portSendEPCommand = 8086;
    public static final int portSendEPReply = 8087;
    public static final int portSendError = 8088;

    public static final String hostName = "localhost";

    public static final int chunckLengthInByte = 10;
    public static final int stringLengtthEncodingInByte = 2;

    public static void main(String[] args){

        Socket tmIn = null;
        Socket tcIn = null;
        Socket pduIn = null;

        Socket tmOut = null;
        Socket tcOut = null;
        Socket pduOut = null;

        try {
            tmIn = new Socket(hostName, portSendUnprotectedTM);
            tcIn = new Socket(hostName, portSendProtectedTC);
            pduIn = new Socket(hostName, portSendEPCommand);
            InputStream tmInstream = tmIn.getInputStream();
            InputStream tcInstream = tcIn.getInputStream();
            InputStream pduInstream = pduIn.getInputStream();

            tmOut = new Socket(hostName, portReceiveProcetedTM);
            tcOut = new Socket(hostName, portReceiveUnprotectedTC);
            pduOut = new Socket(hostName, portReceiveEPReply);
            OutputStream tmOutStream = tmOut.getOutputStream();
            OutputStream tcOutStream = tcOut.getOutputStream();
            OutputStream pduOutStream = pduOut.getOutputStream();

            final ActorSystem<GuardianActor.Command> mainActor = ActorSystem.create(GuardianActor.create(), "guardian-actor");
            int active = 50;
            Map<Byte, byte[]> masterKeys = masterKeys();
            Map<Byte, byte[]> sessionKeys = sessionKeys();
            Map<Integer, Short> vcToSA = getVC();
            Map<Short, Byte> criticalSA = criticalSAs();
            List<Short> standardSA = standardSAs();
            mainActor.tell(new GuardianActor.Start(active, masterKeys, sessionKeys, vcToSA, criticalSA, standardSA, tmOutStream, tcOutStream, pduOutStream));
            try {
                //TODO: maybe find a different solution than timeout until initialization is finished
                Thread.sleep(5000);
                Scanner tmScanner = new Scanner(new InputStreamReader(tmInstream));
                Scanner tcScanner = new Scanner(new InputStreamReader(tcInstream));
                Scanner pduScanner = new Scanner(new InputStreamReader(pduInstream));
                while(tmScanner.hasNextByte()) {
                    byte[] tm = new byte[1115];
                    for(int i = 0; i < 1115; i++) {
                        tm[i] = tmScanner.nextByte();
                    }
                    mainActor.tell(new GuardianActor.TM(tm));
                }
                while(tcScanner.hasNextByte()) {
                    //TODO: scan tc according to length
                    byte[] frameHeader= new byte[5];
                    for(int i = 0; i < 5; i++) {
                        frameHeader[i] = tcScanner.nextByte();
                    }
                    BitSet header = BitSet.valueOf(frameHeader);
                    BitSet length = header.get(22, 32);
                    BitSet augLength = new BitSet(16);
                    for(int i = 0; i < 6; i++) {
                        augLength.clear(i);
                    }
                    int j = 0;
                    for(int i = 6; i < 16; i++){
                        if(length.get(j)) {
                            augLength.set(i);
                        }
                        j++;
                    }
                    byte[] lenArray = augLength.toByteArray();
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    bb.put(lenArray[0]);
                    bb.put(lenArray[1]);
                    short finLength = bb.getShort(0);
                    byte[] tc = new byte[finLength];
                    System.arraycopy(frameHeader, 0, tc, 0, frameHeader.length);
                    //TODO: assumes TC frame length includes header and trailer
                    for(int i = 5; i < finLength; i++) {
                        tc[i] = tcScanner.nextByte();
                    }
                    mainActor.tell(new GuardianActor.TC(tc));
                }
                //TODO: needs to run simultaneously to tm and tc?
                while(pduScanner.hasNextByte()) {
                    byte tag = pduScanner.nextByte();
                    ByteBuffer bb = ByteBuffer.allocate(2);
                    byte length1 = pduScanner.nextByte();
                    byte length2 = pduScanner.nextByte();
                    bb.put(length1);
                    bb.put(length2);
                    short length = bb.getShort(0);
                    byte[] pdu = new byte[length+3];
                    pdu[0] = tag;
                    pdu[1] = length1;
                    pdu[2] = length2;
                    for (int i = 3; i < length + 3; i++) {
                        pdu[i] = pduScanner.nextByte();
                    }
                    mainActor.tell(new GuardianActor.PDU(pdu));
                }
                //from here: testing of ping
                byte[] pdu = new byte[3];
                pdu[0] = (byte) 0b00110001;
                short length = 0;
                pdu[1] = (byte) (length & 0xff);
                pdu[2] = (byte) ((length >> 8) & 0xff);
                mainActor.tell(new GuardianActor.PDU(pdu));
                try {
                    System.out.println(">>> Press ENTER to exit <<<");
                    System.in.read();
                } catch (IOException ignored) {
                } finally {
                    mainActor.terminate();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        catch (UnknownHostException e) {
            System.out.println("Unknown Host...");
            e.printStackTrace();
        }
        catch (IOException e) {
            System.out.println("IOProblem...");
            e.printStackTrace();
        }
        finally {
            if(tmIn != null) {
                try {
                    tmIn.close();
                }
                catch (IOException e) {
                    System.out.println("Can't close socket for TM input");
                    e.printStackTrace();
                }
            }
            if(tcIn != null) {
                try {
                    tcIn.close();
                }
                catch (IOException e) {
                    System.out.println("Can't close socket for TC input");
                    e.printStackTrace();
                }
            }
            if(pduIn != null) {
                try {
                    pduIn.close();
                }
                catch (IOException e) {
                    System.out.println("Can't close socket for PDU input");
                    e.printStackTrace();
                }
            }
            if(tmOut != null) {
                try {
                    tmOut.close();
                }
                catch (IOException e) {
                    System.out.println("Can't close socket for TM output");
                    e.printStackTrace();
                }
            }
            if(tcOut != null) {
                try {
                    tcOut.close();
                }
                catch (IOException e) {
                    System.out.println("Can't close socket for TC output");
                    e.printStackTrace();
                }
            }
            if(pduOut != null) {
                try {
                    pduOut.close();
                }
                catch (IOException e) {
                    System.out.println("Can't close socket for PDU output");
                    e.printStackTrace();
                }
            }
        }
    }

    private static short getShort(byte byte1, byte byte2) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(byte1);
        bb.put(byte2);
        return bb.getShort(0);
    }

    private static Map<Integer, Short> getVC() {
        Map<Integer, Short> temp = new HashMap<>();
        temp.put(0, getShort((byte) 0b00000000, (byte) 0b00000110));
        temp.put(1, getShort((byte) 0b00000000, (byte) 0b00000110));
        temp.put(2, getShort((byte) 0b00000000, (byte) 0b00000110));
        temp.put(3, getShort((byte) 0b00000000, (byte) 0b00000110));
        temp.put(4, getShort((byte) 0b00000000, (byte) 0b00000010));
        return temp;
    }

    private static Map<Byte, byte[]> masterKeys() {
        Map<Byte, byte[]> temp = new HashMap<>();
        try {
            byte keyID = 0;
            Scanner scanner = new Scanner(new File("keys"));

            //read master keys
            while (scanner.hasNextByte() && keyID < 20) {
                byte[] currKey = new byte[32];
                for(int i = 0; i < 32; i++) {
                    currKey[i] = scanner.nextByte();
                }
                temp.put(keyID, currKey);
                keyID++;

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    private static Map<Byte, byte[]> sessionKeys() {
        Map<Byte, byte[]> temp = new HashMap<>();
        try {
            byte keyID = 20;
            Scanner scanner = new Scanner(new File("keys"));
            for(int i = 0; i < 20; i++) {
                if (scanner.hasNextLine()) scanner.nextLine();
            }
            while(scanner.hasNextByte() && keyID < 40) {
                byte[] currKey = new byte[32];
                for(int i = 0; i < 32; i++) {
                    currKey[i] = scanner.nextByte();
                }
                temp.put(keyID, currKey);
                keyID++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    private static Map<Short, Byte> criticalSAs() {
        Map<Short, Byte> temp = new HashMap<>();
        temp.put(getShort((byte) 0b00000000, (byte) 0b00000000), (byte) 0b00010100);
        temp.put(getShort((byte) 0b00000000, (byte) 0b00000001), (byte) 0b00010110);
        temp.put(getShort((byte) 0b00000000, (byte) 0b00000010), (byte) 0b00011000);
        temp.put(getShort((byte) 0b00000000, (byte) 0b00000011), (byte) 0b00011010);
        temp.put(getShort((byte) 0b00000000, (byte) 0b00000100), (byte) 0b00011100);
        temp.put(getShort((byte) 0b00000000, (byte) 0b00000110), (byte) 0b00011110);
        return temp;
    }

    private static List<Short> standardSAs() {
        List<Short> temp = new LinkedList<>();
        temp.add(getShort((byte) 0b00000000, (byte) 0b00000101));
        temp.add(getShort((byte) 0b00000000, (byte) 0b00000111));
        return temp;
    }
}
