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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author thomas
 *
 */
public class BookCoverPreviewServlet extends HttpServlet
{
    private static final long serialVersionUID = -5047973547646493346L;

    private static final Logger LOG = LogManager.getLogger(BookCoverPreviewServlet.class);

    public static final String ROOTPATH_PARAMETER = "rootpath";
    public static final String BOOKSROOT_PARAMETER = "bookspath";
    
    private static final int BUF_SIZE = 65536;
    private static final int MAX_COVER_IMG_HEIGHT = 160;

    private File imgRoot;
    private File booksRoot;
    private File noPreview;
    
    public BookCoverPreviewServlet()
    {
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String path = ServletUtilities.getRelativePath(req);
        File f = new File(imgRoot, path);
        
        if (f.isFile()) {
            doFile(f, req, resp);
        } else {
            // test if there is a book of that name
            int ext = path.lastIndexOf('.');
            if (ext == -1 || !path.endsWith(".png")) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                return;
            }
            String bookName = path.substring(0, ext);
            File bf = new File(booksRoot, bookName + ".epub");
            if (!bf.isFile()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                return;
            }
            createPreview(bf, f);
            if (f.isFile()) {
                doFile(f, req, resp);
            } else if (noPreview.isFile()) {
                doFile(noPreview, req, resp);
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
                return;
            }
        }
    }
    
    /* (non-Javadoc)
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        
        String rootPath = config.getInitParameter(ROOTPATH_PARAMETER);
        if (rootPath == null) {
            throw new ServletException("missing parameter: " + ROOTPATH_PARAMETER);
        }
        String booksPath = config.getInitParameter(BOOKSROOT_PARAMETER);
        if (booksPath == null) {
            throw new ServletException("missing parameter: " + BOOKSROOT_PARAMETER);
        }
        if (rootPath.equals("*WAR-FILE-ROOT*")) {
            rootPath = getFromWebInfRoot(rootPath);
        }
        if (booksPath.equals("*WAR-FILE-ROOT*")) {
            booksPath = getFromWebInfRoot(booksPath);
        }
        imgRoot = new File(rootPath);
        booksRoot = new File(booksPath);
        noPreview = new File(imgRoot, "NoPreview.png");
    }

    private void createPreview(File bookFile, File preview)
    {
        try {
            // open eBook and extract cover image
            InputStream is = new FileInputStream(bookFile);
            EpubReader bookReader = new EpubReader();
            Book book = bookReader.readEpub(is);
            is.close();
            Resource coverResource = book.getCoverImage();
            is = coverResource.getInputStream();
            BufferedImage coverImg = ImageIO.read(is);
            is.close();
            int cWidth = coverImg.getWidth();
            int cHeight = coverImg.getHeight();
            double scale = (double) MAX_COVER_IMG_HEIGHT / (double) cHeight;
            int imgWidth = Double.valueOf(scale * cWidth).intValue();
            BufferedImage previewImg = new BufferedImage(imgWidth, MAX_COVER_IMG_HEIGHT, coverImg.getType());
            Graphics2D g = previewImg.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(coverImg, 0, 0, imgWidth, MAX_COVER_IMG_HEIGHT, 0, 0, coverImg.getWidth(),
                    coverImg.getHeight(), null);
            g.dispose();
            File previewDir = preview.getParentFile();
            previewDir.mkdirs();
            ImageIO.write(previewImg, "png", preview);
        } catch (Exception e) {
            LOG.warn("Failed to open book file " + bookFile.getAbsolutePath(), e);
        }
    }
    
    /**
     * @param rootPath
     * @return
     * @throws ServletException
     */
    private String getFromWebInfRoot(String rootPath) throws ServletException
    {
        String file = BookCoverPreviewServlet.class.getProtectionDomain()
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
        return rootPath;
    }
    
    private void doFile(File f, HttpServletRequest req, HttpServletResponse resp)
    {
        String eTagMatch = req.getHeader("If-None-Match");
        String eTag = ServletUtilities.getETag(f);
        if (eTagMatch != null) {
            if (eTagMatch.equals(eTag)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        }
        
        // setting headers
        String mimeType = getServletContext().getMimeType(f.getName());
        LOG.debug("Mime type: "+ mimeType);
        ServletUtilities.setFileHeader(resp, f, eTag, mimeType);
        
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
                LOG.debug("copied " + total);
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
}
