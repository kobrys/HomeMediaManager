package com.hmm.tvn;

import org.junit.Assert;
import org.junit.Test;

import static com.hmm.tvn.UrlBuilder.*;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;

public class TvnPlayerUrlFactoryTest {

    private static final String SERIES_ID = "seriesId";
    private static final String EPISODE_ID = "episodeId";

    private TvnPlayerUrlFactory urlFactory = new TvnPlayerUrlFactory();

    @Test
    public void shouldBuildQueryForAllEpisodes() {
        String url = urlFactory.queryForAllEpisodesUrl(SERIES_ID);

        assertEquals(url, format(queryTemplateForAllEpisodes(), SERIES_ID));
    }

    @Test
    public void shouldBuildQueryForEpisodeUrl() {
        String url = urlFactory.queryForEpisodeUrl(EPISODE_ID);

        assertEquals(url, format(queryTemplateForEpisodeUrl(), EPISODE_ID));
    }
}