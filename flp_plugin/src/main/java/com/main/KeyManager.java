package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;

import java.util.HashMap;
import java.util.Map;

public class KeyManager extends AbstractBehavior<KeyManager.Command> {

    public interface Command {}


    public static final class KeyInventory implements Command {
        final byte firstKey;
        final byte lastKey;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> pum;
        final ActorRef<SecurityManager.Command> secMan;
        short number;
        byte currKey;
        Map<Byte, KeyState> keyIdToState;

        public KeyInventory(byte firstKey, byte lastKey, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> pum, ActorRef<SecurityManager.Command> secMan, short number, Map<Byte, KeyState> keyIdToState, byte currKey) {
            this.firstKey = firstKey;
            this.lastKey = lastKey;
            this.log = log;
            this.pum = pum;
            this.secMan = secMan;
            this.number = number;
            this.keyIdToState = keyIdToState;
            this.currKey = currKey;
        }

    }

    public static final class ActivateKey implements Command{
        final byte keyId;
        final ActorRef<Log.Command> log;

        public ActivateKey(byte keyId, ActorRef<Log.Command> log) {
            this.keyId = keyId;
            this.log = log;
        }
    }

    public static final class DeactivateKey implements Command{
        final byte keyId;
        final ActorRef<Log.Command> log;

        public DeactivateKey(byte keyId, final ActorRef<Log.Command> log) {
            this.keyId = keyId;
            this.log = log;
        }
    }

    public static final class VerifyKey {

    }

    public static final class OTAR implements Command{

        final byte keyId;
        final byte[] key;
        final ActorRef<Log.Command> replyTo;

        public OTAR(byte keyId, byte[] key, ActorRef<Log.Command> replyTo) {

            this.keyId = keyId;
            this.key = key;
            this.replyTo = replyTo;
        }

    }

    public static final class ReplaceKey {

    }

    public static final class EraseKey {

    }

    public static final class Rekey implements Command {

        final short sPi;
        final byte keyId;
        final byte[] arc;
        final byte[] iv;
        final ActorRef<Log.Command> log;
        final ActorRef<SAManager.Command> sam;

        public Rekey(short sPi, byte keyId, byte[] arc, byte[] iv, ActorRef<Log.Command> log, ActorRef<SAManager.Command> sam) {
            this.sPi = sPi;
            this.keyId = keyId;
            this.arc = arc;
            this.iv = iv;
            this.log = log;
            this.sam = sam;
        }
    }

    public static final class KeyRequested implements Command {

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new KeyManager(context));
    }

    private final Map<Byte, ActorRef<Key.Command>> keyIdToActor = new HashMap<>();

    private KeyManager(ActorContext<Command> context){
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(OTAR.class, this::onOtar)
                .onMessage(ActivateKey.class, this::onActivate)
                .onMessage(DeactivateKey.class, this::onDeactivate)
                .onMessage(Rekey.class, this::onRekey)
                .onMessage(KeyRequested.class, this::onRequest)
                .onMessage(KeyInventory.class, this::onInventory)
                .build();
    }

    private Behavior<Command> onRequest(KeyRequested k) {
        return this;
    }

    //maybe not log here but send reply to SecurityManager?
    private Behavior<Command> onOtar(OTAR o) {
        ActorRef<Key.Command> keyActor = keyIdToActor.get(o.keyId);
        //key with keyId to insert already exists, log accordingly
        if (keyActor != null) {
            o.replyTo.tell(new Log.InsertEntry());
        }
        //create new key, log it
        else {
            keyActor = getContext().spawn(Key.create(o.keyId, o.key, false), "key" + o.keyId);
            keyIdToActor.put(o.keyId, keyActor);
            o.replyTo.tell(new Log.InsertEntry());
        }
        return this;
    }

    private Behavior<Command> onActivate(ActivateKey a) {
        ActorRef<Key.Command> keyActor = keyIdToActor.get(a.keyId);
        //key to activate does not exist
        if (keyActor == null) {
            a.log.tell(new Log.InsertEntry());
        }
        else {
            keyActor.tell(new Key.Activate(a.log));
        }
        return this;
    }

    private Behavior<Command> onDeactivate(DeactivateKey k) {
        ActorRef<Key.Command> keyActor = keyIdToActor.get(k.keyId);
        //key to deactivate does not exist
        if(keyActor == null) {
            k.log.tell(new Log.InsertEntry());
        }
        else {
            keyActor.tell(new Key.Deactivate(k.log));
        }
        return this;
    }

    private Behavior<Command> onRekey(Rekey r) {
        ActorRef<Key.Command> keyActor = keyIdToActor.get(r.keyId);
        //key to rekey with does not exist
        if(keyActor == null) {
            r.log.tell(new Log.InsertEntry());
        }
        else {
            keyActor.tell(new Key.CheckRekey(r.sPi, r.arc, r.iv, r.log, r.sam));
        }
        return this;
    }

    private Behavior<Command> onInventory(KeyInventory k){
        ActorRef<Key.Command> keyActor = keyIdToActor.get(k.currKey);
        //k.currKey++;
        //int i = k.currKey + 1;
        while(keyActor == null && k.currKey <= k.lastKey) {
            k.currKey++;
            keyActor = keyIdToActor.get(k.currKey);
        }
        if(keyActor == null){
            k.log.tell(new Log.InsertEntry());
            k.pum.tell(new PDUManager.KeyInventoryReply(k.number, k.keyIdToState, k.secMan));
        }
        //found a key to check
        else {
            k.number ++;
            keyActor.tell(new Key.KeyInventory(k.number, k.firstKey, k.lastKey, k.currKey, k.keyIdToState, k.pum, k.secMan, k.log, getContext().getSelf()));
        }
        return this;
    }
}
