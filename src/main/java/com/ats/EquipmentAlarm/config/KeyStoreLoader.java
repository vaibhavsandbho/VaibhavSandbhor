package com.ats.EquipmentAlarm.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;

import java.security.Security;



import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.stack.core.util.SelfSignedCertificateBuilder;

import java.security.Security;

public class KeyStoreLoader {

    private static final char[] PASSWORD = "password".toCharArray();
    private static final String CLIENT_ALIAS = "client-ai";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_FILE = "client-keystore1.pfx";

    private KeyPair clientKeyPair;
    private X509Certificate clientCertificate;
    private X509Certificate[] clientCertificateChain;

    // ApplicationUri must be unique and match in client config
    private static final String APPLICATION_NAME = "milo:client2";
    private String applicationUri;

    public KeyStoreLoader load(Path baseDir) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Detect client IP dynamically
        String clientIp = InetAddress.getLocalHost().getHostAddress();
        applicationUri = "urn:" + clientIp + ":" + APPLICATION_NAME;

        Path keystorePath = baseDir.resolve(KEYSTORE_FILE);
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);

        if (!Files.exists(keystorePath)) {
            // --- Create new keystore and certificate ---
            keyStore.load(null, PASSWORD);
        

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            clientKeyPair = generator.generateKeyPair();

            SelfSignedCertificateBuilder builder = new SelfSignedCertificateBuilder(clientKeyPair);
            builder.setCommonName("Eclipse Milo OPC UA Client");
            builder.setOrganization("Ats India pvt.Ltd");
            builder.setOrganizationalUnit("Automation");
            builder.setLocalityName("Pune");
            builder.setStateName("Maharashtra");
            builder.setCountryCode("IN");
            builder.setApplicationUri(applicationUri);
            builder.addDnsName("localhost");
            builder.addIpAddress(clientIp);

            clientCertificate = builder.build();
            clientCertificateChain = new X509Certificate[]{clientCertificate};

            keyStore.setKeyEntry(CLIENT_ALIAS, clientKeyPair.getPrivate(), PASSWORD, clientCertificateChain);

            try (FileOutputStream fos = new FileOutputStream(keystorePath.toFile())) {
                keyStore.store(fos, PASSWORD);
            }

        } else {
            // --- Load existing keystore ---
            try (FileInputStream fis = new FileInputStream(keystorePath.toFile())) {
                keyStore.load(fis, PASSWORD);
            }

            clientKeyPair = new KeyPair(
                keyStore.getCertificate(CLIENT_ALIAS).getPublicKey(),
                (java.security.PrivateKey) keyStore.getKey(CLIENT_ALIAS, PASSWORD)
            );
            clientCertificate = (X509Certificate) keyStore.getCertificate(CLIENT_ALIAS);
            clientCertificateChain = new X509Certificate[]{clientCertificate};
        }

        return this;
    }

    public KeyPair getClientKeyPair() {
        return clientKeyPair;
    }

    public X509Certificate getClientCertificate() {
        return clientCertificate;
    }

    public X509Certificate[] getClientCertificateChain() {
        return clientCertificateChain;
    }

    public String getApplicationUri() {
        return applicationUri;
    }
}






