package utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

public class Encryption {
    private static final String ALGORITHM = (new Object() {
        int t;

        public String toString() {
            byte[] buf = new byte[20];
            t = 1105804250;
            buf[0] = (byte) (t >>> 24);
            t = -2127654572;
            buf[1] = (byte) (t >>> 6);
            t = -73483618;
            buf[2] = (byte) (t >>> 3);
            t = -1510085993;
            buf[3] = (byte) (t >>> 21);
            t = -932986686;
            buf[4] = (byte) (t >>> 21);
            t = 98986055;
            buf[5] = (byte) (t >>> 5);
            t = 2131369968;
            buf[6] = (byte) (t >>> 11);
            t = -2097222804;
            buf[7] = (byte) (t >>> 20);
            t = -1353438415;
            buf[8] = (byte) (t >>> 14);
            t = 270901851;
            buf[9] = (byte) (t >>> 15);
            t = 671516647;
            buf[10] = (byte) (t >>> 9);
            t = -1971673606;
            buf[11] = (byte) (t >>> 21);
            t = -477219556;
            buf[12] = (byte) (t >>> 8);
            t = 1350428740;
            buf[13] = (byte) (t >>> 24);
            t = 2133906667;
            buf[14] = (byte) (t >>> 15);
            t = 1976414729;
            buf[15] = (byte) (t >>> 7);
            t = 1715656990;
            buf[16] = (byte) (t >>> 20);
            t = -1509579080;
            buf[17] = (byte) (t >>> 10);
            t = 1408784824;
            buf[18] = (byte) (t >>> 2);
            t = 376824032;
            buf[19] = (byte) (t >>> 20);
            return new String(buf);
        }
    }.toString());
    private static final String KEY = (new Object() {
        int t;

        public String toString() {
            byte[] buf = new byte[32];
            t = -1042425718;
            buf[0] = (byte) (t >>> 10);
            t = -763962258;
            buf[1] = (byte) (t >>> 1);
            t = -1588764798;
            buf[2] = (byte) (t >>> 23);
            t = -239634286;
            buf[3] = (byte) (t >>> 8);
            t = 1942916327;
            buf[4] = (byte) (t >>> 2);
            t = -1209683779;
            buf[5] = (byte) (t >>> 7);
            t = -858528234;
            buf[6] = (byte) (t >>> 18);
            t = 1973892690;
            buf[7] = (byte) (t >>> 12);
            t = -352937843;
            buf[8] = (byte) (t >>> 1);
            t = -1233575754;
            buf[9] = (byte) (t >>> 20);
            t = 977749715;
            buf[10] = (byte) (t >>> 19);
            t = 163525613;
            buf[11] = (byte) (t >>> 8);
            t = 1898808119;
            buf[12] = (byte) (t >>> 18);
            t = -346325831;
            buf[13] = (byte) (t >>> 14);
            t = -2131336895;
            buf[14] = (byte) (t >>> 2);
            t = -312271801;
            buf[15] = (byte) (t >>> 6);
            t = 1368729816;
            buf[16] = (byte) (t >>> 12);
            t = 1334891531;
            buf[17] = (byte) (t >>> 10);
            t = -1128158551;
            buf[18] = (byte) (t >>> 1);
            t = -193619543;
            buf[19] = (byte) (t >>> 16);
            t = -1427588936;
            buf[20] = (byte) (t >>> 21);
            t = -1360413674;
            buf[21] = (byte) (t >>> 7);
            t = 724986828;
            buf[22] = (byte) (t >>> 21);
            t = -1984989265;
            buf[23] = (byte) (t >>> 4);
            t = -2036921233;
            buf[24] = (byte) (t >>> 6);
            t = 306594921;
            buf[25] = (byte) (t >>> 8);
            t = 643841229;
            buf[26] = (byte) (t >>> 20);
            t = 966501657;
            buf[27] = (byte) (t >>> 7);
            t = 996424091;
            buf[28] = (byte) (t >>> 19);
            t = 2064928309;
            buf[29] = (byte) (t >>> 8);
            t = 228224346;
            buf[30] = (byte) (t >>> 22);
            t = -2029340822;
            buf[31] = (byte) (t >>> 20);
            return new String(buf);
        }
    }.toString());

    public static String encrypt(String valueToEnc, byte[] ivBytes) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Key key = new SecretKeySpec(KEY.getBytes(), (new Object() {
            int t;

            public String toString() {
                byte[] buf = new byte[3];
                t = 1757782277;
                buf[0] = (byte) (t >>> 2);
                t = 1608062864;
                buf[1] = (byte) (t >>> 10);
                t = -100078949;
                buf[2] = (byte) (t >>> 3);
                return new String(buf);
            }
        }.toString()));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encryptedValue = cipher.doFinal(valueToEnc.getBytes());
        return Base64.getEncoder().encodeToString(encryptedValue);
    }

    public static String decrypt(String encryptedValue, byte[] ivBytes) throws Exception {
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        Key key = new SecretKeySpec(KEY.getBytes(), (new Object() {
            int t;

            public String toString() {
                byte[] buf = new byte[3];
                t = 1757782277;
                buf[0] = (byte) (t >>> 2);
                t = 1608062864;
                buf[1] = (byte) (t >>> 10);
                t = -100078949;
                buf[2] = (byte) (t >>> 3);
                return new String(buf);
            }
        }.toString()));
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decodedValue = Base64.getDecoder().decode(encryptedValue);
        byte[] decryptedVal = cipher.doFinal(decodedValue);
        return new String(decryptedVal);
    }
}
