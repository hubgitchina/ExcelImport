package com.huafa.core.util;

import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.BuiltinFormats;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.afterturn.easypoi.excel.annotation.ExcelTarget;
import cn.afterturn.easypoi.excel.entity.params.ExcelCollectionParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelImportEntity;
import cn.afterturn.easypoi.excel.imports.base.ImportBaseService;
import cn.afterturn.easypoi.util.PoiPublicUtil;

public class ExcelCovertCSVReader extends ImportBaseService {

	enum xssfDataType {
		BOOL, ERROR, FORMULA, INLINESTR, SSTINDEX, NUMBER,
	}

	class XSSFSheetHandler extends DefaultHandler {

		private StylesTable stylesTable;

		private ReadOnlySharedStringsTable sharedStringsTable;

		private final PrintStream output;

		private final int minColumnCount;

		private boolean vIsOpen;

		private xssfDataType nextDataType;

		private short formatIndex;
		private String formatString;
		private final DataFormatter formatter;

		private int thisColumn = -1;
		private int lastColumnNumber = -1;

		private StringBuffer value;

		private int firstRow = 0;

		private String[] recordTitle;

		private String[] record;

		private Class<?> pojoClass;

		private Map<String, ExcelImportEntity> excelParams;

		private List collection = Lists.newArrayList();

		private boolean isCellNull = false;

		public XSSFSheetHandler(StylesTable styles, ReadOnlySharedStringsTable strings, int cols,
				PrintStream target, Class<?> pojoClass) throws Exception {

			this.stylesTable = styles;
			this.sharedStringsTable = strings;
			this.minColumnCount = cols;
			this.output = target;
			this.value = new StringBuffer();
			this.nextDataType = xssfDataType.NUMBER;
			this.formatter = new DataFormatter();
			this.record = new String[this.minColumnCount];
			// 每次读取都清空行集合
			// rows.clear();

			this.recordTitle = new String[this.minColumnCount];
			this.pojoClass = pojoClass;

			this.excelParams = Maps.newHashMapWithExpectedSize(this.minColumnCount);
			List<ExcelCollectionParams> excelCollection = Lists.newArrayList();
			String targetId = null;
			if (!Map.class.equals(pojoClass)) {
				Field[] fileds = PoiPublicUtil.getClassFields(pojoClass);
				ExcelTarget etarget = pojoClass.getAnnotation(ExcelTarget.class);
				if (etarget != null) {
					targetId = etarget.value();
				}

				ExcelCovertCSVReader.this.getAllExcelField(targetId, fileds, this.excelParams,
						excelCollection, pojoClass, null, null);
			}
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) {

			if ("inlineStr".equals(name) || "v".equals(name)) {
				vIsOpen = true;
				value.setLength(0);
			} else if ("c".equals(name)) {
				String r = attributes.getValue("r");
				int firstDigit = -1;
				for (int c = 0; c < r.length(); ++c) {
					if (Character.isDigit(r.charAt(c))) {
						firstDigit = c;
						break;
					}
				}
				thisColumn = nameToColumn(r.substring(0, firstDigit));

				this.nextDataType = xssfDataType.NUMBER;
				this.formatIndex = -1;
				this.formatString = null;
				String cellType = attributes.getValue("t");
				String cellStyleStr = attributes.getValue("s");
				if ("b".equals(cellType)) {
					nextDataType = xssfDataType.BOOL;
				} else if ("e".equals(cellType)) {
					nextDataType = xssfDataType.ERROR;
				} else if ("inlineStr".equals(cellType)) {
					nextDataType = xssfDataType.INLINESTR;
				} else if ("s".equals(cellType)) {
					nextDataType = xssfDataType.SSTINDEX;
				} else if ("str".equals(cellType)) {
					nextDataType = xssfDataType.FORMULA;
				} else if (cellStyleStr != null) {
					int styleIndex = Integer.parseInt(cellStyleStr);
					XSSFCellStyle style = stylesTable.getStyleAt(styleIndex);
					this.formatIndex = style.getDataFormat();
					this.formatString = style.getDataFormatString();
					if (this.formatString == null) {
						this.formatString = BuiltinFormats.getBuiltinFormat(this.formatIndex);
					}
				}
			}

		}

		@Override
		public void endElement(String uri, String localName, String name) {

			String thisStr = null;

			if ("v".equals(name)) {
				switch (nextDataType) {
				case BOOL:
					char first = value.charAt(0);
					thisStr = first == '0' ? "FALSE" : "TRUE";
					break;
				case ERROR:
					thisStr = "\"ERROR:" + value.toString() + '"';
					break;
				case FORMULA:
					thisStr = value.toString();
					break;
				case INLINESTR:
					// TODO: have seen an example of this, so it's untested.
					XSSFRichTextString rtsi = new XSSFRichTextString(value.toString());
					// thisStr = '"' + rtsi.toString() + '"';
					thisStr = rtsi.toString();
					break;
				case SSTINDEX:
					String sstIndex = value.toString();
					try {
						int idx = Integer.parseInt(sstIndex);
						XSSFRichTextString rtss = new XSSFRichTextString(
								sharedStringsTable.getEntryAt(idx));
						// thisStr = '"' + rtss.toString() + '"';
						thisStr = rtss.toString();
					} catch (NumberFormatException ex) {
						output.println(
								"Failed to parse SST index '" + sstIndex + "': " + ex.toString());
					}
					break;
				case NUMBER:
					String n = value.toString();
					// 判断是否是日期格式
					if (HSSFDateUtil.isADateFormat(this.formatIndex, n)) {
						Double d = Double.parseDouble(n);
						Date date = HSSFDateUtil.getJavaDate(d);
						thisStr = formateDateToString(date);
					} else if (this.formatString != null) {
						thisStr = formatter.formatRawCellContents(Double.parseDouble(n),
								this.formatIndex, this.formatString);
					} else {
						thisStr = n;
					}
					break;
				default:
					thisStr = "(TODO: Unexpected type: " + nextDataType + ")";
					break;
				}

				if (lastColumnNumber == -1) {
					lastColumnNumber = 0;
				}
				// 判断单元格的值是否为空
				if (thisStr == null || "".equals(isCellNull)) {
					// 设置单元格是否为空值
					isCellNull = true;
				}

				record[thisColumn] = thisStr;

				if (thisColumn > -1) {
					lastColumnNumber = thisColumn;
				}
			} else if ("row".equals(name)) {
				if (minColumns > 0) {
					if (lastColumnNumber == -1) {
						lastColumnNumber = 0;
					}
					if (isCellNull == false)// 判断是否空行
					{

						if (firstRow == 0) {
							recordTitle = record.clone();
							firstRow = 1;

							for (int i = 0; i < record.length; i++) {
								record[i] = null;
							}
						} else {
							Object object = PoiPublicUtil.createObject(pojoClass, null);
							for (int i = 0; i < record.length; i++) {
								String value = record[i];
								String title = recordTitle[i];
								if (excelParams.containsKey(title) || Map.class.equals(pojoClass)) {
									if (excelParams.get(title) != null) {
										Object actualValue = ExcelCovertCSVReader.this.excelValueHandle
												.getValue(object, excelParams, title, value);
										try {
											ExcelCovertCSVReader.this.setValues(
													excelParams.get(title), object, actualValue);
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
								record[i] = null;
							}
							collection.add(object);
						}

						// rows.add(record.clone());
						isCellNull = false;
						// for (int i = 0; i < record.length; i++) {
						// record[i] = null;
						// }
					}
				}
				lastColumnNumber = -1;
			}
		}

		public List getCollection() {

			return collection;
		}

		@Override
		public void characters(char[] ch, int start, int length) {

			if (vIsOpen) {
				value.append(ch, start, length);
			}
		}

		private int nameToColumn(String name) {

			int column = -1;
			for (int i = 0; i < name.length(); ++i) {
				int c = name.charAt(i);
				column = (column + 1) * 26 + c - 'A';
			}
			return column;
		}

		private String formateDateToString(Date date) {

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sdf.format(date);
		}
	}

	private OPCPackage xlsxPackage;
	private int minColumns;
	private PrintStream output;
	private String sheetName;

	private ExcelValueHandle excelValueHandle;

	public ExcelCovertCSVReader(OPCPackage pkg, PrintStream output, String sheetName,
			int minColumns) {

		this.xlsxPackage = pkg;
		this.output = output;
		this.minColumns = minColumns;
		this.sheetName = sheetName;
	}

	public ExcelCovertCSVReader(OPCPackage pkg, int minColumns) {

		this.xlsxPackage = pkg;
		this.minColumns = minColumns;
		this.excelValueHandle = new ExcelValueHandle();
	}

	public <T> List<T> processSheet(StylesTable styles, ReadOnlySharedStringsTable strings,
			InputStream sheetInputStream, Class<T> pojoClass) throws Exception {

		InputSource sheetSource = new InputSource(sheetInputStream);
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		SAXParser saxParser = saxFactory.newSAXParser();
		XMLReader sheetParser = saxParser.getXMLReader();
		XSSFSheetHandler handler = new XSSFSheetHandler(styles, strings, this.minColumns,
				this.output, pojoClass);
		sheetParser.setContentHandler(handler);
		sheetParser.parse(sheetSource);
		return handler.getCollection();
	}

	public <T> List<T> process(Class<T> pojoClass) throws Exception {

		ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(this.xlsxPackage);
		XSSFReader xssfReader = new XSSFReader(this.xlsxPackage);
		List list = null;
		StylesTable styles = xssfReader.getStylesTable();
		XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) xssfReader.getSheetsData();
		while (iter.hasNext()) {
			InputStream stream = iter.next();
			list = processSheet(styles, strings, stream, pojoClass);
			stream.close();
		}

		return list;
	}

	public static <T> List<T> readerExcel(MultipartFile file, int minColumns, Class<T> pojoClass)
			throws Exception {

		if (file == null) {
			return null;
		}
		OPCPackage p = OPCPackage.open(file.getInputStream());
		ExcelCovertCSVReader xlsxTocsv = new ExcelCovertCSVReader(p, minColumns);
		List list = xlsxTocsv.process(pojoClass);
		p.close();
		return list;
	}
}
