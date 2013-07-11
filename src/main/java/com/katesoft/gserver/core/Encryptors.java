package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkArgument;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class Encryptors {
    private static final String DELIMETER = ":";

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
    public static String encode(TextEncryptor encryptor, String... args) {
        checkArgument( args.length > 0 );

        StringBuilder b = new StringBuilder();
        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[i];
            b.append( arg );
            if ( i < args.length - 1 ) {
                b.append( DELIMETER );
            }
        }
        return encryptor.encrypt( b.toString() );
    }
    public static String[] decode(TextEncryptor encryptor, String str) {
        String decrypted = encryptor.decrypt( str );
        return decrypted.split( DELIMETER );
    }
}
