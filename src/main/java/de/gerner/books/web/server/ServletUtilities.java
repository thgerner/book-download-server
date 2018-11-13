/**
 * Copyright (c) 2018 Thomas Gerner
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgment:
 *      This product includes software developed by Thomas Gerner.
 * 4. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.gerner.books.web.server;

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author thomas
 *
 */
public class ServletUtilities
{
    /**
     * Return the relative path associated with a servlet.
     * 
     * @param request
     *      The servlet request we are processing
     */
    public static String getRelativePath(HttpServletRequest request) {

        // Are we being processed by a RequestDispatcher.include()?
        if (request.getAttribute("javax.servlet.include.request_uri") != null) {
            String result = (String) request
                    .getAttribute("javax.servlet.include.path_info");
            if ((result == null) || (result.equals("")))
                result = "/";
            return (result);
        }

        // No, extract the desired path directly from the request
        String result = request.getPathInfo();
        if ((result == null) || (result.equals(""))) {
            result = "/";
        }
        return result;
    }

    /**
     * Get the ETag associated with a file.
     * 
     * @param f
     * @return the ETag
     */
    public static String getETag(File f) {

        String resourceLength = "";
        String lastModified = "";

        if (f != null && !f.isDirectory()) {
            resourceLength = String.valueOf(f.length());
            lastModified = String.valueOf(f.lastModified());
        }

        return "W/\"" + resourceLength + "-" + lastModified + "\"";

    }

    /**
     * Set http header of a file to the reponse
     * @param resp
     * @param f
     * @param eTag
     * @param mimeType
     */
    public static void setFileHeader(HttpServletResponse resp, File f, String eTag, String mimeType)
    {
        long lastModified = f.lastModified();
        resp.setDateHeader("last-modified", lastModified);

        resp.addHeader("ETag", eTag);

        long resourceLength = f.length();
        if (resourceLength > 0) {
            if (resourceLength <= Integer.MAX_VALUE) {
                resp.setContentLength((int) resourceLength);
            } else {
                resp.setHeader("content-length", String.valueOf(resourceLength));
                // is "content-length" the right header?
                // is long a valid format?
            }
        }

        if (mimeType != null) {
            resp.setContentType(mimeType);
        } else {
            String path = f.getName();
            int lastDot = path.lastIndexOf(".");
            if (lastDot == -1) {
                resp.setContentType("text/html");
            } else {
                resp.setContentType("application/" + path.substring(lastDot));
            }
        }
    }
}
