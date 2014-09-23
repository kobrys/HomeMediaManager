package com.hmm;

import static java.lang.String.format;

public class PlayerStaticConfiguration {

    private static final String WORKING_URL = "http://api.tvnplayer.pl/api/?platform=ConnectedTV&terminal=Samsung&format=json&v=2.0&type=series&id=133&limit=20&page=1&sort=newest&m=getItems";
    private static final String WORKING_URL_2 = "http://api.tvnplayer.pl/api/?platform=ConnectedTV&terminal=Samsung&format=json&v=2.0&authKey=ba786b315508f0920eca1c34d65534cd&type=episode&id=32505&m=getItem&deviceScreenHeight=1080&deviceScreenWidth=1920";

    private static final String API_SERVER_URL = "http://api.tvnplayer.pl/api/";
    private static final String FIXED_QUERY_PREFIX = "?platform=ConnectedTV&terminal=Samsung&format=json&v=2.0";
    private static final String QUERY_FOR_SERIES_EPISODES = "&type=series&id=%s&limit=1000&page=1&sort=newest&m=getItems";
    private static final String QUERY_FOR_EPISODE = "&authKey=ba786b315508f0920eca1c34d65534cd&type=episode&id=%s&m=getItem&deviceScreenHeight=1080&deviceScreenWidth=1920";

    public static final String UGOTOWANI_ID = "133";
    public static final String MASTERCHEF_ID = "996";

    public static String getQueryForAllEpisodes(String seriesId) {
        return API_SERVER_URL + FIXED_QUERY_PREFIX + format(QUERY_FOR_SERIES_EPISODES, seriesId);
    }

    public static String getQueryForEpisodeUrl(String episodeId) {
        return API_SERVER_URL + FIXED_QUERY_PREFIX + format(QUERY_FOR_EPISODE, episodeId);
    }

}
