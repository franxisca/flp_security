package com.main;

import akka.actor.typed.javadsl.AbstractBehavior;

public class TMOutStream /*extends AbstractBehavior<TMOutStream.Command> */{

    public interface Command {}

    public static final class TM implements Command {

    }
}
