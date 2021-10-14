package com.main;

import akka.actor.typed.ActorSystem;

import java.io.IOException;
import java.io.InputStream;

public class TMThread extends Thread {
    private final InputStream in;
    private final ActorSystem<GuardianActor.Command> mainActor;

    public TMThread(InputStream in, ActorSystem<GuardianActor.Command> mainActor) {
        this.in = in;
        this.mainActor = mainActor;
    }

    public void run() {
        while (true) {
            try {
                int data = this.in.read();
                if(data != -1) {
                    Main.testTM(this.mainActor);
                    Thread.sleep(5000);
                }
            }
            catch (IOException e) {
                System.out.println("Could not read from TM Input Stream...");
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
