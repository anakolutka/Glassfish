To run connector devtests in GlassFish v3

Checkout the following :

svn -N co https://svn.dev.java.net/svn/glassfish-svn/trunk/v2/appserv-tests
cd appserv-tests
svn co https://svn.dev.java.net/svn/glassfish-svn/trunk/v2/appserv-tests/config
svn co https://svn.dev.java.net/svn/glassfish-svn/trunk/v2/appserv-tests/lib
svn co https://svn.dev.java.net/svn/glassfish-svn/trunk/v2/appserv-tests/util
svn -N co https://svn.dev.java.net/svn/glassfish-svn/trunk/v2/appserv-tests/devtests
cd devtests
svn co https://svn.dev.java.net/svn/glassfish-svn/trunk/v2/appserv-tests/devtests/connector
cd connector/v3

set environment :
export APS_HOME=<appserv-tests> directory
export S1AS_HOME=<GlassFish Installation> directory
Set CLASSPATH to contain javax.resource.jar. 
export CLASSPATH=$S1AS_HOME/modules/javax.resource.jar:$CLASSPATH

$S1AS_HOME/bin/asadmin start-domain domain1
use "ant startDerby" to start derby via appserv-tests (APS_HOME) target so that a stored procedure needed by connector test (cci, cci-embedded) is available

To run all tests : ant all
To run with security manager turned on : ant all-with-security-manager

$S1AS_HOME/bin/asadmin stop-domain domain1
cd $APS_HOME/devtests/connector/v3 
ant stopDerby


Results will be generated as : $APS_HOME/test_results.html
Console output as : $APS_HOME/devtests/connector/v3/connector.output

