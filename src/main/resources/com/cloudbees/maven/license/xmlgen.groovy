import groovy.xml.MarkupBuilder;

mojo.generateLicenseXml.parentFile?.mkdirs()
mojo.generateLicenseXml.withWriter { w ->
    def xml = new MarkupBuilder(w)
    xml.omitNullAttributes = true;

    xml.'l:dependencies'('xmlns:l':'licenses') {
        dependencies.each { d ->
            'l:dependency'(name:d.name, groupId:d.groupId, artifactId:d.artifactId, version:d.version, url:d.url) {
                'l:description'(d.description)
                d.licenses.each { l ->
                    'l:license'(name:l.name, url:l.url)
                }
            }
        }
    }
    log.info("Generated licenses.xml")
}