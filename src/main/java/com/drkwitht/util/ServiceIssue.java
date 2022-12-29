package com.drkwitht.util;

/**
 * Defines service problem codes. Each code can map to an HTTP status.
 */
public enum ServiceIssue {
    NONE,        // Regex(not(4){2}..)
    BAD_REQUEST, // 400
    NOT_FOUND,   // 404
    NO_SUPPORT,  // 501
    UNKNOWN      // 500
}
