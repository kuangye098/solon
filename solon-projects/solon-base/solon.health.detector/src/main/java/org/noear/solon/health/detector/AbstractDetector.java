package org.noear.solon.health.detector;

import org.noear.solon.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 检测器虚拟类
 *
 * @author noear
 * @since 1.5
 * */
public abstract class AbstractDetector implements Detector{
    public static final long ONE_KB = 1024L;
    public static final long ONE_MB = 1048576L;
    public static final long ONE_GB = 1073741824L;
    public static final long ONE_TB = 1099511627776L;
    public static final long ONE_PB = 1125899906842624L;
    public static final long ONE_EB = 1152921504606846976L;

    protected final static String osName=System.getProperty("os.name").toLowerCase();

    public abstract String getName();
    public abstract Map<String,Object> getInfo();

    protected List<String[]> matcher(Pattern pattern, String text) throws Exception {
        List<String[]> infos = new ArrayList();
        if (Utils.isNotEmpty(text)) {
            String[] lines = text.trim().split("\\n");
            for(String line:lines){
                Matcher matcher = pattern.matcher(line);
                while(matcher.find()) {
                    String[] info = new String[matcher.groupCount() + 1];

                    for(int i = 0; i <= matcher.groupCount(); ++i) {
                        info[i] = matcher.group(i).trim();
                    }
                    infos.add(info);
                }
            }
        }
        return infos;
    }

    protected static String formatByteSize(long byteSize) {
        double dataSize = byteSize;
        if (dataSize >= ONE_EB)
            return (Math.round(dataSize / ONE_EB * 100.0D) / 100.0D) + "EB";
        if (dataSize >= ONE_PB)
            return (Math.round(dataSize / ONE_PB * 100.0D) / 100.0D) + "PB";
        if (dataSize >= ONE_TB)
            return (Math.round(dataSize / ONE_TB * 100.0D) / 100.0D) + "TB";
        if (dataSize >= ONE_GB)
            return (Math.round(dataSize / ONE_GB * 100.0D) / 100.0D) + "GB";
        if (dataSize >= ONE_MB)
            return (Math.round(dataSize / ONE_MB * 100.0D) / 100.0D) + "MB";
        if (dataSize >= ONE_KB)
            return (Math.round(dataSize / ONE_KB * 100.0D) / 100.0D) + "KB";
        if (dataSize > 0.0D) {
            return (Math.round(dataSize)) + "B";
        }
        return "0";
    }
}
