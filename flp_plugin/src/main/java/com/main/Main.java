package com.main;

/*import akka.actor.ActorRef;
import akka.actor.ActorSystem;*/
import akka.actor.ActorContext;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

//TODO: iv length 12
//TODO: increment not on least 4 significant bytes
//TODO: TM input


public class Main {

    private static final int OTAR_IV_LENGTH = 12;

    public static final int portReceiveProcetedTM = 8080;
    //public static final int portReceiveUnprotectedTC = 8081;
    public static final int portReceiveUserCommand = 8082;
    public static final int portReceiveEPReply = 8083;

    public static final int portSendProtectedTC = 8084;
    //public static final int portSendUnprotectedTM = 8085;
    public static final int portSendEPCommand = 8086;
    public static final int portSendEPReply = 8087;
    public static final int portSendError = 8088;

    public static final String hostName = "localhost";

    public static final int chunckLengthInByte = 10;
    public static final int stringLengtthEncodingInByte = 2;

    private static final int TC_IV_LENGTH = 12;
    private static final int SPI_LENGTH = 2;
    private static final int ARC_LENGTH = 4;
    private static final int MAC_LENGTH = 16;

    public static void main(String[] args) {

        Socket tcIn = null;
        Socket pduIn = null;

        InputStream tcInstream = null;
        InputStream pduInstream = null;
        OutputStream tmOutStream = null;
        OutputStream pduOutStream = null;

        Socket tmOut = null;
        Socket pduOut = null;
        File tcOut = new File("tc-output");
        File fsrOut = new File("fsr-output");
        final ActorSystem<GuardianActor.Command> mainActor = ActorSystem.create(GuardianActor.create(), "guardian-actor");

        while(tcIn == null || pduIn == null || tmOut == null || pduOut == null) {
            try {
                tcIn = new Socket(hostName, portSendProtectedTC);
                pduIn = new Socket(hostName, portSendEPCommand);
                tmOut = new Socket(hostName, portReceiveProcetedTM);
                pduOut = new Socket(hostName, portReceiveEPReply);
            }
            catch (UnknownHostException e) {
                System.out.println("Unknown Host...");
            }
            catch (IOException e) {
                System.out.println("Waiting for connection...");
                System.out.println("Trying again in 10 seconds...");
                try {
                    Thread.sleep(10000);
                }
                catch (Exception i) {
                    i.printStackTrace();
                }
            }
        }
        try {

            tcInstream = tcIn.getInputStream();
            pduInstream = pduIn.getInputStream();
            tmOutStream = tmOut.getOutputStream();
            pduOutStream = pduOut.getOutputStream();
            int active = 50;
            Map<Byte, byte[]> masterKeys = masterKeys();
            Map<Byte, byte[]> sessionKeys = sessionKeys();
            Map<Integer, Short> vcToSA = getVC();
            Map<Short, Byte> criticalSA = criticalSAs();
            List<Short> standardSA = standardSAs();
            mainActor.tell(new GuardianActor.Start(active, masterKeys, sessionKeys, vcToSA, criticalSA, standardSA, tmOutStream, tcOut, fsrOut, pduOutStream));
            try {
                Thread.sleep(5000);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            while(true) {
                PDUThread pduThread = new PDUThread(pduInstream, mainActor);
                TCThread tcThread = new TCThread(tcInstream, mainActor);
                pduThread.start();
                tcThread.start();
            }
        }
        catch (IOException e) {
            System.out.println("Could not get input/output streams...");
        }

    }

    private static byte[] scanTC(InputStream in) {
        try {
            int data = in.read();
            while (data != -1) {
                //maybe that's broken
                byte[] frameHeader = new byte[5];
                for (int i = 0; i < 5; i++) {
                    frameHeader[i] = (byte) data;
                    data = in.read();
                }
                byte firstLen = (byte) (frameHeader[2] & (byte) 0b00000011);
                int first = firstLen << 8;
                int length = first + (int) (frameHeader[3]) + 1;
                int lengthSec = length + SPI_LENGTH + ARC_LENGTH + TC_IV_LENGTH + MAC_LENGTH;
                byte[] tc = new byte[lengthSec];
                System.arraycopy(frameHeader, 0, tc, 0, frameHeader.length);
                //assumes TC frame length includes header and trailer
                for (int i = 5; i < lengthSec; i++) {
                    tc[i] = (byte) data;
                    data = in.read();
                }
                System.out.println("Received TC");
                System.out.println("Length: " + lengthSec);
                System.out.println(Arrays.toString(tc));
                //mainActor.tell(new GuardianActor.TC(tc));
                return tc;
            }
        }
        catch (IOException e) {
            System.out.println("IOException..");
            System.out.println("Could not read TC");
        }
        return null;
    }


    private static short getShort(byte byte1, byte byte2) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(byte1);
        bb.put(byte2);
        //System.out.println(bb.getShort(0));
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

    private void testPing(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[3];
        pdu[0] = (byte) 0b00110001;
        short length = 0;
        pdu[1] = (byte) (length & 0xff);
        pdu[2] = (byte) ((length >> 8) & 0xff);
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testOtar(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[4 + OTAR_IV_LENGTH + 1 + 32 + 16];
        pdu[0] = (byte) 0b00000001;
        short length = (short) (pdu.length - 3);
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        byte keyID = 40;
        byte[] key = new byte[32];
        for(int i = 0; i < key.length; i++) {
            key[i] = 0;
        }
        byte[] plaintext = new byte[33];
        plaintext[0] = keyID;
        System.arraycopy(key, 0, plaintext, 1, key.length);
        byte[] iv = new byte[OTAR_IV_LENGTH];
        int j = 4;
        for(int i = 0; i < iv.length; i++) {
            iv[i] = 0;
            pdu[j] = 0;
            j++;
        }
        byte master = 0;
        pdu[3] = master;
        byte[] masterKey = new byte[32];
        try {
            Scanner scanner = new Scanner(new File("keys"));
            int i = 0;
            while(scanner.hasNextByte() && i < 32) {
                masterKey[i] = scanner.nextByte();
                i++;
            }
            System.out.println("check master key");
            System.out.println(Arrays.toString(masterKey));
            try {
                byte[] enc = encrypt(plaintext, masterKey, iv);
                System.arraycopy(enc, 0, pdu, j, enc.length);
                System.out.println("check pdu");
                System.out.println(Arrays.toString(pdu));
                mainActor.tell(new GuardianActor.PDU(pdu));

                try {
                    System.out.println("ciphertex:");
                    System.out.println(Arrays.toString(enc));
                    byte[] dec = decrypt(enc, masterKey, iv);
                    System.out.println(Arrays.toString(dec));
                }
                catch (Exception e) {
                    System.out.println("error in decryption");
                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static byte[] decrypt(byte[] cipherText, byte[] masterKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(masterKey, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        return cipher.doFinal(cipherText);
    }

    private static byte[] encrypt(byte[] plain, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        return cipher.doFinal(plain);
    }

    //TODO: correct all my bitshifts (the least significant byte of short is retrieved first not the other way around)

    private static void testKeyActivation(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00000010;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        /*System.out.println("byte one of length");
        System.out.println(pdu[1]);*/
        pdu[1] = (byte) ((length >> 8) & 0xff);
        /*System.out.println("byte two of length");
        System.out.println(pdu[2]);*/
        pdu[3] = (byte) 0b00010100;
        pdu[4] = (byte) 0b00010101;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testKeyDeactivation(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[4];
        pdu[0] = (byte) 0b00000011;
        short length = 1;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] =  (byte) 0b0010100;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testKeyDestruction(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[4];
        pdu[0] = (byte) 0b00000110;
        short length = 1;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = (byte) 0b0010100;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testDumpLog(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[3];
        short length = 0;
        pdu[0] = (byte) 0b00110011;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testEraseLog(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[3];
        short length = 0;
        pdu[0] = (byte) 0b00110100;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testInventory(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00000111;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 40;
        pdu[4] = 40;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testStart(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[9];
        pdu[0] = (byte) 0b00011011;
        short length = 6;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        for(int i = 3; i < pdu.length; i++) {
            pdu[i] = 0;
        }
        pdu[4] = 5;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testStop(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00011110;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 0;
        pdu[4] = 5;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testStatusRequest(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00011111;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 0;
        pdu[4] = 5;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testRekey(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[1+2+2+1+4+12];
        pdu[0] = (byte) 0b00010110;
        short length = 19;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        for(int i = 3; i < pdu.length; i++) {
            pdu[i] = 0;
        }
        pdu[4] = 5;
        pdu[5] = 20;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testExpire(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00011001;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 0;
        pdu[4] = 5;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testSetARSN(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[9];
        pdu[0] = (byte) 0b00011010;
        short length = 6;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 0;
        pdu[4] = 0;
        for(int i = 5; i < pdu.length; i++) {
            pdu[i] = 1;
        }
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testReadARSN(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00010000;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 0;
        pdu[4] = 0;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testSetARSNWindow(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[9];
        pdu[0] = (byte) 0b00010101;
        short length = 6;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        for(int i = 3; i < pdu.length; i++) {
            pdu[i] = 0;
        }
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testReadARSNWindow(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b01010000;
        short length = 2;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 0;
        pdu[4] = 0;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testVerification(ActorSystem<GuardianActor.Command> mainActor) {
        byte[] pdu = new byte[6];
        pdu[0] = (byte) 0b00000100;
        short length = 3;
        pdu[2] = (byte) (length & 0xff);
        pdu[1] = (byte) ((length >> 8) & 0xff);
        pdu[3] = 20;
        pdu[4] = 0;
        pdu[5] = 0;
        mainActor.tell(new GuardianActor.PDU(pdu));
    }

    private static void testTM(ActorSystem<GuardianActor.Command> mainActor) {
        /*try {
            System.out.println("test scanning tm");
            Scanner scanner = new Scanner(new File("TM_log.txt"));
            int i = 0;
            byte[] tm = new byte[1115];
            while(scanner.hasNextLine() && i < 1115) {
                String s = scanner.nextLine();
                String[] tokens = s.split("\\s");
                String[] tokCut = new String[tokens.length - 2];
                System.arraycopy(tokens, 1, tokCut, 0, tokens.length - 2);
                byte[] bytes = DataTypeConverter.parse
                //System.out.println(scanner.nextByte());
                i++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }*/
        byte[] tm = new byte[1115];
        tm[0] = 0x00;
        tm[1] = 0x00;
        tm[2] = 0x04;
        tm[3] = 0x6f;
        tm[4] = 0x02;
        tm[5] = 0x5d;
        tm[6] = 0x00;
        tm[7] = 0x00;
        tm[8] = 0x00;
        tm[9] = 0x00;
        tm[10] = 0x07;
        tm[11] = 0x72;
        tm[12] = 0x08;
        tm[13] = (byte) 0xC0;
        tm[14] = (byte) 0xfe;
        tm[15] = 0x14;
        tm[16] = 0x5a;
        tm[17] = 0x00;
        tm[18] = 0x00;
        tm[19] = 0x00;
        tm[20] = 0x25;
        tm[21] = (byte) 0xd0;
        tm[22] = 0x00;
        tm[23] = 0x00;
        tm[24] = 0x18;
        tm[25] = 0x00;
        tm[26] = 0x07;
        tm[27] = (byte) 0xff;
        tm[28] = (byte) 0xc0;
        tm[29] = 0x00;
        tm[30] = 0x03;
        tm[31] = (byte) 0xf7;
        for(int i = 32; i < tm.length; i++) {
            tm[i] = 0x00;
        }
        mainActor.tell(new GuardianActor.TM(tm));
        for(int i = 0; i < 32; i++) {
            System.out.println(tm[i]);
        }
    }
}
