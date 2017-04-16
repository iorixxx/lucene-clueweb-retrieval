package edu.anadolu.exp;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * ZRisk
 */
public class ZRisk {

    public static void addZRisk2Sheet(Sheet sheet) {

        int c = sheet.getRow(0).getLastCellNum();
        int r = sheet.getLastRowNum() + 1;


        sheet.getRow(0).createCell(0, CellType.STRING).setCellValue("ZRisk");

        final double[] rowSum = new double[r];
        Arrays.fill(rowSum, 0.0);

        final double[] columnSum = new double[c];
        Arrays.fill(columnSum, 0.0);

        double N = 0.0;

        for (int i = 1; i < r; i++) {

            Row row = sheet.getRow(i);

            for (int j = 2; j < c; j++) {

                if (row.getCell(j) == null) {
                    throw new RuntimeException("encountered null cell i=" + i + " j=" + j + " during geoZrisk addition");
                }
                final double cellValue = row.getCell(j).getNumericCellValue();

                rowSum[i] += cellValue;
                columnSum[j] += cellValue;
                N += cellValue;

                //System.out.print(String.format("%.4f", cellValue) + " ");
            }

            //System.out.println();
        }

//        System.out.println(Arrays.toString(rowSum));
//        System.out.println(Arrays.toString(columnSum));
//        System.out.println(N);

        for (int i = 1; i < r; i++) {

            Row row = sheet.getRow(i);
            double zRisk = 0.0;


            int counter = 0;
            for (int j = 2; j < c; j++) {

                final double cellValue = row.getCell(j).getNumericCellValue();
                final double e = rowSum[i] * columnSum[j] / N;

                //System.out.print(String.format("%.7f", e));
                //System.out.print("\t");

                counter++;

                zRisk += (cellValue - e) * (cellValue - e) / e;

            }

            //  System.out.println("\n");

            System.out.println(zRisk);
            row.createCell(0, CellType.NUMERIC).setCellValue(zRisk);

        }

    }

    public static void main(String[] args) throws Exception {

        Path p = Paths.get("/Users/iorixxx/spamSensivityData.xlsx");

        XSSFWorkbook workbook = new XSSFWorkbook(p.toFile());

        Sheet sheet = workbook.getSheet("ndcg100Data");

        addZRisk2Sheet(sheet);

        workbook.close();
    }
}
