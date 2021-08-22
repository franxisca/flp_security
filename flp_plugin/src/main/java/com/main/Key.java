package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Map;

public class Key extends AbstractBehavior<Key.Command> {

    public interface Command {}

    public static final class GetKey implements Command, KeyManager.Command {
        final ActorRef<KeyReply> replyTo;

        public GetKey(ActorRef<KeyReply> replyTo){
            this.replyTo = replyTo;
        }
    }

    public static final class GetMaster implements Command {
        final byte[] keys;
        final byte[] iv;
        final byte[] mac;
        final ActorRef<Log.Command> log;
        final ActorRef<KeyManager.Command> keyMan;

        public GetMaster(byte[] keys, byte[] mac, byte[] iv, ActorRef<Log.Command> log, ActorRef<KeyManager.Command> keyMan) {
            this.keys = keys;
            this.mac = mac;
            this.iv = iv;
            this.log = log;
            this.keyMan = keyMan;
        }
    }

    public static final class KeyReply implements KeyManager.Command , Command{

        final byte[] key;
        public KeyReply(byte[] key) {
            this.key = key;
        }
    }

    public static final class Activate implements Command {

        final ActorRef<Log.Command> log;

        public Activate(ActorRef<Log.Command> log) {
            this.log = log;
        }
    }

    public static final class Deactivate implements Command {
        final ActorRef<Log.Command> log;

        public Deactivate(ActorRef<Log.Command> log) {
            this.log = log;
        }
    }

    //TODO: act on reception of this message, aka encrypt and MAC challenge and send reply
    public static final class Verify implements Command {
        final byte[] challenge;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;

        public Verify(byte[] challenge, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo) {
            this.challenge = challenge;
            this.log = log;
            this.replyTo = replyTo;
        }
    }

    public static final class CheckRekey implements Command {

        final short sPi;
        final byte[] arc;
        final byte[] iv;
        final ActorRef<Log.Command> log;
        final ActorRef<SAManager.Command> sam;

        public CheckRekey(short sPi, byte[] arc, byte[] iv, ActorRef<Log.Command> log, ActorRef<SAManager.Command> sam) {

            this.sPi = sPi;
            this.arc = arc;
            this.iv = iv;
            this.log = log;
            this.sam = sam;
        }
    }

    public static final class KeyInventory implements Command {
        short number;
        final byte firstKey;
        final byte lastKey;
        byte currKey;
        Map<Byte, KeyState> keyIdToState;
        final ActorRef<PDUManager.Command> pum;
        final ActorRef<SecurityManager.Command> secMan;
        final ActorRef<Log.Command> log;
        final ActorRef<KeyManager.Command> keyMan;

        public KeyInventory(short number, byte firstKey, byte lastKey, byte currKey, Map<Byte, KeyState> keyIdToState, ActorRef<PDUManager.Command> pum, ActorRef<SecurityManager.Command> secMan, ActorRef<Log.Command> log, ActorRef<KeyManager.Command> keyMan) {
            this.number = number;
            this.firstKey = firstKey;
            this.lastKey = lastKey;
            this.currKey = currKey;
            this.keyIdToState = keyIdToState;
            this.pum = pum;
            this.secMan = secMan;
            this.log = log;
            this.keyMan = keyMan;
        }
    }

    public static Behavior<Command> create(byte keyId, byte[] key, boolean startUp) {
        return Behaviors.setup(context -> new Key(context, keyId, key, startUp));
    }

    private final byte keyId;
    private final byte[] key;
    private KeyState keyState;
    private boolean startUp;

    private Key(ActorContext<Command> context, byte keyId, byte[] key, boolean startUp){
        super(context);
        this.keyId = keyId;
        this.key = key;
        this.keyState = KeyState.POWERED_OFF;
        this.startUp = startUp;
        context.getLog().info("Key with ID {} initialized", keyId);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Activate.class, this::activate)
                .onMessage(Deactivate.class, this::deactivate)
                .onMessage(CheckRekey.class, this::onRekey)
                .onMessage(GetKey.class, this::onGetKey)
                .onMessage(KeyInventory.class, this::onInventory)
                .onMessage(GetMaster.class, this::onMaster)
                .build();
    }

    private Behavior<Command> onGetKey(GetKey getKey) {
        getKey.replyTo.tell(new KeyReply(this.key));
        return this;
    }

    private Behavior<Command> activate(Activate a) {
        short length = 1;
        byte[] value = new byte[1];
        value[0] = this.keyId;
        //key cannot be activated from its state
        if(this.keyState != KeyState.PRE_ACTIVE) {
            byte tag = (byte) 0b00110010;
            a.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            byte tag = (byte) 0b10000010;
            this.keyState = KeyState.ACTIVE;
            a.log.tell(new Log.InsertEntry(tag,length, value));
        }
        return this;
    }

    private Behavior<Command> deactivate(Deactivate d) {
        short length = 1;
        byte[] value = new byte[1];
        value[0] = this.keyId;
        //key cannot be deactivated from its current state
        if (this.keyState != KeyState.ACTIVE) {
            byte tag = (byte) 0b00110011;
            d.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            byte tag = (byte) 0b10000011;
            this.keyState = KeyState.DEACTIVATED;
            d.log.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }

    private Behavior<Command> onRekey(CheckRekey c) {
        //key in wrong state
        if(this.keyState != KeyState.ACTIVE) {
            byte tag = (byte) 0b00000110;
            short length = 3;
            byte[] value = new byte[3];
            value[0] = (byte) (c.sPi & 0xff);
            value[1] = (byte) ((c.sPi >> 8) & 0xff);
            value[2] = this.keyId;
            c.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            c.sam.tell(new SAManager.Rekey(c.sPi, this.keyId, c.arc, c.iv, c.log));
        }
        return this;
    }

    private Behavior<Command> onInventory(KeyInventory k) {
        k.keyIdToState.put(this.keyId, this.keyState);
        k.keyMan.tell(new KeyManager.KeyInventory(k.firstKey, k.lastKey, k.log, k.pum, k.secMan, k.number, k.keyIdToState, k.currKey));
        return this;
    }

    private Behavior<Command> onMaster(GetMaster m) {
        m.keyMan.tell(new KeyManager.DecOtar(this.keyId, this.key, m.iv, m.mac, m.keys, m.log));
        return this;
    }
}
