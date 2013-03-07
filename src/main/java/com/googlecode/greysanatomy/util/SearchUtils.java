package com.googlecode.greysanatomy.util;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

/**
 * �����ù�����
 * @author chengtongda
 *
 */
public class SearchUtils {

	/**
	 * ��������������ʽ��
	 * @param inst
	 * @return
	 */
	public static Set<Class<?>> searchClassByClassRegex(Instrumentation inst, String classRegex) {
		final Set<Class<?>> matchs = new HashSet<Class<?>>();
		for (Class<?> clazz : inst.getAllLoadedClasses()) {
			if (clazz.getName().matches(classRegex)) {
				matchs.add(clazz);
			}
		}//for
		return matchs;
	}
	
	/**
	 * ���ݸ���������
	 * @param inst
	 * @param supers
	 * @return
	 */
	public static Set<Class<?>> searchClassBySupers(Instrumentation inst, Set<Class<?>> supers) {
		final Set<Class<?>> matchs = new HashSet<Class<?>>();
		for (Class<?> clazz : inst.getAllLoadedClasses()) {
			for( Class<?> superClass : supers ) {
				if( superClass.isAssignableFrom(clazz) ) {
					matchs.add(clazz);
					break;
				}
			}
		}//for
		return matchs;
	}

}
