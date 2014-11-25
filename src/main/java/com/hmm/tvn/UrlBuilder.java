package com.hmm.tvn;

public class UrlBuilder {

    private static final String API_SERVER_URL = "http://api.tvnplayer.pl/api/";
    private static final String FIXED_QUERY_PREFIX = "?platform=ConnectedTV&terminal=Samsung&format=json&v=2.0";
    private static final String SERVER_URL = API_SERVER_URL + FIXED_QUERY_PREFIX;
    private static final String QUERY_FOR_SERIES_EPISODES = "&type=series&id=%s&limit=1000&page=1&sort=newest&m=getItems";
    private static final String QUERY_FOR_EPISODE = "&authKey=ba786b315508f0920eca1c34d65534cd&type=episode&id=%s&m=getItem&deviceScreenHeight=1080&deviceScreenWidth=1920";

    public static String queryTemplateForAllEpisodes() {
        return SERVER_URL + QUERY_FOR_SERIES_EPISODES;
    }

    public static String queryTemplateForEpisodeUrl() {
        return SERVER_URL + QUERY_FOR_EPISODE;
    }
}
