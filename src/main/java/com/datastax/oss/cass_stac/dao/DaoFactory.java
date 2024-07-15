package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.config.ConfigManager;
import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

public class DaoFactory {
    private static final Logger logger = LoggerFactory.getLogger(DaoFactory.class);
    private static DaoFactory instance;
    private final ConfigManager configManager;
    private CqlSession session;
    private GeoTimePartition partitioner;

    private static final String DEFAULT_ASTRA_BUNDLE_USERNAME = "token";
    private static final String DEFAULT_ASTRA_KS_NAME = "stac";
    private static final String DEFAULT_CASSANDRA_USERNAME = "cassandra";
    private static final String DEFAULT_CASSANDRA_PASSWORD = "cassandra";

    private DaoFactory() throws ConfigException {
        this.configManager = ConfigManager.getInstance();
        this.initializePartitioner();
        this.initializeSession();
        this.initializeDaos();
    }

    public static synchronized DaoFactory getInstance() throws ConfigException {
        if (instance == null) {
            instance = new DaoFactory();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down DaoFactory...");
                instance.shutdown();
            }));
        }
        return instance;
    }

    public <T extends IDao<?>> T getDao(@NotNull DaoType daoType) throws ConfigException {
        return switch (daoType) {
            case ITEM -> (T) new ItemDao(this.session, this.partitioner);
            case FEATURE -> (T) new FeatureDao(this.session, this.partitioner);
            case FEATURE_COLLECTION -> (T) new FeatureCollectionDao(this.session, this.partitioner);
        };
    }

    private void initializePartitioner() {
        int geoResolution = configManager.getIntProperty("database.partition.resolution.geo", 6);
        GeoTimePartition.TimeResolution timeResolution = GeoTimePartition.TimeResolution.valueOf(configManager.getProperty("database.partition.resolution.time", "MONTH"));
        this.partitioner = new GeoTimePartition(geoResolution, timeResolution);
    }

    private void initializeDaos() throws ConfigException {
        for (DaoType daoType : DaoType.values()) {
            IDao dao = getDao(daoType);
            dao.initialize();
        }
    }

    private void initializeSession() throws ConfigException {
        CqlSessionBuilder builder = CqlSession.builder();
        String scb = configManager.getProperty("cassandra.secureBundlePath");

        if (null != scb) {
            initializeSessionWithBundle(builder, scb);
        } else {
            initializeSession(builder);
        }

        String keyspace = configManager.getProperty("cassandra.keyspace", DEFAULT_ASTRA_KS_NAME);
        builder.withKeyspace(keyspace);

        this.session = builder.build();
        logger.info("Cassandra connection established");

        MutableCodecRegistry registry = (MutableCodecRegistry) session.getContext().getCodecRegistry();
        registry.register(new TIMESTAMP_OffsetDateTimeCodec());
        logger.info("Cassandra codecs registered");
    }

    private void initializeSession(CqlSessionBuilder builder) {
        String contactPointsProperty = configManager.getProperty("cassandra.contactPoints", "localhost");
        logger.info("Configuring connection with contactPoints " + contactPointsProperty);
        String[] contactPointStrings = contactPointsProperty.split(",");
        int port = configManager.getIntProperty("cassandra.port", 9042);
        Collection<InetSocketAddress> contactPoints = new ArrayList<>();
        for (String contactPoint : contactPointStrings) {
            contactPoints.add(new InetSocketAddress(contactPoint.trim(), port));
        }

        builder.addContactPoints(contactPoints);
        builder.withLocalDatacenter(configManager.getProperty("cassandra.dc", ""));
        String username = configManager.getProperty("cassandra.username", DEFAULT_CASSANDRA_USERNAME);
        String password = configManager.getProperty("cassandra.password", DEFAULT_CASSANDRA_PASSWORD);
        builder.withAuthCredentials(username, password);
    }

    private void initializeSessionWithBundle(CqlSessionBuilder builder, String scb) throws ConfigException {
        logger.info("Configuring connection with SCB " + scb);
        try {
            builder.withCloudSecureConnectBundle(Paths.get(scb));
        } catch (InvalidPathException pe) {
            try {
                builder.withCloudSecureConnectBundle(new URL(scb));
            } catch (MalformedURLException ue) {
                throw new ConfigException("cassandra.secureBundlePath is set but appears to be invalid: " + scb);
            }
        }

        String username = configManager.getProperty("cassandra.username", DEFAULT_ASTRA_BUNDLE_USERNAME);
        String password = configManager.getProperty("cassandra.password");
        builder.withAuthCredentials(username, password);
    }

    private void shutdown() {
        if (this.session != null) {
            this.session.close();
        }
        logger.info("DaoFactory shutdown complete.");
    }

    public enum DaoType {ITEM, FEATURE, FEATURE_COLLECTION}
}
