package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SAManager extends AbstractBehavior<SAManager.Command> {

    public interface Command {}

    public static final class StatusRequest implements Command {
        final short sPi;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> pum;
        final ActorRef<SecurityManager.Command> replyTo;

        public StatusRequest(short sPi, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> pum, ActorRef<SecurityManager.Command> replyTo) {
            this.sPi = sPi;
            this.log = log;
            this.pum = pum;
            this.replyTo = replyTo;
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

        public Rekey(short sPi, byte keyId, byte[] arc, byte[] iv, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.keyId = keyId;
            this.arc = arc;
            this.iv = iv;
            this.log = log;
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

    public static final class SetARSNWindow implements Command {
        final short sPi;
        final long arcWindow;
        final ActorRef<Log.Command> log;

        public SetARSNWindow(short sPi, long arcWindow, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.arcWindow = arcWindow;
            this.log = log;
        }

    }

    public static final class Start implements Command {

    }

    public static final class Stop implements Command{
        final short sPi;
        final ActorRef<Log.Command> log;

        public Stop(short sPi, ActorRef<Log.Command> log) {
            this.sPi = sPi;
            this.log = log;
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
                .onMessage(Rekey.class, this::onRekey)
                .onMessage(Expire.class, this::onExpire)
                .onMessage(SetARSN.class, this::onSetARSN)
                .onMessage(SetARSNWindow.class, this::onSetARSNWindow)
                .onMessage(StatusRequest.class, this::onStatusRequest)
                .onMessage(ReadARSN.class, this::onReadARSN)
                .build();
    }

    private Behavior<Command> onStop(Stop s) {
        ActorRef<SA.Command> saActor = sPiToActor.get(s.sPi);
        //no SA with this spi
        if (saActor == null) {
            s.log.tell(new Log.InsertEntry());
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
            r.log.tell(new Log.InsertEntry());
        }
        else {
            saActor.tell(new SA.Rekey(r.keyId, r.arc, r.iv, r.log));
        }
        return this;
    }

    private Behavior<Command> onExpire(Expire e) {
        ActorRef<SA.Command> saActor = sPiToActor.get(e.sPi);
        //no SA with this sPi
        if(saActor == null ) {
            e.log.tell(new Log.InsertEntry());
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
            s.log.tell(new Log.InsertEntry());
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
            s.log.tell(new Log.InsertEntry());
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
            s.log.tell(new Log.InsertEntry());
        }
        else {
            saActor.tell(new SA.StatusRequest(s.log, s.pum, s.replyTo));
        }
        return this;
    }

    private Behavior<Command> onReadARSN(ReadARSN r) {
        ActorRef<SA.Command> saActor = sPiToActor.get(r.sPi);
        //no SA with this spi
        if(saActor == null) {
            r.log.tell(new Log.InsertEntry());
        }
        else {
            saActor.tell(new SA.ReadARSN(r.log, r.pum, r.replyTo));
        }
        return this;
    }
}
