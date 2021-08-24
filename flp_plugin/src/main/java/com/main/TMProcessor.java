package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class TMProcessor extends AbstractBehavior<TMProcessor.Command> {

    //unencrypted TM frame has 1115 bytes
    //encrypted 1115 + 32 bytes


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

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new TMProcessor(context));
    }

    private TMProcessor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RawTM.class, this::onRaw)
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
}
