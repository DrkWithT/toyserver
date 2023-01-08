package com.drkwitht.util;

/**
 * This data class encapsulates HTTP body data of MIME types <code>text/?</code> for now.
 * @author Derek Tan
 */
public class HTTPBody {
    private HTTPContentType type;
    private int length;
    private String content;

    public HTTPBody(HTTPContentType typeCode, String contentText) {
        if (contentText == null) {
            type = HTTPContentType.TEXT_PLAIN;
            length = 0;
            content = "";
        } else {
            type = typeCode;
            content = contentText;
            length = asBytes().length;
        }
    }

    public HTTPContentType fetchType() {
        return type;
    }

    public int fetchLength() {
        return length;
    }

    public String asText() {
        return content;
    }

    public byte[] asBytes() {
        return content.getBytes();
    }
}
