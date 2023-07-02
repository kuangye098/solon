package org.noear.solon.vault;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.vault.coder.AesVaultCoder;

import java.util.Properties;

/**
 * 脱敏工具
 *
 * @author noear
 * @since 1.9
 */
public class VaultUtils {
    public static final String TAG_PREFIX = "ENC(";
    public static final String TAG_SUFFIX = ")";

    static VaultCoder guardCoder = new AesVaultCoder();

    static {
        //尝试从容器中获取
        Solon.context().getBeanAsync(VaultCoder.class, bean -> {
            guardCoder = bean;
        });
    }

    /**
     * 是否已加密
     */
    public static boolean isEncrypted(String str) {
        if (str == null) {
            return false;
        } else {
            return str.startsWith(TAG_PREFIX) && str.endsWith(TAG_SUFFIX);
        }
    }

    /**
     * 加密
     */
    public static String encrypt(String str) {
        if (Utils.isEmpty(str)) {
            return str;
        }

        try {
            String tmp = guardCoder.encrypt(str);

            return TAG_PREFIX + tmp + TAG_SUFFIX;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解密
     */
    public static String decrypt(String str) {
        if (Utils.isEmpty(str)) {
            return str;
        }

        try {
            str = str.substring(TAG_PREFIX.length(), str.length() - TAG_SUFFIX.length());
            return guardCoder.decrypt(str);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 警惕处理
     * */
    public static String guard(String str) {
        if (VaultUtils.isEncrypted(str)) {
            return VaultUtils.decrypt(str);
        } else {
            return str;
        }
    }

    /**
     * 警惕处理
     * */
    public static Properties guard(Properties props) {
        props.forEach((k, v) -> {
            if (v instanceof String) {
                String val = (String) v;
                if (VaultUtils.isEncrypted(val)) {
                    String val2 = VaultUtils.decrypt(val);
                    props.put(k, val2);
                }
            }
        });

        return props;
    }
}
