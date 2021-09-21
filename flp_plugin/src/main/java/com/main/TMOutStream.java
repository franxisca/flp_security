package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.IOException;
import java.io.OutputStream;

public class TMOutStream extends AbstractBehavior<TMOutStream.Command> {

    public interface Command {}

    public static final class TM implements Command {
        final byte[] tm;

        public TM(byte[] tm) {
            this.tm = tm;
        }
    }

    public static Behavior<Command> create(OutputStream out) {
        return Behaviors.setup(context -> new TMOutStream(context, out));
    }

    private final OutputStream out;

    private TMOutStream(ActorContext<Command> context, OutputStream out) {
        super(context);
        this.out = out;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TM.class, this::onTM)
                .build();
    }

    private Behavior<Command> onTM(TM tm) {
        try {
            for(int i = 0; i < tm.tm.length; i++) {
                this.out.write(tm.tm[i]);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
}
