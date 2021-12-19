package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;


import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCProcessor extends AbstractBehavior<TCProcessor.Command> {

    //max 256 bytes, variable length, in our case too?
    //5 bytes header (+1?) , up to 249 bytes data, 2 bytes CRC
    //security header (18 bytes) between primary header and data, 16 bytes security trailer after data (MAC)


    public interface Command {}

    public static final class EncryptedTC implements Command {
        final byte[] primHeader;
        final byte[] secHeader;
        final byte[] data;
        final int dataLength;
        final byte[] secTrailer;
        final byte[] crc;
        final ActorRef<Module.Command> parent;
        //final ActorRef sam;

        public EncryptedTC(byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<Module.Command> parent) {
            this.primHeader = primHeader;
            this.secHeader = secHeader;
            this.data = data;
            this.dataLength = dataLength;
            this.secTrailer = secTrailer;
            this.crc = crc;
            this.parent = parent;
            //this.sam = sam;
        }

    }

    public static final class TC implements Command {

        final int vcId;
        final byte[] primHeader;
        final byte[] secHeader;
        final byte[] data;
        final int dataLength;
        final byte[] secTrailer;
        final byte[] crc;
        final byte[] arc;
        final byte[] authMask;
        final ActorRef<Module.Command> parent;
        final byte keyId;
        final byte[] key;
        final short sPi;
        final ActorRef<SA.Command> saActor;

        public TC(int vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, byte[] arc, byte[] authMask, ActorRef<Module.Command> parent, byte keyId, byte[] key, short sPi, ActorRef<SA.Command> saActor) {
            this.vcId = vcId;
            this.primHeader = primHeader;
            this.secHeader = secHeader;
            this.data = data;
            this.dataLength = dataLength;
            this.secTrailer = secTrailer;
            this.crc = crc;
            this.arc = arc;
            this.authMask = authMask;
            this.parent = parent;
            this.keyId = keyId;
            this.key = key;
            this.sPi = sPi;
            this.saActor = saActor;
        }
    }

    public static final class BadSA implements Command {
        final short sPi;
        final byte[] secHeader;
        final ActorRef<Module.Command> parent;
        final int code;

        public BadSA(short sPi, byte[] secHeader, ActorRef<Module.Command> parent, int code) {
            this.sPi = sPi;
            this.secHeader = secHeader;
            this.parent = parent;
            this.code = code;
        }

    }

    public static final class Reset implements Command {

    }


    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new TCProcessor(context));
    }
    //final ActorRef<FSRHandler.Command> fsrHandler;
    boolean alarmFlag;
    short lastSpi;
    byte lastArc;

    private TCProcessor(ActorContext<Command> context) {
        super(context);
        //this.fsrHandler = getContext().spawn(FSRHandler.create(), "fsr");
        this.alarmFlag = false;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(EncryptedTC.class, this::onEncTC)
                .onMessage(TC.class, this::onTC)
                .onMessage(BadSA.class, this::onBadSA)
                .onMessage(Reset.class, this::onReset)
                .build();
    }

    private Behavior<Command> onEncTC(EncryptedTC tc) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(tc.secHeader[0]);
        bb.put(tc.secHeader[1]);
        short sPi = bb.getShort(0);
        System.out.println("Received SPI");
        System.out.println(sPi);
        byte vc = tc.primHeader[2];
        //BitSet bits = BitSet.valueOf(vc);
        byte vcOnly = (byte) (vc & 0b11111100);
        byte vcShift = (byte) (vcOnly >> 2);
        int vcId = (int) vcShift;
        /*boolean[] vcId = new boolean[8];
        vcId[0] = false;
        vcId[1] = false;
        for(int i = 2; i < 8; i++){
            byte bit = (byte) ((vc >> i) & 1);
            if(bit == 1) {
                vcId[9-i] = true;
            }
            else {
                vcId[9-i] = false;
            }
        }*/
        System.out.println("Received MAC:");
        System.out.println(Arrays.toString(tc.secTrailer));
        tc.parent.tell(new Module.GetTCInfo(sPi, vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, getContext().getSelf(), tc.parent));
        return this;
    }

    private Behavior<Command> onTC(TC tc) {
        byte[] plaintext;
        byte[] iv = new byte[16];
        System.arraycopy(tc.secHeader, 6, iv, 0, 12);
        for(int i = 12; i < iv.length; i++) {
            iv[i] = 0;
        }
        //byte[] ivFin = new byte[16]
        try {
            byte[] authOnly = new byte[tc.primHeader.length + tc.secHeader.length];
            System.arraycopy(tc.primHeader, 0, authOnly, 0, tc.primHeader.length);
            System.arraycopy(tc.secHeader, 0, authOnly, tc.primHeader.length, tc.secHeader.length);
            plaintext = decrypt(tc.key, iv, tc.data, tc.secTrailer, tc.authMask, authOnly);
            byte[] c = new byte[4];
            System.arraycopy(tc.secHeader, 2, c, 0, 4);
            ByteBuffer bb = ByteBuffer.allocate(8);
            bb.put(c[0]);
            bb.put(c[1]);
            bb.put(c[2]);
            bb.put(c[3]);
            bb.put(tc.arc[0]);
            bb.put(tc.arc[1]);
            bb.put(tc.arc[2]);
            bb.put(tc.arc[2]);
            int tcArc = bb.getInt(0);
            int saArc = bb.getInt(4);
            if(tcArc <= saArc) {
                //this.fsrHandler.tell(new FSRHandler.BadSeq());
                this.alarmFlag = true;
                tc.parent.tell(new Module.TCOut(false, (byte) 0b00000011, null, null));
                tc.parent.tell(new Module.FSR(this.alarmFlag, true, false, false, tc.sPi, c[3]));
            }
            else {
                byte[] secReturn = new byte[tc.primHeader.length + plaintext.length];
                System.arraycopy(tc.primHeader, 0, secReturn, 0, tc.primHeader.length);
                System.arraycopy(plaintext, 0, secReturn, tc.primHeader.length, plaintext.length);
                tc.parent.tell(new Module.TCOut(true, (byte) 0b00000000, secReturn, tc.crc));
                tc.parent.tell(new Module.FSR(this.alarmFlag, false, false, false, tc.sPi, c[3]));
                tc.saActor.tell(new SA.ARCUpdate(tc.arc));
            }

        }
        catch (Exception e) {
            byte[] c = new byte[4];
            System.arraycopy(tc.secHeader, 2, c, 0, 4);
            byte lastSN = c[3];
            this.alarmFlag = true;
            tc.parent.tell(new Module.TCOut(false, (byte) 0b00000010, null, null));
            tc.parent.tell(new Module.FSR(this.alarmFlag, false, true, false, tc.sPi, lastSN));
           // this.fsrHandler.tell(new FSRHandler.BadMAC());
        }
        return this;
    }

    //authentication bitmask is not handled but is all ones anyway
    private static byte[] decrypt(byte[] key, byte[] iv, byte[] data, byte[] tag, byte[] authMask, byte[] authOnly) throws Exception {
        /*System.out.println("Decryption Key: " + Arrays.toString(key));
        System.out.println("Decryption IV: " + Arrays.toString(iv));
        System.out.println("Ciphertext: " + Arrays.toString(data));
        System.out.println("MAC: " + Arrays.toString(tag));
        System.out.println("Authenticated Data: " + Arrays.toString(authOnly));*/
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        byte[] cipherText = new byte[data.length + tag.length];
        System.arraycopy(data, 0, cipherText, 0, data.length);
        //System.arraycopy(crc, 0, cipherText, data.length, crc.length);
        System.arraycopy(tag, 0, cipherText, data.length, tag.length);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        cipher.updateAAD(authOnly);
        return cipher.doFinal(cipherText);
    }

    private Behavior<Command> onBadSA(BadSA b) {
        //this.fsrHandler.tell(new FSRHandler.BadSA());
        this.alarmFlag = true;
        this.lastSpi = b.sPi;
        byte[] arc = new byte[4];
        System.arraycopy(b.secHeader, 2, arc, 0, 4);
        this.lastArc = arc[3];
        b.parent.tell(new Module.TCOut(false, (byte) 0b00000001, null, null));
        b.parent.tell(new Module.FSR(this.alarmFlag, false, false, true, b.sPi, this.lastArc));
        System.out.println("Decryption failed, bad SA failure.");
        System.out.println("Bad SA code: " + b.code);
        //System.out.println(b.code);
        return this;
    }

    private Behavior<Command> onReset(Reset r) {
        this.alarmFlag = false;
        return this;
    }
}
