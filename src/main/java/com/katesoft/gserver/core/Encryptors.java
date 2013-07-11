package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkArgument;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.jasypt.util.text.BasicTextEncryptor;
import org.jasypt.util.text.StrongTextEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Encryptors {
    private static final String DELIMETER = ":";

    public static PasswordEncoder passwordEncryptor(boolean strong) {
        final PasswordEncryptor enc = strong ? new StrongPasswordEncryptor() : new BasicPasswordEncryptor();
        return new PasswordEncoder() {
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return enc.checkPassword( rawPassword.toString(), encodedPassword );
            }
            @Override
            public String encode(CharSequence rawPassword) {
                return enc.encryptPassword( rawPassword.toString() );
            }
        };
    }

    public static TextEncryptor textEncryptor(String password, boolean strong) {
        final org.jasypt.util.text.TextEncryptor enc;
        if ( strong ) {
            StrongTextEncryptor strongTextEncryptor = new StrongTextEncryptor();
            strongTextEncryptor.encrypt( password );
            enc = strongTextEncryptor;
        }
        else {
            BasicTextEncryptor basicTextEncryptor = new BasicTextEncryptor();
            basicTextEncryptor.setPassword( password );
            enc = basicTextEncryptor;
        }
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
