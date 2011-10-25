This repository contains a number of java packages for deploying WebID in servlet containers.
This code is proof of concept code and not tuned for serious use. 

You need to set-up tomcat by following the procedure written out here:
  http://blogs.oracle.com/bblfish/entry/how_to_setup_tomcat_as
This will request client certificates on connection, and will make sure the
usual TLS CA verification mechanism is bypassed.

The packages to be found here are:

foafssl-verifier:
   The core verification package.
foafssl-verifier-sesame:
   A implementation of verification using the Sesame rdf store
foafssl-filter:
   A Servlet Filter that captures the certificate information from the attributes passed to the servlet
          X509Certificate[] certs = (X509Certificate[]) req
                .getAttribute("javax.servlet.request.X509Certificate");
   and verifies the authentication using the foafssl-verifier packages
foafssl-identity-provider:
foafssl-identity-provider-webapp:
   An Identity Provider WebApp as a demo application. This example app is a redirector. It allows one application server to specialise in logging in users, and it redirects them to the main server. This is a very particular application of WebID. A fully enabled WebID application should do the authentciation itself, as this will be a lot more efficient, and improve the user experience.

keygen: 
   This contains the functionality to allow web apps to create keys for keygen enabled browsers and IE inbuilt ActiveX conrol

WebID is integrated much more closely in the bblfish branch of Clerezza which you can find 
  https://github.com/bblfish/clerezza
That shows how certificate creation can be tied into user accounts, how authentication works, contains a test suite, and allows authentication of WebID users without giving them an account.


