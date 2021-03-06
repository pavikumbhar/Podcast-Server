package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.PodcastAssert;
import lan.dk.podcastserver.service.HtmlService;
import lan.dk.podcastserver.service.ImageService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 18/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class BeInSportsFinderTest {

    @Mock HtmlService htmlService;
    @Mock ImageService imageService;
    @InjectMocks BeInSportsFinder beInSportsFinder;

    @Test
    public void should_find_podcast() throws IOException, URISyntaxException {
        /* Given */
        Cover cover = new Cover("https://images.beinsports.com/_04REUK9dN14HyrE2659T4C9zxQ=/670x424/smart/302352-Capture.PNG", 200, 200);
        when(imageService.getCoverFromURL(eq("https://images.beinsports.com/_04REUK9dN14HyrE2659T4C9zxQ=/670x424/smart/302352-Capture.PNG"))).thenReturn(cover);
        when(htmlService.get(eq("http://www.beinsports.com/france/replay/lexpresso"))).thenReturn(readFile("/remote/podcast/beinsports/lexpresso.html"));

        /* When */
        Podcast podcast = beInSportsFinder.find("http://www.beinsports.com/france/replay/lexpresso");

        /* Then */
        PodcastAssert
                .assertThat(podcast)
                .hasUrl("http://www.beinsports.com/france/replay/lexpresso")
                .hasTitle("L'Expresso")
                .hasType("BeInSports")
                .hasCover(cover);
    }

    @Test
    public void should_be_compatible() {
        assertThat(beInSportsFinder.compatibility("http://www.beinsports.com/france/replay/lexpresso")).isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(beInSportsFinder.compatibility("http://www.foo.com/bar/folder")).isGreaterThan(1);
    }
    
    public static Optional<Document> readFile(String uri) throws URISyntaxException, IOException {
        return Optional.of(Jsoup.parse(Paths.get(YoutubeFinderTest.class.getResource(uri).toURI()).toFile(),"UTF-8"));
    }

}