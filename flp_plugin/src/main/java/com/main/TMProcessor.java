package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class TMProcessor extends AbstractBehavior<TMProcessor.Command> {

    //unencrypted TM frame has 1115 bytes
    //encrypted 1115 + 32 bytes

    public interface Command {}

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new TMProcessor(context));
    }

    private TMProcessor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
