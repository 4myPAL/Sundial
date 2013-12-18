/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

package org.quartz;

import java.util.List;

import org.quartz.exceptions.SchedulerException;
import org.quartz.impl.matchers.EverythingMatcher;

/**
 * Client programs may be interested in the 'listener' interfaces that are available from Quartz. The <code>{@link JobListener}</code> interface provides notifications of <code>Job</code> executions.
 * The <code>{@link TriggerListener}</code> interface
 * provides notifications of <code>Trigger</code> firings. The <code>{@link SchedulerListener}</code> interface provides notifications of <code>Scheduler</code> events and errors. Listeners can be
 * associated with local schedulers through the {@link ListenerManager} interface.
 * 
 * @author jhouse
 * @since 2.0 - previously listeners were managed directly on the Scheduler interface.
 */
public interface ListenerManager {

  /**
   * Get the set of Matchers for which the listener will receive events if ANY of the matchers match.
   * 
   * @param listenerName the name of the listener to add the matcher to
   * @return the matchers registered for selecting events for the identified listener
   * @throws SchedulerException
   */
  public List<Matcher<JobKey>> getJobListenerMatchers(String listenerName);

  /**
   * Get a List containing all of the <code>{@link JobListener}</code>s in the <code>Scheduler</code>.
   */
  public List<JobListener> getJobListeners();

  /**
   * Add the given <code>{@link TriggerListener}</code> to the <code>Scheduler</code>, and register it to receive events for Triggers that are matched by ANY of the given Matchers. If no matcher is
   * provided, the <code>EverythingMatcher</code> will be
   * used.
   * 
   * @see Matcher
   * @see EverythingMatcher
   */
  public void addTriggerListener(TriggerListener triggerListener, Matcher<TriggerKey>... matchers);

  /**
   * Add the given <code>{@link TriggerListener}</code> to the <code>Scheduler</code>, and register it to receive events for Triggers that are matched by ANY of the given Matchers. If no matcher is
   * provided, the <code>EverythingMatcher</code> will be
   * used.
   * 
   * @see Matcher
   * @see EverythingMatcher
   */
  public void addTriggerListener(TriggerListener triggerListener, List<Matcher<TriggerKey>> matchers);

  /**
   * Get the set of Matchers for which the listener will receive events if ANY of the matchers match.
   * 
   * @param listenerName the name of the listener to add the matcher to
   * @return the matchers registered for selecting events for the identified listener
   * @throws SchedulerException
   */
  public List<Matcher<TriggerKey>> getTriggerListenerMatchers(String listenerName);

  /**
   * Get a List containing all of the <code>{@link TriggerListener}</code>s in the <code>Scheduler</code>.
   */
  public List<TriggerListener> getTriggerListeners();

  /**
   * Get a List containing all of the <code>{@link SchedulerListener}</code>s registered with the <code>Scheduler</code>.
   */
  public List<SchedulerListener> getSchedulerListeners();

}