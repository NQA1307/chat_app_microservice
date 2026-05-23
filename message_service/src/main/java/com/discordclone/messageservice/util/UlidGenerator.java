package com.discordclone.messageservice.util;

import de.huxhorn.sulky.ulid.ULID;

public final class UlidGenerator {

    private static final ULID ULID = new ULID();

    private UlidGenerator() {}

    public static String next(){
        return ULID.nextULID();
    }
}
