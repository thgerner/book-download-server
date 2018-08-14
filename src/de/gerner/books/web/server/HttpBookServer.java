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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * @author thomas
 *
 */
public class HttpBookServer
{
    private static Logger LOG = LogManager.getLogger(HttpBookServer.class);
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        File bookRoot = new File("/home/thomas/Download/eBooks");
        File resourceRoot = new File("resources");
        File imageRoot = new File(resourceRoot, "img");

        try
        {
            for (int i = 0; i < args.length; i++) {
                if ("-root".equalsIgnoreCase(args[i])) {
                    bookRoot = new File(args[++i]);
                }
            }
        } catch (Exception e) {
            LOG.error("Can't process args.", e);
            System.exit(-1);
        }
        
        Server server = new Server();
        
        // server.setDumpAfterStart(true);

        // jetty server logging
        NCSARequestLog requestLog = new NCSARequestLog("log/jetty-yyyy_mm_dd.request.log");
        requestLog.setAppend(true);
        requestLog.setExtended(true);
        requestLog.setLogTimeZone("GMT");
        requestLog.setLogLatency(true);
        requestLog.setRetainDays(90);
        server.setRequestLog(requestLog);

        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setOutputBufferSize(32768);
        
        ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(http_config));
        http.setPort(8080);
        http.setIdleTimeout(30000);
        
        // Set the connectors
        server.setConnectors(new Connector[] { http });

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.setResourceBase(System.getProperty("java.io.tmpdir"));
        MimeTypes mimeTypes = context.getMimeTypes();
        mimeTypes.addMimeMapping("epub", "application/epub+zip");
        server.setHandler(context);
        
        // books servlet
        ServletHolder booksServletHolder = new ServletHolder(new BookServlet());
        booksServletHolder.setInitParameter(BookServlet.ROOTPATH_PARAMETER, bookRoot.getAbsolutePath()); 
        context.addServlet(booksServletHolder, "/books/*");
        
        // images servlet
        ServletHolder imageServlet = new ServletHolder(new BookCoverPreviewServlet());
        imageServlet.setInitParameter(BookCoverPreviewServlet.ROOTPATH_PARAMETER, imageRoot.getAbsolutePath());
        imageServlet.setInitParameter(BookCoverPreviewServlet.BOOKSROOT_PARAMETER, bookRoot.getAbsolutePath());
        context.addServlet(imageServlet, "/image/*");

        // Add default servlet
        ServletHolder defaultServletHolder = new ServletHolder(new DefaultServlet());
        defaultServletHolder.setInitParameter("resourceBase", resourceRoot.getAbsolutePath());
        context.addServlet(defaultServletHolder, "/");

        try {
            // Start things up!
            server.start();
            server.join();
            http.close();
        } catch (Exception e) {
            LOG.error("Unable to start server.", e);
        }
    }
}
