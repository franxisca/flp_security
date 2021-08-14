package com.main;

public enum KeyState {
    POWERED_OFF {
        public byte toByte() {
            return (byte) 0b00000000;
        }
    },
    ACTIVE {
        public byte toByte() {
            return (byte) 0b00000001;
        }
    },
    PRE_ACTIVE {
        public byte toByte() {
            return (byte) 0b00000010;
        }
    },
    DEACTIVATED {
        public byte toByte() {
            return (byte) 0b00000011;
        }
    };

    public byte toByte() {
        return (byte) 0b11111111;
    }
}
