package com.github.TannerLow.LearnConnect4;

import com.github.TannerLow.JavaML.*;
import com.github.TannerLow.JavaMatrixMath.GPU;
import com.github.TannerLow.JavaMatrixMath.InternalFile;
import com.github.TannerLow.JavaMatrixMath.Matrix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
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
            Layer hiddenLayer1 = new DenseLayer(80, new Relu());// , gpu);
            brainStructure.addLayer(hiddenLayer1);
            Layer hiddenLayer2 = new DenseLayer(80, new Relu());// , gpu);
            brainStructure.addLayer(hiddenLayer2);
            Layer outputLayer = new DenseLayer(7, new Softmax());
            brainStructure.addLayer(outputLayer);
            brainStructure.compile();


            final int POPULATION = 250;

            List<Bot> bots = new ArrayList<>();
            for(int i = 0; i < POPULATION; i++) {
                Bot bot = new Bot(brainStructure.copy());
                bot.generateGenome();
                // System.out.println(genome.getGeneAsString(0));
                // pw.println(genome.getGeneAsString(0));
                //bot.initialize(genome);
                bots.add(bot);
            }

            Matrix input = new Matrix(85, 3);
            bots.get(0).takeTurn(input);
            System.out.println(bots.get(0).getGenome().getGene(0));

            //playAgainstBot(bots.get(0));
            //playBotVsBotVerbose(bots.get(0), bots.get(1));

//            pw.println(bots.get(0).getGenome());
//            Bot botBaby = bots.get(0).produceOffspring(1/1000.0);
//            pw.println(botBaby.getGenome());

            for(int i = 0; i < 500; i++) {
                System.out.println("Epoch: " + i);
                runEpochVsAll(bots);
            }

            Collections.sort(bots);
            playBotVsBotVerbose(bots.get(0), bots.get(1));
        }
    }

    private static void runEpochVsAll(List<Bot> bots) {
        long startTime = System.currentTimeMillis();

        // reset scores
        for(Bot bot : bots) {
            bot.givePoints(-bot.getScore());
        }

//        System.out.println("scores: ");
//        for(Bot bot : bots) {
//            System.out.print(bot.getScore() + " ");
//        }

        // play and score
        long startTime_pns = System.currentTimeMillis();
        for(int i = 0; i < bots.size(); i++) {
            for(int j = i+1; j < bots.size(); j++) {
                playBotVsBot(bots.get(i), bots.get(j));
            }
        }
        System.out.println(((System.currentTimeMillis() - startTime_pns) / 1000.0) + "s elapsed during play and score");


        // compute average
        double averageScore = 0;
        for(Bot bot : bots) {
            averageScore += bot.getScore();
        }
        System.out.println("Average score: " + averageScore / bots.size());

        // rank and kill
        Collections.sort(bots);
        bots.subList(bots.size() / 2, bots.size()).clear();

        // reproduce
        //long startTime_r = System.currentTimeMillis();
        int population = bots.size();
        double mutationChance = 5/1000.0;
        for(int i = 0; i < population; i++) {
            bots.add(bots.get(i).produceOffspring(mutationChance));
        }
        //System.out.println(((System.currentTimeMillis() - startTime_r) / 1000.0) + "s elapsed during reproduction");

        Collections.shuffle(bots);

        System.out.println(((System.currentTimeMillis() - startTime) / 1000.0) + "s elapsed during epoch");
    }

    private static void runEpochVsRandom(List<Bot> bots) {
        // reset scores
        for(Bot bot : bots) {
            bot.givePoints(-bot.getScore());
        }

        // play and score
        for(int i = 0; i < bots.size(); i+=2) {
            playBotVsBot(bots.get(i), bots.get(i+1));
        }

        // compute average
        double averageScore = 0;
        for(Bot bot : bots) {
            averageScore += bot.getScore();
        }
        System.out.println("Average score: " + averageScore / bots.size());

        // rank and kill
        Collections.sort(bots);
        bots.subList(bots.size() / 2, bots.size()).clear();

        // reproduce
        int population = bots.size();
        double mutationChance = 5/1000.0;
        for(int i = 0; i < population; i++) {
            bots.add(bots.get(i).produceOffspring(mutationChance));
        }

        Collections.shuffle(bots);
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

    public static void playBotVsBot(Bot botA, Bot botB) {
        //long startTime = System.currentTimeMillis();

        final int MAX_INVALID_MOVES = 10;

        Connect4 game = new Connect4();
        Bot[] bots = {botA, botB};
        while(game.isContinuable()) {
            Bot bot;
            int botIndex = 0;
            if(game.getCurrentPlayer() == Player.Player2) {
                botIndex = 1;
            }
            bot = bots[botIndex];

            int invalidMoves = 0;
            while(invalidMoves < MAX_INVALID_MOVES) {
                //long startTime_tt = System.currentTimeMillis();
                Matrix decisionMatrix = bot.takeTurn(game.getEncodedGameState(game.getCurrentPlayer()));
                //System.out.println(((System.currentTimeMillis() - startTime_tt)) + "ms elapsed during take turn");

                // use decision matrix to make a play
                int column = maxValueIndex(decisionMatrix);
                if(!game.play(column)) {
                    invalidMoves++;
                }
                else {
                    bot.givePoints(-invalidMoves); // -1 for each attempt to make invalid move
                    bot.givePoints(1); // +1 for valid move
                    break;
                }
            }

            if(invalidMoves == MAX_INVALID_MOVES) {
                bot.givePoints(-1000);
                if(game.getCurrentPlayer() == Player.Player1) {
                    game.defaultTheWinner(Player.Player2);
                }
                else if(game.getCurrentPlayer() == Player.Player2) {
                    game.defaultTheWinner(Player.Player1);
                }
            }
        }

        if (game.getWinner() == Player.Player1) {
            botA.givePoints(game.getNumberOfOpenSpots());
        }
        else if(game.getWinner() == Player.Player2) {
            botB.givePoints(game.getNumberOfOpenSpots());
        }

        //System.out.println(((System.currentTimeMillis() - startTime)) + "ms elapsed during bot vs bot game");
    }

    public static void playBotVsBotVerbose(Bot botA, Bot botB) {
        final int MAX_INVALID_MOVES = 20;

        Connect4 game = new Connect4();
        Bot[] bots = {botA, botB};
        while(game.isContinuable()) {
            Bot bot;
            int botIndex = 0;
            if(game.getCurrentPlayer() == Player.Player2) {
                botIndex = 1;
            }
            bot = bots[botIndex];

            System.out.println(game);

            int invalidMoves = 0;
            while(invalidMoves < MAX_INVALID_MOVES) {
                Matrix decisionMatrix = bot.takeTurn(game.getEncodedGameState(game.getCurrentPlayer()));

                System.out.println("Bot Decision matrix:");
                for(float f : decisionMatrix.data) {
                    System.out.print(f + " ");
                }
                System.out.println();

                // use decision matrix to make a play
                int column = maxValueIndex(decisionMatrix);
                System.out.println("Bot Choice: " + column);
                if(!game.play(column)) {
                   invalidMoves++;
                }
                else {
                    bot.givePoints(-invalidMoves); // -1 for each attempt to make invalid move
                    bot.givePoints(1); // +1 for valid move
                    break;
                }
            }

            if(invalidMoves == MAX_INVALID_MOVES) {
                bot.givePoints(-1000);
                if(game.getCurrentPlayer() == Player.Player1) {
                    game.defaultTheWinner(Player.Player2);
                }
                else if(game.getCurrentPlayer() == Player.Player2) {
                    game.defaultTheWinner(Player.Player1);
                }
            }
        }

        System.out.println("Winner: " + game.getWinner());
        if (game.getWinner() == Player.Player1) {
            botA.givePoints(game.getNumberOfOpenSpots());
        }
        else if(game.getWinner() == Player.Player2) {
            botB.givePoints(game.getNumberOfOpenSpots());
        }
        System.out.println("Bot A score: " + botA.getScore());
        System.out.println("Bot B score: " + botB.getScore());

        System.out.println(game);
    }

    public static int maxValueIndex(Matrix decisionMatrix) {
        int maxIndex = -1;
        float maxValue = -1e9f;

        for(int i = 0; i < decisionMatrix.data.length; i++) {
            float value = decisionMatrix.data[i];
            if(value > maxValue) {
                maxIndex = i;
                maxValue = value;
            }
        }

        return maxIndex;
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

                    // use decision matrix to make a play
                    int maxIndex = -1;
                    float maxValue = -1e9f;
                    for(int i = 0; i < decisionMatrix.data.length; i++) {
                        float value = decisionMatrix.data[i];
                        if(value > maxValue) {
                            maxIndex = i;
                            maxValue = value;
                        }
                    }
                    System.out.println("Bot Choice: " + maxIndex);
                    game.play(maxIndex);

                    break;
                }
            }
            else {
                System.out.println(game);
                System.out.print("Choose column: ");
                int choice = scanner.nextInt();
                System.out.println(game.play(choice));
            }
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