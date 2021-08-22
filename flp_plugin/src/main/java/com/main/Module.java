package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Module extends AbstractBehavior<Module.Command> {

    public interface Command {}

    public static final class getTCInfo implements Command {

    }

    public static final class PDUOut implements Command {
        final byte[] pdu;

        public PDUOut(byte[] pdu) {
            this.pdu = pdu;
        }
    }

    public static final class PDUIn implements Command {

        final byte[] pdu;

        public PDUIn(byte[] pdu) {
            this.pdu = pdu;
        }
    }
    public static Behavior<Command> create(ActorRef<PDUOutstream.Command> pduOut) {
        return Behaviors.setup(context -> new Module(context, pduOut));
    }

    private final ActorRef<SecurityManager.Command> secMan;
    private final ActorRef<PDUOutstream.Command> pduOut;

    private Module(ActorContext<Command> context, ActorRef<PDUOutstream.Command> pduOut) {
        super(context);
        this.secMan = getContext().spawn(SecurityManager.create(getContext().getSelf()), "secMan");
        this.pduOut = pduOut;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PDUIn.class, this::onPDUIn)
                .onMessage(PDUOut.class, this::onPDUout)
                .build();
    }

    private Behavior<Command> onPDUIn(PDUIn pdu) {
        this.secMan.tell(new SecurityManager.PDU(pdu.pdu));
        return this;
    }

    private Behavior<Command> onPDUout(PDUOut p) {
        this.pduOut.tell(new PDUOutstream.PDU());
        return this;
    }
}
