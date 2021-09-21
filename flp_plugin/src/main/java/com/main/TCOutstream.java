package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.IOException;
import java.io.OutputStream;

public class TCOutstream extends AbstractBehavior<TCOutstream.Command> {

    public interface Command {}

    public static final class TC implements Command {
        final byte[] tc;

        public TC(byte[] tc) {
            this.tc = tc;
        }
    }

    public static Behavior<Command> create(OutputStream out) {
        return Behaviors.setup(context -> new TCOutstream(context, out));
    }

    private final OutputStream out;

    private TCOutstream(ActorContext<Command> context, OutputStream out) {
        super(context);
        this.out = out;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TC.class, this::onTC)
                .build();
    }

    private Behavior<Command> onTC(TC tc) {
        try {
            for(int i = 0; i < tc.tc.length; i++) {
                this.out.write(tc.tc[i]);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
}
