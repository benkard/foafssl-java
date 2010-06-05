This contains a few simple foaf+ssl webapp


INSTALL ON TOMCAT (6.x)
-----------------

1. Prepare Tomcat for foaf+ssl usage 
 follow the documentation at
 http://code.google.com/p/jsslutils/wiki/ApacheTomcatUsage
 requires you to install 2 jars in the lib directory
 and to edit the conf/server.xml file

2. It seems that we need to install the log4j libraries
  as explained http://tomcat.apache.org/tomcat-6.0-doc/logging.html

3. Move the war into the webapps directory
  (this should be expanded automatically when tomcat is running)

3.1 Edit the srv/WEB-INF/web.xml file

Note: see also the older HOWTO
http://blogs.sun.com/bblfish/entry/how_to_setup_tomcat_as
