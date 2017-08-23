package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 2009 Web Track topics (query field only)
 * http://trec.nist.gov/data/web/09/wt09.topics.queries-only
 */
public class WT09 extends Track {


    public WT09(String home) {
        super(home);
    }

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWT(Paths.get(home, "topics-and-qrels", "topics.web.1-50.txt"));
    }

    @Override
    protected void populateQRelsMap() throws Exception {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "prels.web.1-50.txt"));
    }

    @Override
    protected Triple processQRelLine(String line) {
        String[] parts = whiteSpaceSplitter.split(line);

        assert parts.length == 5 : "prels file should contain five columns : " + line;

        int queryID = Integer.parseInt(parts[0]);
        String docID = parts[1];
        int judge = Integer.parseInt(parts[2]);

        return new Triple(queryID, docID, judge);
    }


    private static final String[] wt09 = {

            "wt09-1:obama family tree",
            "wt09-2:french lick resort and casino",
            "wt09-3:getting organized",
            "wt09-4:toilet",
            "wt09-5:mitchell college",
            "wt09-6:kcs",
            "wt09-7:air travel information",
            "wt09-8:appraisals",
            "wt09-9:used car parts",
            "wt09-10:cheap internet",
            "wt09-11:gmat prep classes",
            "wt09-12:djs",
            "wt09-13:map",
            "wt09-14:dinosaurs",
            "wt09-15:espn sports",
            "wt09-16:arizona game and fish",
            "wt09-17:poker tournaments",
            "wt09-18:wedding budget calculator",
            "wt09-19:the current",
            //"wt09-20:defender",
            "wt09-21:volvo",
            "wt09-22:rick warren",
            "wt09-23:yahoo",
            "wt09-24:diversity",
            "wt09-25:euclid",
            "wt09-26:lower heart rate",
            "wt09-27:starbucks",
            "wt09-28:inuyasha",
            "wt09-29:ps 2 games",
            "wt09-30:diabetes education",
            "wt09-31:atari",
            "wt09-32:website design hosting",
            "wt09-33:elliptical trainer",
            "wt09-34:cell phones",
            "wt09-35:hoboken",
            "wt09-36:gps",
            "wt09-37:pampered chef",
            "wt09-38:dogs for adoption",
            "wt09-39:disneyland hotel",
            "wt09-40:michworks",
            "wt09-41:orange county convention center",
            "wt09-42:the music man",
            "wt09-43:the secret garden",
            "wt09-44:map of the united states",
            "wt09-45:solar panels",
            "wt09-46:alexian brothers hospital",
            "wt09-47:indexed annuity",
            "wt09-48:wilson antenna",
            "wt09-49:flame designs",
            "wt09-50:dog heat"
    };
}
