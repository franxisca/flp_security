package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.japi.Pair;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

    public static final class KeyDestruction implements Command{
        final byte keyId;
        final ActorRef<Log.Command> log;

        public KeyDestruction(byte keyId, ActorRef<Log.Command> log) {
            this.keyId = keyId;
            this.log = log;
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

    public static final class ActivateReply implements Command {}

    public static final class DeactivateKey implements Command{
        final byte keyId;
        final ActorRef<Log.Command> log;

        public DeactivateKey(byte keyId, final ActorRef<Log.Command> log) {
            this.keyId = keyId;
            this.log = log;
        }
    }

    /*public static final class VerifyKey implements Command{
        final byte keyId;
        final byte[] challenge;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;

        public VerifyKey(byte keyId, byte[] challenge, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo) {
            this.keyId = keyId;
            this.challenge = challenge;
            this.log = log;
            this.replyTo = replyTo;
        }

    }*/

    public static final class VerifyKey implements Command {
        ArrayList<Pair<Byte, byte[]>> keyToChallenge;
        //Map<Byte, byte[]> keyToChallenge;
        final ActorRef<Log.Command> log;
        final ActorRef<PDUManager.Command> replyTo;
        Map<Byte, byte[]> reply;

        public VerifyKey(ArrayList<Pair<Byte, byte[]>> keyToChallenge, ActorRef<Log.Command> log, ActorRef<PDUManager.Command> replyTo, Map<Byte, byte[]> reply) {
            this.keyToChallenge = keyToChallenge;
            this.log = log;
            this.replyTo = replyTo;
            this.reply = reply;
        }
    }

    public static final class OTAR implements Command{

        final byte masterKey;
        final byte[] iv;
        final byte[] mac;
        final byte[] keys;
        final ActorRef<Log.Command> log;

        public OTAR(byte masterKey, byte[] iv, byte[] mac, byte[] keys, ActorRef<Log.Command> log) {

            this.masterKey = masterKey;
            this.iv = iv;
            this.mac = mac;
            this.keys = keys;
            this.log = log;
        }

    }

    public static final class DecOtar implements Command {
        final byte masterId;
        final byte[] masterKey;
        final byte[] iv;
        final byte[] mac;
        final byte[] keys;
        final ActorRef<Log.Command> log;

        public DecOtar(byte masterId, byte[] masterKey, byte[] iv, byte[] mac, byte[] keys, ActorRef<Log.Command> log) {
            this.masterId = masterId;
            this.masterKey = masterKey;
            this.iv = iv;
            this.mac = mac;
            this.keys = keys;
            this.log = log;
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
        final byte keyId;
        final byte[] arc;
        final byte[] authMask;

        public GetTCInfo(/*short sPi, */boolean[] vcId, byte[] primHeader, byte[] secHeader, byte[] data, int dataLength, byte[] secTrailer, byte[] crc, ActorRef<TCProcessor.Command> tcProc, ActorRef<Module.Command> parent, byte keyId, byte[] arc, byte[] authMask/*ActorRef<KeyManager.Command> keyMan*/) {
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
            this.keyId = keyId;
            this.arc = arc;
            this.authMask = authMask;
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
        final byte keyId;
        final ActorRef<TMProcessor.Command> tmProc;
        public GetTMInfo(byte[] frameHeader, byte[] data, byte[] trailer, int channel, short sPi, byte[] arc, byte[] iv, byte[] authMask, byte keyId, ActorRef<TMProcessor.Command> tmProc) {
            this.frameHeader = frameHeader;
            this.data = data;
            this.trailer = trailer;
            this.channel = channel;
            this.sPi = sPi;
            this.arc = arc;
            this.iv = iv;
            this.authMask = authMask;
            this.keyId = keyId;
            this.tmProc = tmProc;
        }
    }

    public static final class KeyRequested implements Command {

    }

    private final int activeKeys;
    private int currentlyActive;

    public static Behavior<Command> create(int activeKeys) {
        return Behaviors.setup(context -> new KeyManager(context, activeKeys));
    }

    private final Map<Byte, ActorRef<Key.Command>> keyIdToActor = new HashMap<>();

    private KeyManager(ActorContext<Command> context, int activeKeys){
        super(context);
        this.activeKeys = activeKeys;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(OTAR.class, this::onOtar)
                .onMessage(ActivateKey.class, this::onActivate)
                .onMessage(ActivateReply.class, this::onActivateReply)
                .onMessage(DeactivateKey.class, this::onDeactivate)
                .onMessage(VerifyKey.class, this::onVerify)
                .onMessage(Rekey.class, this::onRekey)
                .onMessage(KeyRequested.class, this::onRequest)
                .onMessage(KeyInventory.class, this::onInventory)
                .onMessage(KeyDestruction.class, this::onDestruction)
                .onMessage(GetTCInfo.class, this::onTC)
                .onMessage(GetTMInfo.class, this::onTM)
                .build();
    }

    private Behavior<Command> onRequest(KeyRequested k) {
        return this;
    }

    //maybe not log here but send reply to SecurityManager?
    /*private Behavior<Command> onOtar(OTAR o) {
        ActorRef<Key.Command> keyActor = keyIdToActor.get(o.keyId);
        //keyActor with keyId to insert already exists, log accordingly
        if (keyActor != null) {
            byte tag = (byte) 0b00000001;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = o.keyId;
            o.replyTo.tell(new Log.InsertEntry(tag, length, value));
        }
        //create new keyActor, log it
        else {
            keyActor = getContext().spawn(Key.create(o.keyId, o.keyActor, false), "keyActor" + o.keyId);
            keyIdToActor.put(o.keyId, keyActor);
            byte tag = (byte) 0b10000001;
            short length = 1;
            byte[] value = new byte[1];
            value[0] =  o.keyId;
            o.replyTo.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }*/

    private Behavior<Command> onOtar(OTAR o) {
        ActorRef<Key.Command> master = this.keyIdToActor.get(o.masterKey);
        //masterKey does not exist
        if(master == null) {
            //TODO:log
        }
        else {

            //TODO: how to handle otar, maybe don't store keys as actors thus it can be handled in KeyManager?
            master.tell(new Key.GetMaster(o.keys, o.mac, o.iv, o.log, getContext().getSelf()));
        }
        return this;
    }

    private Behavior<Command> onDecOtar(DecOtar d) {

        byte[] plain;
        try {
            plain = decrypt(d.keys, d.masterKey, d.iv, d.mac);
        }
        //TODO: is this an error in decryption? I suppose so
        //TODO: log
        catch (Exception e) {
            plain = new byte[0];
        }
        //decryption worked
        //TODO: assumes decryption returns plain text only
        if(plain.length != 0) {
            for(int i = 0; i < plain.length; i++) {
                byte keyId = plain[i];
                i++;
                byte[] key = new byte[256];
                for(int j = 0; j < 256; j++) {
                    key[j] = plain[i];
                    i++;
                }
                ActorRef<Key.Command> keyActor = this.keyIdToActor.get(keyId);
                //keyActor already exists
                if(keyActor != null) {
                    byte tag = (byte) 0b00000001;
                    short length = 1;
                    byte[] value = new byte[1];
                    value[0] = keyId;
                    d.log.tell(new Log.InsertEntry(tag, length, value));
                }
                //create new keyActor, log it
                else {
                    keyActor = getContext().spawn(Key.create(keyId, key, false), "keyActor" + keyId);
                    this.keyIdToActor.put(keyId, keyActor);
                    byte tag = (byte) 0b10000001;
                    short length = 1;
                    byte[] value = new byte[1];
                    value[0] = keyId;
                    d.log.tell(new Log.InsertEntry(tag, length, value));
                }
            }
        }
        return this;
    }

    //TODO: check if that works, does tag need to be appended to ciphertext?
    public static byte[] decrypt(byte[] keys, byte[] masterKey, byte[] iv, byte[] mac) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKey secretKey = new SecretKeySpec(masterKey, "AES");
        byte[] cipherText = new byte[keys.length + mac.length];
        System.arraycopy(keys, 0, cipherText, 0, keys.length);
        System.arraycopy(mac, 0, cipherText, keys.length, mac.length);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
        return cipher.doFinal(cipherText);
    }

    private Behavior<Command> onActivate(ActivateKey a) {
        ActorRef<Key.Command> keyActor = this.keyIdToActor.get(a.keyId);
        //keyActor to activate does not exist
        if (keyActor == null) {
            byte tag = (byte) 0b00000010;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = a.keyId;
            a.log.tell(new Log.InsertEntry(tag, length, value));
        }
        //already too many keys are active
        else if(this.currentlyActive >= activeKeys) {
            byte tag = (byte) 0b11110010;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = a.keyId;
            a.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            currentlyActive++;
            keyActor.tell(new Key.Activate(a.log, getContext().getSelf()));
        }
        return this;
    }

    //key was not activated as it was in the wrong state before
    private Behavior<Command> onActivateReply(ActivateReply a) {
        this.currentlyActive--;
        return this;
    }

    private Behavior<Command> onDeactivate(DeactivateKey k) {
        ActorRef<Key.Command> keyActor =this.keyIdToActor.get(k.keyId);
        //keyActor to deactivate does not exist
        if(keyActor == null) {
            byte tag = (byte) 0b00000010;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = k.keyId;
            k.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            keyActor.tell(new Key.Deactivate(k.log));
        }
        return this;
    }



    private Behavior<Command> onRekey(Rekey r) {
        ActorRef<Key.Command> keyActor = keyIdToActor.get(r.keyId);
        //keyActor to rekey with does not exist
        if(keyActor == null) {
            byte tag = (byte) 0b00010110;
            short length = 2;
            byte[] value = new byte[3];
            value[0] = (byte)(r.sPi & 0xff);
            value[1] = (byte) ((r.sPi >> 8) & 0xff);
            value[2] = r.keyId;
            r.log.tell(new Log.InsertEntry(tag, length, value));
        }
        else {
            keyActor.tell(new Key.CheckRekey(r.sPi, r.arc, r.iv, r.log, r.sam));
        }
        return this;
    }

    private Behavior<Command> onInventory(KeyInventory k){
        ActorRef<Key.Command> keyActor = this.keyIdToActor.get(k.currKey);
        //k.currKey++;
        //int i = k.currKey + 1;
        while(keyActor == null && k.currKey <= k.lastKey) {
            k.currKey++;
            keyActor = this.keyIdToActor.get(k.currKey);
        }
        if(keyActor == null){
            byte tag = (byte) 0b00000111;
            short length = (short) ((k.number * 2) + 2);
            byte[] value = new byte[length];
            value[0] = (byte) (k.number & 0xff);
            value[1] = (byte) ((k.number >> 8) &0xff);
            Iterator it = k.keyIdToState.entrySet().iterator();
            int i = 2;
            while (it.hasNext()) {
                Map.Entry<Byte, KeyState> pair = (Map.Entry) it.next();
                value[i] = pair.getKey();
                i++;
                value[i] = pair.getValue().toByte();
                i++;
            }
            k.log.tell(new Log.InsertEntry(tag, length, value));
            k.pum.tell(new PDUManager.KeyInventoryReply(k.number, k.keyIdToState, k.secMan));
        }
        //found a keyActor to check
        else {
            k.number ++;
            keyActor.tell(new Key.KeyInventory(k.number, k.firstKey, k.lastKey, k.currKey, k.keyIdToState, k.pum, k.secMan, k.log, getContext().getSelf()));
        }
        return this;
    }

    /*private Behavior<Command> onVerify(VerifyKey v) {
        ActorRef<Key.Command> keyActor = this.keyIdToActor.get(v.keyId);
        //keyActor does not exist
        if(keyActor == null) {
            byte tag = (byte) 0b00000100;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = v.keyId;
            v.log.tell(new Log.InsertEntry(tag, length , value));
        }
        else {
            keyActor.tell(new Key.Verify(v.challenge, v.log, v.replyTo));
        }
        return this;
    }*/

    private Behavior<Command> onVerify(VerifyKey v) {
        //no keys left to verify
        if(v.keyToChallenge.isEmpty()) {
            //TODO
        }
        //verify next key
        else {
            Pair<Byte, byte[]> pair = v.keyToChallenge.get(0);
            ActorRef<Key.Command> keyActor = this.keyIdToActor.get(pair.first());
            //key to verify not found, don't continue, log error
            //TODO: ask if others shall be verified
            if(keyActor == null) {
                //TODO
            }
            else {
                keyActor.tell(new Key.Verify(pair.second(), v.log, v.replyTo, v.keyToChallenge, v.reply, getContext().getSelf()));
            }
        }
        return this;
    }

    private Behavior<Command> onDestruction(KeyDestruction k) {
        ActorRef<Key.Command> keyActor = this.keyIdToActor.get(k.keyId);
        //keyActor to erase does not exist
        if (keyActor == null) {
            byte tag = (byte) 0b00000110;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = k.keyId;
            k.log.tell(new Log.InsertEntry(tag, length, value));
        } else {
            getContext().stop(keyActor);
            this.keyIdToActor.remove(k.keyId);
            byte tag = (byte) 0b10000110;
            short length = 1;
            byte[] value = new byte[1];
            value[0] = k.keyId;
            k.log.tell(new Log.InsertEntry(tag, length, value));
        }
        return this;
    }

    private Behavior<Command> onTC(GetTCInfo tc) {
        ActorRef<Key.Command> keyActor = this.keyIdToActor.get(tc.keyId);
        //keyActor does not exist
        if(keyActor == null) {
            //TODO
        }
        else {
            keyActor.tell(new Key.GetTCInfo(tc.vcId, tc.primHeader, tc.secHeader, tc.data, tc.dataLength, tc.secTrailer, tc.crc, tc.tcProc, tc.parent, tc.arc, tc.authMask));
        }
        return this;
    }

    private Behavior<Command> onTM(GetTMInfo tm) {
        ActorRef<Key.Command> keyActor = this.keyIdToActor.get(tm.keyId);
        //keyActor does not exist
        if(keyActor == null) {
            //TODO
        }
        else {
            keyActor.tell(new Key.GetTMInfo(tm.frameHeader, tm.data, tm.trailer, tm.channel, tm.sPi, tm.arc, tm.iv, tm.authMask, tm.tmProc));
        }
        return this;
    }
}
