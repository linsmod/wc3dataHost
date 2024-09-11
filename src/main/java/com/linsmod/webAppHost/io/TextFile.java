package com.linsmod.webAppHost.io;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TextFile implements Iterable<String>, Closeable {
    private final InputStream inputStream;

    public TextFile(String path) {
        try {
//            if (path.startsWith("content://"))
//                this.inputStream = App.getContentResolver().openInputStream(Uri.parse(path));
//            else
            this.inputStream = new FileInputStream(path);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public TextFile(File file) {
        try {
            this.inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new LineIterator(this.inputStream);
    }

    @Override
    public void close() throws IOException {
        if (inputStream == null)
            inputStream.close();
    }

    private static class LineIterator implements Iterator<String>, AutoCloseable {
        private BufferedReader reader;
        private String nextLine;

        public LineIterator(InputStream inputStream) {
            try {
                reader = new BufferedReader(new InputStreamReader(inputStream));
                advance();
            } catch (IOException e) {
                throw new RuntimeException("Failed to open file for reading", e);
            }
        }

        private void advance() throws IOException {
            nextLine = reader.readLine();
        }

        @Override
        public boolean hasNext() {
            return nextLine != null;
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more lines available");
            }

            String currentLine = nextLine;
            try {
                advance();
            } catch (IOException e) {
                closeReader();
                throw new RuntimeException("Error reading next line", e);
            }

            return currentLine;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove() is not supported");
        }

        private void closeReader() {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("Error closing reader", e);
            } finally {
                reader = null;
            }
        }

        @Override
        public void close() throws Exception {
            closeReader();
        }
    }
}