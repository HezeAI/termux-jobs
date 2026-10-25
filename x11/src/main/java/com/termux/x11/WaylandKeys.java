package com.termux.x11;
/** Android physical keys to Linux evdev. Navigation/Return never become typed text. */
final class WaylandKeys {
    private WaylandKeys(){}
    static int evdev(int android){
        int[] letters={30,48,46,32,18,33,34,35,23,36,37,38,50,49,24,25,16,19,31,20,22,47,17,45,21,44};
        if(android>=29&&android<=54)return letters[android-29];
        if(android>=8&&android<=16)return android-6;if(android==7)return 11;
        if(android>=131&&android<=140)return 59+android-131;if(android==141)return 87;if(android==142)return 88;
        switch(android){
            case 19:return 103;case 20:return 108;case 21:return 105;case 22:return 106;case 23:return 28;
            case 55:return 51;case 56:return 52;case 57:return 56;case 58:return 100;case 59:return 42;case 60:return 54;
            case 61:return 15;case 62:return 57;case 66:case 160:return 28;case 67:return 14;
            case 68:return 41;case 69:return 12;case 70:return 13;case 71:return 26;case 72:return 27;case 73:return 43;case 74:return 39;case 75:return 40;case 76:return 53;
            case 92:return 104;case 93:return 109;case 111:return 1;case 112:return 111;case 113:return 29;case 114:return 97;
            case 115:return 58;case 117:return 125;case 118:return 126;case 120:return 99;case 121:return 119;case 122:return 102;case 123:return 107;case 124:return 110;
            default:return -1;
        }
    }
    // Fixed private keymap slots, without clipboard mutation or per-character keymap reload.
    static int unicode(int point){if(point>=0x410&&point<=0x44f)return 600+point-0x410;if(point==0x401)return 664;if(point==0x451)return 665;return -1;}
}
