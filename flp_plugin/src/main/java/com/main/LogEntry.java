package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class LogEntry extends AbstractBehavior<LogEntry.Command> {

    public interface Command {}

    public static Behavior<Command> create(byte tag, short length, byte[] value) {
        return Behaviors.setup(context -> new LogEntry(context, tag, length, value));
    }

    private final byte tag;
    private final short length;
    private final byte[] value;

    private LogEntry(ActorContext<Command> context, byte tag, short length, byte[] value) {

        super(context);
        this.tag = tag;
        this.length = length;
        this.value = value;

    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
