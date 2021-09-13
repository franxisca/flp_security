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

        switch (this) {
            case POWERED_OFF: {
                return (byte) 0b00000000;
            }
            case ACTIVE: {
                return (byte) 0b00000001;
            }
            case PRE_ACTIVE: {
                return (byte) 0b00000010;
            }
            case DEACTIVATED: {
                return (byte) 0b00000011;
            }
            default: return (byte) 0b11111111;
        }
        //return (byte) 0b11111111;
    }
}
