package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.ActorContext;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PDUManager extends AbstractBehavior<PDUManager.Command> {

    public interface Command {}

    //SecurityManager needs to pass reference to KeyManager when issuing otar to PDUManager
    //TODO: store keys encrypted, fix key length
    public static final class Otar implements Command{
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

    //should work
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

    //should work
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

    //TODO
    //wtf do i get here exactly, needs to be replied to
    public static final class KeyVerification implements Command{
        final byte[] value;
        final ActorRef<KeyManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public KeyVerification (byte[] value, ActorRef<KeyManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
        }
    }

    //reply required
    //i think it works?
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

    //TODO
    //value contains only one SPI but might contain multiple GVC/GMAP ids to add to SA, how is a channel not applicable to SA?
    public static final class StartSA implements Command{
        final byte[] value;
        final ActorRef<SAManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public StartSA (byte[] value, ActorRef<SAManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.value = value;
            this.replyTo = replyTo;
            this.log = log;
        }

    }

    //should work
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

    //should work
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

    //should work
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

    //should work
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

    //should work
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

    //should work, TODO: reply
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

    //should work
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

    //should work
    public static final class Ping implements Command {

        //value is empty for ping, maybe not pass it
        final byte[] value;
        final ActorRef<SecurityManager.Command> replyTo;

        public Ping(byte[] value, ActorRef<SecurityManager.Command> replyTo) {
            this.value = value;
            this.replyTo = replyTo;
        }

    }

    //should work
    public static final class DumpLog implements Command {
        final ActorRef<SecurityManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public DumpLog(ActorRef<SecurityManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.replyTo = replyTo;
            this.log = log;
        }

    }

    //should work
    public static final class EraseLog implements Command {
        final ActorRef<SecurityManager.Command> replyTo;
        final ActorRef<Log.Command> log;

        public EraseLog(ActorRef<SecurityManager.Command> replyTo, ActorRef<Log.Command> log) {
            this.replyTo = replyTo;
            this.log = log;
        }

    }

    //TODO
    //refers to FSR, which is handled by frame processing, maybe just pass back to SecurityManager
    public static final class AlarmFlagReset implements Command {

    }

    //TODO
    public static final class KeyVerificationReply implements Command {

    }

    //TODO: in onReply method (encode keyState)
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

    //TODO
    public static final class SAStatusRequestReply implements Command {
        final SAState[] trans;
        final short sPi;
        final ActorRef<SecurityManager.Command> replyTo;

        public SAStatusRequestReply(short sPi, SAState[] trans, ActorRef<SecurityManager.Command> replyTo) {
            this.trans = trans;
            this.sPi = sPi;
            this.replyTo = replyTo;
        }

    }
    //TODO: check if it works
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
                .onMessage(StartSA.class, this::onStartSA)
                .onMessage(StopSA.class, this::onStopSA)
                .onMessage(RekeySA.class, this::onRekey)
                .onMessage(ExpireSA.class, this::onExpire)
                .onMessage(SetARSN.class, this::onSetARSN)
                .onMessage(SetARSNWindow.class, this::onSetARSNWindow)
                .onMessage(SAStatusRequest.class, this::onStatusRequest)
                .onMessage(SAStatusRequestReply.class, this::onStatusReply)
                .onMessage(ReadARSN.class, this::onReadARSN)
                .onMessage(ReadARSNReply.class, this::onReadARSNReply)
                .onMessage(Ping.class, this::onPing)
                .onMessage(KeyInventory.class, this::onInventory)
                .onMessage(KeyInventoryReply.class, this::onInventoryReply)
                .onMessage(DumpLog.class, this::onDumpLog)
                .onMessage(EraseLog.class, this::onEraseLog)
                .onMessage(DumpLogReply.class, this::onDumpLogReply)
                .onMessage(EraseLogReply.class, this::onEraseLogReply)
                .build();
    }

    private Behavior<Command> onOtar(Otar o) {
        for (int i = 0; i < o.value.length; i = i + 3) {
            byte keyId = o.value[i];
            byte[] key = new byte[2];
            key[0] = o.value[i + 1];
            key[1] = o.value[i + 2];
            o.replyTo.tell(new KeyManager.OTAR(keyId, key, o.log));
        }
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
        byte[] sPi = new byte[2];
        sPi[0] = s.value[0];
        sPi[1] = s.value[1];

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
        byte keyId = r.value[2];
        byte[] arc = new byte[4];
        byte[] iv = new byte[12];
        int j = 0;
        for (int i = 3; i < 7; i++) {
            arc[j] = r.value[i];
            j++;
        }
        j = 0;
        for (int i = 7; i < r.value.length; i++){
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
        for (int i = 3; i < 7; i++) {
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
        //how long is ARSNWindow? now:long
        ByteBuffer bb2 = ByteBuffer.allocate(8);
        for(int i = 0; i < 8; i++) {
            bb2.put(s.value[i+2]);
        }
        long arcWindow = bb2.getLong(0);
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
        //TODO: how to encode SA state transition?
        //TODO reply to SecurityManagaer (s.replyTo)
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
        //TODO find correct tag and how to encode it
        byte tag = (byte) 0b10010000;
        short length = 6;
        //2 byte spi, 4 byte arc
        byte[] value = new byte[6];
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
        Map<Byte, KeyState> reply = new HashMap<>();
        k.keyMan.tell(new KeyManager.KeyInventory(firstKey, lastKey, k.log, getContext().getSelf(), k.secMan, (short) 0, reply, firstKey));
        return this;
    }

    private Behavior<Command> onInventoryReply(KeyInventoryReply k) {
        byte tag = (byte) 0b10000111;
        //length is size of map times 1 (for keyId) + (number of bytes required for keyState (here assuming 1) + 2(for number of keys field)
        short length = (short) ((k.keyIdToState.size() * 2) + 2);
        byte[] value = new byte[length];
        value[0] = (byte) (k.number & 0xff);
        value[1] = (byte) ((k.number >> 8) & 0xff);
        int j = 0;
        Iterator it = k.keyIdToState.entrySet().iterator();
        //TODO: think about sense of the for-loop
        for (int i = 2; i < (2 * k.keyIdToState.size()) && it.hasNext(); i = i+2)
        {
            Map.Entry<Byte, KeyState> pair = (Map.Entry) it.next();
            value[i] = pair.getKey();
            //TODO: encode right keyState as byte
            value[i+1] = 0x00;
        }
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
        byte[] reply = new byte[d.value.length + 3];
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
}
