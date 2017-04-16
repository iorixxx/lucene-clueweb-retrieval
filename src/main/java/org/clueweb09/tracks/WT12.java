package org.clueweb09.tracks;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * 2012 Web Track topics (query field only)
 * http://trec.nist.gov/data/web/12/queries.151-200.txt
 */
public class WT12 extends Track {

    @Override
    protected void populateInfoNeeds() throws IOException {
        populateInfoNeedsWT(Paths.get(home, "topics-and-qrels", "topics.web.151-200.txt"));
    }

    @Override
    protected void populateQRelsMap() throws IOException {
        populateQRelsMap(Paths.get(home, "topics-and-qrels", "qrels.web.151-200.txt"));
    }

    public WT12(String home) {
        super(home);
    }

    /**
     * top 10,000 documents for 2012 Web Track http://plg.uwaterloo.ca/~trecweb/2012.html
     *
     * @return 10, 000
     */
    @Override
    protected int getTopN() {
        return 10000;
    }

    private static final String[] wt12 = {

            "151:403b",
            //"152:angular cheilitis",
            "153:pocono",
            "154:figs",
            "155:last supper painting",
            "156:university of phoenix",
            "157:the beatles rock band",
            "158:septic system design",
            "159:porterville",
            "160:grilling",
            "161:furniture for small spaces",
            "162:dnr",
            "163:arkansas",
            "164:hobby stores",
            "165:blue throated hummingbird",
            "166:computer programming",
            "167:barbados",
            "168:lipoma",
            "169:battles in the civil war",
            "170:scooters",
            "171:ron howard",
            "172:becoming a paralegal",
            "173:hip fractures",
            "174:rock art",
            "175:signs of a heartattack",
            "176:weather strip",
            "177:best long term care insurance",
            "178:pork tenderloin",
            "179:black history",
            "180:newyork hotels",
            "181:old coins",
            "182:quit smoking",
            "183:kansas city mo",
            "184:civil right movement",
            "185:credit report",
            "186:unc",
            "187:vanuatu",
            "188:internet phone service",
            "189:gs pay rate",
            "190:brooks brothers clearance",
            "191:churchill downs",
            "192:condos in florida",
            "193:dog clean up bags",
            "194:designer dog breeds",
            "195:pressure washers",
            "196:sore throat",
            "197:idaho state flower",
            "198:indiana state fairgrounds",
            "199:fybromyalgia",
            "200:ontario california airport"
    };
}
