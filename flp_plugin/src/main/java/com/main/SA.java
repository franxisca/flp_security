package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class SA extends AbstractBehavior<SA.Command> {

    public interface Command {}

    public static final class Stop implements Command {
        final ActorRef<Log.Command> log;

        public Stop(ActorRef<Log.Command> log) {
            this.log = log;
        }
    }

    public static final class Rekey implements Command {
        final byte keyId;
        final byte[] arc;
        final byte[] iv;
        final ActorRef<Log.Command> log;

        public Rekey(byte keyId, byte[] arc, byte[] iv, ActorRef<Log.Command> log) {
            this.keyId = keyId;
            this.arc = arc;
            this.iv = iv;
            this.log = log;
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
        final long arcWindow;
        final ActorRef<Log.Command> log;

        public SetARSNWindow(long arcWindow, ActorRef<Log.Command> log) {
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

    public static Behavior<Command> create(short sPi, int authMaskLength, byte[] authBitMask, boolean critical) {
        return Behaviors.setup(context -> new SA(context, sPi, authMaskLength, authBitMask, critical));
    }

    private final short sPi;
    private byte[] iV;
    private byte[] aRC;
    private final int authMaskLength;
    private final byte[] authBitMask;
    private long aRCWindow;
    private byte keyId;
    private SAState state;
    private SAState prevState;
    //maybe there are multiple channels
    private long channelId;
    private final boolean critical;

    private SA (ActorContext<Command> context, short sPi, byte[] iV, byte[] aRC, int authMaskLength, byte[] authBitMask, long aRCWindow, byte keyId, SAState state, long channelId, boolean critical) {
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
        this.channelId = channelId;
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
                .onMessage(Rekey.class, this::onRekey)
                .onMessage(Expire.class, this::onExpire)
                .onMessage(SetARSN.class, this::onSetARSN)
                .onMessage(SetARSNWindow.class, this::onSetARSNWindow)
                .onMessage(StatusRequest.class, this::onStatusRequest)
                .onMessage(ReadARSN.class, this::onReadARSN)
                .build();
    }

    private Behavior<Command> onStop(Stop s) {
        //SA not in state to be stopped
        if(this.state != SAState.OPERATIONAL) {
            s.log.tell(new Log.InsertEntry());
        }
        else {
            this.channelId = -1;
            this.state = SAState.KEYED;
            this.prevState = SAState.OPERATIONAL;
            s.log.tell(new Log.InsertEntry());
        }
        return this;
    }

    private Behavior<Command> onRekey(Rekey r) {

        //already checked if key exists and is in right state

        //if SA in wrong state to be rekeyed log that
        if(this.state != SAState.UNKEYED) {
            r.log.tell(new Log.InsertEntry());
        }
        else {
            this.keyId = r.keyId;
            this.state = SAState.KEYED;
            this.prevState = SAState.UNKEYED;
            this.aRC = r.arc;
            this.iV = r.iv;
            //log success
            r.log.tell(new Log.InsertEntry());
        }

        return this;
    }

    private Behavior<Command> onExpire(Expire e) {
        //SA not in the right state
        if(this.state != SAState.KEYED) {
            e.log.tell(new Log.InsertEntry());
        }
        else {
            this.keyId = -1;
            this.state = SAState.UNKEYED;
            this.prevState = SAState.KEYED;
            e.log.tell(new Log.InsertEntry());
        }
        return this;
    }

    private Behavior<Command> onSetARSN(SetARSN s) {
        //maybe check if that is a valid value according to ARSNWindow?
        /*if (s.arc > this.aRCWindow) {

        }*/
        this.aRC = s.arc;
        s.log.tell(new Log.InsertEntry());
        return this;
    }

    private Behavior<Command> onSetARSNWindow(SetARSNWindow s) {
        this.aRCWindow = s.arcWindow;
        s.log.tell(new Log.InsertEntry());
        return this;
    }

    private Behavior<Command> onStatusRequest(StatusRequest s) {
        SAState[] trans;
        if (this.prevState != SAState.NAN) {
            trans = new SAState[2];
            trans[0] = this.prevState;
            trans[1] = this.state;
            s.log.tell(new Log.InsertEntry());
        }
        else {
            trans = new SAState[1];
            trans[0] = this.state;
            s.log.tell(new Log.InsertEntry());
        }
        s.replyTo.tell(new PDUManager.SAStatusRequestReply(this.sPi, trans, s.secMan));

        return this;
    }

    private Behavior<Command> onReadARSN(ReadARSN r) {
        r.log.tell(new Log.InsertEntry());
        r.replyTo.tell(new PDUManager.ReadARSNReply(this.sPi, this.aRC, r.secMan));
        return this;
    }
}
