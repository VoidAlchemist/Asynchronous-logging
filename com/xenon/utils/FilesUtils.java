package com.xenon.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * @author Zenon
 */
public class FilesUtils {

    /**
     * Same as {@link Files#newBufferedWriter(Path, Charset, OpenOption...)}, but returns a
     * {@link UnsafeBufferedWriter} instead of a {@link java.io.BufferedWriter}.
     * @param path the path to the file
     * @param cs the charset to use for encoding
     * @param options options specifying how the file is opened
     * @return the resulting {@link UnsafeBufferedWriter}
     * @see UnsafeBufferedWriter
     * @see Files#newBufferedWriter(Path, Charset, OpenOption...)
     */
    public static UnsafeBufferedWriter newUnsafeBufferedWriter(Path path, Charset cs, OpenOption... options) throws IOException {
        CharsetEncoder encoder = cs.newEncoder();
        Writer writer = new OutputStreamWriter(Files.newOutputStream(path, options), encoder);
        return new UnsafeBufferedWriter(writer);
    }
}
