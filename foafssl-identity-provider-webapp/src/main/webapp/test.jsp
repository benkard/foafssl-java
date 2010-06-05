<%@page import="java.io.PrintWriter"%>
<%@page import="net.java.dev.sommer.foafssl.claims.WebIdClaim"%>
<%@page language="java" contentType="text/plain; charset=UTF-8"
        pageEncoding="UTF-8"%>
<%@ page import="java.security.cert.X509Certificate"%>
<%@ page import="javax.security.auth.x500.X500Principal"%>
<%@ page import="java.net.URI"%>
<%@ page import="net.java.dev.sommer.foafssl.principals.WebIdPrincipal"%>
<%@ page import="net.java.dev.sommer.foafssl.verifier.FoafSslVerifier"%>
<%@ page import="net.java.dev.sommer.foafssl.claims.X509Claim"%>
<%
      X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
      if (certs == null) {
         out.println("Cannot find any client certificate.");
      } else {
         out.println("Certificate chain:");
         for (X509Certificate cert : certs) {
            out.println(" - "
                    + cert.getSubjectX500Principal().getName());
         }
         X509Certificate clientCert = certs[0];

         out.println("Public key of client certificate:");
         out.println(clientCert.getPublicKey());

         X509Claim x509Claim = new X509Claim(clientCert);
         boolean verified = x509Claim.verify();

         for (Throwable t : x509Claim.getProblemDescription()) {
            out.println("  -> " + t.getMessage());
            out.println("     ");
            t.printStackTrace(new PrintWriter(out));
         }

         if (verified) {
            out.println("Verified URIs:");
            for (WebIdClaim verifiedClaim : x509Claim.getVerified()) {
               out.println(" - " + verifiedClaim.getPrincipal());
            }
            out.println();
            out.println();
            for (WebIdClaim probClaim : x509Claim.getProblematic()) {
               out.println(" - " + probClaim.getPrincipal());
               for (Throwable t : probClaim.getProblems()) {
                  out.println("  -> " + t.getMessage());
                  out.println("     ");
                  t.printStackTrace(new PrintWriter(out));
               }
            }
         } else {
            out.println("Verification failed.");
         }
      }
%>
