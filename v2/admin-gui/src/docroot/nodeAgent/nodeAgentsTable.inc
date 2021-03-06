<!--
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 
 Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 
 The contents of this file are subject to the terms of either the GNU
 General Public License Version 2 only ("GPL") or the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License. You can obtain
 a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 
 When distributing the software, include this License Header Notice in each
 file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 Sun designates this particular file as subject to the "Classpath" exception
 as provided by Sun in the GPL Version 2 section of the License file that
 accompanied this code.  If applicable, add the following below the License
 Header, with the fields enclosed by brackets [] replaced by your own
 identifying information: "Portions Copyrighted [year]
 [name of copyright owner]"
 
 Contributor(s):
 
 If you wish your version of this file to be governed by only the CDDL or
 only the GPL Version 2, indicate your decision by adding "[Contributor]
 elects to include this software in this distribution under the [CDDL or GPL
 Version 2] license."  If you don't indicate a single choice of license, a
 recipient has the option to distribute your version of this file under
 either the CDDL, the GPL Version 2 or to extend the choice of license to
 its licensees as provided above.  However, if you add GPL Version 2 code
 and therefore, elected the GPL Version 2 license, then the option applies
 only if the new code is made subject to such option by the copyright
 holder.
-->
<!-- nodeAgent/nodeAgentsTable.jsf -->
<sun:table id="nodeAgentsTable" title="$resource{i18n.nodeAgent.TableTitle}" sortPanelToggleButton="#{false}" 
            deselectMultipleButton="$boolean{true}"
            deselectMultipleButtonOnClick="setTimeout('changeOneTableButton()', 0)"
            selectMultipleButton="$boolean{true}"
            selectMultipleButtonOnClick="setTimeout('changeOneTableButton()', 0)" >
         <!afterCreate
            getClientId(component="$this{component}" clientId=>$page{tableId});
        />
    <!facet actionsTop>
    <sun:panelGroup id="topActionsGroup1">
         <!afterCreate
                getClientId(component="$this{component}" clientId=>$page{topActionGroup});
            />
        <sun:button id="newButton" text="$resource{i18n.button.New}" >
            <!command redirect(page="nodeAgentNew.jsf"); />
        </sun:button> 
        
        <sun:button id="button1" text="$resource{i18n.button.Delete}" disabled="#{true}" primary="#{false}"
                onClick="if ( getConfirm(this,'$resource{i18n.msg.JS.confirmDeleteNodeAgent}') )
                    { return submitAndDisable(this, '$resource{i18n.button.Processing}');} 
                    else {return false;}"
            >
            <!command 
            prepareSuccessfulMsg();
            getUIComponent(clientId="$pageSession{nodeAgentsTableRowGroupId}", component=>$attribute{tableRowGroup});      
            getSelectedSingleMapRows(TableRowGroup="${tableRowGroup}" selectedRows=>$attribute{selectedRows});
            deleteSelectedNodeAgents(selectedRows="${selectedRows}");
            setAttribute(key="selectedRows" value="${selectedRows}");
            navigate(page="nodeAgent/nodeAgents.jsf");
            />            
        </sun:button>

        <sun:dropDown id="nodeAgentsFilterDropDown"
            disabled="#{false}" 
            forgetValue="#{true}"
            navigateToValue="#{true}"
            label="View: " 
            labels={"$resource{i18n.common.Summary}", "$resource{i18n.common.Detailed}"}
            rendered='#{true}'
            submitForm="#{true}"
            value="#{sessionScope.nodeAgentsFilterSelectedValue}"
            values={"nodeAgents.jsf", "nodeAgentsDetailed.jsf"}
            >
            <!command
                getUIComponent(clientId="propertyForm:nodeAgentsTable:topActionsGroup1:nodeAgentsFilterDropDown", component=>$attribute{nodeAgentsFilterDropDown});	
                getUIComponentProperty(component="$attribute{nodeAgentsFilterDropDown}", name="value", value=>$page{nodeAgentsFilterDropDownValue});	
                redirect(page="#{nodeAgentsFilterDropDownValue}"); 
            />
        </sun:dropDown>      

    </sun:panelGroup>
    </facet>
    
    <sun:tableRowGroup id="rowGroup1" selected="#{td.value.selected}" data={"$attribute{listOfRows}"} sourceVar="td">
        <!afterCreate
        getClientId(component="$this{component}" clientId=>$page{nodeAgentsTableRowGroupId});
       />
 
        <sun:tableColumn selectId="select" rowHeader="$boolean{false}" id="col0">
            <sun:checkbox id="select" 
                selected="#{td.value.selected}" 
                selectedValue="$boolean{true}" 
                onClick="setTimeout('initAllRows(); changeOneTableButton()', 0)" 
            />
        </sun:tableColumn>    
        
        <sun:tableColumn headerText="$resource{i18n.common.PropertyName}" rowHeader="$boolean{true}" 
            id="col1">
            <event>
                <!beforeEncode
                    setPageSessionAttribute(key="nodeAgentName" value="#{td.value.name}");
                />				    
            </event>
            <sun:hyperlink id="link" text="#{td.value.name}" 
                 url="/nodeAgent/nodeAgentGeneral.jsf?nodeAgentName=#{td.value.name}" />
        </sun:tableColumn>   
        <sun:tableColumn 
            headerText="$resource{i18n.nodeAgent.colHostName}" rowHeader="$boolean{true}" 
            id="col2">
            <staticText id="colHostName" value="#{td.value.hostName}" />
        </sun:tableColumn>
         <sun:tableColumn 
            headerText="$resource{i18n.nodeAgent.colStatus}" rowHeader="$boolean{true}" 
            id="col3">
            <staticText id="colStatus" value="#{td.value.status}" />
        </sun:tableColumn> 
        <sun:tableColumn 
            headerText="$resource{i18n.nodeAgent.colInstances}"  rendered="#{!isSummary}" 
            rowHeader="$boolean{true}" id="col4">
             <event>
                <!beforeEncode
                    getInstancesList(NodeAgentName="#{nodeAgentName}", Result=>$attribute{listOfInstances} );
                />				    
            </event>
            <foreach key="instance" list="#{listOfInstances}">
		<sun:hyperlink rendered="#{!instance.isCluster}" url="../standalone/standaloneInstanceGeneral.jsf?instanceName=#{instance.name}" text="#{instance.name}" />
                <sun:hyperlink rendered="#{instance.isCluster}" url="../cluster/clusterInstanceGeneral.jsf?clusterName=#{instance.clusterName}&instanceName=#{instance.name}" text="#{instance.name}" />
                "<br />
            </foreach>
        </sun:tableColumn>
        <sun:tableColumn 
            headerText="$resource{i18n.nodeAgent.colInstancesStopped}"  rendered="#{isSummary}" 
            rowHeader="$boolean{true}" id="col5">
             <event>
                <!beforeEncode
                    getInstancesList(NodeAgentName="#{nodeAgentName}", Result=>$attribute{listOfInstances} );
                    getNodeAgentInstancesStatus(Instances="#{listOfInstances}", Stopped=>$attribute{stopped}, Restart=>$attribute{restart});
                />				    
            </event>
            <staticText id="colStopped" value="#{stopped}" />
        </sun:tableColumn>
        <sun:tableColumn 
            headerText="$resource{i18n.nodeAgent.colInstancesRestart}"  rendered="#{isSummary}" 
            rowHeader="$boolean{true}" id="col6">
            <staticText id="colRestart" value="#{restart}" />
        </sun:tableColumn>     
    </sun:tableRowGroup>
    
</sun:table>    
