/* $Id: Base64.java,v 1.1 2009/04/22 21:37:08 mickey Exp $ */
package com.sas.groovy.util;

// Implimentation from http://www.wikihow.com/Encode-a-String-to-Base64-With-Java
public class Base64 {

    public static String base64code = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/";
    public static int splitLinesAt = 76;

    public static String encode(String string) {
        String encoded = "";

        // determine how many padding bytes to add to the output
        int paddingCount = (3 - (string.length() % 3)) % 3;

        // add any necessary padding to the input
        string += "\0\0".substring(0, paddingCount);

        // process 3 bytes at a time, churning out 4 output bytes
        // worry about CRLF insertions later
        for (int i = 0; i < string.length(); i += 3) {
            int j = (string.charAt(i) << 16) + (string.charAt(i + 1) << 8) + string.charAt(i + 2);
            encoded = encoded + base64code.charAt((j >> 18) & 0x3f) +
                        base64code.charAt((j >> 12) & 0x3f) +
                        base64code.charAt((j >> 6) & 0x3f) +
                        base64code.charAt(j & 0x3f);
        }

        // replace encoded padding nulls with "="
        return splitLines(encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount));
    }

    public static String splitLines(String string) {
        String lines = "";
        for (int i = 0; i < string.length(); i += splitLinesAt) {
            lines += string.substring(i, Math.min(string.length(), i + splitLinesAt));
            lines += "\r\n";
        }
        return lines;
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            System.err.println("encoding \"" + args[i] + "\"");
            System.out.println(encode(args[i]));
        }
    }
}
