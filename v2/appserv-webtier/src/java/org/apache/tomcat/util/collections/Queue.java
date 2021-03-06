

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * Portions Copyright Apache Software Foundation.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.apache.tomcat.util.collections;

import java.util.Vector;

/**
 * A simple FIFO queue class which causes the calling thread to wait
 * if the queue is empty and notifies threads that are waiting when it
 * is not empty.
 *
 * @author Anil V (akv@eng.sun.com)
 */
public class Queue {
    private Vector vector = new Vector();
    private boolean stopWaiting=false;
    private boolean waiting=false;
    
    /** 
     * Put the object into the queue.
     * 
     * @param	object		the object to be appended to the
     * 				queue. 
     */
    public synchronized void put(Object object) {
	vector.addElement(object);
	notify();
    }

    /** Break the pull(), allowing the calling thread to exit
     */
    public synchronized void stop() {
	stopWaiting=true;
	// just a hack to stop waiting 
	if( waiting ) notify();
    }
    
    /**
     * Pull the first object out of the queue. Wait if the queue is
     * empty.
     */
    public synchronized Object pull() {
	while (isEmpty()) {
	    try {
		waiting=true;
		wait();
	    } catch (InterruptedException ex) {
	    }
	    waiting=false;
	    if( stopWaiting ) return null;
	}
	return get();
    }

    /**
     * Get the first object out of the queue. Return null if the queue
     * is empty. 
     */
    public synchronized Object get() {
	Object object = peek();
	if (object != null)
	    vector.removeElementAt(0);
	return object;
    }

    /**
     * Peek to see if something is available.
     */
    public Object peek() {
	if (isEmpty())
	    return null;
	return vector.elementAt(0);
    }
    
    /**
     * Is the queue empty?
     */
    public boolean isEmpty() {
	return vector.isEmpty();
    }

    /**
     * How many elements are there in this queue?
     */
    public int size() {
	return vector.size();
    }
}
