package com.drkwitht.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import com.drkwitht.util.HTTPContentType;

/**
 * Encapsulates data of any static resource file, given supported MIME type and a valid local file path. Contains helper methods for getting content length or content in various forms such as bytes.
 * @author Derek Tan
 */
public class StaticResource {
    private HTTPContentType type; // MIME content type code 
    private String truePath;      // full filesystem path of static file
    private int length;

    public StaticResource(HTTPContentType contentType, String filePath) throws FileNotFoundException, IOException {
        type = contentType;

        if (filePath == null) {
            throw new FileNotFoundException("Cannot find file on null path.");
        }

        truePath = filePath;

        File resourceFile = new File(truePath);

        length = (int)resourceFile.length();
    }

    public String fetchMIMEType() {
        switch (type) {
            case TEXT_HTML:
                return "text/html";
            case TEXT_CSS:
                return "text/css";
            case TEXT_PLAIN:
            default:
                return "text/plain";
        }
    }

    public int fetchLength() {
        return length;
    }

    public String fetchText() throws IOException {
        char[] buffer = new char[length];

        File resFile = new File(truePath);
        FileReader fReader = new FileReader(resFile);

        if (fReader.read(buffer) == -1) {
            fReader.close();
            throw new IOException("Failed to read resource.");
        }

        fReader.close();

        return new StringBuilder().append(buffer).toString();
    }
}
