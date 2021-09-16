package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static final class TCIn implements Command {
        final byte[] tc;

        public TCIn(byte[] tc) {
            this.tc = tc;
        }
    }

    public static final class TCOut implements Command {
        final boolean verificationStatus;
        final byte verStatCode;
        final byte[] secReturn;

        public TCOut(boolean verificationStatus, byte verStatCode, byte[] secReturn) {
            this.verificationStatus = verificationStatus;
            this.verStatCode = verStatCode;
            this.secReturn = secReturn;
        }
    }

    public static final class TMIn implements Command {
        final byte[] tm;

        public TMIn(byte[] tm) {
            this.tm = tm;
        }
    }

    public static final class TMOut implements Command {

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

    public static final class AlarmFlagReset implements Command {

    }

    public static final class InitSA implements Command {
        final Map<Short, Byte> criticalSAs;
        final List<Short> standardSAs;

        public InitSA(Map<Short, Byte> spiToCritical, List<Short> standardSAs) {
            this.criticalSAs = spiToCritical;
            this.standardSAs = standardSAs;
        }
    }

    public static final class InitKey implements Command {
        final Map<Byte, byte[]> master;
        final Map<Byte, byte[]> session;

        public InitKey(Map<Byte, byte[]> master, Map<Byte, byte[]> session) {
            this.master = master;
            this.session = session;
        }
    }

    public static final class DefaultSA implements Command {
        final Map<Integer, Short> vcToSA;

        public DefaultSA(Map<Integer, Short> vcToSA) {
            this.vcToSA = vcToSA;
        }
    }

    public static Behavior<Command> create(ActorRef<PDUOutstream.Command> pduOut, int activeKeys, ActorRef<TMOutStream.Command> tmOut, ActorRef<TCOutstream.Command> tcOut, ActorRef<GuardianActor.Command> parent) {
        return Behaviors.setup(context -> new Module(context, pduOut, activeKeys, tmOut, tcOut, parent));
    }

    private final ActorRef<SecurityManager.Command> secMan;
    private final ActorRef<PDUOutstream.Command> pduOut;
    private final ActorRef<TMOutStream.Command> tmOut;
    private final ActorRef<TCOutstream.Command> tcOut;
    private final int activeKeys;
    private final Map<Integer, Short> vcIdToSA= new HashMap<>();
    private final ActorRef<TCProcessor.Command> tcProc;
    private final ActorRef<TMProcessor.Command> tmProc;
    //TODO: needs to be configured
    private Map<Integer, Short> vcIdToDefaultSA = new HashMap<>();
    private final ActorRef<GuardianActor.Command> parent;

    private Module(ActorContext<Command> context, ActorRef<PDUOutstream.Command> pduOut, int activeKeys, ActorRef<TMOutStream.Command> tmOut, ActorRef<TCOutstream.Command> tcOut, ActorRef<GuardianActor.Command> parent) {
        super(context);
        this.secMan = getContext().spawn(SecurityManager.create(getContext().getSelf(), activeKeys), "secMan");
        this.pduOut = pduOut;
        this.activeKeys = activeKeys;
        this.tcProc = getContext().spawn(TCProcessor.create(), "tc-processor");
        this.tmProc = getContext().spawn(TMProcessor.create(getContext().getSelf()), "tm-processor");
        this.tmOut = tmOut;
        this.tcOut = tcOut;
        this.parent = parent;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PDUIn.class, this::onPDUIn)
                .onMessage(PDUOut.class, this::onPDUout)
                .onMessage(GetTCInfo.class, this::onTC)
                .onMessage(MapVC.class, this::onMapVC)
                .onMessage(GetTMInfo.class, this::onTM)
                .onMessage(AlarmFlagReset.class, this::onAlarm)
                .onMessage(TCIn.class, this::onTCIn)
                .onMessage(TCOut.class, this::onTCOut)
                .onMessage(TMIn.class, this::onTMIn)
                .onMessage(ReturnTM.class, this::onTMOut)
                .onMessage(InitSA.class, this::onInitSA)
                .onMessage(InitKey.class, this::onInitKey)
                .onMessage(DefaultSA.class, this::onDefaultSA)
                .build();
    }

    private Behavior<Command> onPDUIn(PDUIn pdu) {
        this.secMan.tell(new SecurityManager.PDU(pdu.pdu));
        return this;
    }

    private Behavior<Command> onPDUout(PDUOut p) {
        //TODO
        //this.pduOut.tell(new PDUOutstream.PDU(p.pdu));
        return this;
    }

    private Behavior<Command> onTC(GetTCInfo tc) {
        this.secMan.tell(new SecurityManager.GetTCInfo(tc.sPi, tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.tcProc, tc.parent));
        return this;
    }

    //TODO remove SA when it is stopped, how to replace?
    private Behavior<Command> onMapVC(MapVC m) {
        this.vcIdToSA.put(m.channelId, m.sPi);
        return this;
    }

    private Behavior<Command> onTM (GetTMInfo tm) {
        short sPi = this.vcIdToSA.get(tm.channel);
        //no SA active on this channel, use default SA
        if(!this.vcIdToSA.containsKey(tm.channel)) {
            sPi = this.vcIdToDefaultSA.get(tm.channel);
            this.secMan.tell(new SecurityManager.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc, sPi));
        }
        else {
            this.secMan.tell(new SecurityManager.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc, sPi));
            //saActor.tell(new SA.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc));
        }
        return this;
    }

    private Behavior<Command> onAlarm(AlarmFlagReset a) {
        this.tcProc.tell(new TCProcessor.Reset());
        return this;
    }

    private Behavior<Command> onTCIn(TCIn tc) {
        byte[] frameHeader = new byte[5];
        byte[] secHeader = new byte[18];
        byte[] secTrailer = new byte[16];
        byte[] crc = new byte[2];
        int dataLength = tc.tc.length - (5+18+16+2);
        byte[] data = new byte[dataLength];
        System.arraycopy(tc.tc, 0, frameHeader, 0, 5);
        System.arraycopy(tc.tc, 5, secHeader, 0, 18);
        System.arraycopy(tc.tc, 23, data, 0, dataLength);
        System.arraycopy(tc.tc, (23+dataLength), secTrailer, 0, 16);
        System.arraycopy(tc.tc, tc.tc.length - 2, crc, 0, 2);
        this.tcProc.tell(new TCProcessor.EncryptedTC(frameHeader, secHeader, data, dataLength, secTrailer, crc, getContext().getSelf()));
        return this;
    }

    private Behavior<Command> onTCOut(TCOut tc) {
        //TODO
        //this.tcOut.tell(new TCOutstream.TC(tc.secReturn, tc.verificationStatus, tc.verStatCode));
        return this;
    }

    private Behavior<Command> onTMIn(TMIn tm) {
        byte[] frameHeader = new byte[6];
        byte[] data = new byte[1105];
        byte[] trailer = new byte[4];
        System.arraycopy(tm.tm, 0, frameHeader, 0, 6);
        System.arraycopy(tm.tm, 6, data, 0, 1105);
        System.arraycopy(tm.tm, 1111, trailer, 0, 4);
        this.tmProc.tell(new TMProcessor.RawTM(frameHeader, data, trailer));
        return this;
    }

    private Behavior<Command> onTMOut(ReturnTM tm) {
        //TODO
        //this.tmOut.tell(new TMOutStream.TM(tm.tm));
        return this;
    }

    private Behavior<Command> onInitSA(InitSA in) {
        this.secMan.tell(new SecurityManager.InitSA(in.criticalSAs, in.standardSAs));
        return this;
    }

    private Behavior<Command> onDefaultSA(DefaultSA d) {
        this.vcIdToDefaultSA = d.vcToSA;
        return this;
    }

    private Behavior<Command> onInitKey(InitKey in) {
        this.secMan.tell(new SecurityManager.InitKey(in.master, in.session));
        return this;
    }
}
