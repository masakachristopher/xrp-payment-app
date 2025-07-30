package com.xrp_payment_app.utils;

import com.google.common.primitives.UnsignedInteger;
import org.xrpl.xrpl4j.codec.addresses.KeyType;
import org.xrpl.xrpl4j.crypto.ServerSecret;
import org.xrpl.xrpl4j.crypto.keys.*;
import org.xrpl.xrpl4j.crypto.signing.SignatureService;
import org.xrpl.xrpl4j.crypto.signing.SingleSignedTransaction;
import org.xrpl.xrpl4j.crypto.signing.bc.BcDerivedKeySignatureService;
import org.xrpl.xrpl4j.crypto.signing.bc.BcSignatureService;
import org.xrpl.xrpl4j.model.transactions.Address;
import org.xrpl.xrpl4j.model.transactions.Payment;
import org.xrpl.xrpl4j.model.transactions.XrpCurrencyAmount;


public class SecureSigning {

    public static SingleSignedTransaction<Payment> signWithSeed(String sSecret, Payment payment) {
        Seed seed = Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(sSecret));
        PrivateKey privateKey = seed.deriveKeyPair().privateKey();
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