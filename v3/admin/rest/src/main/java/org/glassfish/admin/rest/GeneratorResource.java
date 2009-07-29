/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
package org.glassfish.admin.rest;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Path;

import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandRunner;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;

/**
 * @author Ludovic Champenois ludo@dev.java.net
 * @author Rajeshwar Patil
 */
@Path("/generator/")
public class GeneratorResource {

    private DomDocument document;
    protected String genDir;

    /** Creates a new instance of xxxResource */
    public GeneratorResource() {
    }

    @GET
    @Produces({"text/plain"})
    public String get() {

        Domain entity = RestService.getDomain();

        File loc =
                new File(System.getProperty("user.home") + "/acvs/v3/admin/rest/src/main/java/org/glassfish/admin/rest/resources");
        loc.mkdirs();
        genDir = loc.getAbsolutePath();
        //        DomDocument dodo = RestService.getHabitat().getComponent(DomDocument.class);
        //        Dom root = dodo.getRoot();
        // System.out.println(" root "+ root );
        Dom dom1 = Dom.unwrap(entity);
        Dom root = dom1.document.getRoot();
        document = dom1.document;

        ConfigModel rootModel = root.model;


        //+ Domain domain = Domain.class.cast(RestService.getHabitat().getComponent(Domain.class.getName(), ""));
        try {
            generateSingle(rootModel);
        } catch (Exception ex) {
            Logger.getLogger(GeneratorResource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "Code Generation done at : " + genDir;

    }

    private void processRedirectsAnnotation(ConfigModel model) {

        Class<? extends ConfigBeanProxy> cbp = null;
        System.out.println("\n\nAnnotation" + model.targetTypeName);
        try {
            cbp = (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.get().loadClass(model.targetTypeName);
            // cbp = (Class<? extends ConfigBeanProxy>)this.getClass().getClassLoader().loadClass(model.targetTypeName) ;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("re Annotation" + model.targetTypeName);
        RestRedirects restRedirects = cbp.getAnnotation(RestRedirects.class);
        System.out.println("re Annotation restRedirects" + restRedirects);
        if (restRedirects != null) {
            System.out.println("LUDO: NOT NULL                Annotation restRedirects" + restRedirects);

            RestRedirect[] values = restRedirects.value();
            for (RestRedirect r : values) {
                System.out.println(r.commandName());
                System.out.println(r.opType());
            }
        }


    }

    private void genHeader(BufferedWriter out) throws IOException {
        out.write("/**\n");
        out.write("* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.\n");
        out.write("* Copyright 2009 Sun Microsystems, Inc. All rights reserved.\n");
        out.write("* Generated code from the com.sun.enterprise.config.serverbeans.*\n");
        out.write("* config beans, based on  HK2 meta model for these beans\n");
        out.write("* see generator at org.admin.admin.rest.GeneratorResource\n");
        out.write("* date=" + new Date() + "\n");
        out.write("* Very soon, this generated code will be replace by asm or even better...more dynamic logic.\n");
        out.write("* Ludovic Champenois ludo@dev.java.net\n");
        out.write("*\n");
        out.write("**/\n");
    }
    private HashMap<String, String> genSingleFiles = new HashMap<String, String>();
    private HashMap<String, String> genListFiles = new HashMap<String, String>();
    private HashMap<String, String> genCommandResourceFiles = new HashMap<String, String>();

    public void generateList(ConfigModel model) throws IOException {

        String serverConfigName = model.targetTypeName.substring(model.targetTypeName.lastIndexOf(".") + 1,
                model.targetTypeName.length());

        if (genListFiles.containsKey(serverConfigName)) {
            return;
        }
        genListFiles.put(serverConfigName, serverConfigName);
        String beanName = getBeanName(serverConfigName);
        File file = new File(genDir + "/List" + beanName + "Resource.java");
        // File file = new File("/Users/ludo/tmp/" + beanName + "Resource.java");
        try {
            file.createNewFile();
        } catch (Exception e) {
        }




        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);
        genHeader(out);
        out.write("package org.glassfish.admin.rest.resources;\n");
        out.write("import com.sun.enterprise.config.serverbeans.*;\n");
        out.write("import javax.ws.rs.*;\n");
        out.write("import java.util.List;\n");
//        out.write("import org.glassfish.admin.rest.TemplateResource;\n");
        out.write("import org.glassfish.admin.rest.TemplateListOfResource;\n");
//        out.write("import com.sun.jersey.api.core.ResourceContext;\n");
        out.write("import org.glassfish.admin.rest.provider.GetResultList;\n");
        out.write("import " + model.targetTypeName + ";\n");



        // out.write("@Path(\"/" + serverConfigName + "/\")\n");
        out.write("public class List" + beanName + "Resource extends TemplateListOfResource<" + beanName + "> {\n\n");

////        out.write("private List<" + beanName + "> entity;\n");
////        out.write("public void setEntity(List<" + beanName + "> p ){\n");
////        out.write("entity = p;\n");
////        out.write("}\n");
////
////
////        out.write("public  List<" + beanName + "> getEntity() {\n");
////        if (beanName.equals("Domain")) {
////            out.write("return RestService.theDomain;\n");
////        } else {
////            out.write("return entity;\n");
////
////        }
////        out.write("}\n");
        String keyAttributeName = null;
        if (model.key == null) {
            try {
                for (String s : model.getAttributeNames()) {//no key, by default use the name attr
                    if (s.equals("name")) {
                        keyAttributeName = getBeanName(s);
                    }
                }
                if (keyAttributeName == null)//nothing, so pick the ifrst one
                {
                    keyAttributeName = getBeanName(model.getAttributeNames().iterator().next());
                }
            } catch (Exception e) {
                keyAttributeName = "ThisIsAModelBug:NoKeyAttr"; //no attr choice fo a key!!! Error!!!
                } //firstone
            } else {
            keyAttributeName = getBeanName(model.key.substring(1, model.key.length()));
        }
        out.write("\n");
        out.write("\t@Path(\"{" + keyAttributeName + "}/\")\n");
        out.write("\tpublic " + beanName + "Resource get" + beanName + "Resource(@PathParam(\"" + keyAttributeName + "\") String id) {\n");
        out.write("\t\t" + beanName + "Resource resource = resourceContext.getResource(" + beanName + "Resource.class);\n");
        out.write("\t\tfor (" + beanName + " c: entity){\n");
        if (model.key == null) {
            out.write("//THIS KEY IS THE FIRST Attribute ONE ludo\n");

        }
        out.write("\t\t\tif(c.get" + keyAttributeName + "().equals(id)){\n");
        out.write("\t\t\t\tresource.setEntity(c);\n");
        out.write("\t\t\t}\n");
        out.write("\t\t}\n");
        out.write("\t\treturn resource;\n");
        out.write("\t}\n\n");
 ///////ludo       generateCommand("List" + beanName, out);
        generateCommandResources("List" + beanName, out);

        out.write("\n");
        generateGetPostCommandMethod("List" + beanName, out);

        out.write("}\n");

        out.close();
        System.out.println("created:" + file.getAbsolutePath());

        generateSingle(model);


    }

    public void generateSingle(ConfigModel model) throws IOException {
        processRedirectsAnnotation(model);

        String serverConfigName = model.targetTypeName.substring(model.targetTypeName.lastIndexOf(".") + 1,
                model.targetTypeName.length());

        if (genSingleFiles.containsKey(serverConfigName)) {
            return;
        }
        genSingleFiles.put(serverConfigName, serverConfigName);
        String beanName = getBeanName(serverConfigName);
        File file = new File(genDir + "/" + beanName + "Resource.java");
        // File file = new File("/Users/ludo/tmp/" + beanName + "Resource.java");
        try {
            file.createNewFile();
        } catch (Exception e) {
        }




        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);
        genHeader(out);
        out.write("package org.glassfish.admin.rest.resources;\n");
        out.write("import com.sun.enterprise.config.serverbeans.*;\n");
        out.write("import javax.ws.rs.*;\n");
//        out.write("import java.util.*;\n");
        out.write("import org.glassfish.admin.rest.TemplateResource;\n");
//        out.write("import org.glassfish.admin.rest.TemplateListOfResource;\n");
//        out.write("import com.sun.jersey.api.core.ResourceContext;\n");
        out.write("import org.glassfish.admin.rest.provider.GetResult;\n");
        out.write("import " + model.targetTypeName + ";\n");



        if (beanName.equals("Domain")) {
            out.write("@Path(\"/" + "domain" + "/\")\n");
        }

        out.write("public class " + beanName + "Resource extends TemplateResource<" + beanName + "> {\n\n");

//        if (!beanName.equals("Domain")) {
//            out.write("private " + beanName + " entity;\n");
//            out.write("public void setEntity(" + beanName + " p ){\n");
//            out.write("entity = p;\n");
//            out.write("}\n");
//        }

        if (beanName.equals("Domain")) {
            out.write("@Override public " + beanName + " getEntity() {\n");
            out.write("return org.glassfish.admin.rest.RestService.getDomain();\n");
            out.write("}\n");
        }

     /////ludo   generateCommand(beanName, out);

        generateCommandResources(beanName, out);

        Set<String> elem = model.getElementNames();

        for (String a : elem) {
            System.out.println("a= " + a);



            ConfigModel.Property prop = model.getElement(a);


            if (prop != null && prop.isLeaf()) {
                System.out.println("proxy.getElement(a).isLeaf() " + a);
            } else {
                ConfigModel.Node node = (ConfigModel.Node) prop;
                //String childbeanName = getBeanName(a);

                ConfigModel childModel = node.getModel();
                String childbeanName = childModel.targetTypeName.substring(childModel.targetTypeName.lastIndexOf(".") + 1,
                        childModel.targetTypeName.length());

                String getterName = getBeanName(a);

                System.out.println("Model.targetTypeName" + model.targetTypeName);
                System.out.println("newModel.targetTypeName" + childModel.targetTypeName);
                System.out.println("ConfigModel.Node node isCollection=" + prop.isCollection());
                System.out.println("ConfigModel.Node node isLeaf=" + prop.isLeaf());
                System.out.println("ConfigModel.Node node xlmname=" + prop.xmlName());
                if (childModel.targetTypeName.endsWith("Named")) {
                    a = "application";
                    childbeanName = "Application";
                    getterName = "Applications";
                    try {
                        Class<?> subType = childModel.classLoaderHolder.get().loadClass(childModel.targetTypeName); ///  a shoulf be the typename


                        List<ConfigModel> lcm = document.getAllModelsImplementing(subType);
                        if (lcm != null) {
                            for (ConfigModel childmodel : lcm) {
                                System.out.println("***childmodel.targetTypeName" + childmodel.targetTypeName);
                                if (childmodel.targetTypeName.equals("com.sun.enterprise.config.serverbeans.Application")) {
                                    childModel = childmodel;
                                }
                            }
                        }
                    } catch (Exception e) {
                    }


                }

                if (!childModel.targetTypeName.endsWith("Resource")) {
                    String prefix = "";
                    if (prop.isCollection()) {
                        prefix = "List";
                    }

                    if (!a.equals("*")) {
                        out.write("\t@Path(\"" + a + "/\")\n");
                        out.write("\tpublic " + prefix + childbeanName + "Resource get" + childbeanName + "Resource() {\n");

                        out.write("\t\t" + prefix + childbeanName + "Resource resource = resourceContext.getResource(" + prefix + childbeanName + "Resource.class);\n");
                        out.write("\t\tresource.setEntity(getEntity().get" + getterName + "() );\n");
                        out.write("\t\treturn resource;\n");
                        out.write("\t}\n");
                    }

                    if (prop.isCollection()) {
                        generateList(childModel);
                    } else {
                        generateSingle(childModel);

                    }


                } else {




                    try {
                        Class<?> subType = childModel.classLoaderHolder.get().loadClass(childModel.targetTypeName); ///  a shoulf be the typename


                        List<ConfigModel> lcm = document.getAllModelsImplementing(subType);
                        if (lcm != null) {
                            for (ConfigModel childmodel : lcm) {
                                System.out.println("--->targetTypeName=" + childmodel.targetTypeName);
                                String newName = childmodel.targetTypeName.substring(childmodel.targetTypeName.lastIndexOf(".") + 1,
                                        childmodel.targetTypeName.length());
                                out.write("@Path(\"" + childmodel.getTagName() + "/\")\n");
                                out.write("public List" + newName + "Resource get" + newName + "Resource() {\n");
                                out.write("List" + newName + "Resource resource = resourceContext.getResource(List" + newName + "Resource.class);\n");
                                out.write("java.util.List<Resource> l = entity.getResources();\n");
                                out.write("java.util.List<" + childmodel.targetTypeName + "> newList = new java.util.ArrayList();\n");
                                out.write("for (Resource r: l){\n");
                                out.write("try {\n");
                                out.write("newList.add((" + childmodel.targetTypeName + ")r);\n");
                                out.write("} catch (Exception e){\n");

                                out.write("}\n");
                                out.write("}\n");
                                out.write("resource.setEntity(newList );\n");
                                out.write("return resource;\n");
                                out.write("}\n\n\n");
                                generateList(childmodel);



                            }
                        }

                        //com.sun.enterprise.config.serverbeans.CustomResource

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }


            }

        }


        out.write("}\n");

        out.close();
        System.out.println("created:" + file.getAbsolutePath());


    }

 

    private String getBeanName(String elementName) {
        String ret = "";
        boolean nextisUpper = true;


        for (int i = 0; i < elementName.length(); i++) {
            if (nextisUpper == true) {
                ret = ret + elementName.substring(i, i + 1).toUpperCase();
                nextisUpper = false;

            } else {
                if (elementName.charAt(i) == '-') {
                    nextisUpper = true;
                } else {
                    nextisUpper = false;
                    ret = ret + elementName.substring(i, i + 1);

                }
            }
        }

        return ret;
    }


    void generateGetPostCommandMethod(String resourceName, BufferedWriter out) throws IOException {
        String commandName = getPostCommandName(resourceName);
        out.write("public String getPostCommand() {\n");
        if (commandName != null) {
            out.write("\treturn \"" + commandName + "\";\n");
        } else {
            out.write("\treturn " + commandName + ";\n");
        }
        out.write("}\n");
    }


    private String getPostCommandName(String resourceName) {
        //FIXME - fetch command name from config bean(RestRedirect annotation).
        for (int i = 0; i < MappingConfigBeansToPOSTCommands.length; i++) {
            if (resourceName.equals(MappingConfigBeansToPOSTCommands[i][0])) {
                    return MappingConfigBeansToPOSTCommands[i][1];
            }
        }
        return null; //POST is not mapped to any create command for this resource
    }

    /*
     * temporary mapping to add Admin Commands to some of our configbeans
     *
     * */
    
    private static String MappingConfigBeansToCommands[][] = {
        {"Domain", "stop-domain"},
        {"Domain", "restart-domain"},
        {"Domain", "uptime"},
        {"Domain", "version"},
        {"Domain", "rotate-log"},
        {"Domain", "get-host-and-port"},
        {"ListApplication", "deploy"},
        {"Application", "redeploy"},
        //  {"ListExternalJndiResource", ""},
        //{"ListWorkSecurityMap", ""},
        //{"ListPersistenceManagerFactoryResource", ""},

        //{"", "create-connector-work-security-map"},
        //  {"", "create-file-user"},
        //    {"", "create-jmsResource"},
        //    {"", "create-jmsdest"},
        //   {"", "create-jvm-options"},
        //    {"", "create-password-alias"},
        ////    {"", "create-ssl"},
        {"ConnectionPool", "ping-connection-pool"},/*
    addResources
    change-admin-password
    disable
    enable
    generate-jvm-report
    get
    get-client-stubs
    // get-host-and-port
    //redeploy
    //undeploy
    // ping-connection-pool

    // create-admin-object
    //create-connector-connection-pool
    // create-connectorResource
    //create-customResource
    //create-javamailResource
    //create-jdbc-connection-pool
    //create-jdbcResource
    // createResource-adapter-config
    delete-admin-object
    delete-audit-module
    delete-auth-realm
    delete-connector-connection-pool
    delete-connectorResource
    delete-connector-work-security-map
    delete-customResource
    delete-file-user
    delete-http-listener
    delete-iiop-listener
    delete-javamailResource
    delete-jdbc-connection-pool
    delete-jdbcResource
    delete-jms-host
    monitor
    delete-jmsResource
    delete-jmsdest
    delete-jvm-options
    delete-message-security-provider
    delete-password-alias
    delete-profiler
    deleteResource-adapter-config
    deleteResource-ref
    update-file-user
    delete-ssl
    update-password-alias
    delete-system-property
    delete-virtual-server


     **/};


    private static String MappingConfigBeansToPOSTCommands[][] = {
        {"ListApplication", "deploy"},
        {"Application", "redeploy"},
        {"ListAdminObjectResource", "create-admin-object"},
        {"ListCustomResource", "create-custom-resource"},
        {"ListJdbcResource", "create-jdbc-resource"},
        {"ListJdbcConnectionPool", "create-jdbc-connection-pool"},
        {"ListConnectorResource", "create-connector-resource"},
        {"ListMailResource", "create-javamail-resource"},
        {"ListResourceAdapterConfig", "create-resource-adapter-config"},
        {"ListConnectorConnectionPool", "create-connector-connection-pool"},
        {"ListAuthRealm", "create-auth-realm"},
        {"ListAuditModule", "create-audit-module"},
        {"ListHttpListener", "create-http-listener"},
        {"ListIiopListener", "create-iiop-listener"},
        {"ListJmsHost", "create-jms-host"},
        {"ListMessageSecurityConfig", "create-message-security-provider"},
        {"JavaConfig", "create-profiler"},
        {"ListResourceRef", "create-resource-ref"},
        {"ListSystemProperty", "create-system-properties"},
        {"ListVirtualServer", "create-virtual-server"},
     };


    private static String ConfigBeansToCommandResourcesMap[][] = {
        //{config-bean, command, method, resource-path}
        {"Domain", "stop-domain", "POST", "stop"},
        {"Domain", "restart-domain", "POST", "restart"},
        {"Domain", "uptime", "GET", "uptime"},
        {"Domain", "version", "GET", "version"},
        {"Domain", "rotate-log", "POST", "rotate-log"},
        {"Domain", "get-host-and-port", "GET", "host-port"},
        ///{"ListApplication", "deploy"},
        ///{"Application", "redeploy"},
        {"ConnectionPool", "ping-connection-pool", "GET", "ping"},
    };


    private void generateCommandResources(String resourceName, 
        BufferedWriter out) throws IOException {

        if (genCommandResourceFiles.containsKey(resourceName)) {
            return;
        }
        genCommandResourceFiles.put(resourceName, resourceName);


        String commandResourcesPaths = "{";
        for (int i = 0; i < ConfigBeansToCommandResourcesMap.length; i++) {
            if (resourceName.equals(ConfigBeansToCommandResourcesMap[i][0])) {
                if (commandResourcesPaths.length() > 1) {
                    commandResourcesPaths = commandResourcesPaths + ", ";
                }
                commandResourcesPaths = commandResourcesPaths + "{" +
                    "\"" + ConfigBeansToCommandResourcesMap[i][3] + "\"" + ", " +
                        "\"" + ConfigBeansToCommandResourcesMap[i][2] + "\"" + "}";

                String commandResourceFileName = genDir + "/" + resourceName +
                    getBeanName(ConfigBeansToCommandResourcesMap[i][3]) +
                        "Resource.java";
                String commandResourceName = resourceName +
                    getBeanName(ConfigBeansToCommandResourcesMap[i][3]) +
                        "Resource";

                //generate command resource for the resource- resourceName
                createCommandResourceFile(commandResourceFileName,
                    commandResourceName, ConfigBeansToCommandResourcesMap[i][1],
                        ConfigBeansToCommandResourcesMap[i][2]);

                //define method with @Path in resource- resourceName
                out.write("@Path(\"" + ConfigBeansToCommandResourcesMap[i][3] + "/\")\n");
                out.write("public " + commandResourceName + " get" +
                    commandResourceName + "() {\n");
                out.write(commandResourceName + " resource = resourceContext.getResource(" + commandResourceName + ".class);\n");
                out.write("return resource;\n");
                out.write("}\n\n");
            }
        }
        commandResourcesPaths = commandResourcesPaths + "}";

        //define method to return command resource paths. only if needed
        if (!commandResourcesPaths.equals("{}")){
        out.write("public String[][] getCommandResourcesPaths() {\n");
        out.write("return new String[][]" +  commandResourcesPaths + ";\n");
        out.write("}\n\n");
        }
    }

    private void createCommandResourceFile(String commandResourceFileName,
        String commandResourceName, String commandName,
            String commandMethod) throws IOException {
        File file = new File(commandResourceFileName);
        try {
            file.createNewFile();
        } catch (Exception e) {}

        FileWriter fstream = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fstream);

        //header
        genHeader(out);

        //package
        out.write("package org.glassfish.admin.rest.resources;\n\n");

        //imports
        out.write("import java.util.HashMap;\n\n");
        out.write("import javax.ws.rs.*;\n");
        out.write("import javax.ws.rs.core.Context;\n");
        out.write("import javax.ws.rs.core.MediaType;\n");
        out.write("import javax.ws.rs.core.Response;\n");
        out.write("import javax.ws.rs.core.UriInfo;\n\n");
        out.write("import org.glassfish.admin.rest.provider.OptionsResult;\n");
        out.write("import org.glassfish.admin.rest.provider.MethodMetaData;\n");
        out.write("import org.glassfish.admin.rest.resources.ResourceUtil;\n");
        out.write("import org.glassfish.admin.rest.RestService;\n");
        out.write("import org.glassfish.api.ActionReport;\n\n");

        //class header
        out.write("public class " + commandResourceName + " {\n\n");

        //constructor
        out.write("public " + commandResourceName + "() {\n");
        out.write("__resourceUtil = new ResourceUtil();\n");
        out.write("}\n");

        //create command method
        createCommandMethod(commandMethod, out);

        //create options method
        createCommandOptionsMethod(out);

        //variable declarations
        out.write("@Context\n");
        out.write("protected UriInfo uriInfo;\n\n");
        out.write("private static final String commandName = \"" + commandName + "\";\n");
        out.write("private static final String commandMethod = \"" + commandMethod + "\";\n");
        out.write("private ResourceUtil __resourceUtil;\n");
        out.write("}\n");

        out.close();
        System.out.println("created:" + file.getAbsolutePath());
    }


    private void createCommandMethod(String commandMethod, BufferedWriter out) throws IOException {
        out.write("@" + commandMethod + "\n");
        out.write("@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})\n");
        out.write("public Response executeCommand(HashMap<String, String> data) {\n");
        out.write("try {\n");
        out.write("if (data.containsKey(\"error\")) {\n");
        out.write("return Response.status(415).entity(\n");
        out.write("\"Unable to parse the input entity. Please check the syntax.\").build();");
        out.write("}/*unsupported media*/\n\n");
        out.write("__resourceUtil.adjustParameters(data);\n\n");

        out.write("ActionReport actionReport = __resourceUtil.runCommand(commandName, data, RestService.getHabitat());\n\n");
        out.write("ActionReport.ExitCode exitCode = actionReport.getActionExitCode();\n\n");

        out.write("if (exitCode == ActionReport.ExitCode.SUCCESS) {\n");
        out.write("return Response.status(200).entity(\"\\\"\" + commandMethod + \" of \"\n");
        out.write("+ uriInfo.getAbsolutePath() + \" executed successfully.\").build();  /*200 - ok*/\n");
        out.write("}\n\n");
        out.write("String errorMessage = actionReport.getMessage();\n");
        out.write("return Response.status(400).entity(errorMessage).build(); /*400 - bad request*/\n");
        out.write("} catch (Exception e) {\n");
        out.write("throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);\n");
        out.write("}\n");
        out.write("}\n");
    }


    private void createCommandOptionsMethod(BufferedWriter out) throws IOException {
        out.write("@OPTIONS\n");
        out.write("@Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.APPLICATION_XML})\n");
        out.write("public OptionsResult options() {\n");
        out.write("OptionsResult optionsResult = new OptionsResult();\n");
        out.write("try {\n");
        out.write("//command method metadata\n");
        out.write("MethodMetaData methodMetaData = __resourceUtil.getMethodMetaData(\n");
        out.write("commandName, RestService.getHabitat(), RestService.logger);\n");

        out.write("optionsResult.putMethodMetaData(commandMethod, methodMetaData);\n");
        out.write("} catch (Exception e) {\n");
        out.write("throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);\n");
        out.write("}\n\n");

        out.write("return optionsResult;\n");
        out.write("}\n\n");
    }
}
