package com.drkwitht.util;

/**
 * This data class encapsulates HTTP body data of MIME types <code>text/?</code> for now.
 */
public class HTTPBody {
    private HTTPContentType type;
    private long length;
    private String content;

    public HTTPBody(HTTPContentType typeCode, String contentText) {
        if (contentText == null) {
            type = HTTPContentType.UNKNOWN;
            length = 0;
            content = "";
        } else {
            type = typeCode;
            length = contentText.length();
            content = contentText;
        }
    }

    public String asText() {
        return content;
    }

    public byte[] asBytes() {
        return content.getBytes();
    }
}
