/**
 * Copyright 2013 Xeiam LLC.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.quartz.classloading.CascadingClassLoadHelper;

/**
 * @author timmolter
 */
public class AnnotatedClassFinderTest {

  @Test
  public void test0() {

    CascadingClassLoadHelper classLoadHelper = new CascadingClassLoadHelper();
    classLoadHelper.initialize();

    Set<Class<? extends Job>> jobClasses = classLoadHelper.getJobClasses("com.xeiam.sundial.jobs");
    Set<String> jobClassNames = new HashSet(jobClasses.size());
    for (Class<? extends Job> jobClass : jobClasses) {
      Assert.assertEquals(jobClass.getPackage().getName(), "com.xeiam.sundial.jobs");
      jobClassNames.add(jobClass.getSimpleName());
    }
    Set<String> expected = new HashSet(Arrays.asList(new String[]{
      "SampleJob1", "SampleJob2", "SampleJob3", "SampleJob4",
      "SampleJob5", "SampleJob6", "SampleJob7", "Concrete"
    }));
    Assert.assertEquals(jobClassNames, expected);
  }
}
