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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author thomas
 *
 */
public class BookServlet extends HttpServlet
{
    private static final long serialVersionUID = 1439615974239704271L;

    private static final Logger LOG = LogManager.getLogger(BookServlet.class);

    public static final String ROOTPATH_PARAMETER = "rootpath";

    private static int BUF_SIZE = 65536;

    private File booksRoot;

    public BookServlet()
    {    
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        if (LOG.isDebugEnabled())
            debugRequest(req.getMethod(), req);
        if (booksRoot == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
            return;
        }
        String path = ServletUtilities.getRelativePath(req);
        File f = new File(booksRoot, path);

        if (!f.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
            LOG.debug("File " + f.getAbsolutePath() + " doesn't exist.");
            return;
        }

        try {

            String eTagMatch = req.getHeader("If-None-Match");
            String eTag = ServletUtilities.getETag(f);
            if (eTagMatch != null) {
                if (eTagMatch.equals(eTag)) {
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }

            if (!f.isDirectory()) {
                if (path.endsWith("/") || (path.endsWith("\\"))) {
                    // path points to a file but ends with / or \ but is a regular file
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                    return;
                }

                // setting headers
                String mimeType = getServletContext().getMimeType(path);
                LOG.debug("Mime type: "+ mimeType);
                ServletUtilities.setFileHeader(resp, f, eTag, mimeType);

                doBody(resp, f);
            } else {
                folderBody(path, resp, req);
            }
        } catch (AccessDeniedException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init()
     */
    @Override
    public void init() throws ServletException
    {
        super.init();

        String rootPath = getInitParameter(ROOTPATH_PARAMETER);
        if (rootPath == null) {
            throw new ServletException("missing parameter: " + ROOTPATH_PARAMETER);
        }
        if (rootPath.equals("*WAR-FILE-ROOT*")) {
            String file = BookServlet.class.getProtectionDomain()
                    .getCodeSource().getLocation().getFile().replace('\\', '/');
            if (file.charAt(0) == '/'
                    && System.getProperty("os.name").indexOf("Windows") != -1) {
                file = file.substring(1, file.length());
            }

            int ix = file.indexOf("/WEB-INF/");
            if (ix != -1) {
                rootPath = file.substring(0, ix).replace('/',
                        File.separatorChar);
            } else {
                throw new ServletException(
                        "Could not determine root of war file. Can't extract from path '"
                                + file + "' for this web container");
            }
        }
        booksRoot = new File(rootPath);
    }

    private void doBody(HttpServletResponse resp, File f)
    {
        try {
            OutputStream out = resp.getOutputStream();
            InputStream in = new FileInputStream(f);
            try {
                int read = -1;
                byte[] copyBuffer = new byte[BUF_SIZE];

                long total = 0;
                while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
                    out.write(copyBuffer, 0, read);
                    total += read;
                }
                LOG.info("Download of file " + f.getAbsolutePath() + ", wrote " + total + "bytes");
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                    LOG.warn("Closing InputStream causes Exception!\n", e);
                }
                try {
                    resp.flushBuffer();
                } catch (Exception e) {
                    LOG.warn("Flushing OutputStream causes Exception!\n", e);
                }
            }
        } catch (Exception e) {
            LOG.debug("Unexpected exception", e);
        }
    }

    private void folderBody(String path, HttpServletResponse resp, HttpServletRequest req) throws IOException
    {
        File f = new File(booksRoot, path);
        if (!f.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
        } else {
            
            int page = 0;
            String pageParam = req.getParameter("page");
            if (pageParam != null && !pageParam.isEmpty()) {
                try {
                    page = Integer.parseInt(pageParam) - 1;
                } catch (Exception e) {
                    LOG.warn("Failed parsing page parameter", e);
                }
            }

            if (f.isDirectory()) {
                String href = req.getContextPath();
                String servletPath = req.getServletPath();
                if (servletPath != null) {
                    if ((href.endsWith("/")) && (servletPath.startsWith("/")))
                        href += servletPath.substring(1);
                    else
                        href += servletPath;
                }
                if ((href.endsWith("/")) && (path.startsWith("/")))
                    href += path.substring(1);
                else
                    href += path;
                if (!href.endsWith("/"))
                    href += "/";

                resp.setContentType("text/html");
                resp.setCharacterEncoding("UTF8");
                OutputStream out = resp.getOutputStream();
                StringBuilder childrenTemp = new StringBuilder();
                childrenTemp.append("<html><head><title>Content of folder");
                childrenTemp.append(path);
                childrenTemp.append("</title>");
                childrenTemp.append(getCSS());
                childrenTemp.append("</head>");
                childrenTemp.append("<body>");
                childrenTemp.append(getHeader(path, resp, req));
                childrenTemp.append("<p>");
                if (!path.equals("/")) {
                    childrenTemp.append("<a class=\"navleft\" href=\"../\"><img src=\"/go-up.png\" alt=\"Parent\" width=\"32\" height=\"32\"/></a>\n");
                }
                ArrayList<File> childrenList = getSortedObjectList(path, f.listFiles());
                int start = page * 12;
                childrenTemp.append("<div class=\"navright\">");
                if (childrenList.size() > 12) {
                    if (page == 0) {
                        childrenTemp.append("<img style=\"margin-right:50px\" src=\"/disabled.png\" alt=\"Left\" width=\"32\" height=\"32\"/>\n");
                    } else {
                        childrenTemp.append("<a style=\"margin-right:50px\" href=\"");
                        childrenTemp.append(href);
                        childrenTemp.append("?page=");
                        childrenTemp.append(page);
                        childrenTemp.append("\"><img src=\"/go-previous.png\" alt=\"Left\" width=\"32\" height=\"32\"/></a>\n");
                    }
                    if (start + 12 < childrenList.size()) {
                        childrenTemp.append("<a href=\"");
                        childrenTemp.append(href);
                        childrenTemp.append("?page=");
                        childrenTemp.append(page + 2);
                        childrenTemp.append("\"><img src=\"/go-next.png\" alt=\"Right\" width=\"32\" height=\"32\"/></a>\n");
                    } else {
                        childrenTemp.append("<img src=\"/disabled.png\" alt=\"Right\" width=\"32\" height=\"32\"/>\n");
                    }
                } else {
                    childrenTemp.append("<img style=\"margin-right:50px\" src=\"/disabled.png\" alt=\"Left\" width=\"32\" height=\"32\"/>\n");
                    childrenTemp.append("<img src=\"/disabled.png\" alt=\"Right\" width=\"32\" height=\"32\"/>\n");
                }
                childrenTemp.append("</div></p>\n");

                childrenTemp.append("<div style=\"clear:both\">\n");
                int i = 0;
                for (int index = start; index < childrenList.size() && i < 12; index++) {
                    if (i % 4 == 0) {
                        childrenTemp.append("<ul>\n");
                    }
                    childrenTemp.append("<li>");
                    appendRow(childrenTemp, childrenList.get(index), href);
                    childrenTemp.append("</li>\n");
                    if (i % 4 == 3) {
                        childrenTemp.append("</ul>\n");
                    }
                    i += 1;
                }
                if (i % 4 != 0) {
                    childrenTemp.append("</ul>\n");
                }
                childrenTemp.append("</div>\n");
                childrenTemp.append(getFooter(path, resp, req));
                childrenTemp.append("</body></html>");
                if (LOG.isDebugEnabled())
                    LOG.debug("HTML: " + childrenTemp.toString());
                out.write(childrenTemp.toString().getBytes("UTF-8"));
            }
        }
    }

    /**
     * @param path
     * @param children
     * @return
     */
    private ArrayList<File> getSortedObjectList(String path, File[] children)
    {
        ArrayList<File> objList = new ArrayList<File>();
        if (children == null) {
            return objList;
        }
        for (File child : children) {
            objList.add(child);
        }
        // sort by folder/modified time
        Collections.sort(objList, new Comparator<File>() {

            @Override
            public int compare(File f1, File f2) {
                if (f1.isDirectory()) {
                    if (f2.isDirectory()) {
                        // latest modified first
                        return (int) ((f2.lastModified() - f1.lastModified()) / 1000);
                    } 
                    return -1;
                } else {
                    if (f2.isDirectory()) {
                        return 1;
                    }
                    // latest modified first
                    return (int) ((f2.lastModified() - f1.lastModified()) / 1000);
                }
            }
        });
        return objList;
    }

    /**
     * @param childrenTemp
     * @param child
     * @param obj
     * @param href
     * @param isEven
     */
    private void appendRow(StringBuilder childrenTemp, File obj, String href)
    {
        String child = obj.getName();

        childrenTemp.append("<a href=\"");
        childrenTemp.append(href);
        childrenTemp.append(child);
        if (obj.isDirectory())
        {
            childrenTemp.append("/\">");
            childrenTemp.append("<img src=\"/bookshelf.png\" alt=\"Folder\" width=\"100\" height=\"160\"/><p>");
            childrenTemp.append(child);
        }
        else
        {
            String imgSrc = "";
            if (href.startsWith("/books/")) {
                imgSrc = href.substring(6);
            }
            childrenTemp.append("\">");
            String mimeType = getServletContext().getMimeType(child);
            if ("application/epub+zip".equals(mimeType)) {
                childrenTemp.append("<img src=\"/image/");
                childrenTemp.append(imgSrc);
                int ext = child.lastIndexOf(".");
                if (ext != -1) {
                    childrenTemp.append(child.substring(0, ext));
                }
                childrenTemp.append(".png\" alt=\"No preview\" width=\"100\" height=\"160\"/><p>");
            } else {
                childrenTemp.append("<img src=\"/image/NoPreview.png\" alt=\"No preview\" width=\"100\" height=\"160\"/><p>");
            }
            childrenTemp.append(createBookName(child));
        }
        childrenTemp.append("</p></a>");
    }
    
    private String createBookName(String fileName)
    {
        StringBuffer sb = new StringBuffer(fileName);
        int ext = sb.lastIndexOf(".");
        if (ext != -1) {
            sb.setLength(ext);
        }
        for (int i = 0; i < sb.length(); i++) {
            char ch = sb.charAt(i);
            if (ch == '_') {
                sb.setCharAt(i, ' ');
            }
        }
        return sb.toString();
    }

    /**
     * Return the CSS styles used to display the HTML representation
     * of the webdav content.
     * 
     * @return String returning the CSS style sheet used to display result in html format
     */
    private String getCSS()
    {
        // link to styles to use
        String retVal = "<link rel=\"stylesheet\" type=\"text/css\" href=\"/books.css\">\n";

        return retVal;
    }

    /**
     * Return the header to be displayed in front of the folder content
     * 
     * @param path
     * @param resp
     * @param req
     * @return String representing the header to be display in front of the folder content
     */
    private String getHeader(String path, HttpServletResponse resp, HttpServletRequest req)
    {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return "<h2>B&uuml;cherssammlung</h2>";
        }
        return "<h2>B&uuml;cherschrank "+path+"</h2>";
    }

    /**
     * Return the footer to be displayed after the folder content
     * 
     * @param path
     * @param resp
     * @param req
     * @return String representing the footer to be displayed after the folder content
     */
    private String getFooter(String path, HttpServletResponse resp, HttpServletRequest req)
    {
        return "";
    }
    
    private void debugRequest(String methodName, HttpServletRequest req) {
        LOG.debug("-----------");
        LOG.debug("BookServlet\n request: methodName = " + methodName);
        LOG.debug("time: " + System.currentTimeMillis());
        LOG.debug("path: " + req.getRequestURI());
        LOG.debug("-----------");
        Enumeration<?> e = req.getHeaderNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.debug("header: " + s + " " + req.getHeader(s));
        }
        e = req.getAttributeNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.debug("attribute: " + s + " " + req.getAttribute(s));
        }
        e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            LOG.debug("parameter: " + s + " " + req.getParameter(s));
        }
    }
}
