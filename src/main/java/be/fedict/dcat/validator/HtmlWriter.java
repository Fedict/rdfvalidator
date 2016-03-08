/*
 * Copyright (c) 2016, Bart Hanssens <bart.hanssens@fedict.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.fedict.dcat.validator;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick and dirty HTML writer
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class HtmlWriter {
    private final static Logger LOG = LoggerFactory.getLogger(HtmlWriter.class);
    
    private final String sep;
    private final OutputStream out;
    
    private void writeln(String s) throws IOException {
        out.write(s.getBytes());
        out.write(s.getBytes());
    }
    
    public void startSection() throws IOException {
        writeln("<section>");
    }
    
    public void endSection() throws IOException {
        writeln("</section>");
        out.flush();
    }
    
    public void startTable(String caption) throws IOException {
        writeln("<table>");
        writeln("<caption>" + caption + "</caption>");
    }
    
    public void tableHeader(List<String> headers) {
    
    }
    
    public void endTable() throws IOException {
        writeln("</table>");
        out.flush();
    }
    
    public void title(String title) throws IOException {
        writeln("<h1>" + title + "</h1>");
        out.flush();
    }
    
    public void startDocument() throws IOException {
        writeln("<html>");
        writeln("<head><title>DCAT Validator Report</head>");
        writeln("<body>");
        out.flush();
    }
    
    public void endDocument() throws IOException {
        writeln("</body>");
        writeln("</html>");
        out.close();
    }
    /**
     * Constructor
     * 
     * @param out output stream
     */
    public HtmlWriter(OutputStream out) {
        this.out = out;
        this.sep = System.lineSeparator();
    }
}
