/*
New BSD license: http://opensource.org/licenses/bsd-license.php

Copyright (c) 2008-2009 Sun Microsystems, Inc.
901 San Antonio Road, Palo Alto, CA 94303 USA. 
All rights reserved.


Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright notice, 
this list of conditions and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.
- Neither the name of Sun Microsystems, Inc. nor the names of its contributors
may be used to endorse or promote products derived from this software 
without specific prior written permission.

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
 */
package net.java.dev.sommer.foafssl.verifier;

import java.util.Iterator;
import java.util.ServiceLoader;
import net.java.dev.sommer.foafssl.claims.WebIdClaim;

/**
 * This is an abstract FOAF+SSL verifier.
 * 
 * @author Bruno Harbulot.
 */
public abstract class FoafSslVerifier {
    // of course this should be created by lookup not here in the code
    private static FoafSslVerifier verifier = null;

    /**
     * Verifies a WebId using FOAF+SSL
     * 
     * @param webid
     *            a Web ID claim
     * @return true if verified
     */
    public abstract boolean verify(WebIdClaim webid);

    
	/**
	 * This returns the singleton instance. If an instance has been previously
	 * bound (e.g. by OSGi declarative services) this instance is returned,
	 * otherwise a new instance is created and providers are injected using the
	 * service provider interface (META-INF/services/)
	 *
	 * @return the singleton instance
	 */
	static public FoafSslVerifier getVerifier() {
		if (verifier == null) {
			synchronized (FoafSslVerifier.class) {
				if (verifier == null) {
					Iterator<FoafSslVerifier> weightedProviders = ServiceLoader
							.load(FoafSslVerifier.class).iterator();
					if (weightedProviders.hasNext()) {
						verifier= weightedProviders.next();
					} else {
						throw new RuntimeException("no FoafSslVerifier could be located");
					}
				}
			}
		}
		return verifier;
	}
}
