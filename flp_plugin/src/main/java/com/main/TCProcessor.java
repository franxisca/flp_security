package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.pattern.Patterns;
import akka.util.Timeout;
import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;


import java.nio.ByteBuffer;
import java.time.Duration;
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


    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new TCProcessor(context));
    }

    private TCProcessor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(EncryptedTC.class, this::onTC)
                .build();
    }

    private Behavior<Command> onTC(EncryptedTC tc) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(tc.secHeader[0]);
        bb.put(tc.secHeader[1]);
        short sPi = bb.getShort(0);
        byte vc = tc.primHeader[2];
        boolean[] vcId = new boolean[6];
        for(int i = 2; i < 8; i++){
            byte bit = (byte) ((vc >> i) & 1);
            if(bit == 1) {
                vcId[7-i] = true;
            }
            else {
                vcId[7-i] = false;
            }
        }


        tc.parent.tell(new Module.getTCInfo(sPi, vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc), getContext().getSelf(), tc.parent);
        //TODO: verify SA on VC
        /*Timeout timeout = Timeout.create(Duration.ofSeconds(5));
        Future<Object> future = Patterns.ask(tc.sam, timeout);
        CompletableFuture<Object> future1 =
                ask(actorA, "request", Duration.ofMillis(1000)).toCompletableFuture();*/
        //TODO: verify SA is operational and key is active
        //TODO: decrypt and authenticate using key from SA
        //TODO: verify ARC
        //TODO: FSR
        return this;
    }
}
