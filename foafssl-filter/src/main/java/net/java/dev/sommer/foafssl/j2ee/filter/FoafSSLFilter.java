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

 Author........: Bruno Harbulot

 -----------------------------------------------------------------------*/
package net.java.dev.sommer.foafssl.j2ee.filter;

import java.io.IOException;
import java.io.PrintStream;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.java.dev.sommer.foafssl.claims.WebIdClaim;
import net.java.dev.sommer.foafssl.claims.X509Claim;

/**
 * @author Bruno Harbulot
 */
public class FoafSSLFilter implements Filter {

    public static final String WEBIDCLAIMS_ATTR_NAME = "net.java.dev.sommer.foafssl.j2ee.webidclaims";

	@Override
    public void init(FilterConfig arg0) throws ServletException {
        // do nothing. Perhaps this sets path regexps arguments?
    }

	@Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        X509Certificate[] certs = (X509Certificate[]) req
                .getAttribute("javax.servlet.request.X509Certificate");
        Collection<? extends WebIdClaim> pls = null;
        try {
            X509Claim x509Claim = new X509Claim(certs[0]);
            if (x509Claim.verify()) {
                pls = x509Claim.getVerified();
                if (pls == null || pls.isEmpty()) {
                    resp.getOutputStream().write("No foaf+ssl certificates".getBytes());
                    return;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(FoafSSLFilter.class.getName()).log(Level.SEVERE, null, ex);
            resp.getOutputStream().write("cought error doing verification:".getBytes());
            ex.printStackTrace(new PrintStream(resp.getOutputStream()));
            return;
        }
        req.setAttribute(WEBIDCLAIMS_ATTR_NAME, pls);
        chain.doFilter(req, resp);
    }

	@Override
    public void destroy() {
        // do nothing yet
    }
}
