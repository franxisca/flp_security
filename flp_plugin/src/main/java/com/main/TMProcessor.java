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
import java.nio.ByteBuffer;
import java.util.BitSet;

public class TMProcessor extends AbstractBehavior<TMProcessor.Command> {

    //unencrypted TM frame has 1115 bytes
    //encrypted 1115 + 32 bytes
    public static final int SPI_SIZE = 2;
    public static final int ARC_SIZE = 4;
    public static final int IV_SIZE = 12;

    public interface Command {}

    //TODO: what input do I det exactly? entire frame or as specified payload without trailer and vc id? how long is vc id?
    //now assuming entire frame
    public static final class RawTM implements Command {
        //6 bytes
        final byte[] frameHeader;
        //1105 bytes
        final byte[] data;
        //4 bytes
        final byte[] trailer;

        public RawTM(byte[] frameHeader, byte[] data, byte[] trailer) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
        }
    }

    public static final class TMInfo implements Command {
        final byte[] frameHeader;
        final byte[] data;
        final byte[] trailer;
        final short sPi;
        final byte[] arc;
        final byte[] iv;
        final byte[] key;
        final byte[] authMask;

        public TMInfo(byte[] frameHeader, byte[] data, byte[] trailer, short sPi, byte[] arc, byte[] iv, byte[] key, byte[] authMask) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.sPi = sPi;
            this.arc = arc;
            this.iv = iv;
            this.key = key;
            this.authMask = authMask;
        }
    }

    public static Behavior<Command> create(ActorRef<Module.Command> parent) {
        return Behaviors.setup(context -> new TMProcessor(context, parent));
    }


    private final ActorRef<Module.Command> parent;

    private TMProcessor(ActorContext<Command> context, ActorRef<Module.Command> parent) {
        super(context);
        this.parent = parent;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RawTM.class, this::onRaw)
                .onMessage(TMInfo.class, this::onTM)
                .build();
    }

    private Behavior<Command> onRaw(RawTM tm) {
        BitSet header = BitSet.valueOf(tm.frameHeader);
        BitSet vc = header.get(12, 15);
        BitSet vcId = new BitSet(8);
        for(int i = 0; i < 5; i++) {
            vcId.clear(i);
        }
        int j = 0;
        for(int i = 5; i < 8; i++) {
            if (vc.get(j)) {
                vcId.set(i);
            }
            j++;
        }
        //is one byte long
        byte[] bytes = vcId.toByteArray();
        byte[] byVc = new byte[4];
        byVc[0] = 0;
        byVc[1] = 0;
        byVc[2] = 0;
        System.arraycopy(bytes, 0, byVc, 3, 1);
        int channleInt = ByteBuffer.wrap(byVc).getInt();
        //TODO: is SA configured for VC if it is started on it?
        return this;
    }

    private Behavior<Command> onTM(TMInfo tm) {
        //TODO: handle authentication Bitmask
        byte[] secHeader = new byte[SPI_SIZE + ARC_SIZE + IV_SIZE];
        byte[] sPi = ByteBuffer.allocate(2).putShort(tm.sPi).array();
        System.arraycopy(sPi, 0, secHeader, 0, 2);
        System.arraycopy(tm.arc, 0, secHeader, sPi.length, ARC_SIZE);
        System.arraycopy(tm.iv, 0, secHeader, (SPI_SIZE+ARC_SIZE), IV_SIZE);
        byte[] plaintext = new byte[tm.frameHeader.length + tm.data.length];
        System.arraycopy(tm.frameHeader, 0, plaintext, 0, tm.frameHeader.length);
        System.arraycopy(tm.data, 0, plaintext, tm.frameHeader.length, tm.data.length);
        try {
            byte[] ciphertext = encrypt(tm.key, tm.iv, plaintext);
            byte[] toReturn = new byte[secHeader.length + ciphertext.length + tm.trailer.length];
            System.arraycopy(secHeader, 0, toReturn, 0, secHeader.length);
            System.arraycopy(ciphertext, 0, toReturn, secHeader.length, ciphertext.length);
            System.arraycopy(tm.trailer, 0, toReturn, (secHeader.length + ciphertext.length), tm.trailer.length);
            this.parent.tell(new Module.ReturnTM(toReturn));
        }
        catch (Exception e) {

        }
        return this;
    }

    private static byte[] encrypt(byte[] key, byte[] iv, byte[] data) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        return cipher.doFinal(data);
    }
}
