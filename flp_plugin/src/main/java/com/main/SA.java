package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;

public class SA extends AbstractBehavior<SA.Command> {

    public interface Command {}

    /*public static final class verifySA implements Command {
        final
    }*/

    public static final class Stop implements Command {
        final ActorRef<Log.Command> log;

        public Stop(ActorRef<Log.Command> log) {
            this.log = log;
        }
    }

    public static final class Start implements Command {
        final int channel;
        final ActorRef<Log.Command> log;
        final ActorRef<Module.Command> module;

        public Start(int channel, ActorRef<Log.Command> log, ActorRef<Module.Command> module) {
            this.channel = channel;
            this.log = log;
            this.module = module;
        }
    }

    public static final class Rekey implements Command {
        final byte keyId;
        final byte[] arc;
        final byte[] iv;
        final ActorRef<Log.Command> log;
        final ActorRef<Key.Command> keyActor;

        public Rekey(byte keyId, byte[] arc, byte[] iv, ActorRef<Log.Command> log, ActorRef<Key.Command> keyActor) {
            this.keyId = keyId;
            this.arc = arc;
            this.iv = iv;
            this.log = log;
            this.keyActor = keyActor;
        }
    }

    public static final class Expire implements Command{
        final ActorRef<Log.Command> log;

        public Expire(ActorRef<Log.Command> log) {
            this.log = log;
        }
    }

    public static final class SetARSN implements Command {
        final byte[] arc;
        final ActorRef<Log.Command> log;

        public SetARSN(byte[] arc, ActorRef<Log.Command> log) {
            this.arc = arc;
            this.log = log;
        }
    }

    public static final class SetARSNWindow implements Command {
        final int arcWindow;
        final ActorRef<Log.Command> log;

        public SetARSNWindow(int arcWindow, ActorRef<Log.Command> log) {
            this.arcWindow = arcWindow;
            this.log = log;
        }
    }

    public static final class StatusRequest implements Command {

        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;
        final ActorRef<SecurityManager.Command> secMan;

        public StatusRequest(ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo, ActorRef<SecurityManager.Command> secMan) {

            this.log = log;
            this.replyTo = replyTo;
            this.secMan = secMan;
        }
    }

    public static final class ReadARSN implements Command {
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;
        final ActorRef<SecurityManager.Command> secMan;

        public ReadARSN(ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo, ActorRef<SecurityManager.Command> secMan) {
            this.log = log;
            this.replyTo = replyTo;
            this.secMan = secMan;
        }
    }

    public static final class ReadARSNWindow implements Command {
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;
        final ActorRef<SecurityManager.Command> secMan;

        public ReadARSNWindow(ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo, ActorRef<SecurityManager.Command> secMan) {
            this.log = log;
            this.replyTo = replyTo;
            this.secMan = secMan;
        }
    }

    public static final class GetTCInfo implements Command {

        //final short sPi;
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

        public GetTCInfo(/*short sPi, */boolean[] vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<TCProcessor.Command> tcProc, ActorRef<Module.Command> parent, ActorRef<KeyManager.Command> keyMan) {
            //this.sPi = sPi;
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
        final ActorRef<KeyManager.Command> keyMan;

        public GetTMInfo(byte[] frameHeader, byte[] data, byte[] trailer, int channel, ActorRef<TMProcessor.Command> tmProc, ActorRef<KeyManager.Command> keyMan) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.channel = channel;
            this.tmProc = tmProc;
            this.keyMan = keyMan;
        }
    }

    public static Behavior<Command> create(short sPi, int authMaskLength, byte[] authBitMask, boolean critical) {
        return Behaviors.setup(context -> new SA(context, sPi, authMaskLength, authBitMask, critical));
    }

    private final short sPi;
    private byte[] iV;
    private byte[] aRC;
    private final int authMaskLength;
    private final byte[] authBitMask;
    private int aRCWindow;
    private byte keyId;
    private ActorRef<Key.Command> keyActor;
    private SAState state;
    private SAState prevState;
    //maybe there are multiple channels
    //private ArrayList<Integer> channels;
    private ArrayList<Integer> channels;
    //private long channelId;
    private final boolean critical;

    private SA (ActorContext<Command> context, short sPi, byte[] iV, byte[] aRC, int authMaskLength, byte[] authBitMask, int aRCWindow, byte keyId, SAState state, ArrayList<Integer> channels, boolean critical) {
        super(context);
        //this.sPi = new byte[sPi.length];
        this.sPi = sPi;
        this.iV = iV;
        this.aRC = aRC;
        this.authMaskLength = authMaskLength;
        this.authBitMask = authBitMask;
        this.aRCWindow = aRCWindow;
        this.keyId = keyId;
        this.state = state;
        this.channels = channels;
        this.critical = critical;
    }
    private SA (ActorContext<Command> context, short sPi, int authMaskLength, byte[] authBitMask, boolean critical) {
        super(context);
        this.sPi = sPi;
        this.authBitMask = authBitMask;
        this.authMaskLength = authMaskLength;
        this.critical = critical;
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
                .build();
    }

    private Behavior<Command> onStop(Stop s) {
        //SA not in state to be stopped
        if(this.state != SAState.OPERATIONAL) {
            byte tag = (byte) 0b00101110;
            short length = 2;
            byte[] value = new byte[2];
            value[0] = (byte) (this.sPi & 0xff);
            value[1] = (byte) ((this.sPi >> 8) & 0xff);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            this.channels.clear();
            this.state = SAState.KEYED;
            this.prevState = SAState.OPERATIONAL;
            byte tag = (byte) 0b10011110;
            short length = 2;
            byte[] value = new byte[2];
            value[0] = (byte) (this.sPi & 0xff);
            value[1] = (byte) ((this.sPi >> 8) & 0xff);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }

    private Behavior<Command> onRekey(Rekey r) {

        //already checked if keyActor exists and is in right state

        short length = 3;
        byte[] value = new byte[3];
        value[0] = (byte) (this.sPi & 0xff);
        value[1] = (byte) ((this.sPi >> 8) & 0xff);
        value[2] = r.keyId;
        //if SA in wrong state to be rekeyed log that
        if(this.state != SAState.UNKEYED) {
            byte tag = (byte) 0b00000110;
            r.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            this.keyId = r.keyId;
            this.keyActor =r.keyActor;
            this.state = SAState.KEYED;
            this.prevState = SAState.UNKEYED;
            this.aRC = r.arc;
            this.iV = r.iv;
            //log success
            byte tag = (byte) 0b10010110;
            r.log.tell(new Log.InsertEntry(tag, length, value));
            r.keyActor.tell(new Key.Used(this.sPi));
        }

        return this;
    }

    private Behavior<Command> onExpire(Expire e) {
        short length = 2;
        byte[] value = new byte[2];
        value[0] = (byte) (this.sPi & 0xff);
        value[1] = (byte) ((this.sPi >> 8) & 0xff);
        //SA not in the right state
        if(this.state != SAState.KEYED) {
            byte tag = (byte) 0b00101001;
            e.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            byte tag = (byte) 0b10011001;
            this.keyId = -1;
            this.keyActor.tell(new Key.NotUsed());
            this.keyActor = null;
            this.state = SAState.UNKEYED;
            this.prevState = SAState.KEYED;
            e.log.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }

    private Behavior<Command> onSetARSN(SetARSN s) {
        short length = 6;
        byte[] value = new byte[6];
        value[0] = (byte) (this.sPi & 0xff);
        value[1] = (byte) ((this.sPi >> 8) & 0xff);
        System.arraycopy(s.arc, 0, value, 2, 4);
        //maybe check if that is a valid value according to ARSNWindow?
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.put(s.arc[0]);
        bb.put(s.arc[1]);
        bb.put(s.arc[2]);
        bb.put(s.arc[3]);
        int arc = bb.getInt(0);
        if((long) arc > this.aRCWindow) {
            byte tag = (byte) 0b00101010;
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            this.aRC = s.arc;
            byte tag = (byte) 0b10011010;
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }

    private Behavior<Command> onSetARSNWindow(SetARSNWindow s) {
        this.aRCWindow = s.arcWindow;
        byte tag = (byte) 0b10010101;
        short length = 10;
        byte[] value = new byte[10];
        value[0] = (byte) (this.sPi & 0xff);
        value[1] = (byte) ((this.sPi >> 8) & 0xff);
        byte[] bytes = ByteBuffer.allocate(8).putLong(s.arcWindow).array();
        System.arraycopy(bytes, 0, value, 2, 8);
        s.log.tell(new Log.InsertEntry(tag, length, value));
        return this;
    }

    private Behavior<Command> onStatusRequest(StatusRequest s) {
        SAState[] trans;
        byte transition;
        if (this.prevState != SAState.NAN) {
            byte tag = (byte) 0b10011111;
            short length = 4;
            byte[] value = new byte[4];
            value[0] = (byte) (this.sPi & 0xff);
            value[1] = (byte) ((this.sPi >> 8) & 0xff);
            value[2] = this.prevState.toByte();
            value[3] = this.state.toByte();
            trans = new SAState[2];
            trans[0] = this.prevState;
            trans[1] = this.state;
            transition = this.state.transition(trans[0], trans[1]);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            byte tag = (byte) 0b10101111;
            short length = 3;
            byte[] value = new byte[3];
            value[0] = (byte) (this.sPi & 0xff);
            value[1] = (byte) ((this.sPi >> 8) & 0xff);
            value[2] = this.state.toByte();
            trans = new SAState[2];
            trans[0] = this.prevState;
            trans[1] = this.state;
            s.log.tell(new Log.InsertEntry(tag, length, value));
            transition = this.state.transition(trans[0], trans[1]);
        }
        s.replyTo.tell(new PDUManager.SAStatusRequestReply(this.sPi, transition, s.secMan));

        return this;
    }

    private Behavior<Command> onReadARSN(ReadARSN r) {
        byte tag = (byte) 0b10010000;
        short length = 6;
        byte[] value = new byte[6];
        value[0] = (byte) (this.sPi & 0xff);
        value[1] = (byte) ((this.sPi >> 8) & 0xff);
        System.arraycopy(this.aRC, 0, value, 2, 4);
        r.log.tell(new Log.InsertEntry(tag, length, value));
        r.replyTo.tell(new PDUManager.ReadARSNReply(this.sPi, this.aRC, r.secMan));
        return this;
    }

    private Behavior<Command> onReadARSNWindow(ReadARSNWindow r) {
        byte tag = (byte) 0b11010000;
        short length = 6;
        byte[] value = new byte[10];
        value[0] = (byte) (this.sPi & 0xff);
        value[1] = (byte) ((this.sPi >> 8) & 0xff);
        byte[] bytes = ByteBuffer.allocate(4).putInt(this.aRCWindow).array();
        System.arraycopy(bytes, 0, value, 2, 4);
        r.log.tell(new Log.InsertEntry(tag, length, value));
        r.replyTo.tell(new PDUManager.ReadARSNWindowReply(this.sPi, this.aRCWindow, r.secMan));
        return this;
    }

    private Behavior<Command> onStart(Start s) {
        if(this.state != SAState.KEYED && this.state != SAState.OPERATIONAL) {
            byte tag = (byte) 0b00101011;
            short length = 6;
            byte[] value = new byte[6];
            value[0] = (byte) (this.sPi & 0xff);
            value[1] = (byte) ((this.sPi >> 8) & 0xff);
            byte[] bytes = ByteBuffer.allocate(4).putInt(s.channel).array();
            System.arraycopy(bytes, 0, value, 2, 4);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            this.state = SAState.OPERATIONAL;
            this.channels.add(s.channel);
            s.module.tell(new Module.MapVC(s.channel, this.sPi));
            byte tag = (byte) 0b10011011;
            short length = 6;
            byte[] value = new byte[6];
            value[0] = (byte) (this.sPi & 0xff);
            value[1] = (byte) ((this.sPi >> 8) & 0xff);
            byte[] bytes = ByteBuffer.allocate(4).putInt(s.channel).array();
            System.arraycopy(bytes, 0, value, 2, 4);
            s.log.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }

    private Behavior<Command> onTC(GetTCInfo tc) {
        //SA not in the right state
        if(this.state != SAState.OPERATIONAL) {
            //TODO: Bad SA flag?
        }
        //SA not applied on channel
        //channel length fixed to 32 bits and stored as integer
        byte[] chan;
        BitSet bits = new BitSet(tc.vcId.length);
        for(int i = 0; i < tc.vcId.length; i++) {
            if(tc.vcId[i]) {
                bits.set(i);
            }
        }
        //chan is a one byte array
        chan = bits.toByteArray();
        byte[] channel = new byte[4];
        channel[0] = 0;
        channel[1] = 0;
        channel[2] = 0;
        System.arraycopy(chan, 0, channel, 3, 1);
        int channelInt = ByteBuffer.wrap(channel).getInt();
        if(!this.channels.contains(channelInt)) {
            //TODO: Bad SA flag?
        }
        else {
            tc.keyMan.tell(new KeyManager.GetTCInfo(tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.tcProc, tc.parent, this.keyId, this.aRC, this.authBitMask));
        }
        return this;
    }

    private Behavior<Command> onTM(GetTMInfo tm) {
        //this.keyActor.tell(new );
        if (this.keyActor != null) {
            this.keyActor.tell(new Key.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, this.sPi, this.aRC, this.iV, this.authBitMask, tm.tmProc));
        }
        else {
            tm.keyMan.tell(new KeyManager.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, this.sPi, this.aRC, this.iV, this.authBitMask, this.keyId, tm.tmProc));
        }
        //tm.tmProc.tell(new TMProcessor.TMInfo(tm.frameHeader, tm.data, tm.trailer, this.sPi, this.aRC, this.iV));
        return this;
    }
}
