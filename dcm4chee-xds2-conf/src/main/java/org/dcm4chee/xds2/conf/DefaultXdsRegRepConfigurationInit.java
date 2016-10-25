package org.dcm4chee.xds2.conf;

import org.dcm4che3.conf.api.ConfigurationAlreadyExistsException;
import org.dcm4che3.conf.api.ConfigurationNotFoundException;
import org.dcm4che3.conf.api.DicomConfiguration;
import org.dcm4che3.conf.api.upgrade.ScriptVersion;
import org.dcm4che3.conf.api.upgrade.UpgradeScript;
import org.dcm4che3.conf.core.api.ConfigurationException;
import org.dcm4che3.net.Connection;
import org.dcm4che3.net.Device;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4che3.net.audit.AuditRecordRepository;
import org.dcm4che3.net.hl7.HL7Application;
import org.dcm4che3.net.hl7.HL7DeviceExtension;
import org.dcm4che3.util.UIDUtils;
import org.dcm4chee.storage.conf.Filesystem;
import org.dcm4chee.storage.conf.FilesystemGroup;
import org.dcm4chee.storage.conf.StorageAvailability;
import org.dcm4chee.storage.conf.StorageConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ScriptVersion("110-0")
public class DefaultXdsRegRepConfigurationInit implements UpgradeScript {

    private static final Logger log = LoggerFactory.getLogger(DefaultXdsRegRepConfigurationInit.class);

    private static final String[] MIME_TYPES2 = new String[]{"application/xml", "application/dicom", "application/pdf",
            "application/msword", "application/msexcel", "text/plain", "text/xml", "image/jpeg", "image/png", "image/tiff"};

    public DefaultXdsRegRepConfigurationInit() {
    }

    @Override
    public void upgrade(UpgradeContext upgradeContext) throws ConfigurationException {

        String deviceName = System.getProperty(XdsExtension.DEVICE_NAME_PROPERTY, XdsExtension.DEF_DEVICE_NAME);
        try {
            upgradeContext.getDicomConfiguration().findDevice(deviceName);
            log.info("XDS device '{}' already exists, no config init performed", deviceName);
        } catch (ConfigurationNotFoundException nf) {
            log.info("XDS device '{}' not found => initializing default config ...", deviceName);
            try {
                applyDefaultConfig(deviceName, upgradeContext.getDicomConfiguration());
            } catch (Exception e) {
                throw new ConfigurationException("Could not initialize default XDS configuration", e);
            }
        }
    }

    public void applyDefaultConfig(String deviceName, DicomConfiguration config) {

        log.info("Initializing default XDS configuration for device {}", deviceName);

        // should work with agility
        String ip = System.getProperty("jboss.bind.address");
        if (ip == null || ip.equals("0.0.0.0")) {
            ip = "localhost";
        }

        boolean useArr = false;
        try {
            // ARR
            Device arr = new Device();
            arr.setDeviceName("syslog");
            List<Connection> arrConnections = new ArrayList<>();
            Connection e = new Connection();
            e.setCommonName("audit-udp");
            e.setProtocol(Connection.Protocol.SYSLOG_UDP);
            e.setPort(514);
            e.setHostname(ip);
            arrConnections.add(e);
            arr.setConnections(arrConnections);

            AuditRecordRepository ext = new AuditRecordRepository();
            ext.setConnections(arrConnections);
            arr.addDeviceExtension(ext);

            config.persist(arr);
        } catch (ConfigurationAlreadyExistsException cee) {
            //noop, thats ok
        }


        Device device = new Device(deviceName);

        // registry
        XdsRegistry registry = new XdsRegistry();
        device.addDeviceExtension(registry);
        registry.setApplicationName("XDS-REGISTRY");

        // affinity domain
        String generatedAffDomain = UIDUtils.createUID();
        String[] affinityDomains = new String[1];
        affinityDomains[0] = generatedAffDomain;
        registry.setAffinityDomain(affinityDomains);

        registry.setAffinityDomainConfigDir("${jboss.server.config.dir}/xds/affinitydomain");
        registry.setAcceptedMimeTypes(MIME_TYPES2);
        registry.setSoapLogDir("../standalone/log/xds");
        registry.setQueryUrl("https://" + ip + ":8443/xds/registry");
        registry.setRegisterUrl("https://" + ip + ":8443/xds/registry");

        // generic source
        XdsSource source = new XdsSource();
        device.addDeviceExtension(source);
        source.setUid("0");
        source.setRegistry(device);
        source.setRepository(device);

        // repository
        XdsRepository rep = new XdsRepository();
        device.addDeviceExtension(rep);
        rep.setApplicationName("XDS-REPOSITORY");
        rep.setRepositoryUID(UIDUtils.createUID());
        rep.setRetrieveUrl("https://" + ip + ":8443/xds/repository");
        rep.setProvideUrl("https://" + ip + ":8443/xds/repository");
        rep.setAcceptedMimeTypes(MIME_TYPES2);
        rep.setSoapLogDir("../standalone/log/xds");
        rep.setCheckMimetype(false);
        HashMap<String, String> fsGrps = new HashMap<String, String>(1);
        fsGrps.put("*", "XDS_ONLINE");
        rep.setFsGroupIDbyAffinity(fsGrps);

        // storage
        StorageConfiguration store = new StorageConfiguration();
        device.addDeviceExtension(store);
        store.setApplicationName("XDS-REPOSITORY-STORAGE");
        FilesystemGroup grp = new FilesystemGroup("XDS_ONLINE", "1GiB");

        String serverHomeDir = System.getProperty("jboss.server.base.dir");
        File fsDir = new File(serverHomeDir, "xds-repository-storage");
        Filesystem fs1 = new Filesystem("xds_fs_1", fsDir.toURI().toString(), 10, StorageAvailability.ONLINE);
        grp.addFilesystem(fs1);
        store.addFilesystemGroup(grp);

        Map<String, Device> deviceBySrcUid = new HashMap<String, Device>();
        deviceBySrcUid.put("*", device);
        rep.setSrcDevicebySrcIdMap(deviceBySrcUid);


        List<Connection> conns = new ArrayList<>();
        // audit upd
        Connection auditConn = new Connection();
        auditConn.setProtocol(Connection.Protocol.SYSLOG_UDP);
        auditConn.setCommonName("audit-udp");
        auditConn.setHostname(ip);
        conns.add(auditConn);

        // hl7
        Connection hl7conn = new Connection();
        hl7conn.setProtocol(Connection.Protocol.HL7);
        hl7conn.setCommonName("hl7-conn");
        hl7conn.setHostname(ip);
        hl7conn.setPort(2576);
        conns.add(hl7conn);

        device.setConnections(conns);

        // HL7 extensions
        HL7DeviceExtension hl7ext = new HL7DeviceExtension();
        HL7Application hl7App = new HL7Application();
        hl7App.setApplicationName("XDS^IMPAX");
        ArrayList<Connection> hl7conns = new ArrayList<Connection>();
        hl7conns.add(hl7conn);
        hl7App.setConns(hl7conns);
        hl7App.setAcceptedMessageTypes(
                "ADT^A01",
                "ADT^A02",
                "ADT^A03",
                "ADT^A04",
                "ADT^A05",
                "ADT^A06",
                "ADT^A07",
                "ADT^A08",
                "ADT^A40",
                "ORM^O01");

        hl7ext.addHL7Application(hl7App);
        device.addDeviceExtension(hl7ext);

        // AuditLogger
        AuditLogger auditLogger = new AuditLogger();
        ArrayList<Connection> alconns = new ArrayList<Connection>();
        alconns.add(auditConn);
        auditLogger.setConnections(alconns);
        auditLogger.setSchemaURI("http://www.dcm4che.org/DICOM/audit-message.rnc");
        // leave blank, so by default there is no logging
        //auditLogger.setAuditRecordRepositoryDevice();
        device.addDeviceExtension(auditLogger);

        config.persist(device);

    }
}


