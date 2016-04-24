package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public abstract class AbstractDownloader implements Runnable, Downloader {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    static final String WS_TOPIC_DOWNLOAD = "/topic/download";
    static final String WS_TOPIC_PODCAST = "/topic/podcast/%s";

    @Getter @Setter @Accessors(chain = true)
    protected Item item;

    String temporaryExtension;
    protected File target = null;
    private PathMatcher hasTempExtensionMatcher;

    @Resource protected ItemRepository itemRepository;
    @Resource protected PodcastRepository podcastRepository;
    @Resource protected PodcastServerParameters podcastServerParameters;
    @Resource protected SimpMessagingTemplate template;
    @Resource protected MimeTypeService mimeTypeService;

    @Setter @Accessors(chain = true)
    protected ItemDownloadManager itemDownloadManager;

    AtomicBoolean stopDownloading = new AtomicBoolean(false);

    @Override
    public void run() {
        logger.debug("Run");
        startDownload();
    }

    @Override
    public void startDownload() {
        item.setStatus(Status.STARTED);
        stopDownloading.set(false);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
        download();
    }

    @Override
    public void pauseDownload() {
        item.setStatus(Status.PAUSED);
        stopDownloading.set(true);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    @Override
    public void stopDownload() {
        item.setStatus(Status.STOPPED);
        stopDownloading.set(true);
        saveSyncWithPodcast();
        itemDownloadManager.removeACurrentDownload(item);
        if (nonNull(target) && target.exists())
            target.delete();
        convertAndSaveBroadcast();
    }

    @Override
    @Transactional
    public void finishDownload() {
        itemDownloadManager.removeACurrentDownload(item);

        if (isNull(target)) {
            resetDownload();
            return;
        }

        item.setStatus(Status.FINISH);
        try {
            if (hasTempExtensionMatcher.matches(target.toPath().getFileName())) {
                Path targetWithoutExtension = target.toPath().resolveSibling(target.toPath().getFileName().toString().replace(temporaryExtension, ""));

                Files.deleteIfExists(targetWithoutExtension);
                Files.move(target.toPath(), targetWithoutExtension);

                target = targetWithoutExtension.toFile();
            }
            item
                .setLength(Files.size(target.toPath()))
                .setMimeType(mimeTypeService.probeContentType(target.toPath()));
        } catch (IOException ignored) {}

        item.setFileName(FilenameUtils.getName(target.toPath().getFileName().toString()));
        item.setDownloadDate(ZonedDateTime.now());

        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    public void resetDownload() {
        stopDownload();
    }

    @Transactional
    public File getTagetFile (Item item) {

        if (nonNull(target)) return target;

        Path finalFile = getDestinationFile(item);
        logger.debug("Creation of file : {}", finalFile.toFile().getAbsolutePath());

        try {
            if (Files.notExists(finalFile.getParent())) Files.createDirectory(finalFile.getParent());

            if (!(Files.exists(finalFile) || Files.exists(finalFile.resolveSibling(finalFile.getFileName() + temporaryExtension)))) {
                return finalFile.resolveSibling(finalFile.getFileName() + temporaryExtension).toFile();
            }

            logger.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            String fileName = finalFile.getFileName().toString();
            return Files.createTempFile(finalFile.getParent(), FilenameUtils.getBaseName(fileName) + "-", "." + FilenameUtils.getExtension(fileName) + temporaryExtension).toFile();
        } catch (IOException e) {
            logger.error("Error during creation of target file", e);
            stopDownload();
            return null;
        }
    }

    private Path getDestinationFile(Item item) {
        String fileName = FilenameUtils.getName(StringUtils.substringBefore(getItemUrl(item), "?"));
        return  Paths.get(itemDownloadManager.getRootfolder(), item.getPodcast().getTitle(), fileName);
    }

    @Transactional
    protected void saveSyncWithPodcast() {
        try {
            item.setPodcast(podcastRepository.findOne(item.getPodcast().getId()));
            itemRepository.save(item);
        } catch (Exception e) {
            logger.error("Error during save and Sync of the item {}", item, e);
        }
    }

    @Transactional
    void convertAndSaveBroadcast() {
        template.convertAndSend(WS_TOPIC_DOWNLOAD, item);
        template.convertAndSend(String.format(WS_TOPIC_PODCAST, item.getPodcast().getId()), item);

        itemDownloadManager.getDownloadings$().onNext(item);
    }

    public String getItemUrl(Item item) {
        return item.getUrl();
    }

    @PostConstruct
    public void postConstruct() {
        temporaryExtension = podcastServerParameters.getDownloadExtension();
        hasTempExtensionMatcher = FileSystems.getDefault().getPathMatcher("glob:*" + temporaryExtension);
    }
}
