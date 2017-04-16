package edu.anadolu.exp;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * /dev/urandom utility for random number generation, taken from Lucene's StringHelper.java
 */
public class URandom {

    public static void main(String[] args) {
        try (DataInputStream is = new DataInputStream(Files.newInputStream(Paths.get("/dev/urandom")))) {
            for (int i = 0; i < 10; i++)
                System.out.println(is.readInt());

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
