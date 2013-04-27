package com.moksamedia.morphia

import java.lang.reflect.Field;

import com.github.jmkgreen.morphia.mapping.MappedField


class MappedMetaClassField extends MappedField {

	MappedMetaClassField(Field f, Class<?> clazz) {
       f.setAccessible(true);
        field = f;
        persistedClass = clazz;
        discover();
	}

	@Override
	public Object getFieldValue(Object classInst) throws IllegalArgumentException {
		try {
			classInst."${field.name}"
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setFieldValue(Object classInst, Object value) throws IllegalArgumentException {
		try {
			try {
				classInst.class.getDeclaredField(field.name)
				field.setAccessible(true)
				field.set(classInst, value)
			}
			catch (NoSuchFieldException ex) {
				classInst.metaClass."${field.name}" = value
			}

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
