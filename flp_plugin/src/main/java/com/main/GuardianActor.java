package com.main;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.List;
import java.util.Map;

public class GuardianActor extends AbstractBehavior<GuardianActor.Command> {

    public interface Command {}

    public static final class Start implements Command {
        //TODO: more initial parameters?
        final int active1;
        final int active2;
        //TODO: do I need differentiation between master and session?
        //lets just do it
        final Map<Byte, byte[]> masterKeys1;
        final Map<Byte, byte[]> masterKeys2;
        final Map<Byte, byte[]> sessionKeys1;
        final Map<Byte, byte[]> sessionKeys2;
        final Map<Integer, Short> vcToDefaultSA1;
        final Map<Integer, Short> vcToDefaultSA2;
        //final Map<Short, Boolean> saToCritical1;
        final Map<Short, Byte> crticalSA1;
        final Map<Short, Byte> criticalSA2;
        final List<Short> standardSA1;
        final List<Short> standardSA2;
        //final Map<Short, Boolean> saToCritical2;


        public Start(int active1, int active2, Map<Byte, byte[]> defKeys1, Map<Byte, byte[]> defKeys2, Map<Byte, byte[]> sessionKeys1, Map<Byte, byte[]> sessionKeys2, Map<Integer, Short> vcToDefaultSA1, Map<Integer, Short> vcToDefaultSA2, Map<Short, Byte> criticalSA1, Map<Short, Byte> criticalSA2, List<Short> standardSA1, List<Short> standardSA2) {
            this.active1 = active1;
            this.active2 = active2;
            this.masterKeys1 = defKeys1;
            this.masterKeys2 = defKeys2;
            this.vcToDefaultSA1 = vcToDefaultSA1;
            this.vcToDefaultSA2 = vcToDefaultSA2;
            this.crticalSA1 = criticalSA1;
            this.criticalSA2 = criticalSA2;
            this.standardSA1 = standardSA1;
            this.standardSA2 = standardSA2;
            this.sessionKeys1 = sessionKeys1;
            this.sessionKeys2 = sessionKeys2;
        }
    }

    static Behavior<Command> create() {
        return Behaviors.setup(GuardianActor::new);
    }

    private GuardianActor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Start.class, this::onStart)
                .build();
    }

    private Behavior<Command> onStart(Start s) {
        //TODO
        ActorRef<Module.Command> module1 = getContext().spawn(Module.create(null, s.active1, null, null, getContext().getSelf()), "module-1");
        module1.tell(new Module.InitSA(s.criticalSA2, s.standardSA1));
        module1.tell(new Module.DefaultSA(s.vcToDefaultSA1));
        module1.tell(new Module.InitKey(s.masterKeys1, s.sessionKeys1));
        return this;
    }
}
