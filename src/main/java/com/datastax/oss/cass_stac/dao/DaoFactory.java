package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.config.ConfigManager;
import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;

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

    public enum DaoType {ITEM}

    private static DaoFactory instance;
    private CqlSession session;
    private final ConfigManager configManager;
    private GeoTimePartition partitioner;

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

    public <T> IDao<T> getDao(DaoType daoType) {
        return switch (daoType) {
            case ITEM -> (IDao<T>) new ItemDao(this.session, this.partitioner);
        };
    }

    private void initializePartitioner() {
        int geoResolution = configManager.getIntProperty("database.partition_resolution.geo", 6);
        GeoTimePartition.TimeResolution timeResolution = GeoTimePartition.TimeResolution.valueOf(configManager.getProperty("database.partition_resolution.time", "MONTH"));
        this.partitioner = new GeoTimePartition(geoResolution, timeResolution);
    }

    private void initializeDaos() {
        for (DaoType daoType : DaoType.values()) {
            IDao dao = getDao(daoType);
            dao.initialize();
        }
    }

    private void initializeSession() throws ConfigException {
        CqlSessionBuilder builder = CqlSession.builder();

        String scb = configManager.getProperty("cassandra.secureBundlePath");
        if (null != scb) {
            logger.info("Configuring connection with SCB "+scb);
            try {
                builder.withCloudSecureConnectBundle(Paths.get(scb));
            }
            catch (InvalidPathException pe) {
                try {
                    builder.withCloudSecureConnectBundle(new URL(scb));
                }
                catch (MalformedURLException ue) {
                    throw new ConfigException("cassandra.secureBundlePath is set but appears to be invalid: "+scb);
                }
            }

            String username = configManager.getProperty("cassandra.username", "token");
            String password = configManager.getProperty("cassandra.password");
            builder.withAuthCredentials(username, password);
        }
        else {
            String contactPointsProperty = configManager.getProperty("cassandra.contactPoints", "localhost");
            logger.info("Configuring connection with contactPoints "+contactPointsProperty);
            String[] contactPointStrings = contactPointsProperty.split(",");
            int port = configManager.getIntProperty("cassandra.port", 9042);
            Collection<InetSocketAddress> contactPoints = new ArrayList<>();
            for (String contactPoint : contactPointStrings) {
                contactPoints.add(new InetSocketAddress(contactPoint.trim(), port));
            }
            builder.addContactPoints(contactPoints);

            String username = configManager.getProperty("cassandra.username", "cassandra");
            String password = configManager.getProperty("cassandra.password", "cassandra");
            builder.withAuthCredentials(username, password);
        }

        String keyspace = configManager.getProperty("cassandra.keyspace", "stac");
        builder.withKeyspace(keyspace);

        this.session = builder.build();
        logger.info("Cassandra connection established");
    }

    private void shutdown() {
        if (this.session != null) {
            this.session.close();
        }
        logger.info("DaoFactory shutdown complete.");
    }
}
