package com.main;

/*import akka.actor.ActorRef;
import akka.actor.ActorSystem;*/
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class Main {
    public static void main(String[] args){
        final ActorSystem<GuardianActor.Command> mainActor = ActorSystem.create(GuardianActor.create(), "guardian-actor");
        int active = 50;
        Map<Byte, byte[]> masterKeys = masterKeys();
        Map<Byte, byte[]> sessionKeys = sessionKeys();
        Map<Integer, Short> vcToSA = getVC();
        Map<Short, Byte> criticalSA = criticalSAs();
        List<Short> standardSA = standardSAs();
        mainActor.tell(new GuardianActor.Start(active, masterKeys, sessionKeys, vcToSA, criticalSA, standardSA));
        try {
            //TODO: maybe find a different solution than timeout until initialization is finished
            Thread.sleep(2000);
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
        }
        catch (InterruptedException e) {
            e.printStackTrace();
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
