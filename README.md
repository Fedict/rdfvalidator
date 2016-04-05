# DCAT-AP Validator

Offline / command line [DCAT-AP 1.1](https://joinup.ec.europa.eu/asset/dcat_application_profile/description) validator.

The jar already contains a set of (SPARQL) rules, the output will be an HTML report.


### Requirements

Requires Java runtime 1.8.


### Running

Invoke with

    # java -jar validator.jar validator.jar -i dcat_ap_file.ttl -o report.html


