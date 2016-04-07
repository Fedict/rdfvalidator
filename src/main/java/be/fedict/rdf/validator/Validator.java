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
package be.fedict.rdf.validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A validator .
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Validator {
    private final static Logger LOG = LoggerFactory.getLogger(Validator.class);
    
    public final static String RULES_BUILTIN = "/dcatap11be";
    public final static String BASE_URI = "http://data.gov.be";
    
    private final Path path;
    private final SimpleResultWriter sw;

    private Repository repo;
    
    
    /**
     * Get the first line comment of the query
     * 
     * @param str
     * @return comment or empty string
     */
    private String getComment(String str) {
        if (str.startsWith("#")) {
            int eol = str.replaceAll("\r", "\n").indexOf("\n");
            if (eol > 1) {
                return str.substring(1, eol);
            }
        }
        return "";
    }            
    
    /**
     * Get path from directory or jar
     * 
     * @param dir
     * @return
     * @throws IOException 
     */
    private Path getPath(String dir) throws IOException {
        if (dir != null && !dir.isEmpty()) {
            LOG.info("Running validation queries from {}", dir);
            return Paths.get(dir);
        }
        
        LOG.info("Using built-in rules {}", Validator.RULES_BUILTIN);
        URI uri;
        try {
            uri = Validator.class.getResource(RULES_BUILTIN).toURI();
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }

        if (uri.getScheme().equals("jar")) {
            FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            return fs.getPath(RULES_BUILTIN);
        }
        
        return Paths.get(uri);
    }

    /**
     * Validate the RDF triples using the rules (SPARQL queries) in a directory,
     * or the built-in set if directory is NULL.
     * 
     * @param directory containing SPARQL queries
     * @throws IOException 
     */
    private List<String> readRules(String dir) throws IOException {
        ArrayList<String> rules = new ArrayList<>(); 
       
        Path pathdir = getPath(dir);
        
        if (Files.isDirectory(pathdir)) {
            DirectoryStream<Path> stream = Files.newDirectoryStream(pathdir);
            for(Path file: stream) {
                LOG.debug("Rule {}", file);
                String rule = new String(Files.readAllBytes(file));
                rules.add(rule);
            }
        } else {
            LOG.warn("Path {} is not a directory", pathdir);
        }
        return rules;
    }
    
    /**
     * Validate using a SPARQL query
     * 
     * @param con RDF triplestore connection
     * @param query query string
     * @param sw result writer
     * @return number of violations (if any)
     * @throws IOException
     */
    private int validateRule(RepositoryConnection con, String query, SimpleResultWriter sw) 
                                                            throws IOException {
        int violations = 0;
        
        TupleQuery q = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
    
        sw.startSection(getComment(query));
        sw.code(query);
        
        try (TupleQueryResult res = q.evaluate()) {
            if (res.hasNext()) {
                List<String> cols = res.getBindingNames();
                
                sw.startTable("");
                sw.columnNames(cols);
        
                while(res.hasNext()) {
                    violations++;
                    BindingSet next = res.next();
                    List<String> row = cols.stream()
                                    .map(col -> next.getValue(col).stringValue())
                                    .collect(Collectors.toList());
                    sw.row(row);
                }
                sw.endTable();
            }
        }
        sw.endSection();
        
        return violations;
    }
    
    /**
     * Validates RDF triples from input stream against rulesets
     *
     * @param dir directory containing SPARQL rules to validate
     * @return number of violations
     * @throws IOException 
     */
    public int validate(String dir) throws IOException {
        int violations = 0;
        
        LOG.debug("Initialize repository");
        repo = new SailRepository(new MemoryStore());
        repo.initialize();
        
        Optional<RDFFormat> fmt = Rio.getParserFormatForFileName(path.toString());
        if (!fmt.isPresent()) {
            throw new IOException("Could not determine file type");
        }
        
        LOG.debug("Adding triples");
        BufferedReader r = Files.newBufferedReader(path);
        
        RepositoryConnection con = repo.getConnection();
        try {
            con.add(r, BASE_URI, fmt.get());
        } catch (RepositoryException cve) {
            LOG.error("Error adding triples", cve);
        }
    
        if(con.isEmpty()) {
            LOG.error("No statements loaded");
            repo.shutDown();
            return -1;
        }
        
        LOG.info("{} triples loaded", con.size());
        
        sw.start();
        
        sw.title("RDF Validation");
        sw.text("Ruleset: " + dir);
        sw.text("File to validate: " + path);
        sw.text("Number of triples: " + con.size());
        sw.text("Current time: " + new Date());
        
        List<String> rules = readRules(dir);
        for(String rule: rules) {
            LOG.debug("Validating {}", rule.replaceAll("\n", " "));
            violations += validateRule(con, rule, sw);
        }
        
        sw.text("Number of violations: " + violations);

        sw.end();
        
        LOG.debug("Shutdown repository");
        repo.shutDown();
    
        return violations;
    }
    
    /**
     * Constructor
     * 
     * @param input input file or URL
     * @param sw simple result writer
     */
    public Validator(Path input, SimpleResultWriter sw) {
        this.path = input;
        this.sw = sw;
    }
}
