package com.deem.zkui.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yuanjun3@asiainfo-sec.com
 */
@Slf4j
public class BeanFactory {

	private static Map<Class<?>, Object> beans = new ConcurrentHashMap<>();

	public static void registry(Class<?> clz, Object target) {
		beans.put(clz, target);
		log.info("注册BEAN, Class: {}, target: {}", clz, target);
	}

	public static <T> T getBean(Class<T> clz) {
		return clz.cast(beans.get(clz));
	}

}
