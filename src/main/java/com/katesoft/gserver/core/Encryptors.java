package com.katesoft.gserver.core;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class Encryptors {
    public static TextEncryptor textEnc(String password) {
        final BasicTextEncryptor enc = new BasicTextEncryptor();
        enc.setPassword( password );
        return new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                return enc.encrypt( text );
            }
            @Override
            public String decrypt(String encryptedText) {
                return enc.decrypt( encryptedText );
            }
        };
    }
}
