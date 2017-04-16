package edu.anadolu.exp;

import edu.anadolu.freq.TFNormalization;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Insert verbose frequency distribution information into MySQL tables.
 */
public final class MySQL {

    static final String CREATE_TABLE = "CREATE TABLE `%s`.`w_%s` ( `value` DOUBLE NOT NULL , `count` INT NOT NULL , PRIMARY KEY (`value`)) ENGINE = InnoDB;";

    static final String DROP_TABLE = "DROP TABLE IF exists `%s`.`w_%s`;";

    static final String INSERT = "INSERT INTO `%s`.`w_%s` (value, count) VALUES (?,?)";

    static final String CREATE_DATABASE = "CREATE DATABASE `%s`;";

    static final String DROP_DATABASE = "DROP DATABASE IF exists `%s`;";


    public static void dump(String home, String tag, TFNormalization tfNormalization) throws Exception {

        final String dbName = tag + tfNormalization.toString();

        Class.forName("com.mysql.jdbc.Driver");
        final Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "");

        conn.createStatement().execute(String.format(DROP_DATABASE, dbName));
        conn.createStatement().execute(String.format(CREATE_DATABASE, dbName));


        Path path = Paths.get(home, "verbose_freqs", tag, tfNormalization.toString());

        final List<Path> paths = Files.walk(path)
                .filter(Files::isRegularFile)
                .collect(Collectors.toList());


        for (Path filePath : paths) {
            String drop = String.format(DROP_TABLE, dbName, filePath.getFileName().toString());
            conn.createStatement().execute(drop);

            String create = String.format(CREATE_TABLE, dbName, filePath.getFileName().toString());
            conn.createStatement().execute(create);

            System.out.println(filePath.getFileName());

            PreparedStatement insertStatement = conn.prepareStatement(String.format(INSERT, dbName, filePath.getFileName().toString()));

            long cdf = 0;
            try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.US_ASCII)) {

                for (; ; ) {
                    String line = reader.readLine();
                    if (line == null)
                        break;

                    if ("value\tcount".equals(line)) continue;

                    String[] parts = line.split("\t");
                    if (parts.length != 2) throw new RuntimeException("parts length is not 2: " + line);


                    final double value = Double.parseDouble(parts[0]);
                    final long count = Long.parseLong(parts[1]);
                    cdf += count;

                    insertStatement.setDouble(1, value);
                    insertStatement.setLong(2, cdf);
                    insertStatement.executeUpdate();

                }

                insertStatement.close();

            }
        }

        conn.close();

    }
}
