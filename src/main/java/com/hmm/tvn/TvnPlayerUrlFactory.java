package com.hmm.tvn;

import static com.hmm.tvn.UrlBuilder.queryTemplateForAllEpisodes;
import static com.hmm.tvn.UrlBuilder.queryTemplateForEpisodeUrl;
import static java.lang.String.format;

public class TvnPlayerUrlFactory {

    public String queryForAllEpisodesUrl(String seriesId) {
        return format(queryTemplateForAllEpisodes(), seriesId);
    }

    public String queryForEpisodeUrl(String episodeId) {
        return format(queryTemplateForEpisodeUrl(), episodeId);
    }
}
