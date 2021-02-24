/**
 * Copyright (c) 2020 Thomas Gerner
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * @author thomas
 *
 */
@Controller
@RequestMapping(value="/image")
public class PreviewController
{
  private static final Logger LOG = LoggerFactory.getLogger(PreviewController.class);

  private static final int MAX_COVER_IMG_HEIGHT = 160;

  @Value("${bookserver.preview.cache}")
  private String imgRoot;

  @Value("${bookserver.ebooks.root}")
  private String booksRoot;

  @GetMapping(path="/**")
  @ResponseBody
  public ResponseEntity<InputStreamResource> downloadPreviewImage(HttpServletRequest req) {

    String imgName = req.getServletPath().substring(7);
    File root = new File(imgRoot);
    File f = new File(root, imgName);

    try {
      if (f.isFile()) {
        return getResponseEntity(f);
      } else {
        // test if there is a book of that name
        int ext = imgName.lastIndexOf('.');
        if (ext == -1 || !imgName.endsWith(".png")) {
          return ResponseEntity.notFound().build();
        }
        String bookName = imgName.substring(0, ext);
        File bRoot = new File(booksRoot);
        File bf = new File(bRoot, bookName + ".epub");
        if (!bf.isFile()) {
          return ResponseEntity.notFound().build();
        }
        createPreview(bf, f);
        if (f.isFile()) {
          return getResponseEntity(f);
        } else {
          // return NoPreview image
          InputStream noPreview = getClass().getResourceAsStream("/static/NoPreview.png");
          if (noPreview != null)
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(new InputStreamResource(noPreview));
          else
            return ResponseEntity.notFound().build();
        }
      }

    } catch (Exception e) {
      return ResponseEntity.notFound().build();
    }
  }

  private ResponseEntity<InputStreamResource> getResponseEntity(File f) throws FileNotFoundException {
    return ResponseEntity.ok()
        .contentLength(f.length())
        .contentType(MediaType.IMAGE_PNG)
        .body(new InputStreamResource(new FileInputStream(f)));
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
      LOG.warn("Failed to open book file {}, {}", bookFile.getAbsolutePath(), e);
    }
  }
}
