package com.microservices.auth.security;

import com.microservices.auth.config.JwtProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Supplies the RSA key pair used to sign/verify RS256 JWTs.
 *
 * <p>On startup it loads {@code private_key.pem} and {@code public_key.pem} from the
 * configured key directory. If they are absent, a fresh 2048-bit pair is generated and
 * persisted there so issued tokens remain valid across restarts. For production, mount
 * pre-generated keys into that directory (the private key must never be committed).
 */
@Slf4j
@Component
public class RsaKeyProvider {

    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE = "public_key.pem";

    @Getter
    private final RSAPrivateKey privateKey;
    @Getter
    private final RSAPublicKey publicKey;

    public RsaKeyProvider(JwtProperties properties) throws Exception {
        Path dir = Path.of(properties.getKeyDirectory());
        Path privatePath = dir.resolve(PRIVATE_KEY_FILE);
        Path publicPath = dir.resolve(PUBLIC_KEY_FILE);

        if (Files.exists(privatePath) && Files.exists(publicPath)) {
            log.info("Loading RSA key pair from {}", dir.toAbsolutePath());
            this.privateKey = loadPrivateKey(Files.readString(privatePath));
            this.publicKey = loadPublicKey(Files.readString(publicPath));
        } else {
            log.warn("No RSA key pair found in {} - generating a new one. "
                    + "Mount stable keys here for production.", dir.toAbsolutePath());
            KeyPair pair = generateKeyPair();
            this.privateKey = (RSAPrivateKey) pair.getPrivate();
            this.publicKey = (RSAPublicKey) pair.getPublic();
            persist(dir, privatePath, publicPath);
        }
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private void persist(Path dir, Path privatePath, Path publicPath) throws Exception {
        Files.createDirectories(dir);
        Files.writeString(privatePath, toPem("PRIVATE KEY", privateKey.getEncoded()));
        Files.writeString(publicPath, toPem("PUBLIC KEY", publicKey.getEncoded()));
        log.info("Persisted generated RSA key pair to {}", dir.toAbsolutePath());
    }

    private RSAPrivateKey loadPrivateKey(String pem) throws Exception {
        byte[] der = decodePem(pem);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private RSAPublicKey loadPublicKey(String pem) throws Exception {
        byte[] der = decodePem(pem);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(der));
    }

    private static String toPem(String type, byte[] der) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }

    private static byte[] decodePem(String pem) {
        String base64 = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
