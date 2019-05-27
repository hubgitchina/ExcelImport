package com.huafa.webapp.controller.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.huafa.core.util.ExcelUtil;
import com.wuwenze.poi.ExcelKit;
import com.wuwenze.poi.handler.ExcelReadHandler;
import com.wuwenze.poi.pojo.ExcelErrorField;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Lists;
import com.huafa.core.dao.test.WpMapper;
import com.huafa.core.entity.test.Wp;
import com.huafa.core.exception.BusinessException;
import com.huafa.core.exception.ExceptionEnum;
import com.huafa.core.util.ExcelCovertCSVReader;
import com.huafa.core.util.PageResultBean;
import com.huafa.core.util.ResultBean;
import com.huafa.webapp.annotation.SysTLog;


@Controller
@RequestMapping(value = "/test")
public class ImportController {

	Logger logger = LoggerFactory.getLogger(this.getClass());

	public List<Wp> createList(int num) {

		List<Wp> listTestVo = Lists.newArrayListWithCapacity(10000);
		for (int i = 0; i < num; i++) {
			Wp testVo = new Wp();
			testVo.setId(IdUtil.simpleUUID());
			testVo.setCreateBy("zhangshan");
			testVo.setE1("zhangshan");
			testVo.setE2("zhangshan");
			testVo.setE3("zhangshan");
			testVo.setE4("zhangshan");
			testVo.setE5("zhangshan");
			testVo.setE6("zhangshan");
			testVo.setE7("zhangshan");
			testVo.setE8("zhangshan");
			testVo.setE9("zhangshan");
			testVo.setE10("zhangshan");
			testVo.setE11("zhangshan");
			testVo.setE12("zhangshan");
			testVo.setE13("zhangshan");
			testVo.setE14(111);
			testVo.setE15(1L);
			testVo.setE16(new Date());
			testVo.setE17("zhangshan");
			testVo.setE18("zhangshan");
			testVo.setE19("zhangshan");
			listTestVo.add(testVo);
		}
		return listTestVo;
	}

	public void multiThreadImport(final int ThreadNum) {
		List<List<Wp>> list = new ArrayList<>(ThreadNum);
		for(int i=0; i<ThreadNum; i++){
			List<Wp> tempList = createList(500);
			list.add(tempList);
		}

		final CountDownLatch cdl = new CountDownLatch(ThreadNum);
		long starttime = System.currentTimeMillis();
		for (int k = 0; k < ThreadNum; k++) {
			int b = k;

			new Thread(new Runnable() {
				@Override
				public void run() {

					try {
						mapper.insertList(list.get(b));
					} catch (Exception e) {
					} finally {
						cdl.countDown();
					}
				}
			}).start();
		}
		try {
			cdl.await();
			long spendtime = System.currentTimeMillis() - starttime;
			System.out.println(ThreadNum + "个线程花费时间:" + spendtime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@PostMapping("/importExcel")
	@ResponseBody
	public ResultBean<Wp> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
		// long start = System.currentTimeMillis();
		// List<Wp> list = ExcelUtil.importExcel(file, 0, 1, Wp.class);
		// long end = System.currentTimeMillis();
		// logger.info("开始时间：{}", start);
		// logger.info("结束时间：{}", end);
		// logger.info("总耗时：{}", end - start);
		// return null;

		// long start = System.currentTimeMillis();
		// List<Wp> successList = Lists.newArrayList();
		// ExcelKit.$Import(Wp.class)
		// .readXlsx(file.getInputStream(), new ExcelReadHandler<Wp>() {
		// @Override
		// public void onSuccess(int sheetIndex, int rowIndex, Wp entity) {
		// successList.add(entity); // 单行读取成功，加入入库队列。
		// }
		//
		// @Override
		// public void onError(int sheetIndex, int rowIndex,
		// List<ExcelErrorField> errorFields) {
		//
		// }
		// });
		//// mapper.insertList(successList);
		// long end = System.currentTimeMillis();
		// logger.info("开始时间：{}", start);
		// logger.info("结束时间：{}", end);
		// logger.info("总耗时：{}", end - start);

//多线程插入
		multiThreadImport(24);

//大数据量Excel导入
//		try {
//			long start = System.currentTimeMillis();
//			List<Wp> list = ExcelCovertCSVReader.readerExcel(file, 19, Wp.class);
//			for (Wp temp : list) {
//				temp.setId(RandomUtil.simpleUUID());
//				temp.setCreateBy("test");
//			}
//			// mapper.insertList(list);
//			long end = System.currentTimeMillis();
//			logger.info("开始时间：{}", start);
//			logger.info("结束时间：{}", end);
//			logger.info("总耗时：{}", end - start);
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

		return null;
	}

	@GetMapping("/import/reviewImport")
	public ResultBean<List<String>> importSellerReview(@RequestParam("filePath") String filePath) {

		long start = System.currentTimeMillis();

		BufferedReader br = null;
		try {
			List<Wp> listTestVo = Lists.newArrayList();
			br = new BufferedReader(new FileReader(filePath));
			String line = "";
			while ((line = br.readLine()) != null) {
				List<String> afterTreatmentLine = Arrays.asList(line.split(","));
				// List<String> afterTreatmentLine = linePretreatment(arrayList);

				// 业务处理
				if (CollectionUtils.isNotEmpty(afterTreatmentLine)) {
					Wp testVo = new Wp();
					testVo.setCreateBy("zhangshan");
					testVo.setE1(afterTreatmentLine.get(0));
					testVo.setE2(afterTreatmentLine.get(1));
					testVo.setE3(afterTreatmentLine.get(2));
					testVo.setE4(afterTreatmentLine.get(3));
					testVo.setE5(afterTreatmentLine.get(4));
					testVo.setE6(afterTreatmentLine.get(5));
					testVo.setE7(afterTreatmentLine.get(6));
					testVo.setE8(afterTreatmentLine.get(7));
					testVo.setE9(afterTreatmentLine.get(8));
					testVo.setE10(afterTreatmentLine.get(9));
					testVo.setE11(afterTreatmentLine.get(10));
					testVo.setE12(afterTreatmentLine.get(11));
					testVo.setE13(afterTreatmentLine.get(12));
					// testVo.setE14(afterTreatmentLine.get(13));
					// testVo.setE15(afterTreatmentLine.get(14));
					// testVo.setE16(afterTreatmentLine.get(15));
					testVo.setE17(afterTreatmentLine.get(16));
					testVo.setE18(afterTreatmentLine.get(17));
					testVo.setE19(afterTreatmentLine.get(18));
					listTestVo.add(testVo);
				}
			}
			mapper.insertList(listTestVo);
		} catch (Exception e) {
		} finally {
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// List<String> unSuccessLineNum = new ArrayList<>();
		// try {
		// List<String> lines = FileUtils.readLines(new File(filePath), "UTF-8");
		// List<Wp> listTestVo = Lists.newArrayListWithCapacity(lines.size());
		// int lineNum = 0;
		// for (String line : lines) {
		// lineNum++;
		// List<String> afterTreatmentLine = Arrays.asList(line.split(","));
		// // List<String> afterTreatmentLine = linePretreatment(arrayList);
		//
		// // 业务处理
		// if (CollectionUtils.isNotEmpty(afterTreatmentLine)) {
		// String e1 = afterTreatmentLine.get(0);
		//
		// Wp testVo = new Wp();
		// testVo.setCreateBy("zhangshan");
		// testVo.setE1(afterTreatmentLine.get(0));
		// testVo.setE2(afterTreatmentLine.get(1));
		// testVo.setE3(afterTreatmentLine.get(2));
		// testVo.setE4(afterTreatmentLine.get(3));
		// testVo.setE5(afterTreatmentLine.get(4));
		// testVo.setE6(afterTreatmentLine.get(5));
		// testVo.setE7(afterTreatmentLine.get(6));
		// testVo.setE8(afterTreatmentLine.get(7));
		// testVo.setE9(afterTreatmentLine.get(8));
		// testVo.setE10(afterTreatmentLine.get(9));
		// testVo.setE11(afterTreatmentLine.get(10));
		// testVo.setE12(afterTreatmentLine.get(11));
		// testVo.setE13(afterTreatmentLine.get(12));
		// testVo.setE14(afterTreatmentLine.get(13));
		// testVo.setE15(afterTreatmentLine.get(14));
		// testVo.setE16(afterTreatmentLine.get(15));
		// testVo.setE17(afterTreatmentLine.get(16));
		// testVo.setE18(afterTreatmentLine.get(17));
		// // System.out.println(afterTreatmentLine);
		// // System.out.println(afterTreatmentLine.size());
		// testVo.setE19(afterTreatmentLine.get(18));
		// listTestVo.add(testVo);
		// }
		// }
		//
		// mapper.insertList(listTestVo);
		// } catch (IOException e) {
		// // 错误日志记录
		// }

		long end = System.currentTimeMillis();
		logger.info("开始时间：{}", start);
		logger.info("结束时间：{}", end);
		logger.info("总耗时：{}", end - start);
		return new ResultBean<List<String>>();
	}

	private List<String> linePretreatment(List<String> lines) {

		List<String> newLines = new ArrayList<>(lines.size());
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("\"") && i != lines.size() - 1 && lines.get(i + 1).endsWith("\"")) {
				String newLine = line + lines.get(i + 1);
				newLines.add(newLine);
				i++;
			} else {
				newLines.add(line);
			}
		}
		return newLines;
	}

	private String getFromListDefaultNull(List<String> list, int index) {

		String str = null;
		try {
			str = StringUtils.trim(list.get(index));
		} catch (Exception e) {
			new BusinessException("-1", "importItemReview get column error");
		}
		return str;
	}

}
