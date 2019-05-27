package com.huafa.core.util;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.afterturn.easypoi.excel.entity.params.ExcelImportEntity;
import cn.afterturn.easypoi.exception.excel.ExcelImportException;
import cn.afterturn.easypoi.exception.excel.enums.ExcelImportEnum;

public class ExcelValueHandle {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExcelValueHandle.class);

	public Object getValue(Object object, Map<String, ExcelImportEntity> excelParams,
			String titleString, String value) {

		ExcelImportEntity entity = excelParams.get(titleString);
		String xclass = "class java.lang.Object";
		if (!(object instanceof Map)) {
			Method setMethod = entity.getMethods() != null && entity.getMethods().size() > 0
					? entity.getMethods().get(entity.getMethods().size() - 1)
					: entity.getMethod();
			Type[] ts = setMethod.getGenericParameterTypes();
			xclass = ts[0].toString();
		}

		return this.getCellValue(xclass, entity, value);
	}

	private Object getCellValue(String xclass, ExcelImportEntity entity, String value) {

		try {
			Object result = null;
			if (value == null || StringUtils.isBlank(value.toString())) {
				return null;
			} else if ("class java.lang.String".equals(xclass)) {
				result = value;
			} else if ("class java.util.Date".equals(xclass)) {
				result = this.getDateData(entity, value);
			} else if ("class java.lang.Double".equals(xclass) && !"double".equals(xclass)) {
				result = Double.valueOf(value);
			} else if ("class java.lang.Float".equals(xclass) && !"float".equals(xclass)) {
				result = Float.valueOf(value);
			} else if ("class java.lang.Integer".equals(xclass) && !"int".equals(xclass)) {
				result = Integer.valueOf(value);
			} else if ("class java.lang.Long".equals(xclass) && !"long".equals(xclass)) {
				result = Long.valueOf(value);
			} else if ("class java.math.BigDecimal".equals(xclass)) {
				result = new BigDecimal(value);
			} else if ("class java.util.Boolean".equals(xclass) && !"boolean".equals(xclass)) {
				// TODO: Boolean型具体规则待定.
			}
			return result;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new ExcelImportException(ExcelImportEnum.GET_VALUE_ERROR);
		}
	}

	private Date getDateData(ExcelImportEntity entity, String value) {

		if (StringUtils.isNotEmpty(entity.getFormat()) && StringUtils.isNotEmpty(value)) {
			SimpleDateFormat format = new SimpleDateFormat(entity.getFormat());
			try {
				return format.parse(value);
			} catch (ParseException var5) {
				LOGGER.error("时间格式化失败,格式化:{},值:{}", entity.getFormat(), value);
				throw new ExcelImportException(ExcelImportEnum.GET_VALUE_ERROR);
			}
		} else {
			return null;
		}
	}
}
