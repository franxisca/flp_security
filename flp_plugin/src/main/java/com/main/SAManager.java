package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAManager extends AbstractBehavior<SAManager.Command> {

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
        final ActorRef<KeyManager.Command> keyMan;

        public GetTCInfo(short sPi, boolean[] vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<TCProcessor.Command> tcProc, ActorRef<Module.Command> parent, ActorRef<KeyManager.Command> keyMan) {
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
            this.keyMan = keyMan;
        }
    }

    public static final class GetTMInfo implements Command {
        final byte[] frameHeader;
        final byte[] data;
        final byte[] trailer;
        final int channel;
        final ActorRef<TMProcessor.Command> tmProc;
        final short sPi;
        final ActorRef<KeyManager.Command> keyMan;

        public GetTMInfo(byte[] frameHeader, byte[] data, byte[] trailer, int channel, ActorRef<TMProcessor.Command> tmProc, short sPi, ActorRef<KeyManager.Command> keyMan) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.channel = channel;
            this.tmProc = tmProc;
            this.sPi = sPi;
            this.keyMan = keyMan;
        }
    }

    public static final class Verify implements Command {

    }

    public static final class StatusRequest implements Command {
        final short sPi;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> pum;
        final ActorRef<SecurityManager.Command> secMan;

        public StatusRequest(short sPi, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> pum, ActorRef<SecurityManager.Command> secMan) {
            this.sPi = sPi;
            this.log = log;
            this.pum = pum;
            this.secMan = secMan;
        }

    }

    public static final class Expire implements Command {
        final short sPi;
        final ActorRef<Log.Command> log;

        public Expire(short sPi, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.log = log;
        }

    }

    public static final class Rekey implements Command{
        final short sPi;
        final byte keyId;
        final byte[] arc;
        final byte[] iv;
        final ActorRef<Log.Command> log;
        final ActorRef<Key.Command> keyActor;

        public Rekey(short sPi, byte keyId, byte[] arc, byte[] iv, ActorRef<Log.Command> log, ActorRef<Key.Command> keyActor) {
            this.sPi = sPi;
            this.keyId = keyId;
            this.arc = arc;
            this.iv = iv;
            this.log = log;
            this.keyActor = keyActor;
        }

    }

    public static final class SetARSN implements Command {
        final short sPi;
        final byte[] arc;
        final ActorRef<Log.Command> log;

        public SetARSN(short sPi, byte[] arc, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.arc = arc;
            this.log = log;
        }

    }

    public static final class ReadARSN implements Command {
        final short sPi;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> pum;
        final ActorRef<SecurityManager.Command> replyTo;

        public ReadARSN(short sPi, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> pum, ActorRef<SecurityManager.Command> replyTo) {
            this.sPi = sPi;
            this.log = log;
            this.pum = pum;
            this.replyTo = replyTo;
        }

    }

    public static final class ReadARSNWindow implements Command {
        final short sPi;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> pum;
        final ActorRef<SecurityManager.Command> replyTo;

        public ReadARSNWindow(short sPi, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> pum, ActorRef<SecurityManager.Command> replyTo) {
            this.sPi = sPi;
            this.log = log;
            this.pum = pum;
            this.replyTo = replyTo;
        }
    }

    public static final class SetARSNWindow implements Command {
        final short sPi;
        final int arcWindow;
        final ActorRef<Log.Command> log;

        public SetARSNWindow(short sPi, int arcWindow, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.arcWindow = arcWindow;
            this.log = log;
        }

    }

    public static final class Start implements Command {
        final short sPi;
        final int channel;
        final ActorRef<Log.Command> log;
        final ActorRef<Module.Command> module;

        public Start(short sPi, int channel, ActorRef<Log.Command> log, ActorRef<Module.Command> module) {
            this.sPi = sPi;
            this.channel = channel;
            this.log = log;
            this.module = module;
        }

    }

    public static final class Stop implements Command{
        final short sPi;
        final ActorRef<Log.Command> log;

        public Stop(short sPi, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.log = log;
        }
    }

    public static final class InitSA implements Command {
        final Map<Short, Byte> critical;
        final List<Short> standard;

        public InitSA(Map<Short, Byte> spiToCritical, List<Short> standard) {
            this.critical = spiToCritical;
            this.standard = standard;
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


    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new SAManager(context));
    }

    private final Map<Short, ActorRef<SA.Command>> sPiToActor = new HashMap<>();

    private SAManager(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Stop.class, this::onStop)
                .onMessage(Start.class, this::onStart)
                .onMessage(Rekey.class, this::onRekey)
                .onMessage(Expire.class, this::onExpire)
                .onMessage(SetARSN.class, this::onSetARSN)
                .onMessage(SetARSNWindow.class, this::onSetARSNWindow)
                .onMessage(StatusRequest.class, this::onStatusRequest)
                .onMessage(ReadARSN.class, this::onReadARSN)
                .onMessage(ReadARSNWindow.class, this::onReadARSNWindow)
                .onMessage(GetTCInfo.class, this::onTC)
                .onMessage(GetTMInfo.class, this::onTM)
                .onMessage(InitSA.class, this::onInitSA)
                .onMessage(ArcIncrement.class, this::onARC)
                .onMessage(IvIncrement.class, this::onIV)
                .build();
    }

    private Behavior<Command> onStop(Stop s) {
        ActorRef<SA.Command> saActor = sPiToActor.get(s.sPi);
        //no SA with this spi
        if (saActor == null) {
            byte tag = (byte) 0b00011110;
            short length = 2;
            byte[] value = new byte[2];
            value[1] = (byte) (s.sPi & 0xff);
            value[0] = (byte) ((s.sPi >> 8) & 0xff);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.Stop(s.log));
        }
        return this;
    }

    private Behavior<Command> onRekey(Rekey r) {
        ActorRef<SA.Command> saActor = sPiToActor.get(r.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b00110110;
            short length = 3;
            byte[] value = new byte[3];
            value[1] = (byte) (r.sPi & 0xff);
            value[0] = (byte) ((r.sPi >> 8) & 0xff);
            value[2] = r.keyId;
            r.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.Rekey(r.keyId, r.arc, r.iv, r.log, r.keyActor));
        }
        return this;
    }

    private Behavior<Command> onExpire(Expire e) {
        ActorRef<SA.Command> saActor = sPiToActor.get(e.sPi);
        //no SA with this sPi
        if(saActor == null ) {
            byte tag = (byte) 0b00011001;
            short length = 2;
            byte[] value = new byte[2];
            value[1] = (byte) (e.sPi & 0xff);
            value[0] = (byte) ((e.sPi >> 8) & 0xff);
            e.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.Expire(e.log));
        }
        return this;
    }

    private Behavior<Command> onSetARSN(SetARSN s) {
        ActorRef<SA.Command> saActor = sPiToActor.get(s.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b00011010;
            short length = 6;
            byte[] value = new byte[6];
            value[1] = (byte) (s.sPi & 0xff);
            value[0] = (byte) ((s.sPi >> 8) & 0xff);
            System.arraycopy(s.arc, 0, value, 2, 4);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.SetARSN(s.arc, s.log));
        }
        return this;
    }

    private Behavior<Command> onSetARSNWindow(SetARSNWindow s) {
        ActorRef<SA.Command> saActor = sPiToActor.get(s.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b00010101;
            short length = 6;
            byte[] value = new byte[6];
            value[1] = (byte) (s.sPi & 0xff);
            value[0] = (byte) ((s.sPi >> 8) & 0xff);
            byte[] bytes = ByteBuffer.allocate(4).putInt(s.arcWindow).array();
            System.arraycopy(bytes, 0, value, 2, 4);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.SetARSNWindow(s.arcWindow, s.log));
        }
        return this;
    }

    private Behavior<Command> onStatusRequest(StatusRequest s) {
        ActorRef<SA.Command> saActor = sPiToActor.get(s.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b00011111;
            short length = 2;
            byte[] value = new byte[2];
            value[1] = (byte) (s.sPi & 0xff);
            value[0] = (byte) ((s.sPi >> 8) & 0xff);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.StatusRequest(s.log, s.pum, s.secMan));
        }
        return this;
    }

    private Behavior<Command> onReadARSN(ReadARSN r) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(r.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b00010000;
            short length = 2;
            byte[] value = new byte[2];
            value[1] = (byte) (r.sPi & 0xff);
            value[0] = (byte) ((r.sPi >> 8) & 0xff);
            r.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.ReadARSN(r.log, r.pum, r.replyTo));
        }
        return this;
    }

    private Behavior<Command> onReadARSNWindow(ReadARSNWindow r) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(r.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b01010000;
            short length = 2;
            byte[] value = new byte[2];
            value[1] = (byte) (r.sPi & 0xff);
            value[0] = (byte) ((r.sPi >> 8) & 0xff);
            r.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.ReadARSNWindow(r.log, r.pum, r.replyTo));
        }
        return this;
    }

    private Behavior<Command> onStart(Start s) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(s.sPi);
        //no SA with this spi
        if(saActor == null) {
            byte tag = (byte) 0b00011011;
            short length = 5;
            byte[] value = new byte[5];
            value[1] = (byte) (s.sPi & 0xff);
            value[0] = (byte) ((s.sPi >> 8) & 0xff);
            byte[] bytes = ByteBuffer.allocate(4).putInt(s.channel).array();
            System.arraycopy(bytes, 0, value, 2, 4);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            saActor.tell(new SA.Start(s.channel, s.log, s.module));
        }
        return this;
    }

    private Behavior<Command> onTC(GetTCInfo tc) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(tc.sPi);
        if(saActor == null) {
            tc.tcProc.tell(new TCProcessor.BadSA(tc.sPi, tc.secHeader, tc.parent));
        }
        else {
            saActor.tell(new SA.GetTCInfo(tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.tcProc, tc.parent, tc.keyMan));
        }
        return this;
    }

    private Behavior<Command> onTM(GetTMInfo tm) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(tm.sPi);
        if(saActor == null) {
            getContext().getLog().info("SA for TM not found");
        }
        else {
            saActor.tell(new SA.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.tmProc, tm.keyMan));
        }
        return this;
    }

    private Behavior<Command> onInitSA(InitSA in) {
        for(Map.Entry<Short, Byte> entry : in.critical.entrySet()) {
            //just use empty bitmask as it is never used
            byte[] bitMask = new byte[0];
            ActorRef<SA.Command> saActor = getContext().spawn(SA.create(entry.getKey(), 0, bitMask, true, entry.getValue()), "sa" + entry.getKey());
            this.sPiToActor.put(entry.getKey(), saActor);
        }
        for(short spi : in.standard) {
            //just use empty bitmask as it is never used
            byte[] bitMask = new byte[0];
            ActorRef<SA.Command> saActor = getContext().spawn(SA.create(spi, 0, bitMask, false, (byte) 0b11111111), "sa" + spi);
            this.sPiToActor.put(spi, saActor);
        }
        return this;
    }

    private Behavior<Command> onARC(ArcIncrement arc) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(arc.sPi);
        if(saActor == null) {
            getContext().getLog().info("could not increase arc for spi" + arc.sPi);
        }
        else {
            saActor.tell(new SA.ArcIncrement());
        }
        return this;
    }

    private Behavior<Command> onIV(IvIncrement iv) {
        ActorRef<SA.Command> saActor = this.sPiToActor.get(iv.sPi);
        if(saActor == null) {
            getContext().getLog().info("could not increase iv for spi" + iv.sPi);
        }
        else {
            saActor.tell(new SA.IvIncrement());
        }
        return this;
    }
}
