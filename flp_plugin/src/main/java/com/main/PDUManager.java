package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.ActorContext;
import akka.japi.Pair;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PDUManager extends AbstractBehavior<PDUManager.Command> {

    public interface Command {}

    private static final int IV_LENGTH = 12;

    //works
    public static final class KeyDestruction implements Command {

       final byte[] value;
       //final ActorRef<PDUManager.Command> replyTo;
       final ActorRef<KeyManager.Command> keyMan;
       final ActorRef<Log.Command> log;
       public KeyDestruction (byte[] value, ActorRef<KeyManager.Command> keyMan, ActorRef<Log.Command> log) {
           this.value = value;
           this.keyMan = keyMan;
           this.log = log;
          // this.replyTo = replyTo;
       }
    }

    //SecurityManager needs to pass reference to KeyManager when issuing otar to PDUManager
    //should work
    public static final class Otar implements Command {
        //final byte[] length;
        final byte[] value;
        final ActorRef<KeyManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public Otar (byte[] value, ActorRef<KeyManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
        }
    }

    //works
    public static final class KeyActivation implements Command {
        final byte[] value;
        final ActorRef<KeyManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public KeyActivation(byte[] value, ActorRef<KeyManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
        }
    }

    //works
    public static final class KeyDeactivation implements Command {
        final byte[] value;
        final ActorRef<KeyManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public KeyDeactivation(byte[] value, ActorRef<KeyManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
        }
    }

    //should work
    //return is according to standard not to specification
    public static final class KeyVerification implements Command{
        final byte[] value;
        final ActorRef<KeyManager.Command> replyTo;
        final ActorRef<Log.Command> log;
        final ActorRef<SecurityManager.Command> secMan;

        public KeyVerification (byte[] value, ActorRef<KeyManager.Command> replyTo, ActorRef<Log.Command> log, ActorRef<SecurityManager.Command> secMan) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
            this.secMan = secMan;
        }
    }

    //reply required
    //works
    public static final class KeyInventory implements Command {
        final byte[] value;
        final ActorRef<KeyManager.Command> keyMan;
        final ActorRef<SecurityManager.Command> secMan;
        final ActorRef<Log.Command> log;

        public KeyInventory(byte[] value, ActorRef<KeyManager.Command> keyMan, ActorRef<SecurityManager.Command> secMan, ActorRef<Log.Command> log) {
            this.value = value;
            this.keyMan = keyMan;
            this.secMan = secMan;
            this.log = log;
        }

    }

    //value contains only one SPI but might contain multiple GVC/GMAP ids to add to SA, how is a channel not applicable to SA?
    //ignoring that maybe not applicable
    //channel ids have 32 bit (size of int)
    //works
    public static final class StartSA implements Command{
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;
        final ActorRef<Module.Command> module;


        public StartSA (byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log, ActorRef<Module.Command> module) {
            this.value = value;
            this.sam = sam;
            this.log = log;
            this.module = module;
        }

    }

    //works
    public static final class StopSA implements Command {
        final byte[] value;
        final ActorRef<SAManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public StopSA (byte[] value, ActorRef<SAManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
        }

    }

    //works
    //the same for critical and standard SAs
    public static final class RekeySA implements Command{
        final byte[] value;
        final ActorRef<KeyManager.Command> replyTo;
        final ActorRef<Log.Command> log;
        final ActorRef<SAManager.Command> sam;

        public RekeySA(byte[] value, ActorRef<KeyManager.Command> replyTo, ActorRef<Log.Command> log, ActorRef<SAManager.Command> sam){
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
            this.sam = sam;
        }

    }

    //works
    //the same for critical and standard SAs
    public static final class ExpireSA implements Command{
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;

        public ExpireSA(byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log) {
            this.value = value;
            this.sam = sam;
            this.log = log;
        }
    }

    //works
    public static final class SetARSN implements Command{
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;

        public SetARSN(byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log) {
            this.value = value;
            this.sam = sam;
            this.log = log;
        }

    }

    //works
    public static final class SetARSNWindow implements Command {
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;

        public SetARSNWindow (byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log) {
            this.value = value;
            this.sam = sam;
            this.log = log;
        }

    }

    //works
    public static final class SAStatusRequest implements Command {
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;
        final ActorRef<SecurityManager.Command> replyTo;

        public SAStatusRequest(byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log, ActorRef<SecurityManager.Command> replyTo) {

            this.value = value;
            this.sam = sam;
            this.log = log;
            this.replyTo = replyTo;
        }

    }

    //works
    public static final class ReadARSN implements Command {
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;
        final ActorRef<SecurityManager.Command> replyTo;

        public ReadARSN(byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log, ActorRef<SecurityManager.Command> replyTo) {
            this.value = value;
            this.sam = sam;
            this.log = log;
            this.replyTo = replyTo;
        }

    }

    //works
    public static final class ReadARSNWindow implements Command {
        final byte[] value;
        final ActorRef<SAManager.Command> sam;
        final ActorRef<Log.Command> log;
        final ActorRef<SecurityManager.Command> replyTo;

        public ReadARSNWindow(byte[] value, ActorRef<SAManager.Command> sam, ActorRef<Log.Command> log, ActorRef<SecurityManager.Command> replyTo) {
            this.value = value;
            this.sam = sam;
            this.log = log;
            this.replyTo = replyTo;
        }
    }

    //works
    public static final class Ping implements Command {

        //value is empty for ping, maybe not pass it
        final byte[] value;
        final ActorRef<SecurityManager.Command> replyTo;

        public Ping(byte[] value, ActorRef<SecurityManager.Command> replyTo) {
            this.value = value;
            this.replyTo = replyTo;
        }

    }

    //works
    public static final class DumpLog implements Command {
        final ActorRef<SecurityManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public DumpLog(ActorRef<SecurityManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.replyTo = replyTo;
            this.log = log;
        }

    }

    //works
    public static final class EraseLog implements Command {
        final ActorRef<SecurityManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public EraseLog(ActorRef<SecurityManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.replyTo = replyTo;
            this.log = log;
        }

    }



    public static final class KeyVerificationReply implements Command {
        final Map<Byte, byte[]> reply;
        final ActorRef<SecurityManager.Command> replyTo;

        public KeyVerificationReply(Map<Byte, byte[]> reply, ActorRef<SecurityManager.Command> replyTo) {
            this.reply = reply;
            this.replyTo = replyTo;
        }
    }

    //should work
    public static final class KeyInventoryReply implements Command {
        final short number;
        final Map<Byte, KeyState> keyIdToState;
        final ActorRef<SecurityManager.Command> replyTo;

        public KeyInventoryReply(short number, Map<Byte, KeyState> keyIdToState, ActorRef<SecurityManager.Command> replyTo) {
            this.number = number;
            this.keyIdToState = keyIdToState;
            this.replyTo = replyTo;
        }

    }

    //should work
    public static final class SAStatusRequestReply implements Command {
        final byte trans;
        final short sPi;
        final ActorRef<SecurityManager.Command> replyTo;

        public SAStatusRequestReply(short sPi, byte trans, ActorRef<SecurityManager.Command> replyTo) {
            this.trans = trans;
            this.sPi = sPi;
            this.replyTo = replyTo;
        }

    }
    //should work
    public static final class ReadARSNReply implements Command {
        final short sPi;
        final byte[] arc;
        final ActorRef<SecurityManager.Command> replyTo;
        public ReadARSNReply (short sPi, byte[] arc, ActorRef<SecurityManager.Command> replyTo) {
            this.sPi = sPi;
            this.arc = arc;
            this.replyTo = replyTo;
        }

    }

    public static final class ReadARSNWindowReply implements Command {
        final short sPi;
        final int arcWindow;
        final ActorRef<SecurityManager.Command> replyTo;

        public ReadARSNWindowReply(short sPi, int arcWindow, ActorRef<SecurityManager.Command> replyTo) {
            this.sPi = sPi;
            this.arcWindow = arcWindow;
            this.replyTo = replyTo;
        }
    }

    //ping can be directly answered
    /*public static final class PingReply {

    }*/

    //should work
    public static final class DumpLogReply implements Command{
        final byte[] value;
        final ActorRef<SecurityManager.Command> replyTo;
        public DumpLogReply(byte[] value, ActorRef<SecurityManager.Command> replyTo) {
            this.value = value;
            this.replyTo = replyTo;
        }

    }

    //should work
    public static final class EraseLogReply implements Command {
        final int number;
        final byte rem;
        final ActorRef<SecurityManager.Command> replyTo;

        public EraseLogReply(int number, byte rem, ActorRef<SecurityManager.Command> replyTo) {
            this.number = number;
            this.rem = rem;
            this.replyTo = replyTo;
        }

    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> new PDUManager(context));
    }

    private PDUManager(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Otar.class, this::onOtar)
                .onMessage(KeyDeactivation.class, this::onDeactivate)
                .onMessage(KeyActivation.class, this::onActivate)
                .onMessage(KeyVerification.class, this::onVerification)
                .onMessage(StartSA.class, this::onStartSA)
                .onMessage(StopSA.class, this::onStopSA)
                .onMessage(RekeySA.class, this::onRekey)
                .onMessage(ExpireSA.class, this::onExpire)
                .onMessage(SetARSN.class, this::onSetARSN)
                .onMessage(SetARSNWindow.class, this::onSetARSNWindow)
                .onMessage(SAStatusRequest.class, this::onStatusRequest)
                .onMessage(SAStatusRequestReply.class, this::onStatusReply)
                .onMessage(ReadARSN.class, this::onReadARSN)
                .onMessage(ReadARSNWindow.class, this::onReadARSNWindow)
                .onMessage(ReadARSNWindowReply.class, this::onReadARSNWindowReply)
                .onMessage(ReadARSNReply.class, this::onReadARSNReply)
                .onMessage(Ping.class, this::onPing)
                .onMessage(KeyInventory.class, this::onInventory)
                .onMessage(KeyInventoryReply.class, this::onInventoryReply)
                .onMessage(DumpLog.class, this::onDumpLog)
                .onMessage(EraseLog.class, this::onEraseLog)
                .onMessage(DumpLogReply.class, this::onDumpLogReply)
                .onMessage(EraseLogReply.class, this::onEraseLogReply)
                .onMessage(KeyDestruction.class, this::onDestruction)
                .onMessage(KeyVerificationReply.class, this::onVerifyReply)
                .build();
    }

    /*private Behavior<Command> onOtar(Otar o) {
        for (int i = 0; i < o.value.length; i = i + 3) {
            byte keyId = o.value[i];
            byte[] keyActor = new byte[32];
            for (int j = 0; j < 32; j++) {
                keyActor[]
            }
            keyActor[0] = o.value[i + 1];
            keyActor[1] = o.value[i + 2];
            o.replyTo.tell(new KeyManager.OTAR(keyId, keyActor, o.log));
        }
        return this;
    }*/

    //should work
    private Behavior<Command> onOtar(Otar o) {
        //TODO: what exactly is to do here?
        byte masterKey = o.value[0];
        byte[] iv = new byte[IV_LENGTH];
        for (int i = 0; i < iv.length; i++) {
            iv[i] = o.value[i+1];
        }
        byte[] cipherText = new byte[o.value.length - (iv.length + 1)];
        System.arraycopy(o.value, (IV_LENGTH + 1), cipherText, 0, cipherText.length);
        o.replyTo.tell(new KeyManager.OTAR(masterKey, iv,cipherText, o.log));
        return this;
    }

    private Behavior<Command> onActivate(KeyActivation k) {
        for (int i = 0; i < k.value.length; i++) {
            byte keyId = k.value[i];
            k.replyTo.tell(new KeyManager.ActivateKey(keyId, k.log));
        }
        return this;
    }

    private Behavior<Command> onDeactivate(KeyDeactivation k) {
        for (int i = 0; i < k.value.length; i++) {
            byte keyId = k.value[i];
            k.replyTo.tell(new KeyManager.DeactivateKey(keyId, k.log));
        }

        return this;
    }



    private Behavior<Command> onStartSA(StartSA s) {
        ByteBuffer bb2 = ByteBuffer.allocate(2);
        bb2.put(s.value[0]);
        bb2.put(s.value[1]);
        short sPi = bb2.getShort(0);
        for(int i = 2; i < s.value.length; i = i + 4) {
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.put(s.value[i]);
            bb.put(s.value[i+1]);
            bb.put(s.value[i+2]);
            bb.put(s.value[i+3]);
            int channel = bb.getInt(0);
            s.sam.tell(new SAManager.Start(sPi, channel, s.log, s.module));
        }

        return this;
    }

    private Behavior<Command> onStopSA(StopSA s) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(s.value[0]);
        bb.put(s.value[1]);
        short sPi = bb.getShort(0);
        s.replyTo.tell(new SAManager.Stop(sPi, s.log));
        return this;
    }

    private Behavior<Command> onRekey(RekeySA r) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(r.value[0]);
        bb.put(r.value[1]);
        short sPi = bb.getShort(0);
        /*System.out.println("test spi on rekey");
        System.out.println(sPi);*/
        byte keyId = r.value[2];
        byte redundant = r.value[3];
        byte[] arc = new byte[4];
        byte[] iv = new byte[12];
        int j = 0;
        for (int i = 4; i < 8; i++) {
            arc[j] = r.value[i];
            j++;
        }
        j = 0;
        for (int i = 8; i < r.value.length; i++){
            iv[j] = r.value[i];
            j++;
        }
        //arc is reset to 0 on each rekey, do we actually update iv?
        r.replyTo.tell(new KeyManager.Rekey(sPi, keyId, arc, iv, r.log, r.sam));
        return this;
    }

    private Behavior<Command> onExpire(ExpireSA e) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(e.value[0]);
        bb.put(e.value[1]);
        short sPi = bb.getShort(0);
        e.sam.tell(new SAManager.Expire(sPi, e.log));
        return this;
    }

    private Behavior<Command> onSetARSN(SetARSN s) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(s.value[0]);
        bb.put(s.value[1]);
        short sPi = bb.getShort(0);
        byte[] arc = new byte[4];
        int j = 0;
        for (int i = 2; i < 6; i++) {
            arc[j] = s.value[i];
            j++;
        }
        s.sam.tell(new SAManager.SetARSN(sPi, arc, s.log));
        return this;
    }

    private Behavior<Command> onSetARSNWindow(SetARSNWindow s) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(s.value[0]);
        bb.put(s.value[1]);
        short sPi = bb.getShort(0);
        //how long is ARSNWindow? now:int
        ByteBuffer bb2 = ByteBuffer.allocate(4);
        for(int i = 0; i < 4; i++) {
            bb2.put(s.value[i+2]);
        }
        int arcWindow = bb2.getInt(0);
        s.sam.tell(new SAManager.SetARSNWindow(sPi, arcWindow, s.log));
        return this;
    }
    //requires reply, will be sent back to pDUManager
    private Behavior<Command> onStatusRequest(SAStatusRequest s) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(s.value[0]);
        bb.put(s.value[1]);
        short sPi = bb.getShort(0);
        s.sam.tell(new SAManager.StatusRequest(sPi, s.log, getContext().getSelf(), s.replyTo));
        return this;
    }

    private Behavior<Command> onStatusReply(SAStatusRequestReply s) {
        //reply PDU has tag 10011111, length, spi, one byte state transition
        byte tag = (byte) 0b10011111;
        short length = 3;
        byte[] value = new byte[3];
        value[1] = (byte) (s.sPi & 0xff);
        value[0] = (byte) ((s.sPi >> 8) & 0xff);
        value[2] = s.trans;
        s.replyTo.tell(new SecurityManager.PDUReply(tag, length, value));
        return this;
    }

    private Behavior<Command> onReadARSN(ReadARSN r) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(r.value[0]);
        bb.put(r.value[1]);
        short sPi = bb.getShort(0);
        r.sam.tell(new SAManager.ReadARSN(sPi, r.log, getContext().getSelf(), r.replyTo));
        return this;
    }

    private Behavior<Command> onReadARSNReply(ReadARSNReply r) {
        byte tag = (byte) 0b10010000;
        short length = 6;
        //2 byte spi, 4 byte arc
        byte[] value = new byte[6];
        value[1] = (byte) (r.sPi & 0xff);
        value[0] = (byte) ((r.sPi >> 8) & 0xff);
        System.arraycopy(r.arc, 0, value, 2, 4);
        r.replyTo.tell(new SecurityManager.PDUReply(tag, length, value));
        return this;
    }

    private Behavior<Command> onReadARSNWindow(ReadARSNWindow r) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.put(r.value[0]);
        bb.put(r.value[1]);
        short sPi = bb.getShort(0);
        r.sam.tell(new SAManager.ReadARSNWindow(sPi, r.log, getContext().getSelf(), r.replyTo));
        return this;
    }

    private Behavior<Command> onReadARSNWindowReply(ReadARSNWindowReply r) {
        byte tag = (byte) 0b11010000;
        short length = 6;
        //2 byte sPi, 4 byte arcWindow
        byte[] value = new byte[6];
        value[1] = (byte) (r.sPi & 0xff);
        value[0] = (byte) ((r.sPi >> 8) & 0xff);
        byte[] bytes = ByteBuffer.allocate(4).putInt(r.arcWindow).array();
        System.arraycopy(bytes, 0, value, 2, 4);
        r.replyTo.tell(new SecurityManager.PDUReply(tag, length, value));
        return this;
    }

    private Behavior<Command> onPing(Ping p) {
        byte tag = (byte) 0b10110001;
        short length = 0;
        p.replyTo.tell(new SecurityManager.PDUReply(tag, length, p.value));
        return this;
    }

    private Behavior<Command> onInventory(KeyInventory k){
        byte firstKey = k.value[0];
        byte lastKey = k.value[1];
        System.out.println("test key inventory values");
        System.out.println(firstKey);
        System.out.println(lastKey);
        Map<Byte, KeyState> reply = new HashMap<>();
        k.keyMan.tell(new KeyManager.KeyInventory(firstKey, lastKey, k.log, getContext().getSelf(), k.secMan, (short) 0, reply, firstKey));
        return this;
    }

    //assumes challenge is 2 bytes long
    private Behavior<Command> onVerification(KeyVerification k) {
        Map<Byte, byte[]> reply = new HashMap<>();
        //Map<Byte, byte[]> keyToChallenge = new HashMap<>();
        ArrayList<Pair<Byte, byte[]>> keyToChallenge = new ArrayList<>();
        byte[] challenge = new byte[2];
        for(int i = 0; i < k.value.length; i++) {
            byte keyId = k.value[i];
            i++;
            for (int j = 0; j < 2; j++) {
                challenge[j] = k.value[i];
                i++;
            }
            //keyToChallenge.put(keyId, challenge);
            keyToChallenge.add(new Pair<>(keyId, challenge));
            //k.replyTo.tell(new KeyManager.VerifyKey(keyId, challenge, k.log, getContext().getSelf()));
        }

        k.replyTo.tell(new KeyManager.VerifyKey(keyToChallenge, k.log, getContext().getSelf(), reply, k.secMan));
        return this;
    }

    private Behavior<Command> onInventoryReply(KeyInventoryReply k) {
        byte tag = (byte) 0b10000111;
        //length is size of map times 1 (for keyId) + (number of bytes required for keyState (here assuming 1) + 2(for number of keys field)
        short length = (short) ((k.keyIdToState.size() * 2) + 2);
        byte[] value = new byte[length];
        value[1] = (byte) (k.number & 0xff);
        value[0] = (byte) ((k.number >> 8) & 0xff);
        int j = 2;
        //Iterator it = k.keyIdToState.entrySet().iterator();
        //if something doesn't work check condition in for loop
        for(Map.Entry<Byte, KeyState> entry : k.keyIdToState.entrySet()) {
            value[j] = entry.getKey();
            j++;
            value[j] = entry.getValue().toByte();
            j++;
        }
        /*for (int i = 2; i < (2 * k.keyIdToState.size() + 2) && it.hasNext(); i = i+2)
        {
            Map.Entry<Byte, KeyState> pair = (Map.Entry) it.next();
            value[i] = pair.getKey();
            value[i+1] = pair.getValue().toByte();
        }*/
        k.replyTo.tell(new SecurityManager.PDUReply(tag, length, value));
        return this;
    }

    private Behavior<Command> onDumpLog(DumpLog d) {
        d.log.tell(new Log.DumpLog(getContext().getSelf(), d.replyTo));
        return this;
    }

    private Behavior<Command> onEraseLog(EraseLog e) {
        e.log.tell(new Log.EraseLog(getContext().getSelf(), e.replyTo));
        return this;
    }

    private Behavior<Command> onDumpLogReply(DumpLogReply d) {
        //byte[] reply = new byte[d.value.length + 3];
        byte tag = (byte) 0b10110011;
        short length = (short) d.value.length;
        /*reply[0] = tag;
        reply[1] = (byte) (length & 0xff);
        reply[2] = (byte) ((length >> 8) & 0xff);
        System.arraycopy(d.value, 0, reply, 3, d.value.length);*/
        d.replyTo.tell(new SecurityManager.PDUReply(tag, length, d.value));
        return this;
    }

    private Behavior<Command> onEraseLogReply(EraseLogReply e) {
        byte tag = (byte) 0b10110100;
        short length = 5;
        byte[] value = ByteBuffer.allocate(5).putInt(e.number).put(e.rem).array();
        e.replyTo.tell(new SecurityManager.PDUReply(tag, length, value));
        return this;
    }

    private Behavior<Command> onDestruction (KeyDestruction k ) {
        for (int i = 0; i < k.value.length; i++) {
            byte keyId = k.value[i];
            k.keyMan.tell(new KeyManager.KeyDestruction(keyId, k.log));
        }
        return this;
    }

    private Behavior<Command> onVerifyReply(KeyVerificationReply k) {
        byte tag = (byte) 0b10000100;
        short length = 0;
        for(Map.Entry<Byte, byte[]> entry : k.reply.entrySet()) {
            length++;
            length = (short) (length + entry.getValue().length);
            length = (short) (length + IV_LENGTH);
        }
        ArrayList<Byte> temp = new ArrayList<>();
        byte[] value = new byte[length];
        for(Map.Entry<Byte, byte[]> entry : k.reply.entrySet()) {
            temp.add(entry.getKey());
            for(int i = 0; i < IV_LENGTH; i++) {
                byte zero = 0;
                temp.add(zero);
            }
            for(int i = 0; i < entry.getValue().length; i++) {
                temp.add(entry.getValue()[i]);
            }
        }
        for(int j = 0; j < length; j++) {
            value[j] = temp.get(j);
        }
        k.replyTo.tell(new SecurityManager.PDUReply(tag, length, value));
        return this;
    }
}
