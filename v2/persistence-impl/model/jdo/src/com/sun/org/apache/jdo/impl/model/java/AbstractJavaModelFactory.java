/*
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the "License").  You may not use this file except 
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt or 
 * https://glassfish.dev.java.net/public/CDDLv1.0.html. 
 * See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL 
 * HEADER in each file and include the License file at 
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable, 
 * add the following below this CDDL HEADER, with the 
 * fields enclosed by brackets "[]" replaced with your 
 * own identifying information: Portions Copyright [yyyy] 
 * [name of copyright owner]
 */

/*
 * Copyright 2005 The Apache Software Foundation.
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

package com.sun.org.apache.jdo.impl.model.java;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import com.sun.org.apache.jdo.model.ModelException;
import com.sun.org.apache.jdo.model.ModelFatalException;
import com.sun.org.apache.jdo.model.java.JavaModel;
import com.sun.org.apache.jdo.model.java.JavaModelFactory;
import com.sun.org.apache.jdo.model.java.JavaType;
import com.sun.org.apache.jdo.util.I18NHelper;

/**
 * Abstract super class for JavaModelFactory implementations. It provides a
 * JavaModel cache and implements the JavaModel lookup method 
 * {@link #getJavaModel(Object key)}.
 * <p>
 * A non-abstract subclass must implement method 
 * {@link #createJavaModel(Object key)}. The lookup method calls this
 * method if it cannot find a JavaModel instance in the cache. The method
 * should also check whether the specified key is of an appropriate type
 * for the JavaModelFactory implementation. A subclass should check whether
 * it can implement method {@link #getJavaType(Object typeDesc)}. The
 * implementation in AbstractJavaModelFactory always throws a
 * ModelFatalException.
 * 
 * @author Michael Bouschen
 * @since 1.0.1
 * @version 2.0
 */
abstract public class AbstractJavaModelFactory
    implements JavaModelFactory
{    
    /**
     * Map of JavaModel instances, key is implementation specific.
     * @see #getJavaModel(Object key)
     */
    private Map modelCache = new HashMap();
    
    /** I18N support */
    private static I18NHelper msg = 
        I18NHelper.getInstance(AbstractJavaModelFactory.class);

    /**
     * Creates a new empty JavaModel instance. A factory implementation may
     * use the specified key when caching the new JavaModel instance. 
     * <p>
     * Each JavaModelFactory imposes its own restrictions for the keys to
     * cache JavaModel instances. Some implementation will allow only keys
     * of a certain type. Some implementations will prohibit
     * <code>null</code> keys. Attempting to use an ineligible key will
     * result in a {@link com.sun.org.apache.jdo.model.ModelException}. This means
     * the specified key is of an inappropriate type for this
     * JavaModelFactory or if the key is <code>null</code> and this 
     * JavaModelFactory does not support <code>null</code> keys.
     * @param key the key that may be used to cache the returned JavaModel instance.
     * @return a new JavaModel instance.
     * @exception ModelException if impossible; the key is of an
     * inappropriate type or the key is <code>null</code> and this
     * JavaModelFactory does not support <code>null</code> keys.
     */
    abstract public JavaModel createJavaModel(Object key)
        throws ModelException;

    /**
     * Returns the JavaModel instance for the specified key.
     * <p>
     * The method throws a {@link com.sun.org.apache.jdo.model.ModelFatalException},
     * if the specified key is of an inappropriate type for this
     * JavaModelFactory or if the key is <code>null</code> and this
     * JavaModelFactory does not support <code>null</code> keys.
     * @param key the key used to cache the returned JavaModel instance.
     * @return a JavaModel instance for the specified key.
     * @exception ModelFatalException the key is of an inappropriate type
     * or the key is <code>null</code> and this JavaModelFactory does not
     * support <code>null</code> keys.
     */
    public JavaModel getJavaModel(Object key)
    {
        synchronized (modelCache) {
            JavaModel javaModel = (JavaModel)modelCache.get(key);
            if (javaModel == null) {
                // create new model and store it using the specified key
                try {
                    javaModel = createJavaModel(key);
                    modelCache.put(key, javaModel);
                }
                catch (ModelException ex) {
                    throw new ModelFatalException(
                        msg.msg("EXC_CannotCreateJavaModel"), ex); //NOI18N
                }
            } 
            return javaModel;
         }
    }

    /**
     * Removes the specified javaModel from the JavaModel cache. Note, if
     * there are multiple entries in the cache with the specified javaModel
     * as value, then all of them get removed. The method does not have an
     * effect, if this factory does not have the specified javaModel.
     * @param javaModel the JavaModel to be removed.
     * @since 2.0
     */
    public void removeJavaModel(JavaModel javaModel) {
        if (javaModel == null) {
            // nothing to be removed => return
            return;
        }
        
        synchronized (modelCache) {
            for (Iterator i = modelCache.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                Object value = entry.getValue();
                if ((javaModel == value) || javaModel.equals(value)) {
                    // found javaModel => remove the entry
                    i.remove();
                }
            }
        }
    }

    /**
     * Removes the JavaModel for the specified key from the JavaModel
     * cache. The method does not have an effect, if this factory does not 
     * have a JavaModel for the the specified key.
     * @param key the key used to find the JavaModel instance to be removed.
     * @since 2.0
     */
    public void removeJavaModel(Object key) {
        synchronized (modelCache) {
            modelCache.remove(key);
        } 
    }

    /**
     * Returns a JavaType instance for the specified type description
     * (optional operation). This method is a convenience method and a
     * short cut for <code>getJavaModel(key).getJavaType(typeName)</code>. 
     * If the factory supports this method, it needs to be able to get the
     * key for the JavaModel lookup and the type name for the JavaType
     * lookup from the specified typeDesc. An example for such a type
     * description is the java.lang.Class instance in the runtime
     * environment. 
     * <p>
     * The method throws a {@link com.sun.org.apache.jdo.model.ModelFatalException}, 
     * if this factory does not support this short cut or if it does not
     * support the specified type description.
     * <p>
     * This implementation always throws a ModelFatalException.
     * @param typeDesc the type description.
     * @return a JavaType instance for the specified type.
     * @exception ModelFatalException this factory does not support this
     * short cut or does not support the specified type description.
     */
    public JavaType getJavaType(Object typeDesc)
    {
        throw new ModelFatalException(msg.msg(
            "EXC_MethodNotSupported", this.getClass().getName(), //NOI18N
            "getJavaType")); //NOI18N
    }
    
}

