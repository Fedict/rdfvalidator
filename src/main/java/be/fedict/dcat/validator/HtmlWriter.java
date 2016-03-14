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
public class HtmlWriter implements SimpleResultWriter {
    private final static Logger LOG = LoggerFactory.getLogger(HtmlWriter.class);
    
    private final String sep;
    private final OutputStream out;
    
    /**
     * Write a HTML line in a somewhat pretty-printed format
     * 
     * @param s
     * @throws IOException 
     */
    private void writeln(String s) throws IOException {
        out.write(s.getBytes());
        out.write(sep.getBytes());
    }
    
    public void startSection() throws IOException {
        writeln("<section>");
    }
    
    public void endSection() throws IOException {
        writeln("</section>");
        out.flush();
    }
    
    /**
     * Start table
     * 
     * @param title caption of the table
     * @throws IOException 
     */
    @Override
    public void startTable(String title) throws IOException {
        startSection();
        writeln("<table>");
        writeln("<caption>" + title + "</caption>");
    }
    
    /**
     * Write a row / row header.
     * 
     * @param tag
     * @param tag
     * @throws IOException 
     */
    private void writeRow(List<String> cells, String tag) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr>");
        for(String cell: cells) {
            sb.append("<").append(tag).append(">").append(cell)
                    .append("</").append(tag).append(">");
        }
        sb.append("</tr>");
        writeln(sb.toString());
    }
    
    /**
     * Write column names for result table
     * 
     * @param headers
     * @throws IOException 
     */
    @Override
    public void columnNames(List<String> headers) throws IOException {
        writeRow(headers, "th");
    }
    
    /**
     * Write one row of result table
     * 
     * @param values
     * @throws IOException 
     */
    @Override
    public void row(List<String> values) throws IOException {
        writeRow(values, "td");
    }
    
    /**
     * End of table
     * 
     * @throws IOException 
     */
    @Override
    public void endTable() throws IOException {
        writeln("</table>");
        endSection();
        out.flush();
    }
    
    /**
     * Print title
     * 
     * @param title
     * @throws IOException 
     */
    @Override
    public void title(String title) throws IOException {
        writeln("<h1>" + title + "</h1>");
        out.flush();
    }
    
    /**
     * Print simple text
     * 
     * @param text
     * @throws IOException 
     */
    @Override
    public void text(String text) throws IOException {
        writeln("<p>" + text + "</p>");
    }
    
    /**
     * Write start of HTML
     * 
     * @throws IOException 
     */
    @Override
    public void start() throws IOException {
        writeln("<html>");
        writeln("<head><title>DCAT Validator Report</title></head>");
        writeln("<body>");
        out.flush();
    }
    
    /**
     * Write end of HTML
     * 
     * @throws IOException 
     */
    @Override
    public void end() throws IOException {
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
