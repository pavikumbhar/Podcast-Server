package lan.dk.podcastserver.business;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.entity.WatchListAssert;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.WatchListRepository;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@RunWith(MockitoJUnitRunner.class)
public class WatchListBusinessTest {

    @Mock WatchListRepository watchListRepository;
    @Mock ItemRepository itemRepository;
    @InjectMocks WatchListBusiness watchListBusiness;

    @Test
    public void should_find_all() {
        /* Given */
        List<WatchList> watchLists = Lists.newArrayList();
        when(watchListRepository.findAll()).thenReturn(watchLists);

        /* When */
        List<WatchList> all = watchListBusiness.findAll();

        /* Then */
        assertThat(all).isSameAs(watchLists);
        verify(watchListRepository, only()).findAll();
    }

    @Test
    public void should_find_all_playlist_with_specified_item() {
        /* Given */
        UUID id = UUID.randomUUID();
        Item item = new Item().setId(id);
        WatchList p1 = WatchList.builder().id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634")).name("First").build();
        WatchList p2 = WatchList.builder().id(UUID.fromString("86faa982-f462-400a-bc9b-91eb299910b6")).name("Second").build();
        Set<WatchList> watchLists = Sets.newHashSet(p1, p2);

        when(itemRepository.findOne(eq(id))).thenReturn(item);
        when(watchListRepository.findContainsItem(eq(item))).thenReturn(watchLists);

        /* When */
        Set<WatchList> watchListOfItem = watchListBusiness.findContainsItem(id);

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchLists);
        verify(itemRepository, only()).findOne(eq(id));
        verify(watchListRepository, only()).findContainsItem(eq(item));
    }

    @Test
    public void should_add_item_to_playlist() {
        /* Given */
        UUID id = UUID.randomUUID();
        Item item = new Item().setId(id).setWatchLists(Sets.newHashSet());
        WatchList watchList = WatchList
                .builder()
                .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                .name("First")
                .items(Sets.newHashSet())
                .build();

        when(itemRepository.findOne(eq(id))).thenReturn(item);
        when(watchListRepository.findOne(eq(watchList.getId()))).thenReturn(watchList);
        when(watchListRepository.save(any(WatchList.class))).then(i -> i.getArguments()[0]);

        /* When */
        WatchList watchListOfItem = watchListBusiness.add(watchList.getId(), id);

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList);
        WatchListAssert
                .assertThat(watchListOfItem)
                .hasItems(item);
        verify(itemRepository, only()).findOne(eq(id));
        verify(watchListRepository, times(1)).findOne(eq(watchList.getId()));
        verify(watchListRepository, times(1)).save(eq(watchList));
    }

    @Test
    public void should_remove_item_to_playlist() {
        /* Given */
        UUID id = UUID.randomUUID();
        Item item = new Item().setId(id).setWatchLists(Sets.newHashSet());
        WatchList watchList = WatchList
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(Sets.newHashSet(item))
                .build();

        when(itemRepository.findOne(eq(id))).thenReturn(item);
        when(watchListRepository.findOne(eq(watchList.getId()))).thenReturn(watchList);
        when(watchListRepository.save(any(WatchList.class))).then(i -> i.getArguments()[0]);

        /* When */
        WatchList watchListOfItem = watchListBusiness.remove(watchList.getId(), id);

        /* Then */
        assertThat(watchListOfItem).isSameAs(watchList);
        WatchListAssert
                .assertThat(watchListOfItem)
                .doesNotHaveItems(item);
        verify(itemRepository, only()).findOne(eq(id));
        verify(watchListRepository, times(1)).findOne(eq(watchList.getId()));
        verify(watchListRepository, times(1)).save(eq(watchList));
    }

    @Test
    public void should_delete() {
        /* Given */
        UUID id = UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634");

        /* When */
        watchListBusiness.delete(id);

        /* Then */
        verify(watchListRepository, only()).delete(eq(id));
    }

    @Test
    public void should_save() {
        /* Given */
        WatchList watchList = WatchList
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(Sets.newHashSet())
                .build();


        /* When */
        watchListBusiness.save(watchList);

        /* Then */
        verify(watchListRepository, only()).save(eq(watchList));
    }

    @Test
    public void should_find_one_by_id() {
        /* Given */
        WatchList watchList = WatchList
                .builder()
                    .id(UUID.fromString("16f7a430-8d4c-45d4-b4ec-68c807b82634"))
                    .name("First")
                    .items(Sets.newHashSet())
                .build();

        when(watchListRepository.findOne(eq(watchList.getId()))).thenReturn(watchList);

        /* When */
        WatchList aWatchList = watchListBusiness.findOne(watchList.getId());

        /* Then */
        assertThat(aWatchList).isSameAs(watchList);
        verify(watchListRepository, only()).findOne(eq(watchList.getId()));
    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(watchListRepository, itemRepository);
    }

}
