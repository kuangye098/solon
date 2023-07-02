package org.noear.solon.web.staticfiles;

/**
 * 静态文件位置
 *
 * @author noear
 * @since 1.0
 * */
public class StaticLocation {
    /**
     * 路径前缀
     */
    public final String pathPrefix;
    /**
     * 路径前缀是文件
     */
    public final boolean pathPrefixAsFile;
    /**
     * 资源仓库
     */
    public final StaticRepository repository;

    /**
     * 资源仓库是否包括路径前缀（默认为：true）
     *
     * @since 1.6
     */
    public final boolean repositoryIncPrefix;

    public StaticLocation(String pathPrefix, StaticRepository repository, boolean repositoryIncPrefix) {
        this.pathPrefix = pathPrefix;
        this.pathPrefixAsFile = (pathPrefix.endsWith("/") == false);
        this.repository = repository;
        this.repositoryIncPrefix = repositoryIncPrefix;
    }
}
