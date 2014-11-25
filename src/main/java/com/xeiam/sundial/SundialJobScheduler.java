/**
 * Copyright 2011 - 2013 Xeiam LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xeiam.sundial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.ServletContext;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.exceptions.SchedulerException;
import org.quartz.impl.SchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry-point to the Sundial scheduler
 *
 * @author timmolter
 */
public class SundialJobScheduler {

  /** slf4J logger wrapper */
  static Logger logger = LoggerFactory.getLogger(SundialJobScheduler.class);

  /** Quartz scheduler */
  private static Scheduler scheduler = null;

  /** global lock */
  private static boolean globalLock = false;

  private static ServletContext servletContext = null;

  /**
   * Gets the underlying Sundial scheduler
   *
   * @return
   */
  public static Scheduler getScheduler() {

    if (scheduler == null) {
      scheduler = createScheduler(10);
    }
    return scheduler;
  }

  /**
   * Creates the Sundial Scheduler
   *
   * @param threadPoolSize
   * @return
   */
  public static Scheduler createScheduler(int threadPoolSize) {

    if (scheduler == null) {
      try {
        scheduler = new SchedulerFactory().getScheduler(threadPoolSize);

      } catch (SchedulerException e) {
        logger.error("COULD NOT CREATE SUNDIAL SCHEDULER!!!" + e);
      }
    }
    return scheduler;
  }

  /**
   * Starts the Sundial Scheduler
   */
  public static void startScheduler() {

    try {
      getScheduler().start();
    } catch (SchedulerException e) {
      logger.error("COULD NOT START SUNDIAL SCHEDULER!!!" + e);

    }
  }

  public static void toggleGlobalLock() {

    globalLock = !globalLock;
  }

  public static void lockScheduler() {

    globalLock = true;
  }

  public static void unlockScheduler() {

    globalLock = false;
  }

  public static boolean getGlobalLock() {

    return globalLock;
  }

  /**
   * @return the ServletContext
   */
  public static ServletContext getServletContext() {

    return servletContext;
  }

  /**
   * @param servletContext the ServletContext to set
   */
  public static void setServletContext(ServletContext servletContext) {

    SundialJobScheduler.servletContext = servletContext;
  }

  /**
   * Starts a Job matching the the given Job Name found in jobs.xml
   *
   * @param jobName
   */
  public static void startJob(String jobName) {

    try {
      JobKey jobKey = new JobKey(jobName);
      getScheduler().triggerJob(jobKey, null);
    } catch (SchedulerException e) {
      logger.error("ERROR SCHEDULING FIRE ONCE JOB!!!", e);
    }

  }

  /**
   * Removes a Job matching the the given Job Name found in jobs.xml
   *
   * @param jobName
   */
  public static void removeJob(String jobName) {

    try {
      JobKey jobKey = new JobKey(jobName);
      getScheduler().removeJob(jobKey);
    } catch (SchedulerException e) {
      logger.error("ERROR REMOVING JOB!!!", e);
    }

  }

  /**
   * Starts a Job matching the the given Job Name found in jobs.xml
   *
   * @param jobName
   */
  public static void startJob(String jobName, Map<String, Object> params) {

    try {

      JobDataMap jobDataMap = new JobDataMap();
      for (String key : params.keySet()) {
        // logger.debug("key= " + key);
        // logger.debug("value= " + pParams.get(key));
        jobDataMap.put(key, params.get(key));
      }
      JobKey jobKey = new JobKey(jobName);
      getScheduler().triggerJob(jobKey, jobDataMap);
    } catch (SchedulerException e) {
      logger.error("ERROR SCHEDULING FIRE ONCE JOB!!!", e);
    }

  }

  /**
   * Triggers a Job interrupt on all Jobs matching the given Job Name
   *
   * @param jobName
   */
  public static void stopJob(String jobName) {

    try {
      List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
      for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
        String currentlyExecutingJobName = jobExecutionContext.getJobDetail().getKey().getName();
        if (currentlyExecutingJobName.equals(jobName)) {
          logger.debug("Matching Job found. Now Stopping!");
          if (jobExecutionContext.getJobInstance() instanceof Job) {
            ((Job) jobExecutionContext.getJobInstance()).interrupt();
          }
          else {
            logger.warn("CANNOT STOP NON-INTERRUPTABLE JOB!!!");
          }
        }
        else {
          logger.debug("Non-matching Job found. Not Stopping!");
        }
      }
    } catch (SchedulerException e) {
      logger.error("ERROR DURING STOP Job!!!" + e);
    }
  }

  /**
   * Triggers a Job interrupt on all Jobs matching the given Job Name, key and value
   *
   * @param jobName
   */
  public static void stopJob(String jobName, String key, String pValue) {

    logger.debug("key= " + key);
    logger.debug("value= " + pValue);
    try {
      List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
      for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
        String currentlyExecutingJobName = jobExecutionContext.getJobDetail().getKey().getName();
        if (currentlyExecutingJobName.equals(jobName)) {
          if (jobExecutionContext.getJobInstance() instanceof Job) {
            JobDataMap jobDataMap = jobExecutionContext.getMergedJobDataMap();
            String value = jobDataMap.getString(key);
            if (value != null & value.equalsIgnoreCase(pValue)) {
              ((Job) jobExecutionContext.getJobInstance()).interrupt();
            }
          }
          else {
            logger.warn("CANNOT STOP NON-INTERRUPTABLE JOB!!!");
          }
        }
        else {
          logger.debug("Non-matching Job found. Not Stopping!");
        }
      }
    } catch (SchedulerException e) {
      logger.error("ERROR DURING STOP Job!!!" + e);
    }
  }

  /**
   * Generates an alphabetically sorted List of all Job names in the DEFAULT job group
   *
   * @return
   */
  public static List<String> getAllJobNames() {

    List<String> allJobNames = new ArrayList<String>();
    try {
      Set<JobKey> allJobKeys = getScheduler().getJobKeys(null);
      for (JobKey jobKey : allJobKeys) {
        allJobNames.add(jobKey.getName());
      }
    } catch (SchedulerException e) {
      logger.error("COULD NOT GET JOB NAMES!!!" + e);
    }
    Collections.sort(allJobNames);

    return allJobNames;
  }

  /**
   * Generates a Map of all Job names with corresponding Triggers
   *
   * @return
   */
  public static Map<String, List<Trigger>> getAllJobsAndTriggers() {

    Map<String, List<Trigger>> allJobsMap = new TreeMap<String, List<Trigger>>();
    try {
      Set<JobKey> allJobKeys = getScheduler().getJobKeys(null);
      for (JobKey jobKey : allJobKeys) {
        List<Trigger> triggers = (List<Trigger>) getScheduler().getTriggersOfJob(jobKey);
        allJobsMap.put(jobKey.getName(), triggers);
      }

    } catch (SchedulerException e) {
      logger.error("COULD NOT GET JOB NAMES!!!" + e);
    }
    return allJobsMap;
  }

  public static boolean isJobRunning(String jobName) {

    try {
      List<JobExecutionContext> currentlyExecutingJobs = getScheduler().getCurrentlyExecutingJobs();
      for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
        String currentlyExecutingJobName = jobExecutionContext.getJobDetail().getKey().getName();
        if (currentlyExecutingJobName.equals(jobName)) {
          logger.debug("Matching running Job found!");
          return true;
        }
      }
    } catch (SchedulerException e) {
      logger.error("ERROR CHECKING RUNNING JOB!!!" + e);
    }
    logger.debug("Matching running NOT Job found!");

    return false;
  }

  /**
   * Halts the Scheduler's firing of Triggers, and cleans up all resources associated with the Scheduler.
   */
  public static void shutdown() {

    logger.debug("shutdown() called.");

    try {
      getScheduler().shutdown(true);
    } catch (Exception e) {
      logger.error("COULD NOT SHUTDOWN SCHEDULER!!!", e);
    }
  }
}
