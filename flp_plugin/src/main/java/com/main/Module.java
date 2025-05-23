package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class Module extends AbstractBehavior<Module.Command> {

    public interface Command {}

    private static final int IV_LENGTH = 12;
    private static final int SPI_LENGTTH = 2;
    private static final int ARC_LENGTH = 4;
    private static final int TAG_LENGTH = 16;

    public static final class GetTCInfo implements Command {

        final short sPi;
        final int vcId;
        final byte[] primHeader;
        final byte[] secHeader;
        final byte[] data;
        final int dataLength;
        final byte[] secTrailer;
        final byte[] crc;
        final ActorRef<TCProcessor.Command> tcProc;
        final ActorRef<Module.Command> parent;

        public GetTCInfo(short sPi, int vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<TCProcessor.Command> tcProc, ActorRef<Module.Command> parent) {
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
        final byte[] crc;

        public TCOut(boolean verificationStatus, byte verStatCode, byte[] secReturn, byte[] crc) {
            this.verificationStatus = verificationStatus;
            this.verStatCode = verStatCode;
            this.secReturn = secReturn;
            this.crc = crc;
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

    public static final class ARCOverflow implements Command {
        final short sPi;
        final int channel;

        public ARCOverflow(short sPi, int channel) {
            this.sPi = sPi;
            this.channel = channel;
        }
    }

    public static final class ARCIncrement implements Command {
        final short sPi;

        public ARCIncrement(short sPi) {
            this.sPi = sPi;
        }
    }

    public static final class IVOverflow implements Command {
        final short sPi;
        final int channel;

        public IVOverflow(short sPi, int channel) {
            this.sPi = sPi;
            this.channel = channel;
        }
    }

    public static final class IVIncrement implements Command {
        final short sPi;

        public IVIncrement(short sPi) {
            this.sPi = sPi;
        }
    }

    public static Behavior<Command> create(ActorRef<PDUOutstream.Command> pduOut, int activeKeys, ActorRef<TMOutStream.Command> tmOut, ActorRef<TCOutstream.Command> tcOut, ActorRef<GuardianActor.Command> parent, File fsr) {
        return Behaviors.setup(context -> new Module(context, pduOut, activeKeys, tmOut, tcOut, parent, fsr));
    }

    private final ActorRef<SecurityManager.Command> secMan;
    private final ActorRef<PDUOutstream.Command> pduOut;
    private final ActorRef<TMOutStream.Command> tmOut;
    private final ActorRef<TCOutstream.Command> tcOut;
    private final int activeKeys;
    private final Map<Integer, Short> vcIdToSA= new HashMap<>();
    private final ActorRef<TCProcessor.Command> tcProc;
    private final ActorRef<TMProcessor.Command> tmProc;
    private Map<Integer, Short> vcIdToDefaultSA = new HashMap<>();
    private final ActorRef<GuardianActor.Command> parent;
    private final File fsr;

    private Module(ActorContext<Command> context, ActorRef<PDUOutstream.Command> pduOut, int activeKeys, ActorRef<TMOutStream.Command> tmOut, ActorRef<TCOutstream.Command> tcOut, ActorRef<GuardianActor.Command> parent, File fsr) {
        super(context);
        this.secMan = getContext().spawn(SecurityManager.create(getContext().getSelf(), activeKeys), "secMan");
        this.pduOut = pduOut;
        this.activeKeys = activeKeys;
        this.tcProc = getContext().spawn(TCProcessor.create(), "tc-processor");
        this.tmProc = getContext().spawn(TMProcessor.create(getContext().getSelf()), "tm-processor");
        this.tmOut = tmOut;
        this.tcOut = tcOut;
        this.parent = parent;
        this.fsr = fsr;
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
                .onMessage(ARCOverflow.class, this::onArcOverflow)
                .onMessage(IVOverflow.class, this::onIVOverflow)
                .onMessage(ARCIncrement.class, this::onArcIncrement)
                .onMessage(IVIncrement.class, this::onIVIncrement)
                .onMessage(FSR.class, this::onFSR)
                .build();
    }

    private Behavior<Command> onPDUIn(PDUIn pdu) {
        this.secMan.tell(new SecurityManager.PDU(pdu.pdu));
        return this;
    }

    private Behavior<Command> onPDUout(PDUOut p) {
        //TODO
        //System.out.println(Arrays.toString(p.pdu));
        this.pduOut.tell(new PDUOutstream.PDU(p.pdu));
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
        //System.out.println("channel id to get default SA");
        //System.out.println(tm.channel);
        //no SA active on this channel, use default SA
        if(!this.vcIdToSA.containsKey(tm.channel)) {
            short sPi = this.vcIdToDefaultSA.get(tm.channel);
            this.secMan.tell(new SecurityManager.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc, sPi));
        }
        else {
            short sPi = this.vcIdToSA.get(tm.channel);
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
        byte[] frameHeader = new byte[6];
        byte[] secHeader = new byte[IV_LENGTH + SPI_LENGTTH + ARC_LENGTH];
        byte[] secTrailer = new byte[TAG_LENGTH];
        byte[] crc = new byte[2];
        int dataLength = tc.tc.length - (frameHeader.length + secHeader.length + secTrailer.length + 2);
        byte[] data = new byte[dataLength];
        System.arraycopy(tc.tc, 0, frameHeader, 0, 6);
        System.arraycopy(tc.tc, frameHeader.length, secHeader, 0, 18);
        System.arraycopy(tc.tc, (frameHeader.length + secHeader.length), data, 0, dataLength);
        System.arraycopy(tc.tc, (frameHeader.length + secHeader.length + dataLength), secTrailer, 0, 16);
        System.arraycopy(tc.tc, tc.tc.length - 2, crc, 0, 2);
        this.tcProc.tell(new TCProcessor.EncryptedTC(frameHeader, secHeader, data, dataLength, secTrailer, crc, getContext().getSelf()));
        return this;
    }

    private Behavior<Command> onTCOut(TCOut tc) {
        //TODO
        System.out.println("Processed TC Frame with verification status:");
        System.out.println(tc.verificationStatus);
        System.out.println("verification status code:");
        System.out.println(tc.verStatCode);
        //System.out.println(Arrays.toString(tc.secReturn));
        byte[] ret = new byte[tc.secReturn.length + tc.crc.length];
        System.arraycopy(tc.secReturn, 0, ret, 0, tc.secReturn.length);
        System.arraycopy(tc.crc, 0, ret, tc.secReturn.length, tc.crc.length);
        System.out.println("Decrypted Frame:");
        System.out.println(toHex(ret));
        this.tcOut.tell(new TCOutstream.TC(tc.verificationStatus, tc.verStatCode, tc.secReturn));
        return this;
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private Behavior<Command> onTMIn(TMIn tm) {
        byte[] frameHeader = new byte[6];
        byte[] data = new byte[1105];
        byte[] trailer = new byte[4];
        System.arraycopy(tm.tm, 0, frameHeader, 0, 6);
        System.arraycopy(tm.tm, 6, data, 0, 1105);
        System.arraycopy(tm.tm, 1111, trailer, 0, 4);
        /*System.out.println("tm frame header test");
        System.out.println(Arrays.toString(frameHeader));*/
        this.tmProc.tell(new TMProcessor.RawTM(frameHeader, data, trailer));
        return this;
    }

    private Behavior<Command> onTMOut(ReturnTM tm) {
        //TODO
        /*System.out.println(Arrays.toString(tm.tm));
        System.out.println(tm.tm.length);*/
        this.tmOut.tell(new TMOutStream.TM(tm.tm));
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

    //TODO:check
    private Behavior<Command> onArcOverflow(ARCOverflow arc) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00011110;
        short length = 2;
        pdu[1] = (byte) (length & 0xff);
        pdu[2] = (byte) ((length >> 8) & 0xff);
        pdu[3] = (byte) (arc.sPi & 0xff);
        pdu[4] = (byte) ((arc.sPi >> 8) & 0xff);
        this.secMan.tell(new SecurityManager.PDU(pdu));
        byte[] start = new byte[9];
        short temp = 0b0000000000000001;
        short nSpi = (short) (arc.sPi ^ temp);
        start[0] = (byte) 0b00011011;
        short length2 = 6;
        start[1] = (byte) (length2 & 0xff);
        start[2] = (byte) ((length2 >> 8) & 0xff);
        start[3] = (byte) (nSpi & 0xff);
        start[4] = (byte) ((nSpi >> 8) & 0xff);
        byte[] bytes = ByteBuffer.allocate(4).putInt(arc.channel).array();
        System.arraycopy(bytes, 0, start, 5, 4);
        this.secMan.tell(new SecurityManager.PDU(start));
        return this;
    }

    private Behavior<Command> onIVOverflow(IVOverflow iv) {
        byte[] pdu = new byte[5];
        pdu[0] = (byte) 0b00011110;
        short length = 2;
        pdu[1] = (byte) (length & 0xff);
        pdu[2] = (byte) ((length >> 8) & 0xff);
        pdu[3] = (byte) (iv.sPi & 0xff);
        pdu[4] = (byte) ((iv.sPi >> 8) & 0xff);
        this.secMan.tell(new SecurityManager.PDU(pdu));
        byte[] start = new byte[9];
        short temp = 0b0000000000000001;
        short nSpi = (short) (iv.sPi ^ temp);
        start[0] = (byte) 0b00011011;
        short length2 = 6;
        start[1] = (byte) (length2 & 0xff);
        start[2] = (byte) ((length2 >> 8) & 0xff);
        start[3] = (byte) (nSpi & 0xff);
        start[4] = (byte) ((nSpi >> 8) & 0xff);
        byte[] bytes = ByteBuffer.allocate(4).putInt(iv.channel).array();
        System.arraycopy(bytes, 0, start, 5, 4);
        this.secMan.tell(new SecurityManager.PDU(start));
        return this;
    }

    private Behavior<Command> onArcIncrement(ARCIncrement arc) {
        this.secMan.tell(new SecurityManager.ArcIncrement(arc.sPi));
        return this;
    }

    private Behavior<Command> onIVIncrement(IVIncrement iv) {
        this.secMan.tell(new SecurityManager.IvIncrement(iv.sPi));
        return this;
    }

    private Behavior<Command> onFSR(FSR fsr) {
        try {
            FileWriter writer = new FileWriter(this.fsr);
            byte firstByte = (byte) 0b11000000;
            byte alarmFlag = 0;
            if(fsr.alarmFlag) {
                alarmFlag = (byte) 0b00001000;
            }
            byte badSN = 0;
            if(fsr.badSN) {
                badSN = (byte) 0b00000100;
            }
            byte badMAC = 0;
            if(fsr.badMAC) {
                badMAC = (byte) 0b00000010;
            }
            byte badSA = 0;
            if(fsr.badSA) {
                badSA = (byte) 0b00000001;
            }
            firstByte = (byte) (firstByte | alarmFlag | badSN | badMAC | badSA);
            byte[] report = new byte[4];
            report[0] = firstByte;
            report[2] = (byte) (fsr.lastSpi & 0xff);
            report[1] = (byte) ((fsr.lastSpi >> 8) & 0xff);
            report[3] = fsr.lastSN;
            writer.write(Arrays.toString(report));
        }
        catch (IOException e) {
            System.out.println("Could not write to file for fsr output..");
        }
        return this;
    }
}
