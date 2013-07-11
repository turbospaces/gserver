package com.katesoft.gserver.core;

import static junit.framework.Assert.assertEquals;

import org.junit.Test;
import org.springframework.security.crypto.encrypt.TextEncryptor;

public class EncryptorsTest {
    TextEncryptor enc = Encryptors.textEncryptor( "XXX", false );

    @Test
    public void works() {
        String str = Encryptors.encode( enc, new String[] { "x123", "y123" } );
        String[] strings = Encryptors.decode( enc, str );
        assertEquals( strings[0], "x123" );
        assertEquals( strings[1], "y123" );
    }
}
