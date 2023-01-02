package com.drkwitht.resource;

import java.io.File;
import java.io.FileReader;

import com.drkwitht.util.HTTPContentType;

/**
 * Encapsulates data of any static resource file, given supported MIME type and a valid local file path.
 */
public class StaticResource {
    private HTTPContentType type; // MIME content type code 
    private String truePath;      // full filesystem path of static file
    private String routePath;     // relative URL in HTTP request
    private String rawData;       // static text
    private int length;

    public StaticResource(String mimeName, String filePath, String routingPath) throws Exception {
        if (mimeName.equals("text/html")) {
            type = HTTPContentType.TEXT_HTML;
        } else if (mimeName.equals("text/css")) {
            type = HTTPContentType.TEXT_CSS;
        } else if (mimeName.equals("text/plain")) {
            type = HTTPContentType.TEXT_PLAIN;
        } else {
            throw new Exception("Unknown MIME type!");
        }

        truePath = filePath;
        routePath = routingPath;

        File resourceFile = new File(truePath);

        length = (int)resourceFile.length();

        char[] buffer = new char[length];

        FileReader fReader = new FileReader(resourceFile);
        int readStatus = fReader.read(buffer);

        if (readStatus == -1) {
            fReader.close();
            throw new Exception("Failed to pre-read resource file.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(buffer);
        rawData = sb.toString();
        
        length = asBytes().length;
    }

    public String asText() {
        return rawData;
    }

    public byte[] asBytes() {
        return rawData.getBytes();
    }
}
