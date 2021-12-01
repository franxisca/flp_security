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
import java.util.Arrays;
import java.util.BitSet;

public class TMProcessor extends AbstractBehavior<TMProcessor.Command> {

    //unencrypted TM frame has 1115 bytes
    //encrypted 1115 + 32 bytes
    public static final int SPI_SIZE = 2;
    public static final int ARC_SIZE = 4;
    public static final int IV_SIZE = 12;

    public interface Command {}

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
        final int channel;

        public TMInfo(byte[] frameHeader, byte[] data, byte[] trailer, short sPi, byte[] arc, byte[] iv, byte[] key, byte[] authMask, int channel) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.sPi = sPi;
            this.arc = arc;
            this.iv = iv;
            this.key = key;
            this.authMask = authMask;
            this.channel = channel;
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
        byte headerByte = tm.frameHeader[1];
        byte vcBits = (byte) (headerByte & 0b00001110);
        byte vcShift = (byte) (vcBits >> 1);
        int channelInt = (int) vcShift;
        this.parent.tell(new Module.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, channelInt, getContext().getSelf()));
        return this;
    }

    private Behavior<Command> onTM(TMInfo tm) {
        //authentication bitmask is unhandled but is specified as all ones anyway
        //arc overflow
        if(tm.arc[0] == -1 && tm.arc[1] == -1 && tm.arc[2] == -1 && tm.arc[3] == -1) {
            this.parent.tell(new Module.ARCOverflow(tm.sPi, tm.channel));
        }
        //increment arc
        else {
            this.parent.tell(new Module.ARCIncrement(tm.sPi));
        }
        byte[] arc = new byte[4];
        arc[3] = 1;
        for(int i = 0; i < 3; i++) {
            arc[i] = 0;
        }
        byte[] secHeader = new byte[SPI_SIZE + ARC_SIZE + IV_SIZE];
        byte[] sPi = ByteBuffer.allocate(2).putShort(tm.sPi).array();
        System.arraycopy(sPi, 0, secHeader, 0, 2);
        //TODO: unfix arc
        //System.arraycopy(tm.arc, 0, secHeader, sPi.length, ARC_SIZE);
        System.arraycopy(arc, 0, secHeader, sPi.length, ARC_SIZE);

        System.arraycopy(tm.iv, 0, secHeader, (SPI_SIZE+ARC_SIZE), IV_SIZE);
        /*byte[] plaintext = new byte[tm.frameHeader.length + tm.data.length];
        System.arraycopy(tm.frameHeader, 0, plaintext, 0, tm.frameHeader.length);
        System.arraycopy(tm.data, 0, plaintext, tm.frameHeader.length, tm.data.length);*/
        try {
            byte[] ivFin = new byte[16];
            System.arraycopy(tm.iv, 0, ivFin, 0, tm.iv.length);
            for(int i = 12; i < ivFin.length; i++) {
                ivFin[i] = 0;
            }
            byte[] ciphertext = encrypt(tm.key, ivFin, tm.data, tm.frameHeader, secHeader);
            byte[] toReturn = new byte[tm.frameHeader.length + secHeader.length + ciphertext.length + tm.trailer.length];
            System.arraycopy(tm.frameHeader, 0, toReturn, 0, tm.frameHeader.length);
            System.arraycopy(secHeader, 0, toReturn, tm.frameHeader.length, secHeader.length);
            System.arraycopy(ciphertext, 0, toReturn, (tm.frameHeader.length + secHeader.length), ciphertext.length);
            System.arraycopy(tm.trailer, 0, toReturn, (tm.frameHeader.length + secHeader.length + ciphertext.length), tm.trailer.length);
            this.parent.tell(new Module.ReturnTM(toReturn));
            //arc overflow
            /*if(tm.arc[0] == -1 && tm.arc[1] == -1 && tm.arc[2] == -1 && tm.arc[3] == -1) {
                this.parent.tell(new Module.ARCOverflow(tm.sPi, tm.channel));
            }
            //increment arc
            else {
                this.parent.tell(new Module.ARCIncrement(tm.sPi));
            }*/
            boolean overflow = true;
            for(int i = 2; i < 12; i++) {
                if(tm.iv[i] != -1) {
                    overflow = false;
                }
            }
            //iv overflow
            if(overflow) {
                this.parent.tell(new Module.IVOverflow(tm.sPi, tm.channel));
            }
            //increment iv
            else {
                this.parent.tell(new Module.IVIncrement(tm.sPi));
            }
        }
        catch (Exception e) {
            getContext().getLog().info("TM encryption error!");
        }
        return this;
    }

    private static byte[] encrypt(byte[] key, byte[] iv, byte[] data, byte[] primHeader, byte[] secHeader) throws Exception {

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] authData = new byte[primHeader.length + secHeader.length];
        System.arraycopy(primHeader, 0, authData, 0, primHeader.length);
        System.arraycopy(secHeader, 0, authData, primHeader.length, secHeader.length);
        cipher.updateAAD(authData);
        return cipher.doFinal(data);
    }
}
