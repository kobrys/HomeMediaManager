package com.hmm;

import static com.hmm.PlayerStaticConfiguration.*;
import static com.hmm.PlayerStaticConfiguration.UGOTOWANI_ID;
import static com.hmm.PlayerStaticConfiguration.getQueryForEpisodeUrl;

public class Main {

    public static void main(String[] args) {
//        System.out.println(getQueryForAllEpisodes(MASTERCHEF_ID));
//        System.out.println(new QueryExecutor().executeGet(getQueryForAllEpisodes(MASTERCHEF_ID)));
        System.out.println(new QueryExecutor().executeGet(getQueryForEpisodeUrl("32164")));
    }
}
