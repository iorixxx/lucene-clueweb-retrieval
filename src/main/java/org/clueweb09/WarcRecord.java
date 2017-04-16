package org.clueweb09;

/**
 * Common Interface for both ClueWeb09 and ClueWeb12 Warc Record Types
 */
public interface WarcRecord {

    String id();

    String content();

    String url();

    String type();
}
