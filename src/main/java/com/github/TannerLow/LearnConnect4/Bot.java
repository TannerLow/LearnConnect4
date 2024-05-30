package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaML.Layer;
import com.github.TannerLow.JavaML.NeuralNet;
import com.github.TannerLow.JavaMatrixMath.Exceptions.DimensionsMismatchException;
import com.github.TannerLow.JavaMatrixMath.Matrix;

import java.security.InvalidParameterException;
import java.util.Random;

public class Bot {
    private NeuralNet brain;
    private Genome genome;

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
        return genome;
    }

    public void initialize(Genome genome) throws DimensionsMismatchException {
        this.genome = genome.copy();

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

    public void mutate(double chancePerGeneMutation) {
        int numberOfMutations = (int) (chancePerGeneMutation * genome.length);
        Random random = new Random();
        for(int i = 0; i < numberOfMutations; i++) {
            int geneIndex = random.nextInt(genome.length);
            genome.mutateGene(geneIndex);
        }
    }

    // TODO switch to int to simply represnt which column to play
    public Matrix takeTurn(Matrix boardState) {
        return brain.predict(boardState);
    }
}
