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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author thomas
 *
 */
@Controller
@RequestMapping(value="/books/shelf")
public class BooksController
{
  private static final Logger LOG = LoggerFactory.getLogger(BooksController.class);
  
  private static final int NUM_ROWS = 2;
  private static final int NUM_COLS = 4;
  
  @Value("${bookserver.ebooks.root}")
  private String booksRoot;

  @GetMapping(path="/**")
  public String controlPage(HttpServletRequest req, @RequestParam(required=false) String page, Model model) {
    
    String bookPath = req.getServletPath();
    if (bookPath.length() > 13) {
      bookPath = bookPath.substring(13);
    } else {
      bookPath = "";
    }
    fillModel(bookPath, page, model);
    return "books";
  }
  
  private void fillModel(String bookPath, String page, Model model) {
    File root = new File(booksRoot);
    File f = new File(root, bookPath);
    
    boolean isSubShelf = !(bookPath.isEmpty() || bookPath.equals("/"));
    model.addAttribute("isSubShelf", isSubShelf);
    if (isSubShelf) {
      File sub = new File(bookPath);
      String parent = sub.getParent();
      model.addAttribute("parentShelf", "/books/shelf/" + (parent == null ? "" : parent));
    }
    
    int rqPage = 0;
    if (page != null && !page.isEmpty()) {
      try {
        rqPage = Integer.parseInt(page) - 1;
      } catch (Exception e) {
        LOG.warn("Failed parsing page parameter", e);
      }
    }
    
    if (f.isDirectory()) {
      ArrayList<File> childrenList = getSortedObjectList(f.listFiles());
      int start = rqPage * NUM_COLS * NUM_ROWS;
      
      boolean hasPrevPage = rqPage > 0;
      model.addAttribute("hasPrevPage", hasPrevPage);
      if (hasPrevPage) {
        model.addAttribute("prevPage", bookPath + "?page=" + Integer.toString(rqPage));
      }
      boolean hasNextsPage = childrenList.size() > start + NUM_COLS * NUM_ROWS;
      model.addAttribute("hasNextPage", hasNextsPage);
      if (hasNextsPage) {
        model.addAttribute("nextPage", bookPath + "?page=" + Integer.toString(rqPage + 2));
      }

      List<BookCol> bookRows = new ArrayList<>();
      int index = start;
      for (int row = 0; row < NUM_ROWS && index < childrenList.size(); row++) {
        BookCol bookCol = new BookCol();
        for (int col = 0; col < NUM_COLS && index < childrenList.size(); col++) {
          Book b = new Book(bookPath, childrenList.get(index++));
          bookCol.getCols().add(b);
        }
        bookRows.add(bookCol);
      }
      model.addAttribute("rows", bookRows);
    }

  }
  
  /**
   * @param children
   * @return
   */
  private ArrayList<File> getSortedObjectList(File[] children)
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
  
  public static class Book {
    
    private String base;
    private File f;
    
    public Book(String base, File f) {
      if (base == null || base.isEmpty()) {
        this.base = "";
      } else {
        this.base = base + "/";
      }
      this.f = f;
    }
    
    public String getBookHref() {
      if (f.isDirectory()) {
        return "/books/shelf/" + base + f.getName();
      } else {
        return "/books/download/" + base + f.getName();
      }
    }
    
    public String getBookImage() {
      if (f.isDirectory()) {
        return "/books/bookshelf.png";
      }
      String name = f.getName();
      if (name.endsWith(".epub")) {
        return "/books/image/" + base + name.substring(0, name.length() - 4) + "png";
      }
      return "/books/NoPreview.png";
    }
    
    public String getAltText() {
      if (f.isDirectory()) {
        return "Folder";
      }
      return "Preview picture";
    }
    
    public String getBookText() {
      StringBuffer sb = new StringBuffer(f.getName());
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
  }
  
  public static class BookCol {
    
    private final List<Book> cols = new ArrayList<>();
    
    public List<Book> getCols() {
      return cols;
    }
  }
}
