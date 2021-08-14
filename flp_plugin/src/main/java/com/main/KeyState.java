package com.main;

public enum KeyState {
    POWERED_OFF,
    ACTIVE,
    PRE_ACTIVE,
    DEACTIVATED;

    //TODO: encode keyStates
    public byte toByte() {
        return (byte) 0b00000000;
    }
}
