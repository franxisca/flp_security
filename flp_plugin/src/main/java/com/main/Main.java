package com.main;

/*import akka.actor.ActorRef;
import akka.actor.ActorSystem;*/
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args){
        final ActorSystem<GuardianActor.Command> mainActor = ActorSystem.create(GuardianActor.create(), "guardian actor");
        int active = 50;
        Map<Byte, byte[]> defKeys = new HashMap<>();
        Map<Byte, byte[]> sessionKeys = new HashMap<>();
        Map<Integer, Short> vcToSA = new HashMap<>();
        Map<Short, Byte> criticalSA = new HashMap<>();
        List<Short> standardSA = new LinkedList<>();
        mainActor.tell(new GuardianActor.Start(active, defKeys, sessionKeys, vcToSA, criticalSA, standardSA));
        //ActorSystem.create(GuardianActor.create(), "guardian actor");
        //ActorRef<GuardianActor.Command> mainActor = ActorSystem.create(GuardianActor.create(), "guardian actor");
    }
}
