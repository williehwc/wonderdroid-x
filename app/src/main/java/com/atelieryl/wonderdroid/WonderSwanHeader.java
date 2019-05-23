
package com.atelieryl.wonderdroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class WonderSwanHeader implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int HEADERLEN = 10;

    private final int developer;

    private final int cartid;

    private final int checksum;

    @SuppressWarnings("unused")
    private final int romsize;

    public final boolean isColor;

    public final boolean isVertical;

    public final String internalname;

    private static byte[] getHeaderFromFile(File rom) {
        byte header[] = new byte[HEADERLEN];
        try {
            FileInputStream fis;
            fis = new FileInputStream(rom);
            FileChannel fc = fis.getChannel();
            fc.read(ByteBuffer.wrap(header), fc.size() - HEADERLEN);
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return header;
    }

    public WonderSwanHeader(File rom) {
        this(getHeaderFromFile(rom));
    }

    public WonderSwanHeader(byte[] header) {
        if (header == null || header.length != HEADERLEN) {
            throw new IllegalArgumentException("Header must be " + HEADERLEN + " bytes");
        }

        developer = (header[0] & 0xFF);
        isColor = (header[1] == 1);
        cartid = (header[2] & 0xFF);
        switch (header[4]) {
            default:
                romsize = 0;
        }
        isVertical = ((header[6] & 0x01) == 1);
        checksum = (header[8] & 0xFF) + ((header[9] << 8) & 0xFFFF);
        internalname = Integer.toString(developer) + "-" + Integer.toString(cartid) + "-"
                + Integer.toString(checksum);

    }

}
