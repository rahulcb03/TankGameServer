package org.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MapConstructor {
    private static final int MAP_WIDTH = 80;
    private static final int MAP_HEIGHT = 60;
    public static void main(String[] args) {
        File map = new File("src/main/java/org/example/map.txt");

        try(FileWriter fw = new FileWriter(map)){
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder
                    .append("1".repeat(MAP_WIDTH))
                    .append("\n");


            for(int x = 0; x< 4; x++){

                for(int i =0; i<((MAP_HEIGHT-2)/4)-1; i++){

                    stringBuilder
                            .append("1")
                            .append("0".repeat(MAP_WIDTH - 2))
                            .append("1")
                            .append("\n");

                }
                if(x==3) continue;
                stringBuilder.append("1")
                        .append("0".repeat(10))
                        .append("1".repeat(MAP_WIDTH-22))
                        .append("0".repeat(10))
                        .append("1\n");
            }


            stringBuilder
                    .append("1")
                    .append("0".repeat(MAP_WIDTH - 2))
                    .append("1")
                    .append("\n");
            stringBuilder
                    .append("1")
                    .append("0".repeat(MAP_WIDTH - 2))
                    .append("1")
                    .append("\n");
            stringBuilder
                    .append("1")
                    .append("0".repeat(MAP_WIDTH - 2))
                    .append("1")
                    .append("\n");
            stringBuilder
                    .append("1".repeat(MAP_WIDTH))
                    .append("\n");

            fw.write(stringBuilder.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        };
    }
}
