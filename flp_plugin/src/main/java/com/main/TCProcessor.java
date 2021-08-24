package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.Patterns;
import akka.util.Timeout;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;


import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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
        final ActorRef sam;

        public EncryptedTC(byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<Module.Command> parent, ActorRef sam) {
            this.primHeader = primHeader;
            this.secHeader = secHeader;
            this.data = data;
            this.dataLength = dataLength;
            this.secTrailer = secTrailer;
            this.crc = crc;
            this.parent = parent;
            this.sam = sam;
        }

    }

    public static final class TC implements Command {

        final boolean[] vcId;
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

        public TC(boolean[] vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, byte[] arc, byte[] authMask, ActorRef<Module.Command> parent, byte keyId, byte[] key) {
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
        }
    }


    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new TCProcessor(context));
    }

    private TCProcessor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(EncryptedTC.class, this::onEncTC)
                .onMessage(TC.class, this::onTC)
                .build();
    }

    private Behavior<Command> onEncTC(EncryptedTC tc) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(tc.secHeader[0]);
        bb.put(tc.secHeader[1]);
        short sPi = bb.getShort(0);
        byte vc = tc.primHeader[2];
        //BitSet bits = BitSet.valueOf(vc);
        boolean[] vcId = new boolean[8];
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
        }


        tc.parent.tell(new Module.GetTCInfo(sPi, vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, getContext().getSelf(), tc.parent));
        //TODO: verify SA on VC
        /*Timeout timeout = Timeout.create(Duration.ofSeconds(5));
        Future<Object> future = Patterns.ask(tc.sam, timeout);
        CompletableFuture<Object> future1 =
                ask(actorA, "request", Duration.ofMillis(1000)).toCompletableFuture();*/
        //TODO: verify SA is operational and keyActor is active
        //TODO: decrypt and authenticate using keyActor from SA
        //TODO: verify ARC
        //TODO: FSR
        return this;
    }

    private Behavior<Command> onTC(TC tc) {
        byte[] plaintext;
        byte[] iv = new byte[12];
        System.arraycopy(tc.secHeader, 6, iv, 0, 12);
        try {
            plaintext = decrypt(tc.key, iv, tc.data, tc.secTrailer, tc.authMask);
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
                //TODO: arc failure, FSR
            }
            else {
                //TODO: verifictaion status, processSecurity Return
                tc.parent.tell(new Module.TC());
            }

        }
        catch (Exception e) {
            plaintext = new byte[0];
            //TODO: FSR
        }
        return this;
    }

    //TODO: handle authentication mask other than all ones
    //TODO: check if decryption works that way
    private static byte[] decrypt(byte[] key, byte[] iv, byte[] data, byte[] tag, byte[] authMask) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        byte[] cipherText = new byte[data.length + tag.length];
        System.arraycopy(data, 0, cipherText, 0, data.length);
        System.arraycopy(tag, 0, cipherText, data.length, tag.length);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        return cipher.doFinal(cipherText);
    }
}
