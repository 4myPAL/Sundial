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

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import com.xeiam.sundial.exceptions.JobInterruptException;
import com.xeiam.sundial.exceptions.RequiredParameterException;

/**
 * @author timmolter
 * @version $Revision: $ $Date: $ $Author: $
 */
public abstract class Job extends JobContainer implements InterruptableJob {

    /**
     * Required no-arg constructor
     */
    public Job() {
    }

    @Override
    public final void execute(JobExecutionContext pJobExecutionContext) throws JobExecutionException {

        // check for global lock
        if (DefaultJobScheduler.getGlobalLock()) {
            logInfo("Global Lock in place! Job aborted.");
            return;
        }

        try {

            initContextContainer(pJobExecutionContext);

            doRun();

        } catch (RequiredParameterException e) {
        } catch (JobInterruptException e) {
        } catch (Exception e) {
            logError("Error executing Job! Job aborted!!!", e);
        } finally {
            cleanup();
            destroyContext(); // remove the JobContext from the ThreadLocal
        }

    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {

        setTerminate();
        logInfo("Interrupt called!");

    }

    /**
     * Override and place any code in here that should be called no matter what after the Job runs or throws an exception.
     */
    public void cleanup() {

    }

    public abstract void doRun() throws JobInterruptException;

}
