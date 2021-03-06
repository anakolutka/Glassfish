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
package cal;

import java.beans.*;
import javax.servlet.http.*;
import javax.servlet.*;
import java.util.Hashtable;

public class TableBean {

  Hashtable table;
  JspCalendar JspCal;
  Entries entries;
  String date;
  String name = null;
  String email = null;
  boolean processError = false;

  public TableBean () {
    this.table = new Hashtable (10);
    this.JspCal = new JspCalendar ();
    this.date = JspCal.getCurrentDate ();
  }

  public void setName (String nm) {
    this.name = nm;
  }

  public String getName () {
    return this.name;
  }
  
  public void setEmail (String mail) {
    this.email = mail;
  }

  public String getEmail () {
    return this.email;
  }

  public String getDate () {
    return this.date;
  }

  public Entries getEntries () {
    return this.entries;
  }

  public void processRequest (HttpServletRequest request) {

    // Get the name and e-mail.
    this.processError = false;
    if (name == null || name.equals("")) setName(request.getParameter ("name"));  
    if (email == null || email.equals("")) setEmail(request.getParameter ("email"));
    if (name == null || email == null ||
		name.equals("") || email.equals("")) {
      this.processError = true;
      return;
    }

    // Get the date.
    String dateR = request.getParameter ("date");
    if (dateR == null) date = JspCal.getCurrentDate ();
    else if (dateR.equalsIgnoreCase("next")) date = JspCal.getNextDate ();
    else if (dateR.equalsIgnoreCase("prev")) date = JspCal.getPrevDate ();

    entries = (Entries) table.get (date);
    if (entries == null) {
      entries = new Entries ();
      table.put (date, entries);
    }

    // If time is provided add the event.
	String time = request.getParameter("time");
    if (time != null) entries.processRequest (request, time);
  }

  public boolean getProcessError () {
    return this.processError;
  }
}






