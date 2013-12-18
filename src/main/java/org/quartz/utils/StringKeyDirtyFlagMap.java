/* 
 * Copyright 2001-2009 Terracotta, Inc. 
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
 */
package org.quartz.utils;

import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * An implementation of <code>Map</code> that wraps another <code>Map</code> and flags itself 'dirty' when it is modified, enforces that all keys are Strings.
 * </p>
 * <p>
 * All allowsTransientData flag related methods are deprecated as of version 1.6.
 * </p>
 */
public class StringKeyDirtyFlagMap extends DirtyFlagMap {

  static final long serialVersionUID = -9076749120524952280L;

  public StringKeyDirtyFlagMap() {

    super();
  }

  public StringKeyDirtyFlagMap(int initialCapacity) {

    super(initialCapacity);
  }

  @Override
  public boolean equals(Object obj) {

    return super.equals(obj);
  }

  @Override
  public int hashCode() {

    return getWrappedMap().hashCode();
  }

  /**
   * Get a copy of the Map's String keys in an array of Strings.
   */
  public String[] getKeys() {

    return (String[]) keySet().toArray(new String[size()]);
  }

  /**
   * <p>
   * Adds the name-value pairs in the given <code>Map</code> to the <code>StringKeyDirtyFlagMap</code>.
   * </p>
   * <p>
   * All keys must be <code>String</code>s.
   * </p>
   */
  @Override
  public void putAll(Map map) {

    for (Iterator entryIter = map.entrySet().iterator(); entryIter.hasNext();) {
      Map.Entry entry = (Map.Entry) entryIter.next();

      // will throw IllegalArgumentException if key is not a String
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * <p>
   * Adds the given <code>String</code> value to the <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  public void put(String key, String value) {

    super.put(key, value);
  }

  /**
   * <p>
   * Adds the given <code>Object</code> value to the <code>StringKeyDirtyFlagMap</code>.
   * </p>
   */
  @Override
  public Object put(Object key, Object value) {

    if (!(key instanceof String)) {
      throw new IllegalArgumentException("Keys in map must be Strings.");
    }

    return super.put(key, value);
  }

  /**
   * <p>
   * Retrieve the identified <code>String</code> value from the <code>StringKeyDirtyFlagMap</code>.
   * </p>
   * 
   * @throws ClassCastException if the identified object is not a String.
   */
  public String getString(String key) {

    Object obj = get(key);

    try {
      return (String) obj;
    } catch (Exception e) {
      throw new ClassCastException("Identified object is not a String.");
    }
  }
}
