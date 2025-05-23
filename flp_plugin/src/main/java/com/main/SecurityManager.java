package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class SecurityManager extends AbstractBehavior<SecurityManager.Command> {

    public interface Command {}

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

    public static final class GetTMInfo implements Command
    {
        final byte[] frameHeader;
        final byte[] data;
        final byte[] trailer;
        final int channel;
        final ActorRef<TMProcessor.Command> tmProc;
        final short sPi;

        public GetTMInfo(byte[] frameHeader, byte[] data, byte[] trailer, int channel, ActorRef<TMProcessor.Command> tmProc, short sPi) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.channel = channel;
            this.tmProc = tmProc;
            this.sPi = sPi;
        }
    }
    public static final class PDUReply implements Command {
        final byte tag;
        final short length;
        final byte[] value;

        public PDUReply(byte tag, short length, byte[] value) {
            this.tag = tag;
            this.length = length;
            this.value = value;
        }
    }

    public static final class PDU implements Command {
        final byte[] pdu;

        public PDU(byte[] pdu) {
            this.pdu = pdu;
        }
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

    public static final class ArcIncrement implements Command {
        final short sPi;

        public ArcIncrement(short sPi) {
            this.sPi = sPi;
        }
    }

    public static final class IvIncrement implements Command {
        final short sPi;

        public IvIncrement(short sPi) {
            this.sPi = sPi;
        }
    }

    public static Behavior<Command> create(ActorRef<Module.Command> parent, int activeKeys) {
        return Behaviors.setup(context -> new SecurityManager(context, parent, activeKeys));
    }

    private final ActorRef<KeyManager.Command> keyMan;
    private final ActorRef<PDUManager.Command> pduMan;
    private final ActorRef<SAManager.Command> saMan;
    private final ActorRef<Log.Command> log;
    private final ActorRef<Module.Command> parent;
    private final int activeKeys;

    private SecurityManager(ActorContext<Command> context, ActorRef<Module.Command> parent, int activeKeys) {
        super(context);
        this.keyMan = getContext().spawn(KeyManager.create(activeKeys), "keyMan");
        this.pduMan = getContext().spawn(PDUManager.create(), "pduMan");
        this.saMan = getContext().spawn(SAManager.create(), "saMan");
        this.log = getContext().spawn(Log.create(), "log");
        this.parent = parent;
        this.activeKeys = activeKeys;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(PDU.class, this::onPDU)
                .onMessage(PDUReply.class, this::onPDUReply)
                .onMessage(GetTCInfo.class, this::onTC)
                .onMessage(GetTMInfo.class, this::onTM)
                .onMessage(InitSA.class, this::onInitSA)
                .onMessage(InitKey.class, this::onInitKey)
                .onMessage(ArcIncrement.class, this::onARC)
                .onMessage(IvIncrement.class, this::onIV)
                .build();
    }

    private Behavior<Command> onPDU(PDU p) {

        byte tag = p.pdu[0];
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(p.pdu[1]);
        bb.put(p.pdu[2]);
        short length = bb.getShort(0);
        //System.out.println("received length:");
        //System.out.println(length);
        byte[] value = new byte[length];
        for (int i = 0; i < length; i++) {
            value[i] = p.pdu[i + 3];
        }
        switch (tag) {
            //otar
            case (byte) 0b00000001: {
                this.pduMan.tell(new PDUManager.Otar(value, this.keyMan, this.log));
                break;
            }
            //keyActor activation
            case (byte) 0b00000010: {
                this.pduMan.tell(new PDUManager.KeyActivation(value, this.keyMan, this.log));
                break;
            }
            //keyActor deactivation
            case (byte) 0b00000011: {
                this.pduMan.tell(new PDUManager.KeyDeactivation(value, this.keyMan, this.log));
                break;
            }
            //keyActor verification
            case (byte) 0b00000100: {
                this.pduMan.tell(new PDUManager.KeyVerification(value, this.keyMan, this.log, getContext().getSelf()));
                break;
            }
            //keyActor inventory
            case (byte) 0b00000111: {
                this.pduMan.tell(new PDUManager.KeyInventory(value, this.keyMan, getContext().getSelf(), this.log));
                break;
            }
            //key destruction
            case (byte) 0b00000110: {
                this.pduMan.tell(new PDUManager.KeyDestruction(value, this.keyMan, this.log));
                break;
            }
            //start SA
            case (byte) 0b00011011: {
                this.pduMan.tell(new PDUManager.StartSA(value, this.saMan, this.log, this.parent));
                break;
            }
            //stop SA
            case (byte) 0b00011110: {
                this.pduMan.tell(new PDUManager.StopSA(value, this.saMan, this.log));
                break;
            }
            //rekey SA
            case (byte) 0b00010110: {
                this.pduMan.tell(new PDUManager.RekeySA(value, this.keyMan, this.log, this.saMan));
                break;
            }
            //expire SA
            case (byte) 0b00011001: {
                this.pduMan.tell(new PDUManager.ExpireSA(value, this.saMan, this.log));
                break;
            }
            //set ARSN
            case (byte) 0b00011010: {
                this.pduMan.tell(new PDUManager.SetARSN(value, this.saMan, this.log));
                break;
            }
            //set ARSN window
            case (byte) 0b00010101: {
                this.pduMan.tell(new PDUManager.SetARSNWindow(value, this.saMan, this.log));
                break;
            }
            //SA status request
            case (byte) 0b00011111: {
                this.pduMan.tell(new PDUManager.SAStatusRequest(value, this.saMan, this.log, getContext().getSelf()));
                break;
            }
            //read ARSN
            case (byte) 0b00010000: {
                this.pduMan.tell(new PDUManager.ReadARSN(value, this.saMan, this.log, getContext().getSelf()));
                break;
            }
            //ping
            case (byte) 0b00110001: {
                this.pduMan.tell(new PDUManager.Ping(value, getContext().getSelf()));
                break;
            }
            //dump log
            case (byte) 0b00110011: {
                this.pduMan.tell(new PDUManager.DumpLog(getContext().getSelf(), this.log));
                break;
            }
            //erase log
            case (byte) 0b00110100: {
                this.pduMan.tell(new PDUManager.EraseLog(getContext().getSelf(), this.log));
                break;
            }
            //alarm flag reset
            case (byte) 0b00110111: {
                //this.pduMan.tell(new PDUManager.AlarmFlagReset());
                this.parent.tell(new Module.AlarmFlagReset());
                break;
            }

            case (byte) 0b01010000: {
                this.pduMan.tell(new PDUManager.ReadARSNWindow(value, this.saMan, this.log, getContext().getSelf()));
                break;
            }

            default:
                break;

        }
        return this;
    }

    private Behavior<Command> onPDUReply(PDUReply p) {
        final byte[] reply = new byte[p.length + 3];
        reply[0] = p.tag;
        //length needs to be provided in bits
        short lengthBits = (short) (p.length * 8);
        reply[2] = (byte) (lengthBits & 0xff);
        reply[1] = (byte) ((lengthBits >> 8) & 0xff);
        System.arraycopy(p.value, 0, reply, 3, p.length);
        this.parent.tell(new Module.PDUOut(reply));
        return this;
    }

    private Behavior<Command> onTC(GetTCInfo tc) {
        this.saMan.tell(new SAManager.GetTCInfo(tc.sPi, tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.tcProc, tc.parent, this.keyMan));
        return this;
    }

    private Behavior<Command> onTM(GetTMInfo tm) {
        this.saMan.tell(new SAManager.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc, tm.sPi, this.keyMan));
        return this;
    }

    private Behavior<Command> onInitSA(InitSA in) {
        this.saMan.tell(new SAManager.InitSA(in.criticalSAs, in.standardSAs));
        return this;
    }
    private Behavior<Command> onInitKey(InitKey in) {
        this.keyMan.tell(new KeyManager.Init(in.master, in.session));
        return this;
    }

    private Behavior<Command> onARC(ArcIncrement arc) {
        this.saMan.tell(new SAManager.ArcIncrement(arc.sPi));
        return this;
    }

    private Behavior<Command> onIV(IvIncrement iv) {
        this.saMan.tell(new SAManager.IvIncrement(iv.sPi));
        return this;
    }
}
