package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class FrameProcessor extends AbstractBehavior<FrameProcessor.Command> {

    public interface Command {}

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new FrameProcessor(context));
    }

    private FrameProcessor(ActorContext<Command> context) {
        super(context);
    }

    public Receive<Command> createReceive() {
        return newReceiveBuilder().build();
    }
}
