package edu.iastate.cs.dream_killer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MinHash Matrix using linear based universal hash functions for permutations
 *
 * @author Nick Gerleman
 */
public class MinHashMatrix {

    /** Immutable mapping of document id to the document's MinHash signature */
    private final List<List<Integer>> mDocumentSignatures;

    /** Number of permuataions in the MinHash matrix */
    private final int mNumPermutations;

    /**
     * Constructor used by the builder to create the matrix
     *
     * @param documentSignatures the document MinHash signatures of the matrix
     * @param numPermutations the number of permutations used when building the matrix
     */
    private MinHashMatrix(List<List<Integer>> documentSignatures, int numPermutations) {
        mDocumentSignatures = documentSignatures;
        mNumPermutations = numPermutations;
    }


    /**
     * Create a builder for the MinHash matrix
     *
     * @return the new Builder
     */
    public static Builder createBuilder() {
        return new Builder();
    }


    /**
     * Return the calculated signature for a given document id
     *
     * @param documentId the id of the document
     * @return the minhash signature for the document
     * @throws IllegalArgumentException if document id doesn't exist
     */
    public List<Integer> getSignatureForDocument(int documentId) {
        if (documentId >= mDocumentSignatures.size())
            throw new IllegalArgumentException("Invalid document id");

        return mDocumentSignatures.get(documentId);
    }


    /**
     * Get the number of permutations used to create the matrix
     *
     * @return the number of permutations
     */
    public int getNumPermutations() {
        return mNumPermutations;
    }


    /**
     * Get the number of documents in the matrix
     *
     * @return the number of documents
     */
    public int getNumDocuments() {
        return mDocumentSignatures.size();
    }


    /**
     * Approximates jaccard similarity between two documents
     *
     * @param document1 the id of the first document
     * @param document2 the id of the second document
     * @return the jaccard similarity in the range of 0..1
     */
    public double estimateJaccardSimilarity(int document1, int document2) {
        List<Integer> doc1Signature = getSignatureForDocument(document1);
        List<Integer> doc2Signature = getSignatureForDocument(document2);

        int numSame = 0;
        for (int i = 0; i < doc1Signature.size(); i++) {
            if (doc1Signature.get(i).equals(doc2Signature.get(i)))
                numSame++;
        }

        return (double)numSame / mNumPermutations;
    }


    /**
     * Builder to be used when constructing the matrix. This is used so that we
     * can easily create an immutable MinHash matrix.
     */
    public static class Builder {

        /** Mutable mapping of document id to the document's MinHash signature */
        private final List<Set<String>> mDocumentTerms;

        /** Set of all terms in all documents */
        private final Set<String> mTermSet;


        /**
         * Construct a new Builder
         */
        private Builder() {
            mDocumentTerms = new ArrayList<>();
            mTermSet = new HashSet<>();
        }


        /**
         * Add a new document to the builder
         *
         * @param terms all term inside of the document
         * @return a unique ID for the document
         */
        public int addDocument(Set<String> terms) {
            if (terms.isEmpty()) {
                throw new IllegalArgumentException("Cannot add empty document");
            }

            int id = mDocumentTerms.size();
            mDocumentTerms.add(terms);
            mTermSet.addAll(terms);
            return id;
        }


        /**
         * Build a MinHash matrix using the documents added to the builder
         *
         * @param  numPermutations the number of permutations to use in the matrix
         * @return the constructed matrix
         */
        public MinHashMatrix build(int numPermutations) {
            final Random randomGenerator = new Random();

            // Create the necessary number of permutation functions
            List<PermutationFunction> permutationFunctions = Stream
                    .generate(() -> new PermutationFunction(randomGenerator))
                    .limit(numPermutations)
                    .collect(Collectors.toList());


            // Create signatures for each document
            Map<String, Integer> termMap = mapTerms(mTermSet);
            List<List<Integer>> minHashSignatures = new ArrayList<>();

            for (Set<String> document : mDocumentTerms) {
                List<Integer> minHashSignature = permutationFunctions.stream()
                        .map((function) -> minHash(document, termMap, function))
                        .collect(Collectors.toList());

                minHashSignatures.add(Collections.unmodifiableList(minHashSignature));
            }

            return new MinHashMatrix(Collections.unmodifiableList(minHashSignatures), numPermutations);
        }


        /**
         * Map a set of terms to unique integers. This mapping is not
         * necessarily deterministic.
         *
         * @param terms a set of terms
         * @return the mapping of terms to integers
         */
        private static Map<String, Integer> mapTerms(Set<String> terms) {
            Map<String, Integer> termMap = new HashMap<>();

            int id = 0;
            for (String term : terms)
                termMap.put(term, id++);

            return termMap;
        }


        /**
         * Generate the minHash of a document using the given permutation function
         *
         * @param terms set of all terms in the document
         * @param termMap mapping of term to a unique integer
         * @param permutationFunction the permutation function to use
         * @return the minimum hashed term
         */
        private static int minHash(Set<String> terms, Map<String, Integer> termMap, PermutationFunction permutationFunction) {
            if (terms.isEmpty()) {
                throw new IllegalArgumentException("Cannot minhash empty list");
            }

            int min = Integer.MAX_VALUE;
            for (String term : terms) {
                int termId = termMap.get(term);
                min = Math.min(min, permutationFunction.permute(termId));
            }

            return min;
        }
    }


    /**
     * A "permutation" function based on uinversal hashing. This is actually
     * just a hash function but this works for our purposes
     */
    private static class PermutationFunction {
        /** Very large prime to use for universal hash function */
        private static final int PRIME = 2_147_483_647;

        /** Random scalar */
        private int mScalar;

        /** Random constant */
        private int mConstant;


        /**
         * Create a random permutation function
         *
         * @param randomGenerator a random number genenerator
         */
        public PermutationFunction(Random randomGenerator) {
            mScalar = randomGenerator.nextInt();
            mConstant = randomGenerator.nextInt();
        }


        /**
         * Return permutation position of a given number
         *
         * @param num the number to permute
         */
        public int permute(int num) {
            return ((num * mScalar) + mConstant) % PRIME;
        }
    }

}
