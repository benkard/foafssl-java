A servlet for services that cannot deploy WebID due to limitations in deploying SSL. This service acts as an Identity Provider. It authenticates users with WebID ( https://webid.info/spec/ ) and redirects them back to the Relying Party with their WebID encoded as an attribute in the URL, a time stamp and a signature to detect man in the middle attacks.
 
This code is proof of concept code and not tuned for serious use. 

SETUP
-----

The procedure for setting up Tomcat for TLS WebID authentication is detailed here
  http://blogs.oracle.com/bblfish/entry/how_to_setup_tomcat_as
This will request client certificates on connection, and will make sure the usual TLS CA verification mechanism is bypassed.

ISSUES
------

Also there is a logical flaw in this application. The server will request a client certificate without the client ever being able to land on that page and look around. The service needs to be rewritten to give the user and chance to see what identity he is using and allow him to change it. (Due to limitations in browsers this is currently not easy to program without some complex hacking - a future version of this will be distributed to work with browser limitations))
