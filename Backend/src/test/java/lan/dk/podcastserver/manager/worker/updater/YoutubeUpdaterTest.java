package lan.dk.podcastserver.manager.worker.updater;

import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.properties.Api;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 30/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class YoutubeUpdaterTest {

    @Mock Api api;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SignatureService signatureService;
    @Mock Validator validator;
    @Mock JdomService jdomService;
    @Mock JsonService jsonService;
    @Mock HtmlService htmlService;
    @Mock UrlService urlService;
    @InjectMocks YoutubeUpdater youtubeUpdater;

    @Before
    public void beforeEach() {
        when(api.getYoutube()).thenReturn("");
        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL(((String) i.getArguments()[0]))));
    }

    @Test
    public void should_be_of_type_youtube() {
        /* Given */
        AbstractUpdater.Type type = youtubeUpdater.type();

        /* Then */
        assertThat(type).isNotNull();
        assertThat(type.key()).isEqualTo("Youtube");
        assertThat(type.name()).isEqualTo("Youtube");
    }

    @Test
    public void should_get_items_for_channel() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();
        when(htmlService.get(any(String.class))).thenReturn(Optional.of(parseHtml("/remote/podcast/youtube/androiddevelopers.html")));
        when(jdomService.parse(any(URL.class))).then(invocationOnMock -> Optional.of(parseXml("/remote/podcast/youtube.androiddevelopers.xml")));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(15);
        verify(jdomService, only()).parse(eq(new URL("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg")));
        verify(htmlService, only()).get(eq("https://www.youtube.com/user/androiddevelopers"));
    }


    @Test
    public void should_get_items_for_playlist() throws IOException, JDOMException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/playlist?list=PLAD454F0807B6CB80")
                .build();

        when(jdomService.parse(any(URL.class))).thenReturn(Optional.of(parseXml("/remote/podcast/youtube/joueurdugrenier.playlist.xml")));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(15);
        verify(jdomService, only()).parse(eq(new URL("https://www.youtube.com/feeds/videos.xml?playlist_id=PLAD454F0807B6CB80")));
    }

    @Test
    public void should_generate_signature() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        when(htmlService.get(any(String.class))).thenReturn(Optional.of(parseHtml("/remote/podcast/youtube/androiddevelopers.html")));
        when(jdomService.parse(any(URL.class))).thenReturn(Optional.of(parseXml("/remote/podcast/youtube.androiddevelopers.xml")));
        when(signatureService.generateMD5Signature(anyString())).thenReturn("Signature");

        /* When */
        String signature = youtubeUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("Signature");
        verify(jdomService, only()).parse(eq(new URL("https://www.youtube.com/feeds/videos.xml?channel_id=UCVHFbqXqoYvEWM1Ddxl0QDg")));
        verify(htmlService, only()).get(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    @Test
    public void should_handle_error_during_signature() throws IOException, JDOMException, URISyntaxException {
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        when(htmlService.get(any(String.class))).thenReturn(Optional.of(parseHtml("/remote/podcast/youtube/androiddevelopers.html")));
        when(jdomService.parse(any(URL.class))).thenReturn(Optional.empty());


        /* When */
        String signature = youtubeUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEmpty();
    }

    @Test
    public void should_return_empty_if_parsing_error() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/feeds/videos.xml?playlist_id=PLYMLK0zkSFQTblsW2biu2m4suKvoomN5D")
                .build();

        when(htmlService.get(any(String.class))).thenReturn(Optional.empty());
        when(jdomService.parse(any(URL.class))).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    @Test
    public void should_return_empty_set_because_html_page_not_found() throws IOException, JDOMException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        when(htmlService.get(any(String.class))).thenReturn(Optional.empty());
        when(jdomService.parse(eq(new URL("https://www.youtube.com/feeds/videos.xml?channel_id=")))).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    @Test
    public void should_return_empty_set_because_of_data_tag_not_find() throws JDOMException, IOException, URISyntaxException {
        /* Given */
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .build();

        when(htmlService.get(any(String.class))).thenReturn(Optional.empty());
        when(jdomService.parse(eq(new URL("https://www.youtube.com/feeds/videos.xml?channel_id=")))).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
        verify(jdomService, only()).parse(eq(new URL("https://www.youtube.com/feeds/videos.xml?channel_id=")));
        verify(htmlService, only()).get(eq("https://www.youtube.com/user/androiddevelopers"));
    }

    @Test
    public void should_get_items_with_API_from_channel() throws IOException, URISyntaxException, ParseException {
        /* Given */
        URL PAGE_1 = new URL("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO");
        URL PAGE_2 = new URL("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=UU_yP2DpIgs5Y1uWC0T03Chw&key=FOO&pageToken=CDIQAA");
        when(api.getYoutube()).thenReturn("FOO");
        Podcast podcast = Podcast.builder().url("https://www.youtube.com/user/joueurdugrenier").items(Sets.newHashSet()).build();
        when(jsonService.from(eq(PAGE_1))).thenReturn(Optional.of(parseJson("/remote/podcast/youtube/joueurdugrenier.json")));
        when(jsonService.from(eq(PAGE_2))).thenReturn(Optional.of(parseJson("/remote/podcast/youtube/joueurdugrenier.2.json")));
        when(htmlService.get(eq(podcast.getUrl()))).thenReturn(Optional.of(parseHtml("/remote/podcast/youtube/joueurdugrenier.html")));

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(87);
    }


    @Test
    public void should_failed_during_fetch_API() throws JDOMException, IOException, URISyntaxException, ParseException {
        /* Given */
        when(api.getYoutube()).thenReturn("FOO");
        Podcast podcast = Podcast.builder()
                .url("https://www.youtube.com/user/androiddevelopers")
                .items(Sets.newHashSet())
                .build();

        when(htmlService.get(any(String.class))).thenReturn(Optional.of(parseHtml("/remote/podcast/youtube/androiddevelopers.html")));
        when(jsonService.from(any(URL.class))).thenReturn(Optional.empty());

        /* When */
        Set<Item> items = youtubeUpdater.getItems(podcast);

        /* Then */
        assertThat(items).hasSize(0);
    }

    private Document parseHtml(String url) throws URISyntaxException, IOException {
        return Jsoup.parse(
                Paths.get(YoutubeUpdaterTest.class.getResource(url).toURI()).toFile(),
                "UTF-8",
                "http://www.youtube.com/");
    }

    private JSONObject parseJson(String url) throws IOException, URISyntaxException, ParseException {
        return JSONObject.class.cast(new JSONParser().parse(Files.newBufferedReader(Paths.get(YoutubeUpdater.class.getResource(url).toURI()))));
    }

    private org.jdom2.Document parseXml(String name) throws JDOMException, IOException, URISyntaxException {
        return new SAXBuilder().build(Paths.get(RSSUpdaterTest.class.getResource(name).toURI()).toFile());
    }

}