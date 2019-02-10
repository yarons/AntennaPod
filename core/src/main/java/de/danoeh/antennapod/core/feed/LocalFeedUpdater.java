package de.danoeh.antennapod.core.feed;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import de.danoeh.antennapod.core.storage.DBTasks;

public class LocalFeedUpdater {

    private static long getFileDuration(File f) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(f.getAbsolutePath());
        String durationStr = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        return Long.parseLong(durationStr);
    }

    /** Starts the import process. */
    public static void startImport(Uri uri, Context context) {
        File f = new File(uri.getPath());
        if (!f.isDirectory()) {
            throw new RuntimeException("invalid path");
        } else {
            startImportDirectory(uri, context);
        }
    }

    private static void startImportDirectory(Uri uri, Context context) {
        //create a feed object for this directory
        File f = new File(uri.getPath());
        String dirUrl = uri.toString();
        Feed dirFeed = new Feed(dirUrl, null, "Local directory (" + dirUrl + ")");
        dirFeed.setItems(new ArrayList<>()); //this seems useless
        //find the feed for this directory (if it exists), or create one
        dirFeed = DBTasks.updateFeed(context, dirFeed)[0];

        //find relevant files and create items for them
        File[] itemFiles = f.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String s) {
                        String m = getMimeType(s);
                        return m != null && (m.startsWith("audio/") || m.startsWith("video/"));
                    }
                });
        List<FeedItem> newItems = new ArrayList<>();
        for (File it: itemFiles) {
            FeedItem found = feedContainsFile(dirFeed, it.getAbsolutePath());
            if (found != null) {
                //TODO update (not implemented yet)
            } else {
                FeedItem item = createFeedItem(it, dirFeed);
                newItems.add(item);
            }
        }
        //don't care about the old items for now, but when the update code comes, we'll change this
        dirFeed.setItems(newItems);

        //add or merge to the db
        Feed[] feeds = DBTasks.updateFeed(context, dirFeed);
    }

    private static FeedItem feedContainsFile(Feed feed, String fileUrl) {
        List<FeedItem> items = feed.getItems();
        for (FeedItem i: items) {
            if (i.getMedia().getFile_url().equals(fileUrl)) {
                return i;
            }
        }
        return null;
    }

    private static FeedItem createFeedItem(File f, Feed whichFeed) {
        //create item
        long globalId = 0;
        Date date = new Date();
        String uuid = UUID.randomUUID().toString();
        FeedItem item = new FeedItem(globalId, f.getName(), uuid,
                f.toString(), date, FeedItem.UNPLAYED, whichFeed);
        item.setAutoDownload(false);

        //add the media to the item
        long duration = getFileDuration(f);
        long size = f.length();
        String absPath = f.getAbsolutePath();
        FeedMedia media = new FeedMedia(0, item, (int)duration, 0, size, getMimeType(absPath), absPath, absPath, true, null, 0, 0);
        item.setMedia(media);

        return item;
    }

    private static String getMimeType(String path) {
        try {
            String extension = path.substring(path.lastIndexOf("."));
            String mimeTypeMap = MimeTypeMap.getFileExtensionFromUrl(extension);
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimeTypeMap);
        } catch (Exception e) {
            return null;
        }
    }
}
