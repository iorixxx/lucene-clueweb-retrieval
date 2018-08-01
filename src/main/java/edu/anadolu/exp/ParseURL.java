package edu.anadolu.exp;

import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.net.URL;

/**
 * https://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
 */
public class ParseURL {

    public static void main(String[] args) throws Exception {

        int i =20;
        for (int spamThreshold = i; spamThreshold < 100; spamThreshold += i) {
            System.out.println(spamThreshold);
        }

        URL aURL = new URL("http://example.com:80/docs/books/tutorial"
                + "/index.html?name=networking&key=value#DOWNLOADING");

        System.out.println("protocol = " + aURL.getProtocol());
        System.out.println("authority = " + aURL.getAuthority());
        System.out.println("host = " + aURL.getHost());
        System.out.println("port = " + aURL.getPort());
        System.out.println("path = " + aURL.getPath());
        System.out.println("query = " + aURL.getQuery());
        System.out.println("filename = " + aURL.getFile());
        System.out.println("ref = " + aURL.getRef());

        int arr1[] = {2, 3, 4, 5, 7, 8, 9, 10};
        int arr2[] = {2, 3, 6};
        int m = arr1.length;
        int n = arr2.length;
        printDifference(arr1, arr2, m, n);

        int a[] = {1, 2, 3, 4, 5};
        int b[] = {3, 4, 5, 6, 7, 8};

        printDifference(a, b, a.length, b.length);

        int x[] = {1, 2, 3, 4, 5};
        int y[] = {};

        printDifference(x, y, x.length, y.length);
    }


    /**
     * The difference of two posting lists, written A - B is the set of all elements of A that are not elements of B.
     * <p>
     * https://www.thoughtco.com/difference-of-two-sets-3126580
     *
     * @param first  universal set
     * @param second negative clause
     * @return complement of second
     * @throws IOException if any
     */
    private static int differenceOfTwoPostings(PostingsEnum first, PostingsEnum second) throws IOException {

        int count = 0;
        int firstDocId = first.nextDoc();
        int secondDocId = second.nextDoc();
        // We are assuming that docEnum are in doc id order
        // According to Lucene documentation, the doc ids are in non decreasing order
        while (true) {

            if (firstDocId == PostingsEnum.NO_MORE_DOCS || secondDocId == PostingsEnum.NO_MORE_DOCS) {
                break;
            }
            if (firstDocId < secondDocId) {

                count++;

                final int freq = first.freq();
                if (freq != 1) throw new RuntimeException("artificial term frequency should be 1! " + freq);
                final int docID = first.docID();

                // get document length from this docID

                firstDocId = first.nextDoc();

            } else if (firstDocId > secondDocId) {

                secondDocId = second.nextDoc();

            } else {

                firstDocId = first.nextDoc();
                secondDocId = second.nextDoc();
            }
        }


        while (firstDocId != PostingsEnum.NO_MORE_DOCS) {

            count++;

            final int freq = first.freq();
            if (freq != 1) throw new RuntimeException("artificial term frequency should be 1! " + freq);
            final int docID = first.docID();

            // get document length from this docID

            firstDocId = first.nextDoc();
        }

        return count;

    }


    /* Function prints all elements of A that are not elements of B. */
    static void printDifference(int first[], int second[], int m, int n) {
        int i = 0, j = 0;
        while (i < m && j < n) {
            if (first[i] < second[j]) {
                System.out.println(first[i]);
                i++;
            } else if (second[j] < first[i]) {
                j++;
            } else {
                j++;
                i++;
            }
        }

        System.out.println("larger");
        /* Print remaining elements of the larger array */
        while (i < m) {
            System.out.println(first[i] + " ");
            i++;

        }

        System.out.println("end");
    }
}
