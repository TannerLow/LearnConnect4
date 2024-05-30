package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaML.*;
import com.github.TannerLow.JavaMatrixMath.GPU;
import com.github.TannerLow.JavaMatrixMath.InternalFile;
import com.github.TannerLow.JavaMatrixMath.Matrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        // playConnect4Manually();
        System.out.println("Testing");

        File file = new File("output.tmp");
        file.createNewFile();

        GPU gpu = new GPU();
        try (gpu; PrintWriter pw = new PrintWriter(file)){
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
            NeuralNet brainStructure = new NeuralNet(85);
            Layer hiddenLayer1 = new DenseLayer(80, new Relu(), gpu);
            brainStructure.addLayer(hiddenLayer1);
            Layer hiddenLayer2 = new DenseLayer(80, new Relu(), gpu);
            brainStructure.addLayer(hiddenLayer2);
            Layer outputLayer = new DenseLayer(7, new Softmax());
            brainStructure.addLayer(outputLayer);
            brainStructure.compile();

            List<Bot> bots = new ArrayList<>();
            for(int i = 0; i < 1000; i++) {
                Bot bot = new Bot(brainStructure.copy());
                Genome genome = bot.generateGenome();
                // System.out.println(genome.getGeneAsString(0));
                // pw.println(genome.getGeneAsString(0));
                bot.initialize(genome);
                bots.add(bot);
            }

            Matrix input = new Matrix(85, 3);
            bots.get(0).takeTurn(input);

            playAgainstBot(bots.get(0));
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

    public static void playAgainstBot(Bot bot) {
        Connect4 game = new Connect4();
        Scanner scanner = new Scanner(System.in);
        while(game.isContinuable()){
            if(game.getCurrentPlayer() == Player.Player1) {
                while(true) {
                    System.out.println("Board state before bot plays:");
                    for(float f : game.getEncodedGameState(Player.Player1).data) {
                        System.out.print(f + " ");
                    }
                    System.out.println();
                    Matrix decisionMatrix = bot.takeTurn(game.getEncodedGameState(Player.Player1));
                    System.out.println("Decision matrix:");
                    for(float f : decisionMatrix.data) {
                        System.out.print(f + " ");
                    }
                    System.out.println();
                    break;
                }
            }

            System.out.println(game);
            System.out.print("Choose column: ");
            int choice = scanner.nextInt();
            System.out.println(game.play(choice));
        }
        System.out.print(game);
        System.out.println("Winner: " + game.getWinner());
        scanner.close();
    }

    public static void playConnect4Manually() {
        Connect4 game = new Connect4();
        Scanner scanner = new Scanner(System.in);
        while(game.isContinuable()){
            System.out.println(game);
            System.out.print("Choose column: ");
            int choice = scanner.nextInt();
            System.out.println(game.play(choice));
        }
        System.out.print(game);
        System.out.println("Winner: " + game.getWinner());
        scanner.close();
    }
}


//int trials = 13847*16;
//int occurances = 12;
//MathContext mc = new MathContext(100, RoundingMode.HALF_UP) ;
//BigDecimal comb = combination(trials, occurances);
//BigDecimal p = BigDecimal.ONE.divide(new BigDecimal(16000));
//BigDecimal chance = comb.multiply(p.pow(occurances), mc).multiply(BigDecimal.ONE.subtract(p).pow(trials-occurances), mc);
//
//        System.out.println(chance);
//    }
//
//public static BigDecimal combination(int n, int k) {
//    BigDecimal product = BigDecimal.ONE;
//    for(long i = 1; i <= k; i++) {
//        product = product.multiply(new BigDecimal(n + 1 - i).divide(new BigDecimal(i), 200, RoundingMode.HALF_UP));
//    }
//    return product.setScale(0, RoundingMode.HALF_UP);
//}