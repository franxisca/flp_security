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
        final ActorRef<SecurityManager.Command> secMan;

        public DumpLog(ActorRef<PDUManager.Command> replyTo, ActorRef<SecurityManager.Command> secMan) {
            this.replyTo = replyTo;
            this.secMan = secMan;
        }
    }

    public static final class EraseLog implements Command {
        final ActorRef<PDUManager.Command> replyTo;
        final ActorRef<SecurityManager.Command> secMan;

        public EraseLog(ActorRef<PDUManager.Command> replyTo, ActorRef<SecurityManager.Command> secMan) {
            this.replyTo = replyTo;
            this.secMan = secMan;
        }

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new Log(context));
    }

    //private final Map<Short, ActorRef<LogEntry.Command>> idToEntry = new LinkedHashMap<>();
    private final LinkedList<InsertEntry> entries = new LinkedList<>();
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
        //ActorRef<LogEntry.Command> logActor = getContext().spawn(LogEntry.create(i.tag, i.length, i.value), "logEntry" + logKey);
        //this.idToEntry.put(logKey, logActor);
        //logKey++;
        this.entries.add(i);
        return this;
    }

    private Behavior<Command> onDump(DumpLog d) {

        ArrayList<Byte> result = new ArrayList<>();
        Iterator it = this.entries.listIterator();
        while(it.hasNext()) {
            InsertEntry i = (InsertEntry) it.next();
            result.add(i.tag);
            //result.add((byte) (i.length & 0xff));
            result.add((byte) ((i.length >> 8) & 0xff));
            result.add((byte) (i.length & 0xff));
            for (int j = 0; j < i.value.length; j++) {
                result.add(i.value[j]);
            }
        }
        byte[] reply = new byte[result.size()];
        for(int i = 0; i < result.size(); i++) {
            reply[i] = result.get(i);
        }
        d.replyTo.tell(new PDUManager.DumpLogReply(reply, d.secMan));
        return this;
    }

    private Behavior<Command> onErase(EraseLog e) {
        this.entries.clear();
        int number = this.entries.size();
        //TODO: remaining space?
        //for now just use all ones byte
        byte rem = (byte) 0b11111111;
        e.replyTo.tell(new PDUManager.EraseLogReply(number, rem, e.secMan));
        return this;
    }
}
