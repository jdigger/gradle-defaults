/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mooregreatsoftware.gradle.bintray;

import fi.iki.elonen.NanoHTTPD;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;

import static fi.iki.elonen.NanoHTTPD.Method.HEAD;
import static fi.iki.elonen.NanoHTTPD.Response.Status.CREATED;
import static fi.iki.elonen.NanoHTTPD.Response.Status.INTERNAL_ERROR;

@SuppressWarnings("RedundantCast")
public class StubBintrayServer extends NanoHTTPD {
    private static final Logger LOG = LoggerFactory.getLogger(StubBintrayServer.class);

    public int putCount = 0;
    public int postCount = 0;


    public StubBintrayServer() {
        super(0);
    }


    @Override
    public Response serve(IHTTPSession session) {
        try {
            if (session.getMethod().equals(HEAD)) {
                return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");
            }
            else if (session.getMethod().equals(Method.PUT)) {
                putCount++;
                session.parseBody(new HashMap<>());
                return newFixedLengthResponse(CREATED, "application/json", "{\"message\": \"success\"}");
            }
            else if (session.getMethod().equals(Method.POST)) {
                postCount++;
                session.parseBody(new HashMap<>());
                return newFixedLengthResponse(CREATED, "application/json", "{\"message\": \"success\"}");
            }
            else {
                LOG.error("Received HTTP request {} - {}", session.getMethod(), session.getUri());
                return super.serve(session);
            }
        }
        catch (IOException ioe) {
            return newFixedLengthResponse(INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
        }
        catch (ResponseException re) {
            return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, (@NonNull String)re.getMessage());
        }
    }

}
