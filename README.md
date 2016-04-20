# RDF Validator

Offline / command line rdf validator.

The output will be an [HTML report](dist/example/validator.html) with a list of 
subjects violating the rule.

### Built-in rulesets

The stand-alone jar already contains a set of 
[DCAT-AP 1.1] (https://joinup.ec.europa.eu/asset/dcat_application_profile/description)
SPARQL rules checking:
 
 * mandatory classes and properties
 * recommended properties
 * the use of the EU Publication Office's [MDR Authority](http://publications.europa.eu/mdr/authority/) lists (controlled vocabularies)
 * best practices (e.g. language tags on literals)


### Requirements

Running the validator only requires a Java 8 runtime, there are no other components 
to install (no database, no RDF triple store, no application server...)

Binaries can be found in [dist/bin](dist/bin), compiling from source requires a 
Java 8 JDK and Maven.

### Notes

* Based on rdf4j (formerly known as Sesame) and other Java open source libraries.
* Logging uses SLF4J.

### Rulesets

A ruleset is just a set of text files, each containing one SPARQL SELECT query
returning violations.

The first line of the SPARQL query can be a comment (starting with a '#'),
this will then be used as a title in the HTML output report.

Example:
```
# Catalog missing mandatory title

PREFIX dcat:    <http://www.w3.org/ns/dcat#>
PREFIX dcterms: <http://purl.org/dc/terms/>

SELECT ?catalog
WHERE {
    ?catalog a dcat:Catalog
    FILTER NOT EXISTS { 
        ?catalog dcterms:title ?value
    }
}
```

### Running

Invoke with

    # java -jar validator.jar -i dcat_ap_file.nt -o report.html

The input file can be RDF/XML (.xml), NTriples (.nt) or Turtle (.ttl)
The output file is always an HTML file.

Use -r to specify a directory containing SPARQL rules

    # java -jar validator.jar -i dcat_ap_file.nt -o report.html -r dir1 dir2


If no ruleset is specified, the built-in rulesets for DCAT-AP 1.1
(mandatory + best practices for data.gov.be) will be used. This is equivalent to 

    # java -jar validator.jar -i dcat_ap_file.nt -o report.html
            -r builtin://dcatap11 builtin://dcatap11be


Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=validator.log
           -jar validator.jar