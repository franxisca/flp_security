package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class FSRHandler extends AbstractBehavior<FSRHandler.Command> {

    public interface Command {}

    public static final class BadSA implements Command {

    }

    public static final class BadMAC implements Command {

    }

    public static final class BadSeq implements Command {

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new FSRHandler(context));
    }


    private FSRHandler(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
