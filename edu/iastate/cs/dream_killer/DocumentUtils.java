package edu.iastate.cs.dream_killer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Various utilities for processing documents and files
 *
 * @author Nick Gerleman
 */
public class DocumentUtils {

    /**
     * Strip Javadoc and internal style comments from a document
     *
     * @param document the original document
     * @return the document with comments stripped
     */
    public static String stripComments(String document) {
        return document.replaceAll("(/\\*[^/]*\\*/)|(//.*)", "");
    }


    /**
     * Enumerate paths in DFS folder from a given root
     *
     * @return a list of paths found inside the given root directory
     * @throws IOException if there are IO errors or the path is invalid
     */
    public static List<Path> enumeratePaths(Path parentDirectory, String glob) throws IOException {
        List<Path> paths = new ArrayList<>();

        DirectoryStream<Path> directChildren = Files.newDirectoryStream(parentDirectory);
        for (Path childPath : directChildren) {
            if (childPath.toFile().isDirectory())
                paths.addAll(enumeratePaths(childPath, glob));
            else
                paths.add(childPath);
        }

        return paths;
    }


    /**
     * Filter a list of paths to only those with a given filename
     *
     * @param paths list of paths to filter
     * @param filename the filename to filter for
     * @return the filtered list of paths
     */
    public static List<Path> filterPathsByFilename(List<Path> paths, String filename) {
        String lowercaseTarget = filename.toLowerCase();

        return paths.stream()
                .filter((path) -> {
                    String lowercaseName = path.getFileName().toString().toLowerCase();
                    return lowercaseName.equals(lowercaseTarget);
                })
                .collect(Collectors.toList());
    }


    /**
     * Find the first path name in the first path closest to the root directory
     * that differs from the second path.
     *
     * @param path1 the first path
     * @param path2 the second path
     * @return the name of the differing path
     * @throws IllegalArgumentException if paths are the same
     */
    public static String leastCommonPathName(Path path1, Path path2) {
        String[] path1Names = path1.toAbsolutePath().toString().split("/");
        String[] path2Names = path2.toAbsolutePath().toString().split("/");

        for (int i = 0; i < path1Names.length && i < path2Names.length; i++) {
            if (!path1Names[i].equals(path2Names[i]))
                return path1Names[i];
        }

        throw new IllegalArgumentException("All common paths up to minimum path length");
    }


    /**
     * Shingle a document into n-grams with a given number of terms. Terms are
     * shingled using whitespace delimited character sequences. Terms are
     * separated by a single space and are converted to lowercase. No stemming
     * is done and no stop words are removed.
     *
     * @param numTerms the number of terms per n-gram
     * @param document the document to shingle
     * @return a set of shingles in the document
     */
    public static Set<String> shingleDocument(int numTerms, String document) {
        Set<String> shingles = new HashSet<>();
        Deque<String> currentShingle = new ArrayDeque<>();
        Scanner documentScanner = new Scanner(document);

        while (documentScanner.hasNext()) {
            currentShingle.addLast(documentScanner.next().toLowerCase());
            if (currentShingle.size() < numTerms)
                continue;

            shingles.add(String.join(" ", currentShingle));
            currentShingle.removeFirst();
        }

        return shingles;
    }
}
