package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Module extends AbstractBehavior<Module.Command> {

    public interface Command {}

    public static final class GetTCInfo implements Command {

        final short sPi;
        final boolean[] vcId;
        final byte[] primHeader;
        final byte[] secHeader;
        final byte[] data;
        final int dataLength;
        final byte[] secTrailer;
        final byte[] crc;
        final ActorRef<TCProcessor.Command> tcProc;
        final ActorRef<Module.Command> parent;

        public GetTCInfo(short sPi, boolean[] vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<TCProcessor.Command> tcProc, ActorRef<Module.Command> parent) {
            this.sPi = sPi;
            this.vcId = vcId;
            this.primHeader = primHeader;
            this.secHeader = secHeader;
            this.data = data;
            this.dataLength = dataLength;
            this.secTrailer = secTrailer;
            this.crc = crc;
            this.tcProc = tcProc;
            this.parent = parent;
        }
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

    public static final class TC implements Command {

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
                .onMessage(GetTCInfo.class, this::onTC)
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

    private Behavior<Command> onTC(GetTCInfo tc) {
        this.secMan.tell(new SecurityManager.GetTCInfo(tc.sPi, tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.tcProc, tc.parent));
        return this;
    }
}
