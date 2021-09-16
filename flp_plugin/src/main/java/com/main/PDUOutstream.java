package com.main;

import akka.actor.typed.javadsl.AbstractBehavior;

public class PDUOutstream /*extends AbstractBehavior<PDUOutstream.Command>*/ {

    public interface Command {}

    public static final class PDU implements Command {

    }
}
