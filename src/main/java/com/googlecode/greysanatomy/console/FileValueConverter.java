package com.googlecode.greysanatomy.console;

import java.io.File;

import joptsimple.ValueConverter;

/**
 * FileµÄvalueOf
 * @author vlinux
 *
 */
public class FileValueConverter implements ValueConverter<File> {

	@Override
	public File convert(String value) {
		return new File(value);
	}

	@Override
	public Class<File> valueType() {
		return File.class;
	}

	@Override
	public String valuePattern() {
		return null;
	}

	

}
