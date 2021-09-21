package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.IOException;
import java.io.OutputStream;

public class PDUOutstream extends AbstractBehavior<PDUOutstream.Command> {

    public interface Command {}

    public static final class PDU implements Command {
        final byte[] pduReply;

        public PDU(byte[] pduReply) {
            this.pduReply = pduReply;
        }
    }

    public static Behavior<Command> create(OutputStream stream) {
        return Behaviors.setup(context -> new PDUOutstream(context, stream));
    }

    private final OutputStream stream;

    private PDUOutstream(ActorContext<Command> context, OutputStream stream) {
        super(context);
        this.stream = stream;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PDU.class, this::onPDU)
                .build();
    }

    private Behavior<Command> onPDU(PDU pdu) {
        try {
            for (int i = 0; i < pdu.pduReply.length; i++) {
                this.stream.write(pdu.pduReply[i]);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

}
