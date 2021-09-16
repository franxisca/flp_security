package com.main;

import akka.actor.typed.javadsl.AbstractBehavior;

public class TCOutstream implements AbstractBehavior<TCOutstream.Command> {

    public interface Command {}

    public static final class TC implements Command {

    }
}
