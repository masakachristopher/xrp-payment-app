package com.xrp_payment_app.utils;

import org.xrpl.xrpl4j.codec.addresses.KeyType;
import org.xrpl.xrpl4j.crypto.ServerSecret;
import org.xrpl.xrpl4j.crypto.keys.*;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.bc.BcDerivedKeySignatureService;
import org.xrpl.xrpl4j.crypto.signing.bc.BcSignatureService;
import org.xrpl.xrpl4j.model.transactions.Payment;

public class SecureSigning {

    public static SingleSignedTransaction<Payment> signWithSeed(String sSecret, Payment payment) {
        ExtractKeyPair extractKeyPair = new ExtractKeyPair();
        PrivateKey privateKey = extractKeyPair.deriveKeyPairFromSecret(sSecret).privateKey();
        SignatureService<PrivateKey> signatureService = new BcSignatureService();
        return signatureService.sign(privateKey, payment);
    }

    public static SingleSignedTransaction<Payment> signWithDerivedKey(ServerSecret secret, Payment payment) {
        SignatureService<PrivateKeyReference> signatureService = new BcDerivedKeySignatureService(() -> secret);
        PrivateKeyReference keyRef = new PrivateKeyReference() {
            @Override
            public KeyType keyType() {
                return KeyType.ED25519;
            }

            @Override
            public String keyIdentifier() {
                return "platform-key-id";
            }
        };
        return signatureService.sign(keyRef, payment);
    }
}