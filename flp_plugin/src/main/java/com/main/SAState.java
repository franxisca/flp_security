package com.main;

public enum SAState {
    POWERED_OFF {
        public byte toByte() {
            return (byte) 0b00000000;
        }
    },
    OPERATIONAL {
        public byte toByte() {
            return (byte) 0b00000001;
        }
    },
    KEYED {
        public byte toByte() {
            return (byte) 0b00000010;
        }
    },
    UNKEYED {
        public byte toByte() {
            return (byte) 0b00000011;
        }
    },
    NAN {
        public byte toByte() {
            return (byte) 0b00001111;
        }
    };

    public byte toByte() {
        return (byte) 0b11111111;
    }

    public byte transition (SAState state1, SAState state2) {
        if(state1 == null) {
            state1 = NAN;
        }
        switch (state1) {
            case POWERED_OFF: {
                switch (state2) {
                    //only for critical SAs
                    case OPERATIONAL: {
                        return (byte) 0b00000001;
                    }
                    //only for standard SAs
                    case UNKEYED: {
                        return (byte) 0b00000011;
                    }
                    default: {
                        return (byte) 0b11111111;
                    }
                }
            }
            case OPERATIONAL: {
                switch (state2) {
                    case POWERED_OFF: {
                        return (byte) 0b00010000;
                    }
                    case KEYED: {
                        return (byte) 0b00010010;
                    }
                    case OPERATIONAL: {
                        return (byte) 0b00010001;
                    }
                    default: {
                        return (byte) 0b11111111;
                    }
                }
            }
            case KEYED: {
                switch (state2) {
                    case OPERATIONAL: {
                        return (byte) 0b00100001;
                    }
                    case UNKEYED: {
                        return (byte) 0b00100011;
                    }
                    case POWERED_OFF: {
                        return (byte) 0b00100000;
                    }
                    default: {
                        return (byte) 0b11111111;
                    }
                }

            }
            case UNKEYED: {
                switch (state2) {
                    case POWERED_OFF: {
                        return (byte) 0b00110000;
                    }
                    case KEYED: {
                        return (byte) 0b00110010;
                    }
                    default: {
                        return (byte) 0b11111111;
                    }
                }
            }
            case NAN: {
                switch (state2) {
                    case POWERED_OFF: {
                        return (byte) 0b11110000;
                    }
                    case OPERATIONAL: {
                        return (byte) 0b11110001;
                    }
                    case KEYED: {
                        return (byte) 0b11110010;
                    }
                    case UNKEYED: {
                        return (byte) 0b11110011;
                    }
                    default: {
                        return (byte) 0b11111111;
                    }
                }
            }
        }
        return (byte) 0b11111111;
    }
}
