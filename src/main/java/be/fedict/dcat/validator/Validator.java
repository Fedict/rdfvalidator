/* Copyright (c) 2016, Bart Hanssens <bart.hanssens@fedict.be>
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.Sail;
import org.openrdf.sail.inferencer.fc.DedupingInferencer;
import org.openrdf.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.spin.SpinSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A validator .
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Validator {
    private final static Logger LOG = LoggerFactory.getLogger(Validator.class);
    
    private final String infile;
    private final String outfile;
    private Repository repo;
        
    
    /**
     * Enumerate resources
     * 
     * @param res
     * @return 
     */
    private File[] enumerateRes(String res) throws IOException {
        File[] files = new File[0];
        
        Enumeration<URL> urls = ClassLoader.getSystemResources(res);
        if (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            File root;
            try {
                root = new File(url.toURI());
                files = root.listFiles();
            } catch (URISyntaxException ex) {
                LOG.error("Could not load resources from {}", url);
                LOG.debug(ex.getMessage());
            }
        }
        return files;
    }
    
    /**
     * Get a Sesame SAIL
     * 
     * @return
     * @throws IOException 
     */
    private Sail getSail() throws IOException {
        File tmp = File.createTempFile("sail", null);
        
        LOG.debug("Creating sail usingg temfile {}", tmp);
        
        MemoryStore mem = new MemoryStore(tmp);
        mem.setPersist(false);
        
        SpinSail sail = new SpinSail();
        sail.setBaseSail(new ForwardChainingRDFSInferencer(
                            new DedupingInferencer(mem)));
        return sail;
    }
    
    /**
     * Validates
     *
     * @param rules rulesets to validate
     * @param stats  create stats
     * @throws IOException 
     */
    public void validate(String[] rules, boolean stats) throws IOException {
        File inf = new File(this.infile);
        
        repo = new SailRepository(getSail());
       
        try (InputStream is = new BufferedInputStream(new FileInputStream(inf))) {
            LOG.info("Reading data from {}", inf);
            repo.getConnection().add(is, "http://data.gov.be", null);
        }
        
        if (stats) {
            enumerateRes("stats");
        }
        
        repo.shutDown();
    }
    
    /**
     * Constructor
     * 
     * @param in
     * @param out
     */
    public Validator(String in, String out) {
        this.infile = in;
        this.outfile = out;
    }
}
