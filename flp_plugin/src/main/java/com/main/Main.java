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

public class Main {

    private static final int OTAR_IV_LENGTH = 12;

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
            /*tmIn = new Socket(hostName, portSendUnprotectedTM);
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
            OutputStream pduOutStream = pduOut.getOutputStream();*/

            final ActorSystem<GuardianActor.Command> mainActor = ActorSystem.create(GuardianActor.create(), "guardian-actor");
            int active = 50;
            Map<Byte, byte[]> masterKeys = masterKeys();
            Map<Byte, byte[]> sessionKeys = sessionKeys();
            Map<Integer, Short> vcToSA = getVC();
            Map<Short, Byte> criticalSA = criticalSAs();
            List<Short> standardSA = standardSAs();
            //TODO
            mainActor.tell(new GuardianActor.Start(active, masterKeys, sessionKeys, vcToSA, criticalSA, standardSA, null, null, null));
            try {
                //TODO: maybe find a different solution than timeout until initialization is finished
                Thread.sleep(5000);
                //TODO
                /*Scanner tmScanner = new Scanner(new InputStreamReader(tmInstream));
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
                }*/
                //from here: testing of ping
                /*byte[] pdu = new byte[3];
                pdu[0] = (byte) 0b00110001;
                short length = 0;
                pdu[1] = (byte) (length & 0xff);
                pdu[2] = (byte) ((length >> 8) & 0xff);
                mainActor.tell(new GuardianActor.PDU(pdu));
                testKeyDestruction(mainActor);
                testKeyActivation(mainActor);
                testKeyDeactivation(mainActor);
                //Thread.sleep(3000);
                //testKeyDestruction(mainActor);
                Thread.sleep(5000);
                testDumpLog(mainActor);
                Thread.sleep(5000);
                testInventory(mainActor);*/
                /*testKeyActivation(mainActor);
                testStart(mainActor);
                Thread.sleep(3000);
                testStatusRequest(mainActor);
                Thread.sleep(3000);
                testRekey(mainActor);
                Thread.sleep(6000);
                testDumpLog(mainActor);
                Thread.sleep(3000);
                testStatusRequest(mainActor);
                Thread.sleep(3000);
                testStart(mainActor);
                Thread.sleep(3000);
                testStatusRequest(mainActor);
                Thread.sleep(3000);
                testStop(mainActor);
                Thread.sleep(3000);
                testStatusRequest(mainActor);
                Thread.sleep(3000);
                testExpire(mainActor);
                Thread.sleep(3000);
                testStatusRequest(mainActor);*/
                /*testSetARSNWindow(mainActor);
                Thread.sleep(3000);
                testReadARSNWindow(mainActor);
                Thread.sleep(3000);
                testDumpLog(mainActor);
                Thread.sleep(3000);
                testEraseLog(mainActor);*/
                //testVerification(mainActor);
                testOtar(mainActor);
                Thread.sleep(3000);
                testInventory(mainActor);
                Thread.sleep(3000);
                testDumpLog(mainActor);
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
        catch (Exception e){e.printStackTrace();}
        /*catch (UnknownHostException e) {
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
        }*/
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

   //TODO: assumes iv length x (see constant), tag length 128
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
        /*byte[] cipherText = new byte[keys.length + mac.length];
        System.arraycopy(keys, 0, cipherText, 0, keys.length);
        System.arraycopy(mac, 0, cipherText, keys.length, mac.length);*/
        //TODO: check tag length
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
}
