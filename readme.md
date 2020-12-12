# 1. Quartz简介
Quartz是一个开源的作业调度框架,它完全由java写成,并设计用于J2SE和J2EE应用中,它提供了巨大的灵活而不牺牲简单性. 

`当定时任务愈加复杂时,使用Spring注解@Schedule已经不能满足业务需要了.`
# 1. Quartz的简单示例
## 1.1 创建一个作业
真正的任务逻辑在这个作业里面实现了`Job`,下例中的HelloQuartz.`在这个类中，我们通过 JobExecutionContext 来获取JobDetail、JobDataMap、key以及通过自定义传入的信息`

```java
/**
 * @author pengcheng
 * @Description: ${todo}
 * @date 2019/11/12   16:05
 */
public class HelloQuartz implements Job{

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 通过JobExecutionContext 对象访问到Quartz运行时候的环境以及Job本身的明细数据
        JobDetail detail = context.getJobDetail();
        String name = detail.getJobDataMap().getString("name");
        //此任务内容
        System.out.println("say hello to "+name+" at "+new Date());
    }
}
```
## 1.2 创建触发器,以及通过调度执行作业
简单的类`QuartzTest`
```java
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;


/**
 * @author pengcheng
 * @Description: ${todo}
 * @date 2019/11/12   15:08
 */
public class QuartzTest {
    public static void main(String[] args) {
        try {
            //创建scheduler
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();

            //定义一个Trigger
            Trigger trigger = newTrigger().withIdentity("trigger1", "group1") //定义name/group
                    .startNow() //一旦加入scheduler,立即生效
                    .withSchedule(simpleSchedule() //使用SimpleTrigger
                    .withIntervalInSeconds(1) //每隔一秒执行一次
                    .repeatForever()) //一直执行，奔腾到老不停歇
                    .build();

            //定义一个JobDetail
            JobDetail job = newJob(HelloQuartz.class) //定义Job为HelloQuartz类,这是真正的执行逻辑所在
                    .withIdentity("job1", "group1") //定义name/group
                    .usingJobData("name", "quartz") //定义属性
                    .build();

            //加入这个调度
            scheduler.scheduleJob(job, trigger);

            //启动之
            scheduler.start();

            //运行一段时间后关闭
            Thread.sleep(10000);
            scheduler.shutdown(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

# 2. Quartz的体系结构 
* Scheduler:调度器,所有的调度都是由它控制
* Trigger:定义触发条件.
* JobDetail & job:`JobDetail定义的是任务数据,而真正的执行逻辑是在Job中.` 

`上例中job是HelloQuartz为什么会设计成JobDetail+Job,不直接使用Job?`
```java
  //定义一个JobDetail
            JobDetail job = newJob(HelloQuartz.class) //定义Job为HelloQuartz类,这是真正的执行逻辑所在
                    .withIdentity("job1", "group1") //定义name/group
                    .usingJobData("name", "quartz") //定义属性
                    .build(); //创建JobDetail
``` 

* 答曰: 
`这是因为任务是有可能并发执行的,如果Scheduler直接使用Job,就会存在对同一个Job实例并发访问的问题`.例如有的job触发条件时1s一次,而此job执行时间需要10s,会存在并发情况.

* JobDetail & Job方式 
sheduler每次执行,都会根据JobDetail创建一个新的实例,这样就可以规避并发访问的问题. ` 
* 查看源码: 
	* `JobBuilder jobBuilder = newJob(HelloQuartz.class);` 
	* `JobDetail jobDetail = jobBuilder.build();`
```java
    /**
     * Create a JobBuilder with which to define a <code>JobDetail</code>,
     * and set the class name of the <code>Job</code> to be executed.
     * 
     * @return a new JobBuilder
     */
    public static JobBuilder newJob(Class <? extends Job> jobClass) {
        JobBuilder b = new JobBuilder();
        b.ofType(jobClass);
        return b;
    }
``` 
```java
 /**
     * Produce the <code>JobDetail</code> instance defined by this 
     * <code>JobBuilder</code>.
     * 
     * @return the defined JobDetail.
     */
    public JobDetail build() {

        JobDetailImpl job = new JobDetailImpl();
        
        job.setJobClass(jobClass);
        job.setDescription(description);
        if(key == null)
            key = new JobKey(Key.createUniqueName(null), null);
        job.setKey(key); 
        job.setDurability(durability);
        job.setRequestsRecovery(shouldRecover);
        
        
        if(!jobDataMap.isEmpty())
            job.setJobDataMap(jobDataMap);
        
        return job;
    }
```	
# 3. Quarzt API 
Quartz的API的风格在2.x以后，采用的是DSL风格（通常意味着fluent interface风格）
## 3.1 builder 
```java
//job相关的builder
import static org.quartz.JobBuilder.*;

//trigger相关的builder
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.DailyTimeIntervalScheduleBuilder.*;
import static org.quartz.CalendarIntervalScheduleBuilder.*;

//日期相关的builder
import static org.quartz.DateBuilder.*;
``` 
## 3.2 关于name和group
JobDetail和Trigger都有name和group 
`name是它们在schedule里面的唯一标识.如果我们要更新一个JobDetail定义,只需要设置一个name相同的detail实例即可` 
`group是一个组织单元,schedule会提供一些对整租操作的API`,比如scheduler.resumeJobs(). 
## 3.3 Trigger 
Trigger:定义触发条件.在详细了解每一种Trigger之前,需要先了解下trigger的一些共性. 
* StartTime & EndTime 
`StartTime和EndTime指定Tirgger会被触发的时间区间.在这个区间之外,Tigger是不会被触发的.`
* 优先级(Priority)
当scheduler比较繁忙的时候，可能在同一个时刻，有多个Trigger被触发了，但资源不足（比如线程池不足）。那么这个时候比剪刀石头布更好的方式，就是设置优先级。优先级高的先执行。
需要注意的是，优先级只有在同一时刻执行的Trigger之间才会起作用，如果一个Trigger是9:00，另一个Trigger是9:30。那么无论后一个优先级多高，前一个都是先执行。
优先级的值默认是5，当为负数时使用默认值。最大值似乎没有指定，但建议遵循Java的标准，使用1-10，不然鬼才知道看到【优先级为10】是时，上头还有没有更大的值。
* Misfire(错失触发）策略
类似的Scheduler资源不足的时候，或者机器崩溃重启等，有可能某一些Trigger在应该触发的时间点没有被触发，也就是Miss Fire了。这个时候Trigger需要一个策略来处理这种情况。每种Trigger可选的策略各不相同。
这里有两个问题需要重点注意: 
MisFire的触发是有一个阈值,这个阈值时配置在JobStore的.只有超过这个阈值,才会算Misfire.小于这个阈值,Quartz是会全部重新触发的. 
所有的Misfire的策略实际上都是解答这两个问题: 
`1. 已经MisFire的任务还要重新触发吗?` 
`2.如果发生MisFire,要调整现有的调度时间吗?` 

## 3.4 Calendar 
这里的Calendar不是jdk的java.util.Calendar,不是为了计算日期的.它的作用是在于补充Trigger的时间.`可以排除或加入一些特定的时间点`
以"每月25日零点自动还卡债"为例,我们想排除每年的2月25日零点这个时间点(因为2.14日,所以2月一定会破产,滑稽).这个时间,就可以用Calendar来实现. 

```java
AnnualCalendar cal = new AnnualCalendar(); //定义一个每年执行Calendar，精度为天，即不能定义到2.25号下午2:00
    java.util.Calendar excludeDay = new GregorianCalendar();
excludeDay.setTime(newDate().inMonthOnDay(2, 25).build());
        cal.setDayExcluded(excludeDay, true);  //设置排除2.25这个日期
        scheduler.addCalendar("FebCal", cal, false, false); //scheduler加入这个Calendar

//定义一个Trigger
        Trigger trigger = newTrigger().withIdentity("trigger1", "group1")
        .startNow()//一旦加入scheduler，立即生效
        .modifiedByCalendar("FebCal") //使用Calendar !!
        .withSchedule(simpleSchedule()
        .withIntervalInSeconds(1)
        .repeatForever())
        .build();
``` 
Quartz体贴地为我们提供以下几种Calendar,注意,所有的Calendar既可以是排除,也可以是包含,取决于:
* HolidayCalendar。指定特定的日期，比如20140613。精度到天。
* DailyCalendar。指定每天的时间段（rangeStartingTime, rangeEndingTime)，格式是HH:MM[:SS[:mmm]]。也就是最大精度可以到毫秒。
* WeeklyCalendar。指定每星期的星期几，可选值比如为java.util.Calendar.SUNDAY。精度是天。
* MonthlyCalendar。指定每月的几号。可选值为1-31。精度是天
* AnnualCalendar。 指定每年的哪一天。使用方式如上例。精度是天。
* CronCalendar。指定Cron表达式。精度取决于Cron表达式，也就是最大精度可以到秒。
## 3.5 Trigger实现类 
Quartz有以下几种trigger实现:
* 1 SimpleTrigger 
指定从某一个时间开始，以一定的时间间隔（单位是毫秒）执行的任务。
它适合的任务类似于：9:00 开始，每隔1小时，执行一次。
它的属性有: 
repeatInterval 重复间隔 

repeatCount重复次数.`实际执行次数是repeatCount+1,因为StartTime的时候一定会被执行一次. 
```java
simpleSchedule()
        .withIntervalInHours(1) //每小时执行一次
        .repeatForever() //次数不限
        .build();

simpleSchedule()
    .withIntervalInMinutes(1) //每分钟执行一次
    .withRepeatCount(10) //次数为10次
    .build();
``` 
* 2 CalendarIntervalTrigger 
类似于Simpletrigger,指定从某一个时间开始,以一定的时间间隔执行任务.但是不同的SimpleTrigger指定的时间间隔为**毫秒**,没办法指定每隔一个月执行一次,`而CalendarIntervalTrigger支持的间隔单位有秒，分钟，小时，天，月，年，星期。` 
相较于SimpleTrigger有两个优势：1、更方便，比如每隔1小时执行，你不用自己去计算1小时等于多少毫秒。 2、支持不是固定长度的间隔，比如间隔为月和年。但劣势是精度只能到秒。
它适合的任务类似于：9:00 开始执行，并且以后每周 9:00 执行一次 
它的属性有:
	* interval 执行间隔 
	* intervalunit 执行间隔单位 (秒,分钟,小时,天,月,年,星期) 
如下: 
```java 
calendarIntervalSchedule()
    .withIntervalInDays(1) //每天执行一次
    .build();

calendarIntervalSchedule()
    .withIntervalInWeeks(1) //每周执行一次
    .build();
``` 
* 3 CronTrigger 
适合于更复杂的任务,它支持类型于linux Cron的语法(并且更强大).基本上它覆盖了以上三个Trigger的绝大部分功能(但不是全部)
如下:
```java 
cronSchedule("0 0/2 8-17 * * ?") // 每天8:00-17:00，每隔2分钟执行一次
    .build();

cronSchedule("0 30 9 ? * MON") // 每周一，9:30执行一次
.build();

weeklyOnDayAndHourAndMinute(MONDAY,9, 30) //等同于 0 30 9 ? * MON 
    .build();
```
## 3.6 JobDetail & Job 
JobDetail是任务的定义,而Job是任务执行的逻辑.在JobDetail里会引用一个Job Class定义.一个最简单的例子 
```java 
public class JobTest {
    public static void main(String[] args) throws SchedulerException, IOException {
           JobDetail job=newJob()
               .ofType(DoNothingJob.class) //引用Job Class
               .withIdentity("job1", "group1") //设置name/group
               .withDescription("this is a test job") //设置描述
               .usingJobData("age", 18) //加入属性到ageJobDataMap
               .build();

           job.getJobDataMap().put("name", "quertz"); //加入属性name到JobDataMap

           //定义一个每秒执行一次的SimpleTrigger
           Trigger trigger=newTrigger()
                   .startNow()
                   .withIdentity("trigger1")
                   .withSchedule(simpleSchedule()
                       .withIntervalInSeconds(1)
                       .repeatForever())
                   .build();

           Scheduler sche=StdSchedulerFactory.getDefaultScheduler();
           sche.scheduleJob(job, trigger);

           sche.start();

           System.in.read();

           sche.shutdown();
    }
}


public class DoNothingJob implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("do nothing");
    }
}
``` 
从上例可以看出,要定义一个任务,需要干几件事:
* 创建一个org.quartz.Job的实现类，并实现实现自己的业务逻辑。比如上面的DoNothingJob。
* 定义一个JobDetail，引用这个实现类
* 加入scheduleJob

QuartZ调度一次任务,会干如下的事:
* 1 JobClass jobClass=JobDetail.getJobClass()
* 2 Job jobInstance=jobClass.newInstance()。所以Job实现类，必须有一个public的无参构建方法。
* 3 `jobInstance.execute(JobExecutionContext context)。JobExecutionContext是Job运行的上下文，可以获得Trigger、Scheduler、JobDetail的信息。` 
也就是说,`每次每次调度都会创建一个新的JOB实例`,这样的好处是有些任务并发执行的时候,不存在对临界资源的并发访问的问题. 

* 4 JobDataMap 
Job每次都是newInStance的实例,那我怎么传值给它?比如我现在有两个发送邮件的任务,一个是发个"lilei",另一个是"hanmeimei",不能说我要写两个Job实现类LiLeiSendEmailJob和HanMeiMeiSendEmailJob.实现的办法是通过`jobDataMap` 
每一个的JoDetail都有一个JobDataMap.JobDataMap本质是一个Map的扩展类,只是提供了一些更便捷的方法,比如getString()之类的.
我们可以在定义JobDetail,加入属性值,方式有二: 
```java
newJob().usingJobData("age", 18) //加入属性到ageJobDataMap

 or

 job.getJobDataMap().put("name", "quertz"); //加入属性name到JobDataMap
```
然后在Job中可以获取这个JobDataMap的值,方式同样有二:
```java
public class HelloQuartz implements Job {
    private String name;

    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail detail = context.getJobDetail();
        JobDataMap map = detail.getJobDataMap(); //方法一：获得JobDataMap
        System.out.println("say hello to " + name + "[" + map.getInt("age") + "]" + " at "
                           + new Date());
    }

    //方法二：属性的setter方法，会将JobDataMap的属性自动注入
    public void setName(String name) { 
        this.name = name;
    }
}
``` 
`对于同一个JobDetail实例,执行的多个Job实例,是共享同样的JobDataMap,也就是说,如果你在任务里修改了里面的值,会对其他Job实例(并发的或者后续的)造成影响.` 
除了JobDetail,Trigger同样有一个JobDataMap,共享范围是所有使用这个Trigger的Job实例. 
* `Job并发` 
Job是有可能并发执行的,比如一个任务要执行10s,而调度算法是每1秒中触发一次,那么就有多个任务被并发执行. 
有时候我们并不想任务并发执行,比如这个任务要去"获得数据库中所有未发邮件的名单",如果是并发执行,就需要一个数据库锁去避免一个数据被多次处理.这个时候一个`@DisallowConcurrentExecution`解决这个问题。
```java 
public class DoNothingJob implements Job {
    @DisallowConcurrentExecution
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("do nothing");
    }
}
``` 
注意: `@DisallowConcurrentExecution是对JobDetail实例生效，也就是如果你定义两个JobDetail，引用同一个Job类，是可以并发执行的.` 
* JobExecutionException
Job.execute()方法是不允许抛出除JobExecutionException之外的所有异常的（包括RuntimeException)，所以编码的时候，最好是try-catch住所有的Throwable，小心处理。
* 其他属性
	* 1 Durability(耐久性？) 
如果一个任务不是durable，那么当没有Trigger关联它的时候，它就会被自动删除。

* 2 RequestsRecovery
如果一个任务是"requests recovery"，那么当任务运行过程非正常退出时（比如进程崩溃，机器断电，但不包括抛出异常这种情况），Quartz再次启动时，会重新运行一次这个任务实例。
可以通过JobExecutionContext.isRecovering()查询任务是否是被恢复的。
## 3.7 Scheduler
Scheduler就是Quartz的大脑,所有的任务都是由它来实施的.
Scheduler包含两个重要的组件:`JobStore和ThreadPool` 
* JobStore:存储运行时信息的，包括Trigger,Schduler,JobDetail，业务锁等。它有多种实现RAMJob(内存实现)，JobStoreTX(JDBC，事务由Quartz管理），JobStoreCMT(JDBC，使用容器事务)，ClusteredJobStore(集群实现)、TerracottaJobStore(什么是Terractta)。
*ThreadPool: 线程池，Quartz有自己的线程池实现。所有任务的都会由线程池执行。 
`SchedulerFactory` 
SchdulerFactory，顾名思义就是来用创建Schduler了，有两个实现：`DirectSchedulerFactory和 StdSchdulerFactory。前者可以用来在代码里定制你自己的Schduler参数。后者是直接读取classpath下的quartz.properties（不存在就都使用默认值）配置来实例化Schduler。通常来讲，我们使用StdSchdulerFactory也就足够了` 
	* DirectSchedulerFactory的创建接口
```java 
    /**
     * Same as
     * {@link DirectSchedulerFactory#createScheduler(ThreadPool threadPool, JobStore jobStore)},
     * with the addition of specifying the scheduler name and instance ID. This
     * scheduler can only be retrieved via
     * {@link DirectSchedulerFactory#getScheduler(String)}
     *
     * @param schedulerName
     *          The name for the scheduler.
     * @param schedulerInstanceId
     *          The instance ID for the scheduler.
     * @param threadPool
     *          The thread pool for executing jobs
     * @param jobStore
     *          The type of job store
     * @throws SchedulerException
     *           if initialization failed
     */
    public void createScheduler(String schedulerName,
            String schedulerInstanceId, ThreadPool threadPool, JobStore jobStore)
        throws SchedulerException;
``` 
* StdSchdulerFactory的配置例子， 更多配置参见文档 
```Java
org.quartz.scheduler.instanceName = DefaultQuartzScheduler
org.quartz.threadPool.class = org.quartz.simpl.SimpleThreadPool
org.quartz.threadPool.threadCount = 10 
org.quartz.threadPool.threadPriority = 5
org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread = true
org.quartz.jobStore.class = org.quartz.simpl.RAMJobStore
```
# 4.Spring boot集成quartz持久化管理
## 4.1 pom文件引入quartz依赖 
Spring Boot 2.0 提供了 spring-boot-starter-quartz 组件集成 Quartz，让我们在项目中使用 Quartz 变得简单。

配置内容:
pom.xml添加spring-boot-start-quartz组件;
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-quartz</artifactId>
</dependency>
``` 