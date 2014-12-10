package ee.cyber.sdsb.confproxy.commandline;

import java.io.OutputStream;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.cli.CommandLine;

import ee.cyber.sdsb.common.conf.globalconf.ConfigurationAnchor;
import ee.cyber.sdsb.common.conf.globalconf.privateparameters.ConfigurationAnchorType;
import ee.cyber.sdsb.common.conf.globalconf.privateparameters.ConfigurationSourceType;
import ee.cyber.sdsb.common.conf.globalconf.privateparameters.ObjectFactory;
import ee.cyber.sdsb.common.util.AtomicSave;
import ee.cyber.sdsb.confproxy.ConfProxyProperties;
import ee.cyber.sdsb.confproxy.util.OutputBuilder;

/**
 * Utility tool for creating an anchor file that is used for downloading
 * the generated global configuration.
 */
public class ConfProxyUtilGenerateAnchor extends ConfProxyUtil {

    ConfProxyUtilGenerateAnchor() {
        super("confproxy-generate-anchor");
        getOptions()
            .addOption(PROXY_INSTANCE)
            .addOption("f", "filename", true, "Filename of the generated anchor");
    }

    @Override
    void execute(CommandLine commandLine)
            throws Exception {
        ensureProxyExists(commandLine);
        final ConfProxyProperties conf = loadConf(commandLine);
        ConfigurationAnchor sourceAnchor =
                new ConfigurationAnchor(conf.getProxyAnchorPath());

        if (commandLine.hasOption("filename")) {
            String filename = commandLine.getOptionValue("f");
            System.out.println("Generating anchor xml to '" + filename + "'");

            AtomicSave.execute(filename, "tmpanchor",
                out -> generateAnchorXml(conf,
                        sourceAnchor.getInstanceIdentifier(), out));
        } else {
            printHelp();
            System.exit(0);
        }
    }

    void generateAnchorXml(ConfProxyProperties conf, String instanceIdentifier,
            OutputStream out) throws Exception {
        JAXBContext jaxbCtx = JAXBContext.newInstance(ObjectFactory.class);
        Marshaller marshaller = jaxbCtx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        ObjectFactory factory = new ObjectFactory();
        ConfigurationSourceType sourceType =
                factory.createConfigurationSourceType();
        sourceType.setDownloadURL(conf.getConfigurationProxyURL() + "/"
                + OutputBuilder.SIGNED_DIRECTORY_NAME);
        for (byte[] cert : conf.getVerificationCerts()) {
            sourceType.getVerificationCert().add(cert);
        }
        ConfigurationAnchorType anchorType =
                factory.createConfigurationAnchorType();
        anchorType.setInstanceIdentifier(instanceIdentifier);
        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTimeZone(TimeZone.getTimeZone("UTC"));
        XMLGregorianCalendar xgcal = DatatypeFactory.newInstance()
              .newXMLGregorianCalendar(gcal);
        anchorType.setGeneratedAt(xgcal);
        anchorType.getSource().add(sourceType);
        JAXBElement<ConfigurationAnchorType> root =
                factory.createConfigurationAnchor(anchorType);

        marshaller.marshal(root, out);
    }
}