package edu.iastate.cs.dream_killer;

import java.util.ArrayList;
import java.util.List;

/**
 * Various utilities involved in probabilistic aspects of the system
 *
 * @author Nick Gerleman
 */
public class ProbabilityUtils {


    /**
     * Gets top similarities for all documents
     *
     * @param matrix the minhash matrix of documents
     * @param numSamples the number of samples to perform
     * @return a sample of maximum document similarities
     */
    public static List<Double> getTopSimilarities(MinHashMatrix matrix, int numSamples) {
        List<Double> maxSimilarities = new ArrayList<>();

        for (int document = 0; document < numSamples; document++) {
            double maxSimilarity = 0;

            for (int otherDocument = 0; otherDocument < matrix.getNumDocuments(); otherDocument++) {
                if (document == otherDocument)
                    continue;

                maxSimilarity = Math.max(maxSimilarity, matrix.estimateJaccardSimilarity(document, otherDocument));
            }
            maxSimilarities.add(maxSimilarity);
        }

        return maxSimilarities;
    }


    /**
     * Calculate the average of a sample
     *
     * @param sample the smaple points
     * @return the average
     */
    public static double averageSample(List<Double> sample) {
        return sample.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .getAsDouble();
    }


    /**
     * Calculate the standard deviation of a sample
     *
     * @param sample the sample points
     * @return
     */
    public static double standardDeviation(List<Double> sample) {
        double average = averageSample(sample);

        double variance = 0;
        for (double samplePoint : sample) {
            double difference = samplePoint - average;
            variance += (difference * difference) / sample.size();
        }

        return Math.sqrt(variance);
    }

}
