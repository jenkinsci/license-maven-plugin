import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult

mojo.generateLicenseHtml.parentFile?.mkdirs()

// generate HTML report from XML
def t = TransformerFactory.newInstance().newTemplates(new StreamSource(mojo.class.getResource("licenses.xslt").toExternalForm()));
t.newTransformer().transform(new StreamSource(mojo.generateLicenseXml), new StreamResult(mojo.generateLicenseHtml));
log.info("Generated ${mojo.generateLicenseHtml}")
