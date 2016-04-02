package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Transactional
public class PodcastBusiness {

    @Resource PodcastServerParameters podcastServerParameters;
    @Resource JdomService jdomService;
    @Resource PodcastRepository podcastRepository;
    @Resource TagBusiness tagBusiness;
    @Resource CoverBusiness coverBusiness;
    @Resource MimeTypeService mimeTypeService;

    //** Delegate du Repository **//
    public List<Podcast> findAll() {
        return podcastRepository.findAll();
    }

    public Podcast save(Podcast entity) {
        return podcastRepository.save(entity);
    }

    public Podcast findOne(UUID id) {
        return podcastRepository.findOne(id);
    }

    public void delete(UUID id) {
        podcastRepository.delete(id);
        //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    }

    public void delete(Podcast entity) {
        podcastRepository.delete(entity);
        //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    }

    public List<Podcast> findByUrlIsNotNull() {
        return podcastRepository.findByUrlIsNotNull();
    }

    //*****//
    public Podcast patchUpdate(Podcast patchPodcast) {
        Podcast podcastToUpdate = this.findOne(patchPodcast.getId());

        if (podcastToUpdate == null)
            throw new PodcastNotFoundException();

        /*
        // Move folder if name has change :
        if (StringUtils.equals(podcastToUpdate.getTitle(), patchPodcast.getTitle())) {
                TODO : Move Folder to new Location using java.nio.FILES and java.nio.PATH
                It must add modification on each item of the podcast (localUrl)

        }
        */

        podcastToUpdate.setTitle(patchPodcast.getTitle());
        podcastToUpdate.setUrl(patchPodcast.getUrl());
        podcastToUpdate.setSignature(patchPodcast.getSignature());
        podcastToUpdate.setType(patchPodcast.getType());

        if (!coverBusiness.hasSameCoverURL(patchPodcast, podcastToUpdate)) {
            patchPodcast.getCover().setUrl(coverBusiness.download(patchPodcast));
        }
        
        podcastToUpdate.setCover(
                coverBusiness.findOne(patchPodcast.getCover().getId())
                    .setHeight(patchPodcast.getCover().getHeight())
                    .setUrl(patchPodcast.getCover().getUrl())
                    .setWidth(patchPodcast.getCover().getWidth())
        );
        podcastToUpdate.setDescription(patchPodcast.getDescription());
        podcastToUpdate.setHasToBeDeleted(patchPodcast.getHasToBeDeleted());
        podcastToUpdate.setTags(patchPodcast.getTags());

        return this.reatachAndSave(podcastToUpdate);
    }

    @Transactional(readOnly = true)
    public String getRss(UUID id, Boolean limit) {
        try {
            return jdomService.podcastToXMLGeneric(findOne(id), limit);
        } catch (IOException e) {
            log.error("Unable to generate RSS for podcast {} with limit {}", id, limit, e);
            return "";
        }
    }

    public Set<Item> getItems(UUID id){
        return podcastRepository.findOne(id).getItems();
    }

    public Podcast reatachAndSave(Podcast podcast) {
        podcast.setTags(tagBusiness.getTagListByName(podcast.getTags()));
        return save(podcast);
    }
    
    public Podcast create(Podcast podcast) {
        Podcast podcastSaved = reatachAndSave(podcast);

        if (!Objects.isNull(podcast.getCover())) {
            coverBusiness.save(podcast.getCover().setUrl(coverBusiness.download(podcast)));
        }

        return podcastSaved;
    }

    public Path coverOf(UUID id) {
        return coverBusiness.getCoverPathOf(findOne(id));
    }
}
