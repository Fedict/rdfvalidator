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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
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
    
    public final static String RULES_STATS = "stats";
    public final static String BASE_URI = "http://data.gov.be";
    
    private final InputStream is;
    private final RDFFormat fmt;
    private final OutputStream os;
    
    private RDFWriter w;
    
    private Repository repo;
    
    
    /**
     * Get a Sesame SAIL
     * 
     * @return
     * @throws IOException 
     */
    private SpinSail getSail() throws IOException {
        LOG.debug("Creating sail");
        
        SpinSail sail = new SpinSail();
        sail.setBaseSail(new ForwardChainingRDFSInferencer(
                            new DedupingInferencer(new MemoryStore())));
        return sail;
    }
    
    
    /**
     * Load the SPIN ruleset(s).
     * 
     * @param rules name(s) of the ruleset(s)
     * @throws IOException 
     */
    private void loadRulesets(String rulesets[]) throws IOException {
        RepositoryConnection con = repo.getConnection();
        
        for (String r: rulesets) {
            LOG.info("Running ruleset {}", r);
            File f = new File(r);
            con.add(f, BASE_URI, RDFFormat.TURTLE);
        }
        
        // Run built-in ruleset if none specified
        if (rulesets.length == 0) {
            LOG.info("No rulesets specified, run built-in set");
            InputStream r = ClassLoader.getSystemResourceAsStream("dcatap-rules.ttl");
            con.add(r, BASE_URI, RDFFormat.TURTLE);
        }
    }
    
    /**
     * Validates RDF triples from input stream against rulesets
     *
     * @param rulesets ruleset(s) to validate
     * @return true if valid
     * @throws IOException 
     */
    public boolean validate(String[] rulesets) throws IOException {
        boolean valid = false;
        
        LOG.debug("Initialize repository");
        repo = new SailRepository(getSail());
        repo.initialize();
 
        loadRulesets(rulesets);
               
        LOG.info("Adding triples");
        
        w = Rio.createWriter(RDFFormat.TURTLE, this.os);
    
        try {
            repo.getConnection().add(is, BASE_URI, this.fmt);
        } catch (RepositoryException cve) {
            LOG.error("Oeps");
        }
        
        if(repo.getConnection().isEmpty()) {
            LOG.error("No statements loaded");
            repo.shutDown();
            return false;
        }
    
        repo.getConnection().export(w);
        
        repo.getConnection().close();
        
        LOG.debug("Shutdown repository");
        repo.shutDown();
        
        return valid;
    }
    
    /**
     * Constructor
     * 
     * @param is input stream
     * @param os output stream
     */
    public Validator(InputStream is, RDFFormat fmt, OutputStream os) {
        this.is = is;
        this.fmt = fmt;
        this.os = os;
    }
}
