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
 * Date: 4/16/11
 * Time: 12:32 AM
 * All them constat stuffs.
 */
public class Constants {
    public static final String TAG = "ANDGRAM2";
    public static final String PREFS_NAME = "andgram2_prefs";

    public static final String AUTHORIZATION_URL = "https://api.instagram.com/oauth/authorize/";
    public static final String ACCESS_TOKEN_ENDPOINT = "https://api.instagram.com/oauth/access_token";
    public static final String REDIRECT_URI = "andgram://";

    public static final String POPULAR_ENDPOINT = "https://api.instagram.com/v1/media/popular/";
    public static final String USER_FEED_ENDPOINT = "https://api.instagram.com/v1/users/self/feed";
    public static final String MEDIA_ENDPOINT = "https://api.instagram.com/v1/media/";
    public static final String LIKE_MEDIA_ENDPOINT = "/likes/";
    public static final String COMMENT_MEDIA_ENDPOINT = "/comments";

    public static final String OUTPUT_DIR = "andgram";

}
