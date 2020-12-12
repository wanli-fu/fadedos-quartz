package com.fadedos.quartzstudy.job;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.xml.soap.Detail;
import java.util.Date;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2020/12/12
 */
public class HelloQuartz  implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        //通过jobExecutionContext对象 获取到quartz运行时的环境以及Job本身的明细数据
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        String value = jobDetail.getJobDataMap().getString("name");
        System.out.println("你好"+value+"此时时间为"+new Date());
    }
}
