package org.clueweb09;

import edu.anadolu.Indexer;

/**
 * Record Interface for GOV2 Test Collection
 * http://ir.dcs.gla.ac.uk/test_collections/gov2-summary.htm
 */
public final class Gov2Record {

    private static final String DOCNO = "<DOCNO>";
    private static final String TERMINATING_DOCNO = "</DOCNO>";

    public static final String DOC = "<DOC>";
    public static final String TERMINATING_DOC = "</DOC>";

    private static final String DOCHDR = "<DOCHDR>";
    private static final String TERMINATING_DOCHDR = "</DOCHDR>";


    public static WarcRecord parseGov2Record(StringBuilder builder) {

        int i = builder.indexOf(DOCNO);
        if (i == -1) throw new RuntimeException("cannot find start tag " + DOCNO);

        if (i != 0) throw new RuntimeException("should start with " + DOCNO);

        int j = builder.indexOf(TERMINATING_DOCNO);
        if (j == -1) throw new RuntimeException("cannot find end tag " + TERMINATING_DOCNO);

        final String docID = builder.substring(i + DOCNO.length(), j).trim();

        i = builder.indexOf(DOCHDR);
        if (i == -1) throw new RuntimeException("cannot find header tag " + DOCHDR);

        j = builder.indexOf(TERMINATING_DOCHDR);
        if (j == -1) throw new RuntimeException("cannot find end tag " + TERMINATING_DOCHDR);

        if (j < i) throw new RuntimeException(TERMINATING_DOCHDR + " comes before " + DOCHDR);

        final String content = builder.substring(j + TERMINATING_DOCHDR.length()).trim();

        return new WarcRecord() {
            @Override
            public String id() {
                return docID;
            }

            @Override
            public String content() {
                return content;
            }

            @Override
            public String url() {
                return null;
            }

            @Override
            public String type() {
                return Indexer.RESPONSE;
            }

            @Override
            public void free() {
                builder.delete(0, builder.length());
            }
        };
    }
}
