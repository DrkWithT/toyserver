package com.drkwitht.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.drkwitht.util.HTTPContentType;

/**
 * Encapsulates data of any static resource file, given supported MIME type and a valid local file path.
 */
public class StaticResource {
    private HTTPContentType type; // MIME content type code 
    private String truePath;      // full filesystem path of static file
    private String rawData;       // static text
    private int length;

    public StaticResource(HTTPContentType contentType, String filePath) throws FileNotFoundException, IOException {
        type = contentType;

        truePath = filePath;

        File resourceFile = new File(truePath);

        length = (int)resourceFile.length();

        char[] buffer = new char[length];

        FileReader fReader = new FileReader(resourceFile);
        int readStatus = fReader.read(buffer);

        if (readStatus == -1) {
            fReader.close();
            throw new IOException("Failed to pre-read resource file.");
        }

        fReader.close();

        StringBuilder sb = new StringBuilder();
        rawData = sb.append(buffer).toString();
        
        length = asBytes().length;
    }

    public String asText() {
        return rawData;
    }

    public byte[] asBytes() {
        return rawData.getBytes();
    }
}
