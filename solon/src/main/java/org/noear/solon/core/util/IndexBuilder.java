package org.noear.solon.core.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.noear.solon.annotation.Inject;

/**
 * 构建初始化的index
 *
 * @author cym1102
 * @since 1.6
 */
public class IndexBuilder {

	private final Map<String, Integer> map = new HashMap<>();
	private final ArrayList<String> classStack = new ArrayList<>();

	/**
	 * 获取bean的初始化index
	 *
	 * @param clazz bean类
	 * @return 顺序index
	 */
	public int buildIndex(Class<?> clazz) {
		return buildIndexDo(clazz, true);
	}

	/**
	 * 获取bean的初始化index
	 *
	 * @param clazz    bean类
	 * @param stackTop 是否为查找栈顶
	 * @return 顺序index
	 */
	private int buildIndexDo(Class<?> clazz, Boolean stackTop) {
		if (stackTop) {
			classStack.clear();

			if (isLoopRelate(clazz, clazz.getName())) {
				String link = "";
				for (int i = 0; i < classStack.size(); i++) {
					link += classStack.get(i);
					if (i != classStack.size() - 1) {
						link += " -> ";
					}
				}


				throw new IllegalStateException("Lifecycle does not support dependency loops: " + link);
			}
		}

		if (map.get(clazz.getName()) != null) {
			return map.get(clazz.getName());
		} else {
			// 找到他的依赖类
			List<Class<?>> clazzList = findRelateClass(clazz);

			// 没有依赖类, 直接返回0
			if (clazzList.size() == 0) {
				map.put(clazz.getName(), 0);
				return 0;
			}

			// 找到依赖类中最大的index
			Integer maxIndex = null;
			for (Class<?> clazzRelate : clazzList) {
				// 避免进入死循环
				if (classStack.contains(clazzRelate.getName())) {
					continue;
				} else {
					classStack.add(clazzRelate.getName());
				}

				int index = buildIndexDo(clazzRelate, false);

				if (maxIndex == null) {
					maxIndex = index;
				} else if (maxIndex < index) {
					maxIndex = index;
				}
			}

			if (maxIndex == null) {
				maxIndex = 0;
			}

			// 返回maxIndex + 1
			map.put(clazz.getName(), maxIndex + 1);
			return maxIndex + 1;
		}
	}

	/**
	 * 寻找依赖类
	 *
	 * @param clazz
	 * @return 依赖类集合
	 */
	private List<Class<?>> findRelateClass(Class<?> clazz) {
		List<Class<?>> clazzList = new ArrayList<>();
		Field[] fields = ReflectUtil.getDeclaredFields(clazz);

		for (Field field : fields) {
			if (field.isAnnotationPresent(Inject.class)) {
				Inject inject = field.getAnnotation(Inject.class);
				if (inject.value().contains("${")) {
					//注入的是参数, 略过
					continue;
				}

				if (clazz.equals(field.getType())) {
					//自己注入自己，略过
					continue;
				}

				clazzList.add(field.getType());
			}
		}

		return clazzList;
	}

	/**
	 * 检查是否循环依赖
	 *
	 * @param clazz
	 * @return 是否循环依赖
	 */
	private boolean isLoopRelate(Class<?> clazz, String topName) {
		if (classStack.contains(clazz.getName())) {
			return false;
		}

		classStack.add(clazz.getName()); // 入栈

		//寻找依赖类
		List<Class<?>> clazzList = findRelateClass(clazz);

		for (Class<?> clazzRelate : clazzList) {
			if (clazzRelate.getName().equals(topName)) {
				classStack.add(clazzRelate.getName()); // 入栈
				return true;
			}
		}

		for (Class<?> clazzRelate : clazzList) {
			if (isLoopRelate(clazzRelate, topName)) {
				return true;
			}
		}

		classStack.remove(clazz.getName()); // 出栈
		return false;
	}
}
