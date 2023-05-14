package org.noear.solon.extend.impl;

import org.noear.solon.core.ResourceScanner;

import java.io.IOException;
import java.net.URL;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 资源扫描器-静态扩展实现（静态扩展约定：org.noear.solon.extend.impl.XxxxExt）
 *
 * @author noear
 * @since 1.5
 */
public class ResourceScannerExt extends ResourceScanner {

	@Override
	protected void scanDo(URL url, String path, Predicate<String> filter, Set<String> urls) throws IOException {
		super.scanDo(url, path, filter, urls);

	}
}
