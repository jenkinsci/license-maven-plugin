// generate HTML report from XML
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.Templates

Templates t = TransformerFactory.newInstance().newTemplates(new StreamSource(mojo.class.getResource("licenses.xslt").toExternalForm()));

generate {
    mojo.generateLicenseHtml.parentFile?.mkdirs()
    def tr = t.newTransformer()
    tr.setParameter("groupId", mojo.project.groupId)
    tr.setParameter("artifactId", mojo.project.artifactId)
    tr.setParameter("version", mojo.project.version)
    tr.transform(new StreamSource(mojo.generateLicenseXml), new StreamResult(mojo.generateLicenseHtml));
    log.info("Generated ${mojo.generateLicenseHtml}")
}
