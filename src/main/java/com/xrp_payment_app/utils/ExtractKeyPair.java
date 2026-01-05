package com.xrp_payment_app.utils;

import org.xrpl.xrpl4j.crypto.keys.*;

public class ExtractKeyPair {
    public KeyPair deriveKeyPairFromSecret (String secret) {
        Seed seed = Seed.fromBase58EncodedSecret(Base58EncodedSecret.of(secret));
        return seed.deriveKeyPair();
    }
}
