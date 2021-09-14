package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        final boolean verificationStatus;
        final byte verStatCode;
        final byte[] secReturn;

        public TC(boolean verificationStatus, byte verStatCode, byte[] secReturn) {
            this.verificationStatus = verificationStatus;
            this.verStatCode = verStatCode;
            this.secReturn = secReturn;
        }
    }

    public static final class FSR implements Command {
        final boolean alarmFlag;
        final boolean badSN;
        final boolean badMAC;
        final boolean badSA;
        final short lastSpi;
        final byte lastSN;

        public FSR(boolean alarmFlag, boolean badSN, boolean badMAC, boolean badSA, short lastSpi, byte lastSN) {
            this.alarmFlag = alarmFlag;
            this.badSN = badSN;
            this.badMAC = badMAC;
            this.badSA = badSA;
            this.lastSpi = lastSpi;
            this.lastSN = lastSN;
        }
    }

    public static final class ReturnTM implements Command {
        final byte[] tm;

        public ReturnTM(byte[] tm) {
            this.tm = tm;
        }
    }
    public static final class MapVC implements Command {
        final int channelId;
        final short sPi;

        public MapVC(int channelId, short sPi) {
            this.channelId = channelId;
            this.sPi = sPi;
        }
    }

    public static final class GetTMInfo implements Command {
        final byte[] frameHeader;
        final byte[] data;
        final byte[] trailer;
        final int channel;
        final ActorRef<TMProcessor.Command> tmProc;

        public GetTMInfo(byte[] frameHeader, byte[] data, byte[] trailer, int channel, ActorRef<TMProcessor.Command> tmProc) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.channel = channel;
            this.tmProc = tmProc;
        }
    }

    public static Behavior<Command> create(ActorRef<PDUOutstream.Command> pduOut, int activeKeys) {
        return Behaviors.setup(context -> new Module(context, pduOut, activeKeys));
    }

    private final ActorRef<SecurityManager.Command> secMan;
    private final ActorRef<PDUOutstream.Command> pduOut;
    private final int activeKeys;
    private final Map<Integer, Short> vcIdToSA= new HashMap<>();
    //TODO: needs to be configured
    private final Map<Integer, Short> vcIdToDefaultSA = new HashMap<>();

    private Module(ActorContext<Command> context, ActorRef<PDUOutstream.Command> pduOut, int activeKeys) {
        super(context);
        this.secMan = getContext().spawn(SecurityManager.create(getContext().getSelf(), activeKeys), "secMan");
        this.pduOut = pduOut;
        this.activeKeys = activeKeys;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PDUIn.class, this::onPDUIn)
                .onMessage(PDUOut.class, this::onPDUout)
                .onMessage(GetTCInfo.class, this::onTC)
                .onMessage(MapVC.class, this::onMapVC)
                .onMessage(GetTMInfo.class, this::onTM)
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

    private Behavior<Command> onMapVC(MapVC m) {
        this.vcIdToSA.put(m.channelId, m.sPi);
        return this;
    }

    private Behavior<Command> onTM (GetTMInfo tm) {
        short sPi = this.vcIdToSA.get(tm.channel);
        //no SA active on this channel, use default SA
        if(!this.vcIdToSA.containsKey(tm.channel)) {
            //TODO
        }
        else {
            this.secMan.tell(new SecurityManager.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc, sPi));
            //saActor.tell(new SA.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc));
        }
        return this;
    }
}
