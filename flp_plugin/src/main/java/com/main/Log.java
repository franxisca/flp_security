package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import javax.swing.text.html.HTMLDocument;
import java.util.*;

public class Log extends AbstractBehavior<Log.Command> {

    public interface Command{}

    public static final class InsertEntry implements Command{
        final byte tag;
        final short length;
        final byte[] value;

        public InsertEntry(byte tag, short length, byte[] value) {
            this.tag = tag;
            this.length = length;
            this.value = value;
        }
    }

    public static final class DumpLog implements Command {
        final ActorRef<PDUManager.Command> replyTo;

        public DumpLog(ActorRef<PDUManager.Command> replyTo) {
            this.replyTo = replyTo;
        }
    }

    public static final class EraseLog implements Command {
        final ActorRef<PDUManager.Command> replyTo;

        public EraseLog(ActorRef<PDUManager.Command> replyTo) {
            this.replyTo = replyTo;
        }

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new Log(context));
    }

    private final Map<Short, ActorRef<LogEntry.Command>> idToEntry = new LinkedHashMap<>();
    private short logKey;
    private Log(ActorContext<Command> context) {
        super(context);
        this.logKey = 0;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(InsertEntry.class, this::onInsert)
                .onMessage(DumpLog.class, this::onDump)
                .onMessage(EraseLog.class, this::onErase)
                .build();
    }

    private Behavior<Command> onInsert(InsertEntry i) {
        ActorRef<LogEntry.Command> logActor = getContext().spawn(LogEntry.create(i.tag, i.length, i.value), "logEntry" + logKey);
        this.idToEntry.put(logKey, logActor);
        logKey++;
        return this;
    }

    private Behavior<Command> onDump(DumpLog d) {

        for (Map.Entry<Short, ActorRef<LogEntry.Command>> entry: this.idToEntry.entrySet()) {

        }
        return this;
    }

    private Behavior<Command> onErase(EraseLog e) {
        for(Map.Entry<Short, ActorRef<LogEntry.Command>> entry: this.idToEntry.entrySet()) {
            getContext().stop(entry.getValue());
            idToEntry.remove(entry.getKey());
        }
        int number = this.idToEntry.size();
        //TODO: remaining space?
        e.replyTo.tell(new PDUManager.EraseLogReply());
        return this;
    }
}
