/*
 * New BSD license: http://opensource.org/licenses/bsd-license.php
 *
 * Copyright (c) 2010
 * Henry Story
 * http://bblfish.net/
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *  this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 * - Neither the name of bblfish.net nor the names of its contributors
 *  may be used to endorse or promote products derived from this software
 *  without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */


package net.java.dev.sommer.foafssl.keygen;

import sun.security.pkcs.PKCS8Key;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPrivateKey;

/**
 * Key implementation for RSA private keys, non-CRT form (modulus, private
 * exponent only). For CRT private keys, see RSAPrivateCrtKeyImpl. We need
 * separate classes to ensure correct behavior in instanceof checks, etc.
 *
 * Note: RSA keys must be at least 512 bits long
 *
 * Hacked for tests by Henry Story. Don't use this in production.
 *
 * @see sun.security.rsa.RSAPrivateCrtKeyImpl
 * @see sun.security.rsa.RSAKeyFactory
 *
 * @since   1.5
 * @author  Andreas Sterbenz
 */
public final class RSAPrivateKeyImpl extends PKCS8Key implements RSAPrivateKey {

    private static final long serialVersionUID = -33106691987952810L;

    private final BigInteger n;         // modulus
    private final BigInteger d;         // private exponent

    /**
     * Construct a key from its components. Used by the
     * RSAKeyFactory and the RSAKeyPairGenerator.
     */
    RSAPrivateKeyImpl(BigInteger n, BigInteger d) throws InvalidKeyException {
        this.n = n;
        this.d = d;
        // generate the encoding
        try {
            DerOutputStream out = new DerOutputStream();
            out.putInteger(0); // version must be 0
            out.putInteger(n);
            out.putInteger(0);
            out.putInteger(d);
            out.putInteger(0);
            out.putInteger(0);
            out.putInteger(0);
            out.putInteger(0);
            out.putInteger(0);
            DerValue val =
                new DerValue(DerValue.tag_Sequence, out.toByteArray());
            key = val.toByteArray();
        } catch (IOException exc) {
            // should never occur
            throw new InvalidKeyException(exc);
        }
    }

    // see JCA doc
    public String getAlgorithm() {
        return "RSA";
    }

    // see JCA doc
    public BigInteger getModulus() {
        return n;
    }

    // see JCA doc
    public BigInteger getPrivateExponent() {
        return d;
    }

    // return a string representation of this key for debugging
    public String toString() {
        return "Sun RSA private key, " + n.bitLength() + " bits\n  modulus: "
                + n + "\n  private exponent: " + d;
    }

}