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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.servlet.http.HttpServletRequest;

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

/**
 * @author thomas
 *
 */
@Controller
@RequestMapping(value="/books/download")
public class DownloadController
{
  private static final Logger LOG = LoggerFactory.getLogger(DownloadController.class);

  @Value("${bookserver.ebooks.root}")
  private String booksRoot;

  @GetMapping(path="/**")
  @ResponseBody
  public ResponseEntity<InputStreamResource> downloadBook(HttpServletRequest req) {
    
    String bookName = req.getServletPath().substring(15);
    File root = new File(booksRoot);
    File f = new File(root, bookName);
    
    if (f.isFile()) {
      try {
        String fileName = f.getName();
        return ResponseEntity.ok()
            .contentLength(f.length())
            .contentType(MediaType.valueOf("application/epub+zip"))
            .lastModified(f.lastModified())
            .header("Content-disposition", "attachment; filename=\""+ fileName + "\"")
            .body(new InputStreamResource(new FileInputStream(f)));
      } catch (FileNotFoundException e) {
        LOG.warn("No such book {}", f, e);
      }
    }
    return ResponseEntity.notFound().build();
  }
}
