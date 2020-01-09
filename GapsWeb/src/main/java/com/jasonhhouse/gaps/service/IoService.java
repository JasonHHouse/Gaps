/*
 * Copyright 2019 Jason H House
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.jasonhhouse.gaps.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonhhouse.gaps.Movie;
import com.jasonhhouse.gaps.PlexSearch;
import com.jasonhhouse.gaps.Rss;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IoService.class);

    private final String STORAGE_FOLDER;

    private final String TEMP_STORAGE_FOLDER;

    private static final String STORAGE = "movieIds.json";

    private static final String RECOMMENDED_MOVIES = "recommendedMovies.json";

    public static final String RSS_FEED_JSON_FILE = "rssFeed.json";

    public static final String PROPERTIES = "gaps.properties";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public IoService() {
        //Look for properties file for file locations
        String os = System.getProperty("os.name");
        if (os.contains("Windows")) {
            //Default to the same folder as the jar
            String decodedPath = "";
            try {
                String path = new File(new File(new File(IoService.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent()).getParent()).getParent();
                decodedPath = URLDecoder.decode(path, "UTF-8");
                decodedPath = decodedPath.substring("file:\\".length());
            } catch (UnsupportedEncodingException e) {
                //Do nothing
            }
            STORAGE_FOLDER = decodedPath + "\\";
            TEMP_STORAGE_FOLDER = decodedPath + "\\temp\\";
        } else {
            STORAGE_FOLDER = "/usr/data/";
            TEMP_STORAGE_FOLDER = "/tmp/";
        }
    }

    public boolean doesRecommendedFileExist() {
        return new File(STORAGE_FOLDER + RECOMMENDED_MOVIES).exists();
    }

    public @NotNull String getRecommendedMovies() {
        try {
            Path path = new File(STORAGE_FOLDER + RECOMMENDED_MOVIES).toPath();
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            LOGGER.error("Check for the recommended file next time", e);
            return "";
        }
    }

    public boolean doesRssFileExist() {
        return new File(STORAGE_FOLDER + RSS_FEED_JSON_FILE).exists();
    }

    public @NotNull String getRssFile() {
        try {
            Path path = new File(STORAGE_FOLDER + RSS_FEED_JSON_FILE).toPath();
            return new String(Files.readAllBytes(path));
        } catch (IOException e) {
            LOGGER.error("Check for RSS file next time", e);
            return "";
        }
    }

    /**
     * Write the recommended movie list to the RSS file for endpoint to display.
     *
     * @param recommended The recommended movies. (IMDB ID is required.)
     */
    public void writeRssFile(Set<Movie> recommended) {
        File file = new File(STORAGE_FOLDER + RSS_FEED_JSON_FILE);

        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                LOGGER.error("Can't delete existing file " + STORAGE_FOLDER + RSS_FEED_JSON_FILE);
                return;
            }
        }

        try {
            boolean created = file.createNewFile();
            if (!created) {
                LOGGER.error("Can't create file " + STORAGE_FOLDER + RSS_FEED_JSON_FILE);
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Can't create file " + STORAGE_FOLDER + RSS_FEED_JSON_FILE, e);
            return;
        }

        // Create writer that java will close for us.
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            List<Rss> rssList = recommended.stream().map(movie -> new Rss(movie.getImdbId(), movie.getYear(), movie.getTvdbId(), movie.getName(), movie.getPosterUrl())).collect(Collectors.toList());
            byte[] output = objectMapper.writeValueAsBytes(rssList);
            outputStream.write(output);
        } catch (FileNotFoundException e) {
            LOGGER.error("Can't find file " + RECOMMENDED_MOVIES, e);
        } catch (IOException e) {
            LOGGER.error("Can't write to file " + RECOMMENDED_MOVIES, e);
        }
    }

    public void migrateJsonSeedFileIfNeeded() {
        final File seedFile = new File(STORAGE_FOLDER + STORAGE);
        if (seedFile.exists()) {
            LOGGER.info("Seed file exists, not copying over");
            return;
        }

        final File tempSeed = new File(TEMP_STORAGE_FOLDER + STORAGE);
        try {
            Files.move(tempSeed.toPath(), seedFile.toPath());
            LOGGER.info("Seed file doesn't exist, copying over");
        } catch (IOException e) {
            LOGGER.error("Failed to copy seed file over", e);
        }
    }

    /**
     * Prints out all recommended movies to recommendedMovies.json
     */
    public void writeRecommendedToFile(Set<Movie> recommended) {
        LOGGER.info("writeRecommendedToFile()");
        final String fileName = STORAGE_FOLDER + RECOMMENDED_MOVIES;
        File file = new File(fileName);
        writeMovieIdsToFile(recommended, file);
    }

    /**
     * Prints out all movies to a text file movieIds.json
     */
    public void writeMovieIdsToFile(Set<Movie> everyMovie) {
        LOGGER.info("writeMovieIdsToFile()");
        final String fileName = STORAGE_FOLDER + STORAGE;
        File file = new File(fileName);
        writeMovieIdsToFile(everyMovie, file);
    }

    public void writeMovieIdsToFile(Set<Movie> everyMovie, File file) {
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                LOGGER.error("Can't delete existing file " + file.getName());
                return;
            }
        }

        try {
            boolean created = file.createNewFile();
            if (!created) {
                LOGGER.error("Can't create file " + file.getAbsolutePath());
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Can't create file " + file.getAbsolutePath(), e);
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] output = objectMapper.writeValueAsBytes(everyMovie);
            outputStream.write(output);
        } catch (FileNotFoundException e) {
            LOGGER.error("Can't find file " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            LOGGER.error("Can't write to file " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Prints out all recommended files to a text file called gaps_recommended_movies.txt
     */
    public Set<Movie> readMovieIdsFromFile() {
        Set<Movie> everyMovie = Collections.emptySet();
        final String fileName = STORAGE_FOLDER + STORAGE;
        File file = new File(fileName);
        if (!file.exists()) {
            LOGGER.warn("Can't find json file '" + fileName + "'. Most likely first run.");
            return everyMovie;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder fullFile = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                fullFile.append(line);
            }

            LOGGER.debug(fullFile.toString());
            everyMovie = objectMapper.readValue(fullFile.toString(), new TypeReference<Set<Movie>>() {
            });
            LOGGER.debug("everyMovie.size():" + everyMovie.size());
        } catch (FileNotFoundException e) {
            LOGGER.error("Can't find file " + fileName);
        } catch (IOException e) {
            LOGGER.error("Can't write to file " + fileName);
        }

        return everyMovie;
    }

    /**
     * Prints out all recommended files to a text file called gaps_recommended_movies.txt
     */
    public void writeToFile(Set<Movie> recommended) {
        File file = new File(RECOMMENDED_MOVIES);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                LOGGER.error("Can't delete existing file " + RECOMMENDED_MOVIES);
                return;
            }
        }

        try {
            boolean created = file.createNewFile();
            if (!created) {
                LOGGER.error("Can't create file " + RECOMMENDED_MOVIES);
                return;
            }
        } catch (IOException e) {
            LOGGER.error("Can't create file " + RECOMMENDED_MOVIES, e);
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(RECOMMENDED_MOVIES)) {
            for (Movie movie : recommended) {
                String output = movie.toString() + System.lineSeparator();
                outputStream.write(output.getBytes());
            }
        } catch (FileNotFoundException e) {
            LOGGER.error("Can't find file " + RECOMMENDED_MOVIES, e);
        } catch (IOException e) {
            LOGGER.error("Can't write to file " + RECOMMENDED_MOVIES, e);
        }
    }

    public void writeProperties(PlexSearch plexSearch) throws IOException {
        Properties properties = new Properties();

        if (StringUtils.isNotEmpty(plexSearch.getMovieDbApiKey())) {
            properties.setProperty(PlexSearch.MOVIE_DB_API_KEY, plexSearch.getMovieDbApiKey());
        }

        if (StringUtils.isNotEmpty(plexSearch.getAddress())) {
            properties.setProperty(PlexSearch.ADDRESS, plexSearch.getAddress());
        }

        if (plexSearch.getPort() != null) {
            properties.setProperty(PlexSearch.PORT, Integer.toString(plexSearch.getPort()));
        }

        if (StringUtils.isNotEmpty(plexSearch.getPlexToken())) {
            properties.setProperty(PlexSearch.PLEX_TOKEN, plexSearch.getPlexToken());
        }

        properties.store(new FileWriter(new File(STORAGE_FOLDER + PROPERTIES)), "");
    }

    public PlexSearch readProperties() throws IOException {
        File file = new File(STORAGE_FOLDER + PROPERTIES);
        PlexSearch plexSearch = new PlexSearch();

        if (!file.exists()) {
            LOGGER.warn(file + " does not exist");
            return plexSearch;
        }

        Properties properties = new Properties();
        properties.load(new FileReader(file));

        if (properties.containsKey(PlexSearch.MOVIE_DB_API_KEY)) {
            String movieDbApiKey = properties.getProperty(PlexSearch.MOVIE_DB_API_KEY);
            plexSearch.setMovieDbApiKey(movieDbApiKey);
        }

        if (properties.containsKey(PlexSearch.ADDRESS)) {
            String address = properties.getProperty(PlexSearch.ADDRESS);
            plexSearch.setAddress(address);
        }

        if (properties.containsKey(PlexSearch.PORT)) {
            String port = properties.getProperty(PlexSearch.PORT);
            plexSearch.setPort(Integer.parseInt(port));
        }

        if (properties.containsKey(PlexSearch.PLEX_TOKEN)) {
            String plexToken = properties.getProperty(PlexSearch.PLEX_TOKEN);
            plexSearch.setPlexToken(plexToken);
        }

        return plexSearch;
    }
}
