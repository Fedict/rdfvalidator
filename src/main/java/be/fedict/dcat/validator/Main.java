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
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
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
    
    private static final Options OPTS = new Options();
    
    static {
        OPTS.addOption(Option.builder("i").longOpt("in")
                            .desc("Input file or URL")
                            .hasArg().build());
        OPTS.addOption(Option.builder("o").longOpt("out")
                            .desc("Report output file")
                            .hasArg().build());
        OPTS.addOption(Option.builder("r").longOpt("rulesets")
                            .desc("Use one or more rulesets")
                            .hasArg().hasArgs()
                            .build());
        OPTS.addOption(Option.builder("s").longOpt("statistics")
                            .desc("Run statistics")
                            .build());
        OPTS.addOption(Option.builder("h").longOpt("help")
                            .desc("Print this help text")
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
        HelpFormatter help = new HelpFormatter();
        help.printHelp("Validator", "DCAT-AP 1.1 validator", OPTS, null);
    }
    
    /**
     * Main
     * 
     * @param args 
     */
    public static void main(String[] args) {
        CommandLine cmd = parseArgs(args);
        
        if (cmd == null || cmd.hasOption('h')) {
            printHelp();
            System.exit(-1);
        }
        
        String infile = cmd.getOptionValue('i');
        if (infile == null || infile.isEmpty()) {
            LOG.error("Missing input file or URL");
            printHelp();
            System.exit(-2);
        }
        
        String outfile = cmd.getOptionValue('o');
        if (outfile == null || outfile.isEmpty()) {
            LOG.warn("Missing output file");
            printHelp();
            System.exit(-2);
        }
        
        String[] rules = cmd.getOptionValues('r');
        if (rules == null || rules.length == 0) {
            LOG.warn("No ruleset specified");
            rules = new String[0];
        }
        
        boolean stats = cmd.hasOption('s');

        LOG.info("Reading data from {}, writing to {}", infile, outfile);
        
        try(InputStream is = new BufferedInputStream(new FileInputStream(infile));
                OutputStream os = new BufferedOutputStream(new FileOutputStream(outfile))) {
            
            HtmlWriter w = new HtmlWriter(os);
            
            w.start();
            w.text("File to validate: " + infile);
            w.text("Current time: " + new Date());
            
            Validator validator = new Validator(is, w);
            validator.validate(rules, stats);
            
            w.end();
        } catch (IOException ex) {
            LOG.error("Validation failed {}", ex.getMessage());
            System.exit(-3);
        }
        
        System.exit(0);
    }
}
