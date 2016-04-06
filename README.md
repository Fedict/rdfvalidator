# RDF Validator

Offline / command line rdf validator.

The stand-alone jar already contains a set of [DCAT-AP 1.1](https://joinup.ec.europa.eu/asset/dcat_application_profile/description) validator.
SPARQL rules checking:
 
 * mandatory classes and properties
 * recommended properties
 * the use of the EU Publication Office's [MDR Authority](http://publications.europa.eu/mdr/authority/) lists (controlled vocabularies)
 * best practices (e.g. language tags on literals)

The output will be an HTML report with a list of subjects violating the rule.


### Requirements

Running the validator only requires a Java 8 runtime, there are no other components to install
(no database, no RDF triple store, no application server...)

Binaries can be found in [dist/bin](dist/bin), compiling from source requires a Java 8 JDK and Maven.

### Notes

* Based on rdf4j (formerly known as Sesame) and other Java open source libraries.
* Logging uses SLF4J.

### Running

Invoke with

    # java -jar validator.jar -i dcat_ap_file.ttl -o report.html

Use -D to set logging level and save the log to a file

    # java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug 
           -Dorg.slf4j.simpleLogger.logFile=validator.log
           -jar validator.jar