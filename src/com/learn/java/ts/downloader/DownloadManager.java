package com.learn.java.ts.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface DownloadManager {
    CompletableFuture<List<File>> download(List<URL> urls, String baseFolder);
}

class DownloadManagerImpl implements DownloadManager {

    private final static String POSTFIX_PATH = "temp";
    public final static int DEFAULT_BUFFER_SIZE = 1024;

    @Override
    public CompletableFuture<List<File>> download(List<URL> urls, String baseFolder) {
        System.out.println("=============[ START TO DOWNLOAD FILES ]==============");
        // Setting
        var asyncFileList = new ArrayList<CompletableFuture<File>>();
        var storageFolder = createStorageFolder(baseFolder);
        if (storageFolder == null) return CompletableFuture.supplyAsync(List::of);

        // Process
        for(int i = 0; i < urls.size(); i++) {
            var asyncFile = downloadAsync(i, urls.get(i), storageFolder);
            asyncFileList.add(asyncFile);
        }

        return sequence(asyncFileList).thenApply(files -> {
            var nullFiles = files.stream().filter(Objects::isNull).collect(Collectors.toList());
            if (nullFiles.size() > 0) return List.of();
            else                      return files;
        });
    }

    private CompletableFuture<File> downloadAsync(int index, URL url, File folder) {
        return CompletableFuture.supplyAsync(() -> download(index, url, folder));
    }

    private File download(int index, URL url, File folder) {
        try {
            File tsFile = new File(folder + "/" + index + ".ts");

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();

            try (FileOutputStream outputStream = new FileOutputStream(tsFile, false)) {
                int read;
                byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
                while ((read = input.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            }

            System.out.println("Download single file: " + tsFile.getPath());
            return tsFile;
        } catch (Exception e) {
            return null;
        }
    }

    private File createStorageFolder(String baseFolder) {
        try {
            var containFolder = new File(baseFolder + "/" + POSTFIX_PATH);
            if (!containFolder.isDirectory()) {
                containFolder.delete();
            }
            if (!containFolder.exists()) {
                containFolder.mkdirs();
            }

            return containFolder;
        } catch (Exception e) {
            return null;
        }
    }

    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> com) {
        return CompletableFuture.allOf(com.toArray(new CompletableFuture<?>[0]))
                .thenApply(v -> com.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
                );
    }
}
