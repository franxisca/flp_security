package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class TCProcessor extends AbstractBehavior<TCProcessor.Command> {

    public interface Command {}

    public static final class EncryptedTC implements Command {

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new TCProcessor(context));
    }

    private TCProcessor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
