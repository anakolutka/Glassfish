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
package org.glassfish.admingui.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.layout.LayoutDefinitionManager;
import com.sun.jsftemplating.layout.LayoutViewHandler;
import com.sun.jsftemplating.layout.descriptors.LayoutDefinition;

import org.glassfish.admingui.plugin.ConsolePluginService;
import org.glassfish.admingui.plugin.IntegrationPoint;
import org.glassfish.admingui.plugin.IntegrationPointComparator;

import org.jvnet.hk2.component.Habitat;

import java.net.URL;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Properties;


import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import com.sun.webui.theme.ThemeContext;


import org.glassfish.admingui.common.plugin.ConsoleClassLoader;

import org.glassfish.admingui.theme.AdminguiThemeContext;

/**
 *  <p>	This class will provide JSFTemplating <code>Handler</code>s that
 *	provide access to {@link IntegrationPoint}s and possibily other
 *	information / services needed to provide plugin functionality 
 *	i.e. getting resources, etc.).</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public class PluginHandlers {

    /**
     *	<p> Constructor.</p>
     */
    protected PluginHandlers() {
    }

    /**
     *	<p> Find and return the <code>ConsolePluginService</code>.  This method
     *	    uses the HK2 <code>Habitat</code> to locate the
     *	    <code>ConsolePluginService</code>.</p>
     *
     *	@param	ctx The <code>FacesContext</code>.
     *
     *	@returns The <code>ConsolePluginService</code>.
     */
    private static ConsolePluginService getPluginService(FacesContext ctx) {
	// We need to get the ServletContext to find the Habitat
	ServletContext servletCtx = (ServletContext)
	    (ctx.getExternalContext()).getContext();

	// Get the Habitat from the ServletContext
	Habitat habitat = (Habitat) servletCtx.getAttribute(
	    org.glassfish.admingui.common.plugin.ConsoleClassLoader.HABITAT_ATTRIBUTE);

//	System.out.println("Habitat:" + habitat);

	return habitat.getByType(ConsolePluginService.class);
    }


    /**
     *	<p> This handler provides access to {@link IntegrationPoint}s for the requested key.</p>
     *
     *	@param	handlerCtx	The <code>HandlerContext</code>.
     */
    @Handler(id="getIntegrationPoints",
    	input={
            @HandlerInput(name="type", type=String.class, required=true)},
        output={
            @HandlerOutput(name="points", type=List.class)})
    public static void getIntegrationPoints(HandlerContext handlerCtx) {
	String type = (String) handlerCtx.getInputValue("type");
	List<IntegrationPoint> value =
	    getIntegrationPoints(handlerCtx.getFacesContext(), type);
	handlerCtx.setOutputValue("points", value);
    }

    /**
     *
     */
    public static List<IntegrationPoint> getIntegrationPoints(FacesContext context, String type) {
	return getPluginService(context).getIntegrationPoints(type);
    }

    /**
     *	<p> This handler adds {@link IntegrationPoint}s of a given type to a
     *	    <code>UIComponent</code> tree.  It looks for
     *	    {@link IntegrationPoint}s using the given <code>type</code>.  It
     *	    then sorts the results (if any) by <code>parentId</code>, and then
     *	    by priority.  It next interates over each one looking for a
     *	    <code>UIComponent</code> with an <code>id</code> which matches the
     *	    its own <code>parentId</code> value.  It then uses the content of
     *	    the {@link IntegrationPoint} to attempt to include the .jsf page
     *	    it refers to under the identified parent component.</p>
     */
    @Handler(id="includeIntegrations",
    	input={
            @HandlerInput(name="type", type=String.class, required=true),
	    @HandlerInput(name="root", type=UIComponent.class, required=false)})
    public static void includeIntegrations(HandlerContext handlerCtx) {
	// Get the input
	String type = (String) handlerCtx.getInputValue("type");
	UIComponent root = (UIComponent) handlerCtx.getInputValue("root");

	// Get the IntegrationPoints
	FacesContext ctx = handlerCtx.getFacesContext();
	List<IntegrationPoint> points = getIntegrationPoints(ctx, type);

	// Include them
	includeIntegrationPoints(ctx, root, getSortedIntegrationPoints(points));
    }

    @Handler(id="includeFirstIntegrationPoint",
    	input={
            @HandlerInput(name="type", type=String.class, required=true),
	    @HandlerInput(name="root", type=UIComponent.class, required=false)})
    public static void includeFirstIP(HandlerContext handlerCtx) {
	// Get the input
	String type = (String) handlerCtx.getInputValue("type");
	UIComponent root = (UIComponent) handlerCtx.getInputValue("root");

	// Get the IntegrationPoints
	FacesContext ctx = handlerCtx.getFacesContext();
	Set<IntegrationPoint> points =
	    getSortedIntegrationPoints(getIntegrationPoints(ctx, type));
	if (points != null) {
	    Iterator<IntegrationPoint> it = points.iterator();
	    if (it.hasNext()) {
		// Get the first one...
		IntegrationPoint point = it.next();

		// Include the first one...
		includeIntegrationPoint(
		    ctx, getIntegrationPointParent(root, point), point);
	    }
	}
    }


    /**
     *	<p> This method sorts the given {@link IntegrationPoint}'s by parentId
     *	    and then by priority.  It returns a <code>SortedSet</code> of the
     *	    results with the ABC order parentId.  When parentId's match, the
     *	    highest piority will appear first.</p>
     */
    public static Set<IntegrationPoint> getSortedIntegrationPoints(List<IntegrationPoint> points) {
	// Make sure we have something...
	if (points == null) {
	    return null;
	}

	// Use a TreeSet to sort automatically
	Set<IntegrationPoint> sortedSet =
	    new TreeSet<IntegrationPoint>(
		IntegrationPointComparator.getInstance());
// FIXME: Check for duplicates! Modify "id" if there is a duplicate?
	sortedSet.addAll(points);
	return sortedSet;
    }


    /**
     *
     *	@param	points	This parameter should be the {@link IntegrationPoint}s
     *			to include in the order in which you want to include
     *			them if that matters (i.e. use <code>SortedSet</code>).
     */
    public static void includeIntegrationPoints(FacesContext ctx, UIComponent root, Set<IntegrationPoint> points) {
	if (points == null) {
	    // Do nothing...
	    return;
	}
	if (root == null) {
	    // No root is specified, search whole page
	    root = ctx.getViewRoot();
	}

	// Iterate
	IntegrationPoint point;
	Iterator<IntegrationPoint> it = null;
	int lastSize = 0;
	int currSize = points.size();
	String parentId = null;
	String lastParentId = null;
	while (currSize != lastSize) {
	    // Stop loop by comparing previous size
	    lastSize = currSize;
	    it = points.iterator();
	    lastParentId = "";
	    UIComponent parent = root;

	    // Iterate through the IntegrationPoints
	    while (it.hasNext()) {
		point = it.next();

		// Optimize for multiple plugins for the same parent
		parentId = point.getParentId();
		if ((parentId == null) || !parentId.equals(lastParentId)) {
		    // New parent (or root -- null)
		    parent = getIntegrationPointParent(root, point);
		}
		if (parent == null) {
		    // Didn't find the one specified!
// FIXME: log FINE!  Note this may not be a problem, keep iterating to see if we find it later.
//System.out.println("The specified parentId (" + parentId + ") was not found!"); 
		    lastParentId = null;
		    continue;
		}
		lastParentId = parent.getId();

		// Add the content
		includeIntegrationPoint(ctx, parent, point);

		// We found the parent, remove from our list of IPs to add
		it.remove();

	    }

	    // Get the set size to see if we have any left to process
	    currSize = points.size();
	}
    }

    /**
     *	<p> This method returns the parent for the content of the given
     *	    {@link IntegrationPoint}.</p>
     *
     *	@param	root	The <code>UIComponent</code> in which to search for
     *			the parent.
     *	@param	point	The {@link IntegrationPoint} which is looking for its
     *			parent <code>UIComponent</code>.
     */
    public static UIComponent getIntegrationPointParent(UIComponent root, IntegrationPoint point) {
	UIComponent parent = null;
	String parentId = point.getParentId();
	if (parentId == null) {
	    // If not specified, just stick it @ the root
	    parentId = root.getId();
	    parent = root;
	} else {
	    parent = findComponentById(root, parentId);
	}

	// Return the IntegrationPoint parent
	return parent;
    }

    /**
     *	<p> This method includes a single {@link IntegrationPoint} under the
     *	    given parent <code>UIComponent</code>.</p>
     *
     *	@param	ctx	The <code>FacesContext</code>.
     *	@param	parent	The parent for the {@link IntegrationPoint}.
     *	@param	point	The {@link IntegrationPoint}.
     */
    public static void includeIntegrationPoint(FacesContext ctx, UIComponent parent, IntegrationPoint point) {
	// Add the content
	String content = point.getContent();
	while (content.startsWith("/")) {
	    content = content.substring(1);
	}
        String key = content;
        if (!key.contains("://")) {
            key = "/" + point.getConsoleConfigId() + "/" + content;
        }
	LayoutDefinition def =
	    LayoutDefinitionManager.getLayoutDefinition(ctx, key);
	LayoutViewHandler.buildUIComponentTree(ctx, parent, def);
    }


    /**
     *	<p> This method search for the requested simple id in the given
     *	    <code>UIComponent</code>.  If the id matches the UIComponent, it
     *	    is returned, otherwise, it will search the children and facets
     *	    recursively.</p>
     *
     *	@param	base	The <code>UIComponent</code> to search.
     *	@param	id	The <code>id</code> we're looking for.
     *
     *	@return	The UIComponent, or null.
     */
    private static UIComponent findComponentById(UIComponent base, String id) {
	// Check if this is the one we're looking for
	if (id.equals(base.getId())) {
	    return base;
	}

	// Not this one, check its kids
	Iterator<UIComponent> it = base.getFacetsAndChildren();
	UIComponent comp = null;
	while (it.hasNext()) {
	    // Recurse
	    comp = findComponentById(it.next(), id);
	    if (comp != null) {
		// Found!
		return comp;
	    }
	}

	// Not found
	return null;
    }
    /**
     *	<p> This method initializes the theme using the given
     *	    <code>themeName</code> and <code>themeVersion</code>.  If these
     *	    values are not supplied, "suntheme" and "4.2" will be used
     *	    respectively.  This method should be invoked before the theme is
     *	    accessed (for example on the initPage or beforeCreate of the login
     *	    page).</p>
     *
     */
    @Handler(id = "getTheme", input = {
        @HandlerInput(name = "themeName", type = String.class),
        @HandlerInput(name = "themeVersion", type = String.class)
        }, 
        output = {
            @HandlerOutput(name = "themeContext", type = ThemeContext.class)
        })
    public static void getTheme(HandlerContext handlerCtx) {
        String themeName = (String) handlerCtx.getInputValue("themeName");
        String themeVersion = (String) handlerCtx.getInputValue("themeVersion");
        ThemeContext themeContext = AdminguiThemeContext.getInstance(
                handlerCtx.getFacesContext(), themeName, themeVersion);
        handlerCtx.setOutputValue("themeContext", themeContext);
    }

    /**
     *	<p> This method gets the <code>themeName</code> and <code>themeVersion</code>
     *	    via <code>Integration Point</code>.  If more than one is provided
     *	    the one with the lowest <code>priority</code> number will be used. 
     *	    This method should be invoked before the theme is 
     *	    accessed (for example on the initPage or beforeCreate of the login page).</p>
     */
    @Handler(id = "getThemeFromIntegrationPoints", output = {
        @HandlerOutput(name = "themeContext", type = ThemeContext.class)
    })
    public static void getThemeFromIntegrationPoints(HandlerContext handlerCtx) {
        FacesContext ctx = handlerCtx.getFacesContext();
        String type = "org.glassfish.admingui:customtheme";
        List<IntegrationPoint> ipList = getIntegrationPoints(ctx, type);
        if (ipList != null) {
            //if more than one integration point is provided then we
            //need to find the lowest priority number
            int lowest = getLowestPriorityNum(ipList);
            for (IntegrationPoint ip : ipList) {
                int priority = ip.getPriority();
                if (priority == lowest) {
                    String content = ip.getContent();
                    if (content == null || content.equals("")) {
                        throw new IllegalArgumentException("No Properties File Name Provided!");
                    }
                    ClassLoader pluginCL = ConsoleClassLoader.findModuleClassLoader(ip.getConsoleConfigId());
                    URL propertyFileURL = pluginCL.getResource("/" + content);
                    try {
                        Properties propertyMap = new Properties();
                        propertyMap.load(propertyFileURL.openStream());
                        ThemeContext themeContext =
			    AdminguiThemeContext.getInstance(ctx, propertyMap);
			themeContext.setDefaultClassLoader(pluginCL);
                        handlerCtx.setOutputValue("themeContext", themeContext);
                    } catch (Exception ex) {
                        throw new RuntimeException(
                                "Unable to access properties file '" + content + "'!", ex);
                    }
                }
            }
        }

    }

    private static int getLowestPriorityNum(List ipList) {
        Iterator iter = ipList.iterator();
            //assuming priority values can only be 1 to 100
            int lowest = 101;
            while (iter.hasNext()) {
                IntegrationPoint iP = (IntegrationPoint) iter.next();
                if (iP.getPriority() < lowest) {
                    lowest = iP.getPriority();                    
                }
            }

        return lowest;
    }
    
}
