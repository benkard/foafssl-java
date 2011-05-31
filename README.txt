This repository contains a number of java packages for deploying WebID in servlet containers.
This code is proof of concept code and not tuned for serious use. 

You need to set-up tomcat by following the procedure written out here:
  http://blogs.oracle.com/bblfish/entry/how_to_setup_tomcat_as
This will request client certificates on connection, and will make sure the
usual TLS CA verification mechanism is bypassed.

You can install the identity provider webapp located in the 
  foafssl-identity-provider-webapp
package.  It contains all the jars to get running. This example app is a redirector. It allows
one application server to specialise in logging in users, and it redirects them to the main
server. This is a very particular application of WebID. A fully enabled WebID application should
do the authentciation itself, as this will be a lot more efficient, and improve the user experience.



