/**
 * Copyright 2011 Xeiam LLC.
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

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author timmolter
 */
public class Sample {

    /**
     * Make sure jobs.xml, log4j.xml and quartz.properties are on the classpath!
     * 
     * @param args
     */
    public static void main(String[] args) {

        System.out.println("Starting scheduler...");
        try {

            System.out.println("Getting scheduler.");
            Scheduler scheduler = new StdSchedulerFactory().getScheduler(5);
            System.out.println("Starting scheduler.");
            scheduler.start();
            System.out.println("Scheduler started.");

        } catch (SchedulerException exc) {
            System.out.println("Failed to initialize scheduler/jobs! " + exc.getMessage());
        }

    }

}
