package com.googlecode.greysanatomy.util;

import static com.googlecode.greysanatomy.util.GaCheckUtils.isIn;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.lang.StringUtils;

/**
 * ���乤����
 * @author vlinux
 *
 */
public class GaReflectUtils {

	/**
	 * �Ӱ�package�л�ȡ���е�Class
	 * 
	 * @param pack
	 * @return
	 * @author taote
	 * <p>����ժ���� http://www.oschina.net/code/snippet_129830_8767</p>
	 */
	public static Set<Class<?>> getClasses(String pack) {

		// ��һ��class��ļ���
		Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
		// �Ƿ�ѭ������
		boolean recursive = true;
		// ��ȡ�������� �������滻
		String packageName = pack;
		String packageDirName = packageName.replace('.', '/');
		// ����һ��ö�ٵļ��� ������ѭ�����������Ŀ¼�µ�things
		Enumeration<URL> dirs;
		try {
			dirs = Thread.currentThread().getContextClassLoader()
					.getResources(packageDirName);
			// ѭ��������ȥ
			while (dirs.hasMoreElements()) {
				// ��ȡ��һ��Ԫ��
				URL url = dirs.nextElement();
				// �õ�Э�������
				String protocol = url.getProtocol();
				// ��������ļ�����ʽ�����ڷ�������
				if ("file".equals(protocol)) {
//					System.err.println("file���͵�ɨ��");
					// ��ȡ��������·��
					String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
					// ���ļ��ķ�ʽɨ���������µ��ļ� ����ӵ�������
					findAndAddClassesInPackageByFile(packageName, filePath,
							recursive, classes);
				} else if ("jar".equals(protocol)) {
					// �����jar���ļ�
					// ����һ��JarFile
//					System.err.println("jar���͵�ɨ��");
					JarFile jar;
					try {
						// ��ȡjar
						jar = ((JarURLConnection) url.openConnection())
								.getJarFile();
						// �Ӵ�jar�� �õ�һ��ö����
						Enumeration<JarEntry> entries = jar.entries();
						// ͬ���Ľ���ѭ������
						while (entries.hasMoreElements()) {
							// ��ȡjar���һ��ʵ�� ������Ŀ¼ ��һЩjar����������ļ� ��META-INF���ļ�
							JarEntry entry = entries.nextElement();
							String name = entry.getName();
							// �������/��ͷ��
							if (name.charAt(0) == '/') {
								// ��ȡ������ַ���
								name = name.substring(1);
							}
							// ���ǰ�벿�ֺͶ���İ�����ͬ
							if (name.startsWith(packageDirName)) {
								int idx = name.lastIndexOf('/');
								// �����"/"��β ��һ����
								if (idx != -1) {
									// ��ȡ���� ��"/"�滻��"."
									packageName = name.substring(0, idx)
											.replace('/', '.');
								}
								// ������Ե�����ȥ ������һ����
								if ((idx != -1) || recursive) {
									// �����һ��.class�ļ� ���Ҳ���Ŀ¼
									if (name.endsWith(".class")
											&& !entry.isDirectory()) {
										// ȥ�������".class" ��ȡ����������
										String className = name.substring(
												packageName.length() + 1,
												name.length() - 6);
										try {
											// ��ӵ�classes
											classes.add(Class
													.forName(packageName + '.'
															+ className));
										} catch (ClassNotFoundException e) {
											// log
											// .error("����û��Զ�����ͼ����� �Ҳ��������.class�ļ�");
//											e.printStackTrace();
										}
									}
								}
							}
						}
					} catch (IOException e) {
						// log.error("��ɨ���û�������ͼʱ��jar����ȡ�ļ�����");
//						e.printStackTrace();
					}
				}
			}
		} catch (IOException e) {
//			e.printStackTrace();
		}

		return classes;
	}

	/**
	 * ���ļ�����ʽ����ȡ���µ�����Class
	 * 
	 * @param packageName
	 * @param packagePath
	 * @param recursive
	 * @param classes
	 * 
	 * @author taote
	 * <p>����ժ���� http://www.oschina.net/code/snippet_129830_8767</p>
	 */
	private static void findAndAddClassesInPackageByFile(String packageName,
			String packagePath, final boolean recursive, Set<Class<?>> classes) {
		// ��ȡ�˰���Ŀ¼ ����һ��File
		File dir = new File(packagePath);
		// ��������ڻ��� Ҳ����Ŀ¼��ֱ�ӷ���
		if (!dir.exists() || !dir.isDirectory()) {
			// log.warn("�û�������� " + packageName + " ��û���κ��ļ�");
			return;
		}
		// ������� �ͻ�ȡ���µ������ļ� ����Ŀ¼
		File[] dirfiles = dir.listFiles(new FileFilter() {
			// �Զ�����˹��� �������ѭ��(������Ŀ¼) ��������.class��β���ļ�(����õ�java���ļ�)
			public boolean accept(File file) {
				return (recursive && file.isDirectory())
						|| (file.getName().endsWith(".class"));
			}
		});
		// ѭ�������ļ�
		for (File file : dirfiles) {
			// �����Ŀ¼ �����ɨ��
			if (file.isDirectory()) {
				findAndAddClassesInPackageByFile(
						packageName + "." + file.getName(),
						file.getAbsolutePath(), recursive, classes);
			} else {
				// �����java���ļ� ȥ�������.class ֻ��������
				String className = file.getName().substring(0,
						file.getName().length() - 6);
				try {
					// ��ӵ�������ȥ
					// classes.add(Class.forName(packageName + '.' +
					// className));
					// �����ظ�ͬѧ�����ѣ�������forName��һЩ���ã��ᴥ��static������û��ʹ��classLoader��load�ɾ�
					classes.add(Thread.currentThread().getContextClassLoader()
							.loadClass(packageName + '.' + className));
				} catch (ClassNotFoundException e) {
					// log.error("����û��Զ�����ͼ����� �Ҳ��������.class�ļ�");
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * ��ȡһ�����µ����г�Ա(�������ࡢ˽�г�Ա)
	 * @param clazz
	 * @return
	 */
	public static Set<Field> getFileds(Class<?> clazz) {
		final Set<Field> fields = new LinkedHashSet<Field>();
		final Class<?> parentClazz = clazz.getSuperclass();
		for( Field field : clazz.getDeclaredFields() ) {
			fields.add(field);
		}
		if( null != parentClazz ) {
			fields.addAll(getFileds(parentClazz));
		}
		return fields;
	}
	
	/**
	 * ��ȡһ�����µ�ָ����Ա
	 * @param clazz
	 * @param name
	 * @return
	 */
	public static Field getField(Class<?> clazz, String name) {
		for( Field field : getFileds(clazz) ) {
			if( StringUtils.equals(field.getName(), name) ) {
				return field;
			}
		}//for
		return null;
	}
	
	
	/**
	 * ��ȡ����ĳ����Ա��ֵ
	 * @param target
	 * @param field
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFieldValueByField(Object target, Field field) throws IllegalArgumentException, IllegalAccessException {
		final boolean isAccessible = field.isAccessible();
		try {
			field.setAccessible(true);
			return (T)field.get(target);
		} finally {
			field.setAccessible(isAccessible);
		}
	}
	
	/**
	 * ��ȡ����ĳ����Ա��ֵ
	 * @param target
	 * @param fieldName
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFieldValueByFieldName(Object target, String fieldName) throws IllegalArgumentException, IllegalAccessException {
		if(StringUtils.isEmpty(fieldName)){
			return (T)target;
		}
		return (T)getFieldValueByField(target, getField(target.getClass(), fieldName));
	}
	
	/**
	 * ���ö���ĳ����Ա��ֵ
	 * @param field
	 * @param value
	 * @param target
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static void set(Field field, Object value, Object target) throws IllegalArgumentException, IllegalAccessException {
		final boolean isAccessible = field.isAccessible();
		try {
			field.setAccessible(true);
			field.set(target, value);
		} finally {
			field.setAccessible(isAccessible);
		}
	}
	
	/**
	 * ���ַ���ת��Ϊָ�����ͣ�Ŀǰֻ֧��9�����ͣ�8�ֻ������ͣ��������װ�ࣩ�Լ��ַ���
	 * @param t
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T valueOf(Class<T> t, String value) {
		if( isIn(t, int.class, Integer.class) ) {
			return (T) Integer.valueOf(value);
		} else if( isIn(t, long.class, Long.class) ) {
			return (T) Long.valueOf(value);
		} else if( isIn(t, double.class, Double.class) ) {
			return (T) Double.valueOf(value);
		} else if( isIn(t, float.class, Float.class) ) {
			return (T) Float.valueOf(value);
		} else if( isIn(t, char.class, Character.class) ) {
			return (T) Character.valueOf(value.charAt(0));
		} else if( isIn(t, byte.class, Byte.class) ) {
			return (T) Byte.valueOf(value);
		} else if( isIn(t, boolean.class, Boolean.class) ) {
			return (T) Boolean.valueOf(value);
		} else if( isIn(t, short.class, Short.class) ) {
			return (T) Short.valueOf(value);
		} else if( isIn(t, String.class) ) {
			return (T) value;
		} else {
			return null;
		}
	}
	
	/**
	 * ��filepath�ĸ�ʽ<p>java/lang/String</p>ת��Ϊclasspath<p>java.lang.String.class</p>
	 * @param path
	 * @return
	 */
	public static String toClassPath(String filePath) {
		return filePath.replaceAll("/", ".");
	}
	
	/**
	 * ������ö�ջ
	 * @return
	 */
	public static String jstack() {

		int i = 0;
		final StringBuilder jstackSB = new StringBuilder();
		for( StackTraceElement ste : Thread.currentThread().getStackTrace() ) {
			GaStringUtils.rightFill(jstackSB, i*2, " ");
			if( 0 != i++ ) {
				jstackSB.append("`-- ");
			}
			jstackSB.append(ste.toString()).append("\n");
		}
		
		return jstackSB.toString();
		
	}
	
}
