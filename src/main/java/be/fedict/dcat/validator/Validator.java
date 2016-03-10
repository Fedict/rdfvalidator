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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
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
    
    public final static String RULES_STATS = "stats";
    public final static String BASE_URI = "http://data.gov.be";
    
    private final InputStream is;
    private final SimpleResultWriter sw;
    
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
            try {
                File root = new File(url.toURI());
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
        
        LOG.debug("Creating sail using tempfile {}", tmp);
        
       // MemoryStore mem = new MemoryStore(tmp);
        //mem.setPersist(false);
        
        SpinSail sail = new SpinSail();
        sail.setBaseSail(new ForwardChainingRDFSInferencer(
                              new DedupingInferencer(new MemoryStore())));
        return sail;
    }
    
    
    /**
     * Run a query
     * 
     * @param q query
     */
    private void runQuery(String q) throws IOException {
        TupleQuery query = repo.getConnection()
                                   .prepareTupleQuery(QueryLanguage.SPARQL, q);
            
        try (TupleQueryResult res = query.evaluate()) {
            sw.startTable(q);
            
            List<String> cols = res.getBindingNames();
            sw.columnNames(cols);
            
            int i = 0;
            while(res.hasNext()) {
                List<String> row = new ArrayList<>();
                
                BindingSet bset = res.next();
                bset.iterator().forEachRemaining(cell -> 
                                        row.add(cell.getValue().stringValue()));
                sw.row(row);
                i++;
            }
            LOG.debug("Query had {} results", i);
            sw.endTable();
        }

    }
            
    /**
     * Run the ruleset
     * 
     * @param rules name of the ruleset
     * @throws IOException 
     */
    private void runRuleset(String rules) throws IOException {
        File[] files = enumerateRes(rules);
        
        for (File f: files) {
            String q = new String(Files.readAllBytes(Paths.get(f.toURI())));
            LOG.info("Loaded query {}", q);
            
            runQuery(q);
        }
    }
    
    /**
     * Validates RDF triples from input stream against rulesets
     *
     * @param rulesets ruleset(s) to validate
     * @param stats also create statistics
     * @return true if valid
     * @throws IOException 
     */
    public boolean validate(String[] rulesets, boolean stats) throws IOException {
        boolean valid = false;
        
        LOG.debug("Initialize repository");
        repo = new SailRepository(getSail());
        repo.initialize();
        
        LOG.debug("Adding triples");
        repo.getConnection().add(is, BASE_URI, RDFFormat.NTRIPLES);
        
        if(repo.getConnection().isEmpty()) {
            LOG.error("No statements loaded");
            repo.shutDown();
            return false;
        }
        
        for(String ruleset: rulesets) {
            runRuleset(ruleset);
        }
        
        if (stats) {
            runRuleset(RULES_STATS);
        }
        
        LOG.debug("Shutdown repository");
        repo.shutDown();
        
        return valid;
    }
    
    /**
     * Constructor
     * 
     * @param is inputstream
     * @param sw simple result writer
     */
    public Validator(InputStream is, SimpleResultWriter sw) {
        this.is = is;
        this.sw = sw;
    }
}
