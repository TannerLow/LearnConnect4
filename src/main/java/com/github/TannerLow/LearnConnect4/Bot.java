package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaML.Layer;
import com.github.TannerLow.JavaML.NeuralNet;
import com.github.TannerLow.JavaMatrixMath.Exceptions.DimensionsMismatchException;
import com.github.TannerLow.JavaMatrixMath.Matrix;

import java.security.InvalidParameterException;
import java.util.Random;

public class Bot implements Comparable<Bot> {
    private NeuralNet brain;
    private Genome genome;
    private int score = 0;

    public Bot(NeuralNet brainStructure) throws InvalidParameterException {
        if(brainStructure == null) {
            throw new InvalidParameterException("brainStructure of Bot cannot be null initialized");
        }

        brain = brainStructure;
    }

    public Genome generateGenome() {
        int genesNeeded = 0;

        for(Layer layer : brain.layers) {
            genesNeeded += layer.getWeights().data.length;
            genesNeeded += layer.getBiases().data.length;
        }

        genome = new Genome(genesNeeded);
        genome.randomizeGenome();
        updateFromGenome();
        return genome;
    }

    public void initialize(Genome genome) throws DimensionsMismatchException {
        // TODO add dimension mismatch exception based on genome length
        this.genome = genome.copy();

        updateFromGenome();
    }

    private void updateFromGenome() {
        int offset = 0;
        for(Layer layer : brain.layers) {
            Matrix weights = layer.getWeights();
            Matrix newWeights = new Matrix(weights.rows, weights.cols);

            for(int i = 0; i < newWeights.data.length; i++) {
                newWeights.data[i] = genome.getGene(i + offset);
            }

            offset += newWeights.data.length;
            layer.setWeights(newWeights);

            Matrix biases = layer.getBiases();
            Matrix newBiases = new Matrix(biases.rows, biases.cols);

            for(int i = 0; i < newBiases.data.length; i++) {
                newBiases.data[i] = genome.getGene(i + offset);
            }

            offset += newBiases.data.length;
            layer.setBiases(newBiases);
        }
    }

    public Bot produceOffspring(double chancePerGeneMutation) {
        Bot bot = new Bot(brain.copy());
        bot.genome = genome.copy();
        bot.mutate(chancePerGeneMutation);
        return bot;
    }

    public void mutate(double chancePerGeneMutation) {
        int numberOfMutations = (int) (chancePerGeneMutation * genome.length);
        Random random = new Random();
        for(int i = 0; i < numberOfMutations; i++) {
            int geneIndex = random.nextInt(genome.length);
            genome.mutateGene(geneIndex);
        }
        updateFromGenome();
    }

    // TODO switch to int to simply represent which column to play
    public Matrix takeTurn(Matrix boardState) {
        return brain.predict(boardState);
    }

    public void givePoints(int additionalPoints) {
        score += additionalPoints;
    }

    public int getScore() {
        return score;
    }

    public Genome getGenome() {
        return genome;
    }

    // higher score = -1
    // equal score = 0
    // lower score = 1
    @Override
    public int compareTo(Bot other) {
        return other.score - score;
    }
}
