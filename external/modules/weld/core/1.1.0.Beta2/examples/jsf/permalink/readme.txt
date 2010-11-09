Weld Permalink Example (Servlet Environment)
============================================

This example demonstrates the use of Weld in a Servlet environment (Tomcat 6
/ Jetty 6). Contextual state management and dependency injection are handled by
JSR-299. Transaction and persistence context management is handled by the EJB 3
container. No alterations are required to be made to the Servlet container. All
services are self-contained within the deployment.

This example uses a Maven 2 build. Execute the following command to build the
WAR. The WAR will will be located in the target directory after completion of
the build.

 mvn

Now you are ready to deploy.

== Deploying to JBoss AS

If you run a normal Maven build, the artifact it produces is deployable to JBoss
AS by default:

 mvn package

Just copy target/weld-permalink.war to the JBoss AS deploy directory. Open this
local URL to access the running application:

 http://localhost:8080/weld-permalink
 
Alternatively, run ant restart to have the app copied to you ${jboss.home}

== Deploying to standalone Tomcat

If you want to run the application on a standalone Tomcat 6, first download and
extract Tomcat 6. This build assumes you will be running Tomcat in its default
configuration, with a hostname of localhost and port 8080. Before starting
Tomcat, add the following line to conf/tomcat-users.xml to allow the Maven
Tomcat plugin to access the manager application, then start Tomcat:

 <user username="admin" password="" roles="manager"/>

To override this username and password, add a <server> with id tomcat in your
Maven 2 settings.xml file, set the <username> and <password> elements to the
appropriate values and uncomment the <server> element inside the
tomcat-maven-plugin configuration in the pom.xml.

You can deploy the packaged archive to Tomcat via HTTP PUT using this command:

 mvn clean package tomcat:deploy -Ptomcat

Then you use this command to undeploy the application:

 mvn tomcat:undeploy

Instead of packaging the WAR, you can deploy it as an exploded archive
immediately after the war goal is finished assembling the exploded structure:

 mvn compile war:exploded tomcat:exploded -Ptomcat

Once the application is deployed, you can redeploy it using the following command:

 mvn tomcat:redeploy

But likely you want to run one or more build goals first before you redeploy:

 mvn compile tomcat:redeploy -Ptomcat
 mvn war:exploded tomcat:redeploy -Ptomcat
 mvn compile war:exploded tomcat:redeploy -Ptomcat

The application is available at the following local URL:

 http://localhost:8080/weld-permalink

= Importing the project into Eclipse

The recommended way to setup a Weld example in Eclipse is to use the m2eclipse
plugin. This plugin derives the build classpath from the dependencies listed in
the pom.xml file. It also has direct integration with Maven build commands.

To get started open Eclipse and import the project by selecting "Maven
Projects" and browsing to the project folder. You can now develop the project
in Eclipse just like any other project.

You could also prepare the Eclipse project before hand, then import the project
into Eclipse. First, transform the pom.xml into an m2eclipse Eclipse project
using this command:

 mvn eclipse:m2eclipse

Now go into Eclipse an import the project by selecting "Existing projects into
workspace" and selecting the project folder. Both approaches use the Eclipse
project configuration defined in the pom.xml file.

vim:tw=80
