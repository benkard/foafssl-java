/*
 *  New BSD license: http://opensource.org/licenses/bsd-license.php
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
 * - Neither the name of bblfish.net, Inc. nor the names of its contributors
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
package net.java.dev.sommer.foafssl.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * A TeeInputStream wraps an Input Stream (which remains an input stream)
 * and pipes everything read to an outputstream, which can be used for
 * caching the input file.  This is inspired by the unix tee(1) command.
 *
 * This Stream can also deal with skipping and rewinds, which won't affect
 * the output to the tee stream.
 *
 * Created on Sep 24, 2007, 12:41:22 PM
 */
public class TeeInputStream
        extends FilterInputStream {

   final static Logger log = Logger.getLogger(TeeInputStream.class.getName());
   final static OutputStream NULLOUT = new NullOutputStream();
   OutputStream forkedOut = null;
   boolean noskip = true;
   int teepos = 0;
   int actualpos = 0; //here we have to guess a bit.
   int markpos = -1;
   int readlimit = -1;

   /**
    *
    * @param wrappedStream the wrapped input stream
    * @param teeStream the stream to write to
    */
   public TeeInputStream(InputStream wrappedStream,
           OutputStream teeStream) {
      super(wrappedStream);

      if (teeStream == null)
         forkedOut = System.out;
      else
         forkedOut = teeStream;
   }

   public TeeInputStream(InputStream chainedStream) {
      this(chainedStream, null);
   }

   /**
    * Output to the Tee stream should never be skipped
    * (not implemented yet)
    * @param noskip if true, never skip anything in the underlying stream when outputing to 
    */
   public void setNoskip(boolean noskip) {
      this.noskip = noskip;
   }

   /**
    * Implementation for parent's abstract write method.
    * This writes out the passed in character to the both,
    * the chained stream and "tee" stream.
    */
   @Override
   public int read() throws IOException {
      int c = super.read();
      actualpos++;
      if (teepos < actualpos) {
         try {
            forkedOut.write(c);
            teepos++;
         } catch (IOException e) {
            log.log(Level.WARNING, "TeeInputStream encountered an Exception when writing to output stream. Turning off Output.", e);
            forkedOut = NULLOUT;
         }
      }
      return c;
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int read = super.read(b, off, len);
      if (read == -1) return read;
      int oldpos = actualpos;
      actualpos += read;
      if (teepos < actualpos) {
         try {
            int teeOff=off, teeLen=read;
            if (oldpos<teepos) {
               int alreadyRead= (teepos - oldpos); // the 
               teeOff = off + alreadyRead;
               teeLen = read - alreadyRead;
            }
            forkedOut.write(b, teeOff, teeLen);
            teepos += teeLen;
         } catch (IOException e) {
            log.log(Level.WARNING, "TeeInputStream encountered an Exception when writing to output stream. Turning off Output.", e);
            forkedOut = NULLOUT;
         }
      }
      return read;
   }

   /**
    * We don't just skip, we in fact read and write the output to the tee stream
    * @param n the number of bytes to skip. WARNING: Currently converted to an int.
    * @return the number of bytes read
    * @throws IOException
    */
   @Override
   public long skip(long n) throws IOException {
      byte[] b = new byte[(int)n];
      int read = super.read(b);
      return read;
   }

   @Override
   public synchronized void mark(int readlimit) {
      super.mark(readlimit);
      this.markpos = actualpos;
      this.readlimit = readlimit;
   }

   @Override
   public synchronized void reset() throws IOException {
      if (markpos != -1 && readlimit != -1) {
         if (markpos+readlimit >= actualpos) {
               super.reset();
               actualpos = markpos;
         }
      }
      markpos = readlimit = -1;
   }

   /**
    * Closes both, chained and tee, streams.
    */
   @Override
   public void close() throws IOException {
      super.close();
      forkedOut.close();
   }
}
