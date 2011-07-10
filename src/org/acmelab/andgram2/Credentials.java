/*
 * Copyright 2011, Mark L. Chang <mark.chang@gmail.com>. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY Mark L. Chang ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MARK L. CHANG OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Mark L. Chang.
 */

package org.acmelab.andgram2;

/**
 * Created by IntelliJ IDEA.
 * User: mchang
 * Date: 4/15/11
 * Time: 11:44 AM
 * SEKRET.
 */
public class Credentials {

    /*
        Instagram credentials come from the developer's account. You can register a new
        client (aka. Andgram2) here: http://instagram.com/developer/manage/

        Copy your Client ID and Client Secret into the below Strings
     */
    public static final String CLIENT_ID = "";
    public static final String CLIENT_SECRET = "";

    /*
        Android doesn't include all SSL CA certs, obviously. The signing certificate
        Instagram uses is not in my build of Android. So, you need to bundle all
        the certificates in the signing path, all the way to the root CA. To do so,
        you need to make keystore with all the certificates. Mine is stored here
        in res/raw. I believe it has to have a password associated with it. The
        variable below is that password.

        Instructions on how to make a cert bundle can be found here:
        http://bit.ly/dNKsFt

        I am not including my keystore password here.
     */
    public static final String KEYSTORE_PASSWORD = "";
}
