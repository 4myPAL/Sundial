/*
 * Copyright 2001-2010 Terracotta, Inc.
 * Copyright 2011 Xeiam LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */

package org.quartz.xml;

import static org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CalendarIntervalTrigger.IntervalUnit;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.exceptions.ObjectAlreadyExistsException;
import org.quartz.exceptions.SchedulerException;
import org.quartz.simpl.CascadingClassLoadHelper;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.MutableTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Parses an XML file that declares Jobs and their schedules (Triggers), and processes the related data. The xml document must conform to the format defined in "job_scheduling_data_1_8.xsd" The same
 * instance can be used again and again, however a single instance is not thread-safe.
 *
 * @author James House
 * @author Past contributions from <a href="mailto:bonhamcm@thirdeyeconsulting.com">Chris Bonham</a>
 * @author Past contributions from pl47ypus
 * @author timmolter
 * @since Quartz 1.8
 */
public class XMLSchedulingDataProcessor implements ErrorHandler {

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constants. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private static final String QUARTZ_XSD_PATH_IN_JAR = "com/xeiam/sundial/xml/job_scheduling_data.xsd";

  public static final String QUARTZ_XML_DEFAULT_FILE_NAME = "jobs.xml";

  /**
   * XML Schema dateTime datatype format.
   * <p>
   * See <a href="http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime"> http://www.w3.org/TR/2001/REC-xmlschema-2-20010502/#dateTime</a>
   */
  private static final String XSD_DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss";

  private static final SimpleDateFormat dateFormat = new SimpleDateFormat(XSD_DATE_FORMAT);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Data members. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  // scheduling commands
  private List<JobDetail> loadedJobs = new LinkedList<JobDetail>();
  private List<Trigger> loadedTriggers = new LinkedList<Trigger>();

  private Collection<Exception> validationExceptions = new ArrayList<Exception>();

  private ClassLoadHelper classLoadHelper = null;
  private List<String> jobGroupsToNeverDelete = new LinkedList<String>();
  private List<String> triggerGroupsToNeverDelete = new LinkedList<String>();

  private DocumentBuilder docBuilder = null;
  private XPath xpath = null;

  private final Logger loggger = LoggerFactory.getLogger(XMLSchedulingDataProcessor.class);

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Constructors. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  /**
   * Constructor for JobSchedulingDataLoader.
   *
   * @param clh class-loader helper to share with digester.
   * @throws ParserConfigurationException if the XML parser cannot be configured as needed.
   */
  public XMLSchedulingDataProcessor() throws ParserConfigurationException {

    classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();

    initDocumentParser();
  }

  /**
   * Initializes the XML parser.
   *
   * @throws ParserConfigurationException
   */
  private void initDocumentParser() throws ParserConfigurationException {

    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

    docBuilderFactory.setNamespaceAware(false);
    docBuilderFactory.setValidating(true);

    docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");

    docBuilderFactory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaSource", resolveSchemaSource());

    docBuilder = docBuilderFactory.newDocumentBuilder();

    docBuilder.setErrorHandler(this);

    xpath = XPathFactory.newInstance().newXPath();
  }

  private Object resolveSchemaSource() {

    InputSource inputSource = null;

    InputStream is = null;

    // try {
    is = classLoadHelper.getResourceAsStream(QUARTZ_XSD_PATH_IN_JAR);

    if (is == null) {
      loggger.warn("Could not load xml scheme from classpath");
    }
    else {
      inputSource = new InputSource(is);
    }

    return inputSource;
  }

  /**
   * Add the given group to the list of job groups that will never be deleted by this processor, even if a pre-processing-command to delete the group is encountered.
   *
   * @param group
   */
  public void addJobGroupToNeverDelete(String group) {

    if (group != null) {
      jobGroupsToNeverDelete.add(group);
    }
  }

  /**
   * Add the given group to the list of trigger groups that will never be deleted by this processor, even if a pre-processing-command to delete the group is encountered.
   *
   * @param group
   */
  public void addTriggerGroupToNeverDelete(String group) {

    if (group != null) {
      triggerGroupsToNeverDelete.add(group);
    }
  }

  /**
   * Process the xml file in the given location, and schedule all of the jobs defined within it.
   *
   * @param fileName meta data file name.
   */
  public void processFile(String fileName, boolean failOnFileNotFound) throws Exception {

    boolean fileFound = false;
    InputStream f = null;
    try {
      String furl = null;

      File file = new File(fileName); // files in filesystem
      if (!file.exists()) {
        URL url = classLoadHelper.getResource(fileName);
        if (url != null) {
          try {
            furl = URLDecoder.decode(url.getPath(), "UTF-8");
          } catch (UnsupportedEncodingException e) {
            furl = url.getPath();
          }
          file = new File(furl);
          try {
            f = url.openStream();
          } catch (IOException ignor) {
            // Swallow the exception
          }
        }
      }
      else {
        try {
          f = new java.io.FileInputStream(file);
        } catch (FileNotFoundException e) {
          // ignore
        }
      }

      if (f == null) {
        fileFound = false;
      }
      else {
        fileFound = true;
      }
    } finally {
      try {
        if (f != null) {
          f.close();
        }
      } catch (IOException ioe) {
        loggger.warn("Error closing jobs file " + fileName, ioe);
      }
    }

    if (!fileFound) {
      if (failOnFileNotFound) {
        throw new SchedulerException("File named '" + fileName + "' does not exist.");
      }
      else {
        loggger.warn("File named '" + fileName + "' does not exist. This is OK if you don't want to use an XML job config file.");
      }
    }
    else {
      processFile(fileName);
    }
  }

  /**
   * Process the xmlfile named <code>fileName</code> with the given system ID.
   *
   * @param fileName meta data file name.
   * @param systemId system ID.
   */
  private void processFile(String fileName) throws ValidationException, ParserConfigurationException, SAXException, IOException, SchedulerException, ClassNotFoundException, ParseException,
  XPathException {

    prepForProcessing();

    loggger.info("Parsing XML file: " + fileName);
    InputSource is = new InputSource(getInputStream(fileName));
    // is.setSystemId(systemId);

    process(is);

    maybeThrowValidationException();
  }

  /*
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Interface. ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */

  private void prepForProcessing() {

    clearValidationExceptions();

    loadedJobs.clear();
    loadedTriggers.clear();
  }

  private void process(InputSource is) throws SAXException, IOException, ParseException, XPathException, ClassNotFoundException {

    // load the document
    Document document = docBuilder.parse(is);

    //
    // Extract Job definitions...
    //

    NodeList jobNodes = (NodeList) xpath.evaluate("/job-scheduling-data/schedule/job", document, XPathConstants.NODESET);

    loggger.debug("Found " + jobNodes.getLength() + " job definitions.");

    for (int i = 0; i < jobNodes.getLength(); i++) {

      Node jobDetailNode = jobNodes.item(i);

      String jobName = getTrimmedToNullString(xpath, "name", jobDetailNode);
      String jobGroup = getTrimmedToNullString(xpath, "group", jobDetailNode);
      String jobDescription = getTrimmedToNullString(xpath, "description", jobDetailNode);
      String jobClassName = getTrimmedToNullString(xpath, "job-class", jobDetailNode);
      Class jobClass = classLoadHelper.loadClass(jobClassName);

      JobDetail jobDetail = newJob(jobClass).withIdentity(jobName, jobGroup).withDescription(jobDescription).build();

      NodeList jobDataEntries = (NodeList) xpath.evaluate("job-data-map/entry", jobDetailNode, XPathConstants.NODESET);

      for (int k = 0; k < jobDataEntries.getLength(); k++) {
        Node entryNode = jobDataEntries.item(k);
        String key = getTrimmedToNullString(xpath, "key", entryNode);
        String value = getTrimmedToNullString(xpath, "value", entryNode);
        jobDetail.getJobDataMap().put(key, value);
      }

      if (loggger.isDebugEnabled()) {
        loggger.debug("Parsed job definition: " + jobDetail);
      }

      addJobToSchedule(jobDetail);
    }

    //
    // Extract Trigger definitions...
    //

    NodeList triggerEntries = (NodeList) xpath.evaluate("/job-scheduling-data/schedule/trigger/*", document, XPathConstants.NODESET);

    loggger.debug("Found " + triggerEntries.getLength() + " trigger definitions.");

    for (int j = 0; j < triggerEntries.getLength(); j++) {

      Node triggerNode = triggerEntries.item(j);
      String triggerName = getTrimmedToNullString(xpath, "name", triggerNode);
      String triggerGroup = getTrimmedToNullString(xpath, "group", triggerNode);
      String triggerDescription = getTrimmedToNullString(xpath, "description", triggerNode);
      String triggerMisfireInstructionConst = getTrimmedToNullString(xpath, "misfire-instruction", triggerNode);
      String triggerPriorityString = getTrimmedToNullString(xpath, "priority", triggerNode);
      String triggerCalendarRef = getTrimmedToNullString(xpath, "calendar-name", triggerNode);
      String triggerJobName = getTrimmedToNullString(xpath, "job-name", triggerNode);
      String triggerJobGroup = getTrimmedToNullString(xpath, "job-group", triggerNode);

      int triggerPriority = Trigger.DEFAULT_PRIORITY;
      if (triggerPriorityString != null) {
        triggerPriority = Integer.valueOf(triggerPriorityString);
      }

      String startTimeString = getTrimmedToNullString(xpath, "start-time", triggerNode);
      String startTimeFutureSecsString = getTrimmedToNullString(xpath, "start-time-seconds-in-future", triggerNode);
      String endTimeString = getTrimmedToNullString(xpath, "end-time", triggerNode);

      Date triggerStartTime = null;
      if (startTimeFutureSecsString != null) {
        triggerStartTime = new Date(System.currentTimeMillis() + (Long.valueOf(startTimeFutureSecsString) * 1000L));
      }
      else {
        triggerStartTime = (startTimeString == null || startTimeString.length() == 0 ? new Date() : dateFormat.parse(startTimeString));
      }
      Date triggerEndTime = endTimeString == null || endTimeString.length() == 0 ? null : dateFormat.parse(endTimeString);

      TriggerKey triggerKey = new TriggerKey(triggerName, triggerGroup);

      ScheduleBuilder sched = null;

      if (triggerNode.getNodeName().equals("simple")) {
        String repeatCountString = getTrimmedToNullString(xpath, "repeat-count", triggerNode);
        String repeatIntervalString = getTrimmedToNullString(xpath, "repeat-interval", triggerNode);

        int repeatCount = repeatCountString == null ? SimpleTrigger.REPEAT_INDEFINITELY : Integer.parseInt(repeatCountString);
        long repeatInterval = repeatIntervalString == null ? 0 : Long.parseLong(repeatIntervalString);

        sched = simpleSchedule().withIntervalInMilliseconds(repeatInterval).withRepeatCount(repeatCount);

        if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length() != 0) {
          if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_FIRE_NOW")) {
            ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionFireNow();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT")) {
            ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNextWithExistingCount();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT")) {
            ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNextWithRemainingCount();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT")) {
            ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNowWithExistingCount();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT")) {
            ((SimpleScheduleBuilder) sched).withMisfireHandlingInstructionNowWithRemainingCount();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
            // do nothing.... (smart policy is default)
          }
          else {
            throw new ParseException("Unexpected/Unhandlable Misfire Instruction encountered '" + triggerMisfireInstructionConst + "', for trigger: " + triggerKey, -1);
          }
        }
      }
      else if (triggerNode.getNodeName().equals("cron")) {
        String cronExpression = getTrimmedToNullString(xpath, "cron-expression", triggerNode);
        String timezoneString = getTrimmedToNullString(xpath, "time-zone", triggerNode);

        TimeZone tz = timezoneString == null ? null : TimeZone.getTimeZone(timezoneString);

        sched = cronSchedule(cronExpression).inTimeZone(tz);

        if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length() != 0) {
          if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_DO_NOTHING")) {
            ((CronScheduleBuilder) sched).withMisfireHandlingInstructionDoNothing();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW")) {
            ((CronScheduleBuilder) sched).withMisfireHandlingInstructionFireAndProceed();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
            // do nothing.... (smart policy is default)
          }
          else {
            throw new ParseException("Unexpected/Unhandlable Misfire Instruction encountered '" + triggerMisfireInstructionConst + "', for trigger: " + triggerKey, -1);
          }
        }
      }
      else if (triggerNode.getNodeName().equals("calendar-interval")) {
        String repeatIntervalString = getTrimmedToNullString(xpath, "repeat-interval", triggerNode);
        String repeatUnitString = getTrimmedToNullString(xpath, "repeat-interval-unit", triggerNode);

        int repeatInterval = Integer.parseInt(repeatIntervalString);

        IntervalUnit repeatUnit = IntervalUnit.valueOf(repeatUnitString);

        sched = calendarIntervalSchedule().withInterval(repeatInterval, repeatUnit);

        if (triggerMisfireInstructionConst != null && triggerMisfireInstructionConst.length() != 0) {
          if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_DO_NOTHING")) {
            ((CalendarIntervalScheduleBuilder) sched).withMisfireHandlingInstructionDoNothing();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_FIRE_ONCE_NOW")) {
            ((CalendarIntervalScheduleBuilder) sched).withMisfireHandlingInstructionFireAndProceed();
          }
          else if (triggerMisfireInstructionConst.equals("MISFIRE_INSTRUCTION_SMART_POLICY")) {
            // do nothing.... (smart policy is default)
          }
          else {
            throw new ParseException("Unexpected/Unhandlable Misfire Instruction encountered '" + triggerMisfireInstructionConst + "', for trigger: " + triggerKey, -1);
          }
        }
      }
      else {
        throw new ParseException("Unknown trigger type: " + triggerNode.getNodeName(), -1);
      }

      Trigger trigger =
          newTrigger().withIdentity(triggerName, triggerGroup).withDescription(triggerDescription).forJob(triggerJobName, triggerJobGroup).startAt(triggerStartTime).endAt(triggerEndTime)
              .withPriority(triggerPriority).modifiedByCalendar(triggerCalendarRef).withSchedule(sched).build();

      NodeList jobDataEntries = (NodeList) xpath.evaluate("job-data-map/entry", triggerNode, XPathConstants.NODESET);

      for (int k = 0; k < jobDataEntries.getLength(); k++) {
        Node entryNode = jobDataEntries.item(k);
        String key = getTrimmedToNullString(xpath, "key", entryNode);
        String value = getTrimmedToNullString(xpath, "value", entryNode);
        trigger.getJobDataMap().put(key, value);
      }

      if (loggger.isDebugEnabled()) {
        loggger.debug("Parsed trigger definition: " + trigger);
      }

      addTriggerToSchedule(trigger);
    }
  }

  private String getTrimmedToNullString(XPath xpath, String elementName, Node parentNode) throws XPathExpressionException {

    String str = (String) xpath.evaluate(elementName, parentNode, XPathConstants.STRING);

    if (str != null) {
      str = str.trim();
    }

    if (str != null && str.length() == 0) {
      str = null;
    }

    return str;
  }

  /**
   * Returns a <code>List</code> of jobs loaded from the xml file.
   * <p/>
   *
   * @return a <code>List</code> of jobs.
   */
  private List<JobDetail> getLoadedJobs() {

    return Collections.unmodifiableList(loadedJobs);
  }

  /**
   * Returns a <code>List</code> of triggers loaded from the xml file.
   * <p/>
   *
   * @return a <code>List</code> of triggers.
   */
  private List<Trigger> getLoadedTriggers() {

    return Collections.unmodifiableList(loadedTriggers);
  }

  /**
   * Returns an <code>InputStream</code> from the fileName as a resource.
   *
   * @param fileName file name.
   * @return an <code>InputStream</code> from the fileName as a resource.
   */
  private InputStream getInputStream(String fileName) {

    return this.classLoadHelper.getResourceAsStream(fileName);
  }

  private void addJobToSchedule(JobDetail job) {

    loadedJobs.add(job);
  }

  private void addTriggerToSchedule(Trigger trigger) {

    loadedTriggers.add(trigger);
  }

  private Map<JobKey, List<MutableTrigger>> buildTriggersByFQJobNameMap(List<MutableTrigger> triggers) {

    Map<JobKey, List<MutableTrigger>> triggersByFQJobName = new HashMap<JobKey, List<MutableTrigger>>();

    for (MutableTrigger trigger : triggers) {
      List<MutableTrigger> triggersOfJob = triggersByFQJobName.get(trigger.getJobKey());
      if (triggersOfJob == null) {
        triggersOfJob = new LinkedList<MutableTrigger>();
        triggersByFQJobName.put(trigger.getJobKey(), triggersOfJob);
      }
      triggersOfJob.add(trigger);
    }

    return triggersByFQJobName;
  }

  /**
   * Schedules the given sets of jobs and triggers.
   *
   * @param sched job scheduler.
   * @exception SchedulerException if the Job or Trigger cannot be added to the Scheduler, or there is an internal Scheduler error.
   */
  public void scheduleJobs(Scheduler sched) throws SchedulerException {

    List<JobDetail> jobs = new LinkedList<JobDetail>(getLoadedJobs());
    List<MutableTrigger> triggers = new LinkedList(getLoadedTriggers());

    loggger.info("Adding " + jobs.size() + " jobs, " + triggers.size() + " triggers.");

    Map<JobKey, List<MutableTrigger>> triggersByFQJobName = buildTriggersByFQJobNameMap(triggers);

    // add each job, and it's associated triggers
    Iterator<JobDetail> itr = jobs.iterator();
    while (itr.hasNext()) {
      JobDetail detail = itr.next();

      itr.remove(); // remove jobs as we handle them...

      JobDetail dupeJ = sched.getJobDetail(detail.getKey());

      if (dupeJ != null) {
        loggger.info("Replacing job: " + detail.getKey());
      }
      else {
        loggger.info("Adding job: " + detail.getKey());
      }

      List<MutableTrigger> triggersOfJob = triggersByFQJobName.get(detail.getKey());

      // log.debug("detail.isDurable()" + detail.isDurable());
      if (!detail.isDurable() && (triggersOfJob == null || triggersOfJob.size() == 0)) {
        if (dupeJ == null) {
          throw new SchedulerException("A new job defined without any triggers must be durable: " + detail.getKey());
        }

        if ((dupeJ.isDurable() && (sched.getTriggersOfJob(detail.getKey()).size() == 0))) {
          throw new SchedulerException("Can't change existing durable job without triggers to non-durable: " + detail.getKey());
        }
      }

      if (dupeJ != null || detail.isDurable()) {
        sched.addJob(detail, true); // add the job if a replacement or durable
      }
      else {
        boolean addJobWithFirstSchedule = true;

        // Add triggers related to the job...
        Iterator<MutableTrigger> titr = triggersOfJob.iterator();
        while (titr.hasNext()) {
          MutableTrigger trigger = titr.next();
          triggers.remove(trigger); // remove triggers as we handle them...

          if (trigger.getStartTime() == null) {
            trigger.setStartTime(new Date());
          }

          boolean addedTrigger = false;
          while (addedTrigger == false) {
            Trigger dupeT = sched.getTrigger(trigger.getKey());
            if (dupeT != null) {

              if (!dupeT.getJobKey().equals(trigger.getJobKey())) {
                loggger.warn("Possibly duplicately named ({}) triggers in jobs xml file! ", trigger.getKey());
              }

              sched.rescheduleJob(trigger.getKey(), trigger);
            }
            else {
              if (loggger.isDebugEnabled()) {
                loggger.debug("Scheduling job: " + trigger.getJobKey() + " with trigger: " + trigger.getKey());
              }

              try {
                if (addJobWithFirstSchedule) {
                  loggger.debug("here1");

                  sched.scheduleJob(detail, trigger); // add the job if it's not in yet...
                  addJobWithFirstSchedule = false;
                }
                else {
                  loggger.debug("here2");

                  sched.scheduleJob(trigger);
                }
              } catch (ObjectAlreadyExistsException e) {
                if (loggger.isDebugEnabled()) {
                  loggger.debug("Adding trigger: " + trigger.getKey() + " for job: " + detail.getKey() + " failed because the trigger already existed.  "
                      + "This is likely due to a race condition between multiple instances " + "in the cluster.  Will try to reschedule instead.");
                }
                continue;
              }
            }
            addedTrigger = true;
          }
        }
      }
    }

    // add triggers that weren't associated with a new job... (those we already handled were removed above)
    for (MutableTrigger trigger : triggers) {

      if (trigger.getStartTime() == null) {
        trigger.setStartTime(new Date());
      }

      boolean addedTrigger = false;
      while (addedTrigger == false) {
        Trigger dupeT = sched.getTrigger(trigger.getKey());
        if (dupeT != null) {

          if (!dupeT.getJobKey().equals(trigger.getJobKey())) {
            loggger.warn("Possibly duplicately named ({}) triggers in jobs xml file! ", trigger.getKey());
          }

          sched.rescheduleJob(trigger.getKey(), trigger);
        }
        else {
          if (loggger.isDebugEnabled()) {
            loggger.debug("Scheduling job: " + trigger.getJobKey() + " with trigger: " + trigger.getKey());
          }

          try {
            sched.scheduleJob(trigger);
          } catch (ObjectAlreadyExistsException e) {
            if (loggger.isDebugEnabled()) {
              loggger.debug("Adding trigger: " + trigger.getKey() + " for job: " + trigger.getJobKey() + " failed because the trigger already existed.  "
                  + "This is likely due to a race condition between multiple instances " + "in the cluster.  Will try to reschedule instead.");
            }
            continue;
          }
        }
        addedTrigger = true;
      }
    }

  }

  /**
   * ErrorHandler interface. Receive notification of a warning.
   *
   * @param e The error information encapsulated in a SAX parse exception.
   * @exception SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void warning(SAXParseException e) throws SAXException {

    addValidationException(e);
  }

  /**
   * ErrorHandler interface. Receive notification of a recoverable error.
   *
   * @param e The error information encapsulated in a SAX parse exception.
   * @exception SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void error(SAXParseException e) throws SAXException {

    addValidationException(e);
  }

  /**
   * ErrorHandler interface. Receive notification of a non-recoverable error.
   *
   * @param e The error information encapsulated in a SAX parse exception.
   * @exception SAXException Any SAX exception, possibly wrapping another exception.
   */
  @Override
  public void fatalError(SAXParseException e) throws SAXException {

    addValidationException(e);
  }

  /**
   * Adds a detected validation exception.
   *
   * @param e SAX exception.
   */
  private void addValidationException(SAXException e) {

    validationExceptions.add(e);
  }

  /**
   * Resets the the number of detected validation exceptions.
   */
  private void clearValidationExceptions() {

    validationExceptions.clear();
  }

  /**
   * Throws a ValidationException if the number of validationExceptions detected is greater than zero.
   *
   * @exception ValidationException DTD validation exception.
   */
  private void maybeThrowValidationException() throws ValidationException {

    if (validationExceptions.size() > 0) {
      throw new ValidationException("Encountered " + validationExceptions.size() + " validation exceptions.", validationExceptions);
    }
  }
}
