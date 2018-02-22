package hello;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by ludwikbukowski on 12/02/18.
 */
public class ConsistentHashingUtils {
    // 2^32 as a range for keys
    private static Integer keyDomain = java.lang.Integer.MAX_VALUE;

    public static Integer getNode(String key) {
        Integer code = abs(key.hashCode());
        Integer range = keyDomain / Main.NODES_NUMBER;
        int part = 0;
        while(code > part * range){part++;}
        return part % (Main.NODES_NUMBER-1);
    }

    private static Integer abs(int i) {
        if(i<0) return -i;
        return i;
    }
}
