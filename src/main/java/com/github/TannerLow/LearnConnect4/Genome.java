package com.github.TannerLow.LearnConnect4;

import java.util.Random;

public class Genome {

    private final static float divisor = (float) (Short.MAX_VALUE + 1) / 4;
    private final short[] genes;
    public final int length;

    public Genome(int length) {
        this.length = length;
        genes = new short[length];
    }

    public float getGene(int index) {
        return genes[index] / divisor;
    }

    public void randomizeGenome() {
        Random random = new Random();
        for(int i = 0; i < genes.length; i++) {
            genes[i] = (short) random.nextInt(Short.MIN_VALUE, Short.MAX_VALUE + 1);
        }
    }

    public Genome copy() {
        Genome genomeCopy = new Genome(genes.length);
        System.arraycopy(genes, 0, genomeCopy.genes, 0, genes.length);
        return genomeCopy;
    }

    public void mutateGene(int geneIndex) {
        short gene = genes[geneIndex];
        Random random = new Random();
        int bitToFlip = random.nextInt(16);
        genes[geneIndex] = (short) (gene ^ (1 << bitToFlip));
    }

    public String getGeneAsString(int index) {
        return Integer.toHexString(Float.floatToIntBits(getGene(index)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("[length: ");
        sb.append(genes.length);
        sb.append("] ");

        for(int i = 0; i < genes.length; i++) {
            sb.append(Integer.toHexString(Float.floatToIntBits(getGene(i))));
            sb.append(' ');
        }

        return sb.toString();
    }
}
