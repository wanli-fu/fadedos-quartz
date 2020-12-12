package com.fadedos.quartzstudy.job;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2020/12/12
 */
public class QuartzTest {
    public static void main(String[] args)  {
        try {
            //1.创建调度器Schedule
            Scheduler defaultScheduler = StdSchedulerFactory.getDefaultScheduler();


            //2.定义一个触发条件trigger
  /*      Trigger trigger = newTrigger().withIdentity("trigger1", "group1") //定义name/group
                .startNow() //一旦加入scheduler,立即生效
                .startAt(new Date()) //可以指定时间生效
                .withSchedule(simpleSchedule() //使用SimpleTrigger
                        .withIntervalInSeconds(1) //每隔一秒执行一次
                        .repeatForever()) //一直执行，奔腾到老不停歇
                .build();*/
            Trigger trigger = newTrigger()
                    .withIdentity("trigger1", "group1")
                    .startNow()
                    .withSchedule(simpleSchedule().withIntervalInSeconds(1).repeatForever())
                    .build();


            //3.定义一个JobDetail
        /*JobDetail job = newJob(HelloQuartz.class) //定义Job为HelloQuartz类,这是真正的执行逻辑所在
                .withIdentity("job1", "group1") //定义name/group
                .usingJobData("name", "quartz") //定义属性
                .build();*/
            JobDetail job = newJob(HelloQuartz.class)
                    .withIdentity("job1", "group1")
                    .usingJobData("name", "这是定时任务框架quartz")
                    .build();

            //4.加入这个调度schedule
            defaultScheduler.scheduleJob(job, trigger);

            //5.启动
            defaultScheduler.start();

            //此处运行一断时间关闭
            Thread.sleep(10000);
            defaultScheduler.shutdown(true);
        } catch (SchedulerException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
