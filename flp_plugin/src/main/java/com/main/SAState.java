package com.main;

public enum SAState {
    POWERED_OFF,
    OPERATIONAL,
    KEYED,
    UNKEYED,
    NAN;

    public byte toByte() {
        //TODO: encode SAStates
        return (byte) 0b00000000;
    }
}
