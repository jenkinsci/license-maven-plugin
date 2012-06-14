import groovy.xml.StreamingMarkupBuilder;

generate {
    mojo.generateLicenseXml.parentFile?.mkdirs()
    mojo.generateLicenseXml.withWriter('UTF-8') { w ->
        def xml = new StreamingMarkupBuilder()
        xml.encoding = "UTF-8"
        def doc = xml.bind {
            mkp.xmlDeclaration()
            'l:dependencies'('xmlns:l':'licenses', groupId:mojo.project.groupId, artifactId:mojo.project.artifactId, version:mojo.project.version) {
                dependencies.each { d ->
                    'l:dependency'(name:d.name, groupId:d.groupId, artifactId:d.artifactId, version:d.version, url:d.url) {
                        'l:description'(d.description)
                        d.licenses.each { l ->
                            'l:license'(name:l.name, url:l.url)
                        }
                    }
                }
            }
        }
        doc.writeTo(w);
        log.info("Generated ${mojo.generateLicenseXml}")
    }
    log.info("Outside the withWriter block ${mojo.generateLicenseXml}")
}