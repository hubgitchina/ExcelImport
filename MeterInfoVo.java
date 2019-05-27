package com.huafa.core.entity.meter.vo;


import cn.afterturn.easypoi.excel.annotation.Excel;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import java.util.Date;
import java.util.List;

public class MeterInfoVo {

    @Excel(name = "抄数Id", orderNum = "1")
    private String meterDataId;

    @Excel(name = "抄表日期", orderNum = "2",  exportFormat="yyyy-MM-dd", importFormat="yyyy-MM-dd")
    private Date readDate;

}
