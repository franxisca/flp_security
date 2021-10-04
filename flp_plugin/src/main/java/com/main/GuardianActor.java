package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class GuardianActor extends AbstractBehavior<GuardianActor.Command> {

    public interface Command {}

    public static final class Start implements Command {
        //TODO: make configurable
        final int active1;
        final Map<Byte, byte[]> masterKeys;
        final Map<Byte, byte[]> sessionKeys;
        final Map<Integer, Short> vcToDefaultSA1;
        final Map<Short, Byte> criticalSA1;
        final List<Short> standardSA1;
        final OutputStream tmStream;
        final File tcStream;
        final File fsr;
        final OutputStream pduStream;


        public Start(int active1, Map<Byte, byte[]> defKeys1, Map<Byte, byte[]> sessionKeys1, Map<Integer, Short> vcToDefaultSA1, Map<Short, Byte> criticalSA1, List<Short> standardSA1, OutputStream tmStream, File tcStream, File fsrOut, OutputStream pduStream) {
            this.active1 = active1;
            this.masterKeys = defKeys1;
            this.vcToDefaultSA1 = vcToDefaultSA1;
            this.criticalSA1 = criticalSA1;
            this.standardSA1 = standardSA1;
            this.sessionKeys = sessionKeys1;
            this.tmStream = tmStream;
            this.tcStream = tcStream;
            this.fsr = fsrOut;
            this.pduStream = pduStream;
        }
    }

    public static final class PDU implements Command {
        final byte[] pdu;

        public PDU(byte[] pdu) {
            this.pdu = pdu;
        }
    }


    public static final class TC implements Command {
        final byte[] tc;

        public TC(byte[] tc) {
            this.tc = tc;
        }
    }

    public static final class TM implements Command {
        final byte[] tm;

        public TM(byte[] tm) {
            this.tm = tm;
        }
    }

    static Behavior<Command> create() {
        return Behaviors.setup(GuardianActor::new);
    }

    private ActorRef<Module.Command> module;

    private GuardianActor(ActorContext<Command> context) {
        super(context);
        module = null;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Start.class, this::onStart)
                .onMessage(PDU.class, this::onPDU)
                .onMessage(TM.class, this::onTM)
                .onMessage(TC.class, this::onTC)
                .build();
    }

    private Behavior<Command> onStart(Start s) {
        ActorRef<PDUOutstream.Command> pduOut = getContext().spawn(PDUOutstream.create(s.pduStream), "pdu-output-stream");
        ActorRef<TMOutStream.Command> tmOut = getContext().spawn(TMOutStream.create(s.tmStream), "tm-output-stream");
        ActorRef<TCOutstream.Command> tcOut = getContext().spawn(TCOutstream.create(s.tcStream), "tc-output-stream");
        ActorRef<Module.Command> module1 = getContext().spawn(Module.create(pduOut, s.active1, tmOut, tcOut, getContext().getSelf(), s.fsr), "module-1");
        this.module = module1;
        module1.tell(new Module.InitSA(s.criticalSA1, s.standardSA1));
        module1.tell(new Module.DefaultSA(s.vcToDefaultSA1));
        module1.tell(new Module.InitKey(s.masterKeys, s.sessionKeys));
        return this;
    }

    private Behavior<Command> onPDU(PDU pdu) {
        this.module.tell(new Module.PDUIn(pdu.pdu));
        return this;
    }

    private Behavior<Command> onTC(TC tc) {
        this.module.tell(new Module.TCIn(tc.tc));
        return this;
    }

    private Behavior<Command> onTM(TM tm) {
        this.module.tell(new Module.TMIn(tm.tm));
        return this;
    }
}
