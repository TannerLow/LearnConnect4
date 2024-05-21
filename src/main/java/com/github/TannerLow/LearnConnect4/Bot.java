package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaML.Layer;
import com.github.TannerLow.JavaML.NeuralNet;
import com.github.TannerLow.JavaMatrixMath.Exceptions.DimensionsMismatchException;
import com.github.TannerLow.JavaMatrixMath.Matrix;

import java.util.Random;

public class Bot {
    private NeuralNet brain;
    private Genome genome;

    public Bot(NeuralNet brainStructure) {
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
        return genome;
    }

    public void initialize(Genome genome) throws DimensionsMismatchException {
        this.genome = genome.copy();

        int offset = 0;
        for(Layer layer : brain.layers) {
            Matrix weights = layer.getWeights();
            Matrix newWeights = new Matrix(weights.rows, weights.cols);

            for(; offset < newWeights.data.length; offset++) {
                newWeights.data[offset] = genome.getGene(offset);
            }

            layer.setWeights(newWeights);

            Matrix biases = layer.getBiases();
            Matrix newBiases = new Matrix(biases.rows, biases.cols);

            for(; offset < newBiases.data.length; offset++) {
                newBiases.data[offset] = genome.getGene(offset);
            }

            layer.setBiases(newBiases);
        }
    }

    public void mutate(double chancePerGeneMutation) {
        int numberOfMutations = (int) (chancePerGeneMutation * genome.length);
        Random random = new Random();
        for(int i = 0; i < numberOfMutations; i++) {
            int geneIndex = random.nextInt(genome.length);
            genome.mutateGene(geneIndex);
        }
    }
}
