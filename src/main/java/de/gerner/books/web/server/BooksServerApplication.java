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
 *    must display the following acknowledgement:
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

import org.eclipse.jetty.servlet.DefaultServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;

/**
 * @author thomas
 *
 */
/**
 * @author thomas
 *
 */
@SpringBootApplication
public class BooksServerApplication {

    private File bookRoot = new File("/home/thomas/Download/eBooks");
    private File resourceRoot = new File("resources");
    private File imageRoot = new File(resourceRoot, "img");

	@Bean
	public ServletRegistrationBean<BookServlet> bookServletRegistrationBean() {
		ServletRegistrationBean<BookServlet> bean = new ServletRegistrationBean<BookServlet>(new BookServlet(), "/books/*");
		bean.addInitParameter(BookServlet.ROOTPATH_PARAMETER, bookRoot.getAbsolutePath());
		bean.setEnabled(true);
		
		return bean;
	}

	@Bean
	public ServletRegistrationBean<BookCoverPreviewServlet> imageServletRegistrationBean() {
		ServletRegistrationBean<BookCoverPreviewServlet> bean = new ServletRegistrationBean<BookCoverPreviewServlet>(new BookCoverPreviewServlet(), "/image/*");
		bean.addInitParameter(BookCoverPreviewServlet.ROOTPATH_PARAMETER, imageRoot.getAbsolutePath());
		bean.addInitParameter(BookCoverPreviewServlet.BOOKSROOT_PARAMETER, bookRoot.getAbsolutePath());
		bean.setEnabled(true);
		
		return bean;
	}
	
	@Bean
	public ServletRegistrationBean<DefaultServlet> defaultServletRegistrationBean() {
		ServletRegistrationBean<DefaultServlet> bean = new ServletRegistrationBean<DefaultServlet>(new DefaultServlet(), "/*");
		bean.addInitParameter("resourceBase", resourceRoot.getAbsolutePath());
		
		return bean;
	}
	
	@Bean
	public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> jettyCustomizer() {
		return new WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>() {

			@Override
			public void customize(ConfigurableServletWebServerFactory factory) {
				
				MimeMappings mm = new MimeMappings(MimeMappings.DEFAULT);
				mm.add("epub", "application/epub+zip");
				factory.setMimeMappings(mm);
			}
		};
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpringApplication.run(BooksServerApplication.class, args);
	}
}
