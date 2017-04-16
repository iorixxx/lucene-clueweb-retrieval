package edu.anadolu.knn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by iorixxx on 5/1/16.
 */
class Rank {

    final String model;

    Rank(String model) {
        this.model = model;
    }

    int winRank = -1;
    int losRank = -1;

    private void check() {
        if (-1 == winRank || -1 == losRank)
            throw new RuntimeException(toString());
    }

    double s() {
        check();
        return winRank + losRank;
    }

    double e() {
        check();
        return winRank * losRank;
    }

    @Override
    public String toString() {
        return model + " w=" + winRank + " l=" + losRank + " e=" + e();
    }

    public static void main(String[] args) {
        List<Rank> ranks = new ArrayList<>();
        for (int win = 1; win <= 8; win++)
            for (int los = 1; los <= 8; los++) {
                Rank rank = new Rank("");
                rank.winRank = win;
                rank.losRank = los;
                ranks.add(rank);
            }

        ranks.sort((r1, r2) -> (int) Math.signum(r1.s() - r2.s()));
        Collections.reverse(ranks);
        System.out.println(ranks);

        ranks.sort((r1, r2) -> (int) Math.signum(r1.e() - r2.e()));
        Collections.reverse(ranks);
        System.out.println(ranks);

    }
}
