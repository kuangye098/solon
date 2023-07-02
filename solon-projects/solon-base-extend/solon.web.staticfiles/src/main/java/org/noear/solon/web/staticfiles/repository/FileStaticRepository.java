package org.noear.solon.web.staticfiles.repository;

import org.noear.solon.web.staticfiles.StaticRepository;

import java.io.File;
import java.net.URL;

/**
 * 文件型静态仓库（支持位置例：/user/ 或 file:///user/）
 *
 * @author noear
 * @since 1.5
 */
public class FileStaticRepository implements StaticRepository {
    String location;

    /**
     * 构建函数
     *
     * @param location 位置
     */
    public FileStaticRepository(String location) {
        setLocation(location);
    }

    /**
     * 设置位置
     *
     * @param location 位置
     */
    protected void setLocation(String location) {
        if (location == null) {
            return;
        }

        this.location = location;
    }

    /**
     * @param relativePath 例：demo/file.htm （没有'/'开头）
     * */
    @Override
    public URL find(String relativePath) throws Exception {
        File file = new File(location, relativePath);

        if (file.exists()) {
            return file.toURI().toURL();
        } else {
            return null;
        }
    }
}
