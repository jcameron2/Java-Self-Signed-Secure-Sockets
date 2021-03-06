package edu.usf.myweb.jcameron2;

import sun.security.x509.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CertificateHelper {

    private CertificateHelper() {
    }

    public static void createSignedCertificateKeyStore(String serverCommonName, String caCommonName, File caKeyStore, File keyStorePath) throws GeneralSecurityException, IOException {

        //Get CA information
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        char[] password = "".toCharArray();

        FileInputStream fis = new FileInputStream(caKeyStore);
        ks.load(fis, password);
        fis.close();

        PrivateKey caPrivateKey = (PrivateKey) ks.getKey("privateKey", password);
        Certificate caCertificate = ks.getCertificate("certificate");

        //Generate our signed client certificate
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        //Generate some RSA key pairs
        KeyPair rsaKeyPair = keyPairGenerator.generateKeyPair();

        final String caDN = "CN=" + caCommonName;

        ks = KeyStore.getInstance(KeyStore.getDefaultType());

        ks.load(null, password);

        Certificate certificate = CertificateHelper.generateSignedCertificate(
                "CN=" + serverCommonName, rsaKeyPair.getPublic(),
                caPrivateKey, caDN, 365, "SHA256withRSA");

        ks.setKeyEntry(serverCommonName, rsaKeyPair.getPrivate(), password, new Certificate[]{certificate, caCertificate});

        ks.setCertificateEntry(caCommonName, caCertificate);

        FileOutputStream fos = new FileOutputStream(keyStorePath);
        ks.store(fos, password);
        fos.close();

    }

    public static void createCAKeyStore(String commonName, File keyStorePath) throws GeneralSecurityException, IOException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);

        //Generate some RSA key pairs
        KeyPair rsaCAKeyPair = keyPairGenerator.generateKeyPair();

        final String caDN = "CN=" + commonName;

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        char[] password = "".toCharArray();
        ks.load(null, password);

        Certificate caCertificate = CertificateHelper.generateCA(caDN, rsaCAKeyPair, 365, "SHA256withRSA");

        ks.setCertificateEntry("certificate", caCertificate);
        ks.setKeyEntry("privateKey", rsaCAKeyPair.getPrivate(), password, new Certificate[]{caCertificate});

        FileOutputStream fos = new FileOutputStream(keyStorePath);
        ks.store(fos, password);
        fos.close();

    }

    public static X509Certificate generateSignedCertificate(String dn,
                                                            PublicKey publicKey,
                                                            PrivateKey caPrivateKey,
                                                            String caDN,
                                                            int days, String algorithm)
            throws GeneralSecurityException, IOException {

        X509CertInfo info = new X509CertInfo();

        Date from = new Date();
        Date to = new Date(from.getTime() + TimeUnit.DAYS.toMillis(days));

        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, new X500Name(caDN));
        info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(caPrivateKey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(caPrivateKey, algorithm);

        return cert;
    }

    /**
     * Create a self-signed X.509 Example
     *
     * @param dn        the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
     * @param pair      the KeyPair
     * @param days      how many days from now the Example is valid for
     * @param algorithm the signing algorithm, eg "SHA1withRSA"
     */
    public static X509Certificate generateCA(String dn, KeyPair pair, int days, String algorithm)
            throws GeneralSecurityException, IOException {

        PrivateKey privkey = pair.getPrivate();
        X509CertInfo info = new X509CertInfo();

        Date from = new Date();
        Date to = new Date(from.getTime() + TimeUnit.DAYS.toMillis(days));

        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        // Sign the cert to identify the algorithm that's used.
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);

        // Update the algorith, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);
        return cert;
    }

}
