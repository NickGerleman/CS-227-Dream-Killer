package edu.iastate.cs.dream_killer;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Tool used to detect trivial cases of plagiarism in submitted Java programs.
 * This program attempts to intelligently classify clusters of assignments that
 * appear to have greater than normal similarity.
 *
 * @author Nick Gerleman
 */
public class Main {

    /** Number of terms in the n-gram to use for shingle */
    private static final int NUM_NGRAM_TERMS = 3;

    /** The number of permutations to use for the MinHash matrix */
    private static final int NUM_PERMUTATIONS = 2_500;

    /** The factor relative to standard deviation defining suspicious submissions */
    private static final double STD_FACTOR = 2;

    /**
     * Main method
     *
     * @param args the directory to search and then the names of files to tests
     */
    public static void main(String[] args) {
        if(args.length < 2) {
            System.out.println("Usage: dreamkiller targetDirectory file1 file2...");
            System.exit(1);
        }

        System.out.println("Enumerating Files...");
        List<Path> documentPaths = null;
        try {
            documentPaths = DocumentUtils.enumeratePaths(Paths.get(args[0]), "*.java");
        } catch (IOException e) {
            System.err.println("Unable to read target directory");
            e.printStackTrace();
            System.exit(0);
        }
        System.out.println(documentPaths.size() + " files found\n");

        for (int i = 1; i < args.length; i++) {
            try {
                processSubmissionFile(documentPaths, args[i]);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
    }


    /**
     * Process a submission file, print information and write the file
     *
     * @param allFilePaths all file paths corresponding to the submission filename
     * @param filename the submission filename
     * @throws IOException if an IO error occurs
     */
    private static void processSubmissionFile(List<Path> allFilePaths, String filename) throws IOException {
        System.out.println("Processing " + filename + " submissions...");
        if (allFilePaths.size() < 2) {
            System.out.println("Cannot cluster a single file");
            return;
        }

        // Filter files to the relevant submissions
        List<Path> filePaths = DocumentUtils.filterPathsByFilename(allFilePaths, filename);
        MinHashMatrix.Builder matrixBuilder = MinHashMatrix.createBuilder();
        Map<Integer, String> documentIdStudentMap = new HashMap<>();

        // Read all relevant files
        for (int i = 0; i < filePaths.size(); i++) {
            displayLine(String.format("Reading files (%d/%d)", i, filePaths.size()));

            String document = new String(Files.readAllBytes(filePaths.get(i)));
            document = DocumentUtils.stripComments(document);
            Set<String> fileTerms = DocumentUtils.shingleDocument(NUM_NGRAM_TERMS, document);

            int documentId = matrixBuilder.addDocument(fileTerms);
            String studentName = DocumentUtils.leastCommonPathName(
                    filePaths.get(i),
                    filePaths.get((i + 1) % filePaths.size()));
            documentIdStudentMap.put(documentId, studentName);
        }

        // Build the matrix from read files
        displayLine(String.format("%d files read\n", filePaths.size()));
        System.out.println("Generating MinHash matrix...");
        MinHashMatrix matrix = matrixBuilder.build(NUM_PERMUTATIONS);
        clusterSubmissions(filename, documentIdStudentMap, matrix);
    }


    /**
     * Cluster the submissions using the minhash matrix and print results
     *
     * @param filename the submission filename
     * @param documentIdStudentMap map of document id to student name
     * @param matrix the MinHash matrix
     * @throws FileNotFoundException if a file cannot be written
     */
    private static void clusterSubmissions(String filename, Map<Integer, String> documentIdStudentMap, MinHashMatrix matrix) throws FileNotFoundException {
        System.out.println("Clustering Submissions...");
        List<Double> similaritySample = ProbabilityUtils.getTopSimilarities(matrix, matrix.getNumDocuments());
        double averageSimilarity = ProbabilityUtils.averageSample(similaritySample);
        double standardDeviation = ProbabilityUtils.standardDeviation(similaritySample);

        Map<String, List<SimilarityInfo>> clusters = new HashMap<>();

        for (int i = 0; i < matrix.getNumDocuments(); i++) {
            for (int j = 0; j < matrix.getNumDocuments(); j++) {
                if (i == j)
                    continue;

                double similarity = matrix.estimateJaccardSimilarity(i, j);
                if (similarity < averageSimilarity + (STD_FACTOR * standardDeviation))
                    continue;

                String studentName = documentIdStudentMap.get(i);
                SimilarityInfo info = new SimilarityInfo(documentIdStudentMap.get(j), similarity);
                if (!clusters.containsKey(studentName))
                    clusters.put(studentName, new ArrayList<>());

                clusters.get(studentName).add(info);
            }
        }

        System.out.format("%d submissions have suspicious similarity\n\n", clusters.size());

        PrintWriter outFile = new PrintWriter(new File(filename.replace(".java", "") + " Clusters.txt"));
        outFile.println(generateDescription(filename, clusters, averageSimilarity, standardDeviation));
        outFile.close();
    }


    /**
     * Create a summary of a submission file's clusters
     *
     * @param filename the submission file
     * @param clusters the clusters of similar documents
     * @param average the average max similarity found for a document
     * @param standardDeviation the standard deviation of maximum similarity found for a document
     * @return a string with the summary
     */
    private static String generateDescription(String filename, Map<String, List<SimilarityInfo>> clusters, double average, double standardDeviation) {
        StringBuilder out = new StringBuilder();
        out.append(filename + '\n');
        out.append(underlineString(filename) + '\n');
        out.append(String.format("Average Max Similarity: %.3f\n", average));
        out.append(String.format("Standard Deviation: %.3f\n\n\n", standardDeviation));


        for (String studentName : clusters.keySet()) {
            out.append(studentName + '\n');
            out.append(underlineString(studentName) + '\n');
            List<SimilarityInfo> similar = clusters.get(studentName);
            for (SimilarityInfo otherSubmission : similar) {
                out.append(String.format("%.3f %s\n", otherSubmission.similarity, otherSubmission.studentName));
            }
            out.append('\n');
        }

        return out.toString();
    }


    /**
     * Generates a string of hyphens matching the length of the given string
     *
     * @param input the given string
     * @return a string of hyphens
     */
    private static String underlineString(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            builder.append('-');
        }
        return builder.toString();
    }


    /**
     * Display a line of text on the console, backing over the previous
     *
     * @param text the text to display
     */
    private static void displayLine(String text) {
        System.out.print("\r");
        System.out.print(text);
    }


    /**
     * Structure containing document similarity information
     */
    private static class SimilarityInfo {
        public String studentName;
        public double similarity;

        public SimilarityInfo(String studentName, double similarity) {
            this.studentName = studentName;
            this.similarity = similarity;
        }
    }
}
