package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
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
        final ActorRef<KeyManager.Command> parent;

        public Activate(ActorRef<Log.Command> log, ActorRef<KeyManager.Command> parent) {
            this.log = log;
            this.parent = parent;
        }
    }

    public static final class Deactivate implements Command {
        final ActorRef<Log.Command> log;

        public Deactivate(ActorRef<Log.Command> log) {
            this.log = log;
        }
    }

    public static final class Used implements Command {
        final short sPi;

        public Used(short sPi) {
            this.sPi = sPi;
        }
    }

    public static final class NotUsed implements Command {}

    public static final class Verify implements Command {
        final byte[] challenge;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;
        final ArrayList<Pair<Byte, byte[]>> keyToChallenge;
        final Map<Byte, byte[]> reply;
        final ActorRef<KeyManager.Command> keyMan;
        final ActorRef<SecurityManager.Command> secMan;

        public Verify(byte[] challenge, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo, ArrayList<Pair<Byte, byte[]>> keyToChallenge, Map<Byte, byte[]> reply, ActorRef<KeyManager.Command> keyMan, ActorRef<SecurityManager.Command> secMan) {
            this.challenge = challenge;
            this.log = log;
            this.replyTo = replyTo;
            this.keyToChallenge = keyToChallenge;
            this.reply = reply;
            this.keyMan = keyMan;
            this.secMan = secMan;
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
        //final ActorRef<KeyManager.Command> keyMan;
        //final byte keyId;
        final byte[] arc;
        final byte[] authMask;
        final short sPi;

        public GetTCInfo(/*short sPi, */boolean[] vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<TCProcessor.Command> tcProc, ActorRef<Module.Command> parent/*, byte keyId*/, byte[] arc, byte[] authMask, short sPi) {
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
            //this.keyMan = keyMan;
            //this.keyId = keyId;
            this.arc = arc;
            this.authMask = authMask;
            this.sPi = sPi;
        }
    }

    public static final class GetTMInfo implements Command {

        final byte[] frameHeader;
        final byte[] data;
        final byte[] trailer;
        final int channel;
        final short sPi;
        final byte[] arc;
        final byte[] iv;
        final byte[] authMask;
        final ActorRef<TMProcessor.Command> tmProc;

        public GetTMInfo(byte[] frameHeader, byte[] data, byte[] trailer, int channel,short sPi, byte[] arc, byte[] iv, byte[] authMask, ActorRef<TMProcessor.Command> tmProc) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.channel = channel;
            this.sPi = sPi;
            this.arc = arc;
            this.iv = iv;
            this.authMask = authMask;
            this.tmProc = tmProc;
        }
    }

    public static Behavior<Command> create(byte keyId, byte[] key, boolean master) {
        return Behaviors.setup(context -> new Key(context, keyId, key, master));
    }

    private final byte keyId;
    private final byte[] key;
    private KeyState keyState;
    private boolean master;
    private boolean inUse;

    private Key(ActorContext<Command> context, byte keyId, byte[] key, boolean startUp){
        super(context);
        this.keyId = keyId;
        this.key = key;
        if(startUp) {
            this.keyState = KeyState.ACTIVE;
        }
        else {
            this.keyState = KeyState.PRE_ACTIVE;
        }
        this.master = startUp;
        //context.getLog().info("Key with ID {} initialized", keyId);
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
                .onMessage(GetTCInfo.class, this::onTC)
                .onMessage(Used.class, this::onUsed)
                .onMessage(NotUsed.class, this::onNotUsed)
                .onMessage(Verify.class, this::onVerify)
                .onMessage(GetTMInfo.class, this::onTM)
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
        //keyActor cannot be activated from its state
        if(this.keyState != KeyState.PRE_ACTIVE) {
            byte tag = (byte) 0b00110010;
            a.log.tell(new Log.InsertEntry(tag, length, value));
            a.parent.tell(new KeyManager.ActivateReply());
        }
        else {
            byte tag = (byte) 0b10000010;
            this.keyState = KeyState.ACTIVE;
            a.log.tell(new Log.InsertEntry(tag,length, value));
            System.out.println("key activation successful " + this.keyId);
            //System.out.println(this.keyId);
        }
        return this;
    }

    private Behavior<Command> deactivate(Deactivate d) {
        short length = 1;
        byte[] value = new byte[1];
        value[0] = this.keyId;
        //keyActor cannot be deactivated from its current state
        if (this.keyState != KeyState.ACTIVE) {
            byte tag = (byte) 0b00110011;
            d.log.tell(new Log.InsertEntry(tag, length, value));
        }
        //key is still used by an SA
        else if(this.inUse) {
            byte tag = (byte) 0b00010011;
            d.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            byte tag = (byte) 0b10000011;
            this.keyState = KeyState.DEACTIVATED;
            d.log.tell(new Log.InsertEntry(tag, length, value));
            System.out.println("key deactivation successful " + this.keyId);
        }
        return this;
    }

    private Behavior<Command> onRekey(CheckRekey c) {
        //keyActor in wrong state
        if(this.keyState != KeyState.ACTIVE) {
            byte tag = (byte) 0b00100110;
            short length = 3;
            byte[] value = new byte[3];
            value[1] = (byte) (c.sPi & 0xff);
            value[0] = (byte) ((c.sPi >> 8) & 0xff);
            value[2] = this.keyId;
            c.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            c.sam.tell(new SAManager.Rekey(c.sPi, this.keyId, c.arc, c.iv, c.log, getContext().getSelf()));
        }
        return this;
    }

    private Behavior<Command> onInventory(KeyInventory k) {
        k.keyIdToState.put(this.keyId, this.keyState);
        //byte curr = k.currKey++;
        k.keyMan.tell(new KeyManager.KeyInventory(k.firstKey, k.lastKey, k.log, k.pum, k.secMan, k.number, k.keyIdToState, k.currKey));
        return this;
    }

    private Behavior<Command> onMaster(GetMaster m) {
        if(!this.master) {
            byte tag = (byte) 0b00100001;
            short length  = 1;
            byte[] value = new byte[1];
            value[0] = this.keyId;
            m.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            m.keyMan.tell(new KeyManager.DecOtar(this.keyId, this.key, m.iv, m.mac, m.keys, m.log));
        }
        return this;
    }

    private Behavior<Command> onTC(GetTCInfo tc) {
        //keyActor is not ACTIVE, should not happen
        if(this.keyState != KeyState.ACTIVE) {
            tc.tcProc.tell(new TCProcessor.BadSA(tc.sPi, tc.secHeader, tc.parent));
        }
        //return keyActor to TCProcessor
        else {
            tc.tcProc.tell(new TCProcessor.TC(tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.arc, tc.authMask, tc.parent, this.keyId, this.key, tc.sPi));
        }
        return this;
    }

    private Behavior<Command> onUsed(Used u) {
        this.inUse = true;
        return this;
    }

    private Behavior<Command> onNotUsed (NotUsed n) {
        this.inUse = false;
        return this;
    }

    private Behavior<Command> onVerify (Verify v) {
        byte[] response;
        try {
            response = encrypt(v.challenge, this.key);
            byte[] reply = new byte[response.length + 2];
            reply[0] = 0;
            reply[1] = 0;
            System.arraycopy(response, 0, reply, 2, response.length);
            v.reply.put(this.keyId, reply);
            v.keyMan.tell(new KeyManager.VerifyKey(v.keyToChallenge, v.log, v.replyTo, v.reply, v.secMan));
        }
        catch (Exception e) {

            //encryption did not work, should not happen
            getContext().getLog().info("key verification encryption failed");
        }
        return this;
    }

    //TODO: check if it works
    private static byte[] encrypt(byte[] challenge, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        byte[] iv = new byte[2];
        /*for(int i = 0; i < 2; i++) {
            iv[i] = 0;
        }*/
        /*iv[0] = 0;
        iv[1] = 0;*/
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        return cipher.doFinal(challenge);
    }

    private Behavior<Command> onTM (GetTMInfo tm) {
        tm.tmProc.tell(new TMProcessor.TMInfo(tm.frameHeader, tm.data, tm.trailer, tm.sPi, tm.arc, tm.iv, this.key, tm.authMask, tm.channel));
        return this;
    }

}
