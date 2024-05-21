package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaML.*;
import com.github.TannerLow.JavaMatrixMath.GPU;
import com.github.TannerLow.JavaMatrixMath.InternalFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("Testing");

        GPU gpu = new GPU();
        try (gpu){
            // Load GPU program code into memory
            String matricesKernelFilePath = "kernels/Matrices.cl";
            String matricesKernelCode = readFromInternalFile(matricesKernelFilePath);
            if(matricesKernelCode == null) {
                throw new IOException("Failed to read file: " + matricesKernelFilePath);
            }

            gpu.initialize(true);
            int programId = gpu.loadProgram(matricesKernelCode);
            gpu.loadKernel(programId, "Matrices", "matrixMultiply");
            gpu.loadKernel(programId, "Matrices", "addRowToRows");
            gpu.loadKernel(programId, "Matrices", "addColToCols");
            gpu.loadKernel(programId, "Matrices", "relu");
            gpu.loadKernel(programId, "Matrices", "horizontalSoftmax");
            gpu.loadKernel(programId, "Matrices", "verticalSoftmax");

            // Create empty brain layout
            NeuralNet brainStructure = new NeuralNet(84);
            Layer hiddenLayer1 = new DenseLayer(80, new Relu(), gpu);
            brainStructure.addLayer(hiddenLayer1);
            Layer hiddenLayer2 = new DenseLayer(80, new Relu(), gpu);
            brainStructure.addLayer(hiddenLayer2);
            Layer outputLayer = new DenseLayer(7, new Softmax(), gpu);
            brainStructure.addLayer(outputLayer);
            brainStructure.compile();

            File file = new File("output.tmp");
            file.createNewFile();
            PrintWriter pw = new PrintWriter(file);

            List<Bot> bots = new ArrayList<>();
            for(int i = 0; i < 1000; i++) {
                Bot bot = new Bot(brainStructure.copy());
                Genome genome = bot.generateGenome();
                // System.out.println(genome.getGeneAsString(0));
                pw.println(genome.getGeneAsString(0));
                bot.initialize(genome);
                bots.add(bot);
            }
            
            pw.close();
        }
    }

    private static String readFromInternalFile(String filepath) {
        try(InputStream fileInputStream = InternalFile.getInstance().getFileInputStream(filepath)) {
            byte[] bytes = fileInputStream.readAllBytes();
            String fileContent = new String(bytes, StandardCharsets.UTF_8);
            return fileContent;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}