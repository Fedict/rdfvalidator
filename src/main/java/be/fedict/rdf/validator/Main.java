/* Copyright (c) 2016, FPS BOSA DG DT
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

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quick and dirty DCAT-AP 1.1 validator.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public class Main {
    private final static Logger LOG = LoggerFactory.getLogger(Main.class);
    
    private static final String[] BUILTINS = 
                                    { "datagovbe", "dcatap11", "dcatap11be" };
    
    private static final Options OPTS = new Options();
    
    static {
        OPTS.addOption(Option.builder("i").longOpt("input")
                            .desc("RDF input file or URL")
                            .hasArg().argName("FILE").required()
                            .build());
        OPTS.addOption(Option.builder("o").longOpt("output")
                            .desc("HTML report output file")
                            .hasArg().argName("FILE").required()
                            .build());
        OPTS.addOption(Option.builder("r").longOpt("rulesets")
                            .desc("Use rulesets with SPARQL rules (path or built-in)")
                            .hasArgs().argName("RULESET")
                            .build());
        OPTS.addOption(Option.builder("h").longOpt("help")
                            .desc("Print this help text")
                            .build());
        OPTS.addOption(Option.builder("v").longOpt("version")
                            .desc("Version")
                            .build());
    }
    
    /**
     * Parse command line arguments
     * 
     * @param args arguments
     * @return parsed command line or null
     */
    private static CommandLine parseArgs(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        
        try {
            cmd = parser.parse(OPTS, args);    
        } catch (ParseException ex) {
            LOG.error("Error parsing command line {}", ex.getMessage());
        }
        return cmd;
    }
    
    /**
     * Print help
     */
    private static void printHelp() {
        String name = Main.class.getPackage().toString();
        
        StringBuilder buf = new StringBuilder("\nBuilt-in rulesets: ");
        for (String s : BUILTINS ) {
            buf.append("builtin://").append(s).append(" ");
        }
        
        HelpFormatter help = new HelpFormatter();
        help.printHelp("java -jar Validator.jar", "\n" + name + "\n", 
                        OPTS, buf.toString(), true);
    }
    
    /**
     * Main
     * 
     * @param args 
     */
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        
        if (cmd == null || cmd.hasOption('h') || cmd.hasOption('v')) {
            printHelp();
            System.exit(-2);
        }
        
        String infile = cmd.getOptionValue('i');
        if (infile == null || infile.isEmpty()) {
            LOG.error("Missing input file or URL");
            printHelp();
            System.exit(-3);
        }
        
        String outfile = cmd.getOptionValue('o');
        if (outfile == null || outfile.isEmpty()) {
            LOG.error("Missing report output file");
            printHelp();
            System.exit(-4);
        }
        
        String[] rules = cmd.getOptionValues('r');
        if (rules == null || rules.length == 0) {
            LOG.warn("No rulesets specified");
            LOG.warn("Using built-in DCAT-AP rulesets");
            rules = new String[]{ "builtin://dcatap11", "builtin://dcatap11be" };
        }
        
        LOG.info("Reading data from {}, writing to {}", infile, outfile);

        int issues = 0;
        
        try{
            HtmlWriter w = new HtmlWriter(Paths.get(outfile));
            Validator validator = new Validator(Paths.get(infile), w);
            validator.init();
            issues  = validator.validate(rules);
            validator.close();
        } catch (IOException ex) {
            LOG.error("Validation failed {}", ex.getMessage());
            System.exit(-4);
        }
        
        System.exit(issues);
    }
}
