## [![Sundial](https://raw.githubusercontent.com/timmolter/Sundial/develop/etc/Sundial_64_64.png)](http://xeiam.com/sundial) Sundial

A Lightweight Job Scheduling Framework for Java.

## In a Nutshell

Sundial makes adding scheduled jobs to your Java application a walk in the park. Simply define jobs, define triggers, and start the Sundial scheduler.

## Long Description

Sundial is a lightweight Java job scheduling framework forked from
Quartz (http://www.quartz-scheduler.org/) stripped down to the bare essentials. Sundial also hides the
nitty-gritty configuration details of Quartz, reducing the time
needed to get a simple RAM job scheduler up and running. Sundial
uses a ThreadLocal wrapper for each job containing a HashMap for
job key-value pairs. Convenience methods allow easy access to these
parameters. JobActions are reusable components that also have
access to the context parameters. If you are looking
for a hassle-free 100% Java job scheduling framework that is easy to integrate
into your applications, look no further.

## Features

 * [x] Apache 2.0 license
 * [x] ~150 KB Jar
 * [x] In-memory multi-threaded jobs
 * [x] Define jobs and triggers in job.xml
 * [x] or define jobs and triggers via annotations
 * [x] or define jobs and triggers programmatically
 * [x] Cron Triggers
 * [x] Simple Triggers
 * [x] Java 6 and up
 * [x] Depends only on slf4j

## Create a Job Class

```java
public class SampleJob extends com.xeiam.sundial.Job {

  @Override
  public void doRun() throws JobInterruptException {
    // Do something interesting...
  }
}
```
##  ...with CronTrigger or SimpleTrigger Annotation
```java
@CronTrigger(cron = "0/5 * * * * ?")
```
```java
@SimpleTrigger(repeatInterval = 30, timeUnit = TimeUnit.SECONDS)
```

## Start Sundial Job Scheduler

```java
public static void main(String[] args) {

  SundialJobScheduler.startScheduler("com.xeiam.sundial.jobs"); // package with annotated Jobs
}
```

## Alternatively, Put an XML File Called jobs.xml on Classpath

```xml
<?xml version='1.0' encoding='utf-8'?>
<job-scheduling-data>

	<schedule>

		<!-- job with cron trigger -->
		<job>
			<name>SampleJob3</name>
			<job-class>com.foo.bar.jobs.SampleJob3</job-class>
			<concurrency-allowed>true</concurrency-allowed>
		</job>
		<trigger>
			<cron>
				<name>SampleJob3-Trigger</name>
				<job-name>SampleJob3</job-name>
				<cron-expression>*/15 * * * * ?</cron-expression>
			</cron>
		</trigger>

		<!-- job with simple trigger -->
		<job>
			<name>SampleJob2</name>
			<job-class>com.foo.bar.jobs.SampleJob2</job-class>
			<job-data-map>
				<entry>
					<key>MyParam</key>
					<value>42</value>
				</entry>
			</job-data-map>
		</job>
		<trigger>
			<simple>
				<name>SampleJob2-Trigger</name>
				<job-name>SampleJob2</job-name>
				<repeat-count>5</repeat-count>
				<repeat-interval>5000</repeat-interval>
			</simple>
		</trigger>

	</schedule>

</job-scheduling-data>
```

## Or, Define Jobs and Triggers Manually

```java
SundialJobScheduler.addJob("SampleJob", "com.xeiam.sundial.jobs.SampleJob");
SundialJobScheduler.addCronTrigger("SampleJob-Cron-Trigger", "SampleJob", "0/10 * * * * ?");
SundialJobScheduler.addSimpleTrigger("SampleJob-Simple-Trigger", "SampleJob", -1, TimeUnit.SECONDS.toMillis(3));
```

## More Functions

```java
// asynchronously start a job by name
SundialJobScheduler.startJob("SampleJob");

// interrupt a running job
SundialJobScheduler.stopJob("SampleJob");

// remove a job from the scheduler
SundialJobScheduler.removeJob("SampleJob");

// remove a trigger from the scheduler
SundialJobScheduler.removeTrigger("SampleJob-Trigger");

// lock scheduler
SundialJobScheduler.lockScheduler();

// unlock scheduler
SundialJobScheduler.unlockScheduler();

// check if job a running
SundialJobScheduler.isJobRunning("SampleJob");
```
And many more useful functions. See all here: https://github.com/timmolter/Sundial/blob/develop/src/main/java/com/xeiam/sundial/SundialJobScheduler.java

## Job Data Map
```java
// asynchronously start a job by name with data map
Map<String, Object> params = new HashMap<>();
params.put("MY_KEY", new Integer(660));
SundialJobScheduler.startJob("SampleJob1", params);
```
```java
// annotate CronTrigger with data map (separate key/values with ":" )
@CronTrigger(cron = "0/5 * * * * ?", jobDataMap = { "KEY_1:VALUE_1", "KEY_2:1000" })
public class SampleJob extends Job {
}
```
```xml
<!-- configure data map in jobs.xml -->
<job>
  <name>SampleJob</name>
  <job-class>com.xeiam.sundial.jobs.SampleJob</job-class>
  <job-data-map>
    <entry>
      <key>MyParam</key>
      <value>42</value>
    </entry>
  </job-data-map>
</job>
```
```java
// access data inside Job
String value1 = getJobContext().get("KEY_1");
logger.info("value1 = " + value1);
```

## Get Organized with Job Actions!

With `JobAction`s, you can encapsule logic that can be shared by different `Job`s.
```java
public class SampleJobAction extends JobAction {

  @Override
  public void doRun() {

    Integer myValue = getJobContext().get("MyValue");

    // Do something interesting...
  }
}
```
```java
// Call the JobAction from inside a Job
getJobContext().put("MyValue", new Integer(123));
new SampleJobAction().run();
```

Now go ahead and [study some more examples](http://xeiam.com/sundial-example-code), [download the thing](http://xeiam.com/sundial-change-log) and [provide feedback](https://github.com/timmolter/Sundial/issues).

## Getting the Goods
### Non-Maven
Download Jar: http://xeiam.com/sundial-change-log
#### Dependencies
* org.slf4j.slf4j-api-1.7.10

### Maven

The Sundial release artifacts are hosted on Maven Central.

Add the Sundial library as a dependency to your pom.xml file:

```xml
<dependency>
    <groupId>com.xeiam</groupId>
    <artifactId>sundial</artifactId>
    <version>2.0.0</version>
</dependency>
```

For snapshots, add the following to your pom.xml file:

```xml
<repository>
  <id>sonatype-oss-snapshot</id>
  <snapshots/>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
</repository>

<dependency>
    <groupId>com.xeiam</groupId>
    <artifactId>sundial</artifactId>
    <version>2.0.1-SNAPSHOT</version>
</dependency>
```

## Building

    mvn clean package  
    mvn javadoc:javadoc  

## Cron Expressions in jobs.xml

See the Cron Trigger tutorial over at [quartz-scheduler.org](http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/crontrigger).
Here are a few examples:  

Expression | Meaning
------------- | -------------
0 0 12 * * ? | Fire at 12pm (noon) every day
0 15 10 * * ? | Fire at 10:15am every day
0 15 10 ? * MON-FRI | Fire at 10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
0 0/10 * * * ? | Fire every 10 mintes starting at 12 am (midnight) every day

## Bugs
Please report any bugs or submit feature requests to [Sundial's Github issue tracker](https://github.com/timmolter/Sundial/issues).  

## Continuous Integration
[![Build Status](https://travis-ci.org/timmolter/Sundial.png?branch=develop)](https://travis-ci.org/timmolter/Sundial.png)  
[Build History](https://travis-ci.org/timmolter/Sundial/builds)  

## Donations
15MvtM8e3bzepmZ5vTe8cHvrEZg6eDzw2w  
