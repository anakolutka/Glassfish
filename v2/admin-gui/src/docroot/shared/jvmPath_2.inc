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
<!-- shared/jvmPath_2.inc -->

#include "alertMsg.inc"      
                <!-- Page Title -->
                <sun:title id="propertyContentPage" title="$resource{i18n.jvm.PathSettingsTitle}"
                    helpText="$resource{i18n.jvm.PathSettingsPageHelp}">
                    <!-- Buttons  -->                    
                    <!facet pageButtonsTop>
#include "shared/jvmPathButtons.inc"
                    </facet>    
                    </sun:title>
                   
                    <!-- PropertySheet .... -->
                    <sun:propertySheet id="propertySheet">			 
                        <!-- Text Field section -->               
                        <sun:propertySheetSection id="propertSectionTextField">
                            <sun:property id="SystemClasspathProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.SystemClasspath}" helpText="$resource{i18n.jvm.SystemClasspathHelp}">
                                <sun:textArea id="systemClasspath" rows="$int{4}" columns="$int{75}" text="#{requestScope.systemClasspath}"/>   
                            </sun:property>
                            <sun:property id="EnvironmentClasspathProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.EnvironmentClasspath}">
                                <sun:checkbox label="$resource{i18n.common.Ignore}" selected="#{requestScope.ignoreEnvClasspath}" />         
                            </sun:property>
                            <sun:property id="ServerClasspathProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.ServerClasspath}" helpText="$resource{i18n.jvm.ServerClasspathHelp}">
                                <sun:textArea id="ServerClasspath" rows="$int{4}" columns="$int{75}" text="#{requestScope.serverClasspath}"/>       
                            </sun:property>
                            <sun:property id="ClasspathPrefixProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.ClasspathPrefix}" helpText="$resource{i18n.jvm.ClasspathPrefixHelp}">
                                <sun:textArea id="ClasspathPrefix" rows="$int{4}" columns="$int{75}" text="#{requestScope.classpathPrefix}"/>        
                            </sun:property>
                            <sun:property id="ClasspathSuffixProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.ClasspathSuffix}" helpText="$resource{i18n.jvm.ClasspathSuffixHelp}">
                                <sun:textArea id="ClasspathSuffix" rows="$int{4}" columns="$int{75}" text="#{requestScope.classpathSuffix}"/>        
                            </sun:property> 
                            <sun:property id="NativeLibraryPathPrefixProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.NativeLibraryPathPrefix}" helpText="$resource{i18n.jvm.NativeLibraryPathPrefixHelp}">
                                <sun:textArea id="NativeLibraryPathPrefix" rows="$int{4}" columns="$int{75}" text="#{requestScope.nativeLibPathPrefix}"/>      
                            </sun:property>      
                            <sun:property id="NativeLibraryPathSuffixProp"  labelAlign="left" noWrap="#{true}" overlapLabel="#{false}" label="$resource{i18n.jvm.NativeLibraryPathSuffix}" helpText="$resource{i18n.jvm.NativeLibraryPathSuffixHelp}">
                                <sun:textArea id="NativeLibraryPathSuffix" rows="$int{4}" columns="$int{75}" text="#{requestScope.nativeLibPathSuffix}"/>      
                            </sun:property>                  
                            "<br /><br />
                        </sun:propertySheetSection>
                    </sun:propertySheet>    
                     <sun:title  id="title2" >
                    <!-- Buttons  -->                    
                    <!facet pageButtonsBottom>
#include "shared/jvmPathButtons.inc"
                    </facet>                            
                </sun:title>
	<sun:hidden id="helpKey" value="$attribute{helpKey}" />
            </sun:form>
        </sun:body>
    </sun:html>
</sun:page>
