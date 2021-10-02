package com.main;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class TCOutstream extends AbstractBehavior<TCOutstream.Command> {

    public interface Command {}

    public static final class TC implements Command {
        final byte[] tc;
        final boolean verStat;
        final byte verStatCode;

        public TC(boolean verStat, byte verStatCode, byte[] tc) {
            this.tc = tc;
            this.verStat = verStat;
            this.verStatCode = verStatCode;
        }
    }

    public static Behavior<Command> create(File out) {
        return Behaviors.setup(context -> new TCOutstream(context, out));
    }

    private final File out;

    private TCOutstream(ActorContext<Command> context, File out) {
        super(context);
        this.out = out;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(TC.class, this::onTC)
                .build();
    }

    private Behavior<Command> onTC(TC tc) {
        /*try {
            for(int i = 0; i < tc.tc.length; i++) {
                this.out.write(tc.tc[i]);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }*/
        try {
            FileWriter tcWriter = new FileWriter(this.out);
            if(tc.verStat) {
                tcWriter.write(1);
            }
            else {
                tcWriter.write(0);
            }
            tcWriter.write(tc.verStatCode);
            tcWriter.write(Arrays.toString(tc.tc));
        }
        catch (IOException e) {
            System.out.println("Could not write to file for tc output..");
        }
        return this;
    }
}
