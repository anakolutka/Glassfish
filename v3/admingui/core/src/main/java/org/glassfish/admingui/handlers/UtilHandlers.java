/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

/*
 * UtilHandlers.java
 *
 * Created on August 31, 2006, 2:36 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.glassfish.admingui.handlers;

import org.glassfish.admingui.util.GuiUtil;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

import com.sun.webui.jsf.component.Hyperlink;


import java.io.File;
import java.util.GregorianCalendar;
import java.util.Map;
import java.net.URLDecoder;
import java.text.DecimalFormat;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.io.UnsupportedEncodingException;


/**
 *
 * @author Jennifer Chou
 */
public class UtilHandlers {
    
    /** Creates a new instance of UtilHandlers */
    public UtilHandlers() {
    }
    

    
    
    /**
     *	<p> Adds the specified (signed) amount of time to the given calendar 
     *      field, based on the calendar's rules and returns the resulting Date.
     *      See <code>java.util.GregorianCalendar</code> add(int field, int amount). </p>
     *
     *  <p> Input value: "Field" -- Type: <code>Integer</code> 
     *          - <code>java.util.Calendar</code> field</p>
     *  <p> Input value: "Amount" -- Type: <code>Integer</code>
     *          - the amount of date or time to be added to the field.</p>
     *  <p> Output value: "Date" -- Type: <code>java.util.Date</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="calendarAdd",
    	input={
	    @HandlerInput(name="Field", type=Integer.class, required=true),
            @HandlerInput(name="Amount", type=Integer.class, required=true)},
        output={
            @HandlerOutput(name="Date", type=java.util.Date.class)})
    public static void calendarAdd(HandlerContext handlerCtx) {
        int field = ((Integer) handlerCtx.getInputValue("Field")).intValue();
        int amount = ((Integer) handlerCtx.getInputValue("Amount")).intValue();
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(field, amount);
        handlerCtx.setOutputValue("Date", cal.getTime());        
    }
    
    /**
     *	<p> Creates a new File instance by converting the given pathname string 
     *      into an abstract pathname. If the given string is the empty string, 
     *      then the result is the empty abstract pathname. </p>
     *
     *  <p> Input value: "Pathname" -- Type: <code>String</code> 
     *  <p> Output value: "File" -- Type: <code>java.io.File</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="getFile",
    	input={
	    @HandlerInput(name="Pathname", type=String.class, required=true)},
        output={
            @HandlerOutput(name="File", type=File.class)})
    public static void getFile(HandlerContext handlerCtx) {
        String pathname = (String) handlerCtx.getInputValue("Pathname");
        handlerCtx.setOutputValue("File", pathname != null ? new File(pathname) : null);        
    }
    
    /**
     *	<p> Returns the name of the file or directory denoted by this abstract 
     *      pathname. This is just the last name in the pathname's name sequence. 
     *      If the pathname's name sequence is empty, then the empty string is returned. </p>
     *
     *  <p> Input value: "File" -- Type: <code>java.io.File</code> 
     *  <p> Output value: "Name" -- Type: <code>String</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="fileGetName",
    	input={
	    @HandlerInput(name="File", type=File.class, required=true)},
        output={
            @HandlerOutput(name="Name", type=String.class)})
    public static void fileGetName(HandlerContext handlerCtx) {
        File file = (File) handlerCtx.getInputValue("File");
        String name = file != null ? file.getName() : "" ;
        handlerCtx.setOutputValue("Name", name != null ? name : "");        
    }
    
    /**
     *	<p> Returns the value to which the input map maps the input key. </p>
     *
     *  <p> Input value: "Map" -- Type: <code>java.util.Map</code> 
     *  <p> Input value: "Key" -- Type: <code>Object</code>
     *  <p> Output value: "Value" -- Type: <code>Object</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="mapGet",
    	input={
	    @HandlerInput(name="Map", type=Map.class, required=true),
            @HandlerInput(name="Key", type=Object.class, required=true)},
        output={
            @HandlerOutput(name="Value", type=Object.class)})
    public static void mapGet(HandlerContext handlerCtx) {
        Map map = (Map) handlerCtx.getInputValue("Map");
        Object key = (Object) handlerCtx.getInputValue("Key");
        handlerCtx.setOutputValue("Value", (Object) map.get(key));        
    }
    
    
    /**
     *	<p> Compare if 2 objects is equal </p>
     *
     *  <p> Input value: "obj1" -- Type: <code>Object</code> 
     *  <p> Input value: "obj2" -- Type: <code>Object</code>
     *  <p> Output value: "equal" -- Type: <code>Object</code></p>
     *	@param	context	The HandlerContext.
     */
    @Handler(id="compare",
    	input={
	    @HandlerInput(name="obj1", type=Object.class, required=true),
            @HandlerInput(name="obj2", type=Object.class, required=true)},
        output={
            @HandlerOutput(name="objEqual", type=Boolean.class)})
    public static void compare(HandlerContext handlerCtx) {
        boolean ret = false;
        Object obj1 = (Object) handlerCtx.getInputValue("obj1");
        Object obj2 = (Object) handlerCtx.getInputValue("obj2");
        if (obj1 != null){
            ret = obj1.equals(obj2);
        }else{
            if (obj2 == null)
                ret = true;
        }
        handlerCtx.setOutputValue("objEqual", ret);        
    }
    
    /**
     * <p> This method displays the save successful message when the page refresh.
     * @param context The HandlerContext.
     */
   @Handler(id="prepareSuccessfulMsg")
    public static void prepareSuccessful(HandlerContext handlerCtx){
        GuiUtil.prepareSuccessful(handlerCtx);
    }

    /** 
     * <p> This method sets the attributes that will be used by the alert component
     *     display the message to user.
     *     If type is not specifed, it will be 'info' by default.
     * <p> Input value: "summary" -- Type: <code>java.lang.String</code></p>
     * <p> Input value: "type" -- Type: <code>java.lang.String</code></p>
     * <p> Input value: "detail" -- Type: <code>java.lang.String</code></p>
     * @param context The HandlerContext.
     */
     @Handler(id="prepareAlertMsg",
     input={
        @HandlerInput(name="summary", type=String.class, required=true),
        @HandlerInput(name="type",  type=String.class),
        @HandlerInput(name="detail",  type=String.class)
      })
    public static void prepareAlertMsg(HandlerContext handlerCtx){
        String summary = (String) handlerCtx.getInputValue("summary");
        String type = (String) handlerCtx.getInputValue("type");
        String detail = (String) handlerCtx.getInputValue("detail");
        GuiUtil.prepareAlert(handlerCtx, type, summary, detail);
    }
     
    /**
     * <p> This method decodes a String using "UTF-8" as default
     * if scheme is not specified.
     */
     @Handler(id="decodeString",
     input={
        @HandlerInput(name="str", type=String.class, required=true),
        @HandlerInput(name="scheme", type=String.class)},
     output={
        @HandlerOutput(name="output", type=String.class)
	    })
    public static void decodeString(HandlerContext handlerCtx) {
        String str = (String) handlerCtx.getInputValue("str");
        String scheme = (String) handlerCtx.getInputValue("scheme");
        if (GuiUtil.isEmpty(str)){
            handlerCtx.setOutputValue("output", "");
            return;
        }
        
        if (GuiUtil.isEmpty(scheme))
            scheme = "UTF-8";
        try{
            String output=URLDecoder.decode(str, scheme);
            handlerCtx.setOutputValue("output", output);
        }catch(UnsupportedEncodingException ex) {
            ex.printStackTrace();
            handlerCtx.setOutputValue("output", str);
        }
     }

    @Handler(id="createHyperlinkArray",
	    input={},
	    output={
		@HandlerOutput(name="links", type=Hyperlink[].class)
	    })
    public static void createHyperlinkArray(HandlerContext handlerCtx) {
	FacesContext ctx = handlerCtx.getFacesContext();
	ExternalContext extCtx = ctx.getExternalContext();
	Map<String, String[]> reqParams = extCtx.getRequestParameterValuesMap();
	String linkText[] = reqParams.get("text");
	String linkUrl[] = reqParams.get("urls");
	if (linkText == null) {
	    // No data!  Should we add something here anyway?
	    return;
	}

	int len = linkText.length;
	Hyperlink arr[] = new Hyperlink[len];
	String url = null;
	String ctxPath = extCtx.getRequestContextPath();
	int ctxPathSize = ctxPath.length();
	for (int idx=0; idx < len; idx++) {
	    // FIXME: Set parent
	    arr[idx] = new Hyperlink();
	    arr[idx].setId("bcLnk" + idx);
	    arr[idx].setText(linkText[idx]);
	    url = linkUrl[idx];
	    if (url.startsWith(ctxPath)) {
		url = url.substring(ctxPathSize);
	    }
	    arr[idx].setUrl(url);
	}
	handlerCtx.setOutputValue("links", arr);
    }
    
    @Handler(id="dummyHyperlinkArray",
	    input={},
	    output={
		@HandlerOutput(name="links", type=Hyperlink[].class)
	    })
    public static void dummyHyperlinkArray(HandlerContext handlerCtx) {
            Hyperlink arr[] = new Hyperlink[1];
            arr[0]=new Hyperlink();
            arr[0].setText(">");
        handlerCtx.setOutputValue("links", arr);
    }
    
    @Handler(id="roundTo2DecimalPoint",
    input={
        @HandlerInput(name="input", type=Double.class)},
    output={
        @HandlerOutput(name="output", type=String.class)
    })
    public static void roundTo2DecimalPoint(HandlerContext handlerCtx) {
        DecimalFormat df= new DecimalFormat();
        df.setMaximumFractionDigits(2);
        try{
            Double input = (Double) handlerCtx.getInputValue("input");
            String output = (input==null)? "": df.format(input);
            handlerCtx.setOutputValue("output", output);
        }catch (Exception ex){
            ex.printStackTrace();
            handlerCtx.setOutputValue("output", "");
        }
    }
            
            
            
}
