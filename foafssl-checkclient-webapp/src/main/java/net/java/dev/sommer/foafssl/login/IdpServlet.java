/**-----------------------------------------------------------------------
  
Copyright (c) 2009, The University of Manchester, United Kingdom.
All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, 
      this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright 
      notice, this list of conditions and the following disclaimer in the 
      documentation and/or other materials provided with the distribution.
 * Neither the name of the The University of Manchester nor the names of 
      its contributors may be used to endorse or promote products derived 
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.

-----------------------------------------------------------------------*/
package net.java.dev.sommer.foafssl.login;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bouncycastle.util.encoders.Base64;

import net.java.dev.sommer.foafssl.principals.FoafSslPrincipal;
import net.java.dev.sommer.foafssl.verifier.DereferencingFoafSslVerifier;
import net.java.dev.sommer.foafssl.verifier.FoafSslVerifier;

/**
 * @author Bruno Harbulot (Bruno.Harbulot@manchester.ac.uk)
 * 
 */
public class IdpServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public static final transient Logger LOG = Logger.getLogger(IdpServlet.class.getName());

    public static final String SIGNATURE_PARAMNAME = "sig";
    public static final String SIGALG_PARAMNAME = "sigalg";
    public static final String TIMESTAMP_PARAMNAME = "ts";
    public static final String WEBID_PARAMNAME = "webid";
    public static final String AUTHREQISSUER_PARAMNAME = "authreqissuer";

    public final static String KEYSTORE_JNDI_INITPARAM = "keystore";
    public final static String DEFAULT_KEYSTORE_JNDI_INITPARAM = "keystore/signingKeyStore";
    public final static String KEYSTORE_PATH_INITPARAM = "keystorePath";
    public final static String KEYSTORE_TYPE_INITPARAM = "keystoreType";
    public final static String KEYSTORE_PASSWORD_INITPARAM = "keystorePassword";
    public final static String KEY_PASSWORD_INITPARAM = "keyPassword";
    public final static String ALIAS_INITPARAM = "keyAlias";

    private static FoafSslVerifier FOAF_SSL_VERIFIER = new DereferencingFoafSslVerifier();

    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    /**
     * Initialises the servlet: loads the keystore/keys to use to sign the
     * assertions and the issuer name.
     */
    @Override
    public void init() throws ServletException {
        KeyStore keyStore = null;

        String keystoreJdniName = getInitParameter(KEYSTORE_JNDI_INITPARAM);
        if (keystoreJdniName == null) {
            keystoreJdniName = DEFAULT_KEYSTORE_JNDI_INITPARAM;
        }
        String keystorePath = getInitParameter(KEYSTORE_PATH_INITPARAM);
        String keystoreType = getInitParameter(KEYSTORE_TYPE_INITPARAM);
        String keystorePassword = getInitParameter(KEYSTORE_PASSWORD_INITPARAM);
        String keyPassword = getInitParameter(KEY_PASSWORD_INITPARAM);
        if (keyPassword == null)
            keyPassword = keystorePassword;
        String alias = getInitParameter(ALIAS_INITPARAM);

        try {
            Context ctx = null;
            try {
                keyStore = (KeyStore) new InitialContext().lookup("java:comp/env/"
                        + keystoreJdniName);

            } finally {
                if (ctx != null) {
                    ctx.close();
                }
            }
        } catch (NameNotFoundException e) {
        } catch (NamingException e) {
            LOG.log(Level.SEVERE, "Error configuring servlet.", e);
            throw new ServletException(e);
        }
        if (keyStore == null) {
            try {
                InputStream ksInputStream = null;

                try {
                    if (keystorePath != null) {
                        ksInputStream = new FileInputStream(keystorePath);
                    }
                    keyStore = KeyStore.getInstance((keystoreType != null) ? keystoreType
                            : KeyStore.getDefaultType());
                    keyStore.load(ksInputStream, keystorePassword != null ? keystorePassword
                            .toCharArray() : null);
                } finally {
                    if (ksInputStream != null) {
                        ksInputStream.close();
                    }
                }
            } catch (FileNotFoundException e) {
                LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
                throw new ServletException("Could not load keystore.");
            } catch (KeyStoreException e) {
                LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
                throw new ServletException("Could not load keystore.");
            } catch (NoSuchAlgorithmException e) {
                LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
                throw new ServletException("Could not load keystore.");
            } catch (CertificateException e) {
                LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
                throw new ServletException("Could not load keystore.");
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
                throw new ServletException("Could not load keystore.");
            }
        }

        try {
            if (alias == null) {
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String tempAlias = aliases.nextElement();
                    if (keyStore.isKeyEntry(tempAlias)) {
                        alias = tempAlias;
                        break;
                    }
                }
            }
            if (alias == null) {
                LOG.log(Level.SEVERE,
                                "Error configuring servlet, invalid keystore configuration: alias unspecified or couldn't find key at alias: "
                                        + alias);
                throw new ServletException(
                        "Invalid keystore configuration: alias unspecified or couldn't find key at alias: "
                                + alias);
            }
            privateKey = (PrivateKey) keyStore.getKey(alias,
                    keyPassword != null ? keyPassword.toCharArray() : null);
            publicKey = keyStore.getCertificate(alias).getPublicKey();
        } catch (UnrecoverableKeyException e) {
            LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
            throw new ServletException("Could not load keystore.");
        } catch (KeyStoreException e) {
            LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
            throw new ServletException("Could not load keystore.");
        } catch (NoSuchAlgorithmException e) {
            LOG.log(Level.SEVERE, "Error configuring servlet (could not load keystore).", e);
            throw new ServletException("Could not load keystore.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Collection<? extends FoafSslPrincipal> verifiedWebIDs = null;

        /*
         * Verifies the certificate passed in the request.
         */
        X509Certificate[] certificates = (X509Certificate[]) request
                .getAttribute("javax.servlet.request.X509Certificate");
        if (certificates != null) {
            X509Certificate foafSslCertificate = certificates[0];
            try {
                verifiedWebIDs = FOAF_SSL_VERIFIER.verifyFoafSslCertificate(foafSslCertificate);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception when verifying the certificate.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        if ((verifiedWebIDs != null) && (verifiedWebIDs.size() > 0)) {
            String replyTo = request.getParameter(AUTHREQISSUER_PARAMNAME);
            //todo: should one test that replyTo is a URL?

            try {
                if ((replyTo != null) && (replyTo.length() > 0)) {
                    String authnResp = createSignedResponse(verifiedWebIDs, replyTo);
                    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
                    response.setHeader("Location", authnResp);
                } else {
                    usage(response, verifiedWebIDs);
                }
            } catch (InvalidKeyException e) {
                LOG.log(Level.SEVERE, "Error when signing the response.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (NoSuchAlgorithmException e) {
                LOG.log(Level.SEVERE, "Error when signing the response.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (SignatureException e) {
                LOG.log(Level.SEVERE, "Error when signing the response.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    /**
     *
     *
     * @param verifiedWebIDs a list of webIds identifying the user (only the fist will be used)
     * @param simpleRequestParam the service that the response is sent to
     * @param privKey the private key used by this service
     * @return the URL of the response with the webid, timestamp appended and signed
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    private String createSignedResponse(Collection<? extends FoafSslPrincipal> verifiedWebIDs,
            String simpleRequestParam) throws NoSuchAlgorithmException,
            UnsupportedEncodingException, InvalidKeyException, SignatureException {
        /*
         * Reads the FoafSsl simple auth request.
         */
        String authnResp = simpleRequestParam;

        String sigAlg = null;
//        String sigAlgUri = null;
        if ("RSA".equals(privateKey.getAlgorithm())) {
            sigAlg = "SHA1withRSA";
//            sigAlgUri = "rsa-sha1";
        } else if ("DSA".equals(privateKey.getAlgorithm())) {
            sigAlg = "SHA1withDSA";
//            sigAlgUri = "dsa-sha1";
        } else {
            throw new NoSuchAlgorithmException("Unsupported key algorithm type.");
        }

        URI webId = verifiedWebIDs.iterator().next().getUri();
        authnResp += "?" + WEBID_PARAMNAME + "="
                + URLEncoder.encode(webId.toASCIIString(), "UTF-8");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        authnResp += "&" + TIMESTAMP_PARAMNAME + "="
                + URLEncoder.encode(dateFormat.format(Calendar.getInstance().getTime()), "UTF-8");
//  we are not passing information on the public key via the url, so since the information in the url is incomplete, there is no need to
//  pass the algorithm information either
//        authnResp += "&" + SIGALG_PARAMNAME + "=" + URLEncoder.encode(sigAlgUri, "UTF-8");

        String signedMessage = authnResp;
        Signature signature = Signature.getInstance(sigAlg);
        signature.initSign(privateKey);
        signature.update(signedMessage.getBytes("UTF-8"));
        byte[] signatureBytes = signature.sign();
        authnResp += "&" + SIGNATURE_PARAMNAME + "="
                + URLEncoder.encode(new String(Base64.encode(signatureBytes)), "UTF-8");
        return authnResp;
    }

   /** 
    * Web page to explain the usage of this servlet. 
    * Please improove, by externalising the html. 
    */
   private void usage(HttpServletResponse response, Collection<? extends FoafSslPrincipal> verifiedWebIDs) throws IOException {
      StringBuffer res = new StringBuffer();
      res.append("<html><head><title>foaf+ssl identity servlet</title></head><body>")
              .append("<h1>foaf+ssl identity provider servlet</h1>")
              .append("<p>This is a very basic Identity Provider for <a href='http://esw.w3.org/topic/foaf+ssl'>foaf+ssl</a>.")
            .append(" It identifies a user connecting using ssl to this service, and returns ")
            .append("the <a href='http://esw.w3.org/topic/WebID'>WebId</a> of the user to the service in a secure manner.")
            .append("The user that just connected right now for example has ");
      if (verifiedWebIDs.size() == 0) {
         res.append(" no verified webIds.");
      } else {
         res.append(" the following WebIds:<ul>");
         for (FoafSslPrincipal ids: verifiedWebIDs) {
            res.append("<li><a href='").append(ids.getUri()).append("'>")
                    .append(ids.getUri()).append("</a></li>");
         }
         res.append("</ul>");
      }
      res.append("</p>")
              .append("<h3>How does it work?</h3>")
              .append("<p>This service just sends a redirect to the service given by the '")
              .append(AUTHREQISSUER_PARAMNAME)
              .append("' parameter. ")
              .append(" The redirected to URL is constructed on the following pattern:")
              .append("<pre><b>$").append(AUTHREQISSUER_PARAMNAME).append("?")
              .append(WEBID_PARAMNAME).append("=$webid&amp;")
              .append(TIMESTAMP_PARAMNAME).append("=$timeStamp</b>&amp;")
              .append(SIGALG_PARAMNAME).append("=$URLSignature")
              .append("</pre>");
      res.append("Where the above variables have the following meanings:")
              .append("<ul><li><code>$").append(AUTHREQISSUER_PARAMNAME)
              .append("</code>: is the URL passed by the server in the initial request</li>")
              .append("<li><code>$webid</code> is the webid of the user connecting")
              .append("<li><code>$timeStamp</code> is a time stamp in XML Schema format (same as used by Atom).")
              .append(" This is needed to reduce the ease of developing replay attacks.")
              .append("<li><code>$URLSignature</code> is the signature of the whole url in bold above")
              .append("</ul>");
      res.append("The public key used by this service that verifies the signature is:");
      if ("RSA".equals(privateKey.getAlgorithm())) {
           RSAPublicKey certRsakey = (RSAPublicKey)publicKey;
         res.append("<ul><li>Key Type: RSA</li>")
                 .append("<li>public exponent: ").append(certRsakey.getPublicExponent())
                 .append("</li>");
         res.append("<li>modulus: ").append(certRsakey.getModulus())
                 .append("</li></ul>");
         res.append("The signature uses the SHA1withRSA algorithm.");
      } else {
         //todo for other
      }

      res.append("<h3>Try it out from here</h3>");
      res.append("<form action='' method='get'>")
              .append("Requesting service URL: <input type='text' size='80' name='").append(AUTHREQISSUER_PARAMNAME).append("'></input>")
              .append("<input type='submit' value='test this'>")
              .append("</form>");

      res.append("</p></body></html>");
      response.getWriter().print(res);
   }
}
