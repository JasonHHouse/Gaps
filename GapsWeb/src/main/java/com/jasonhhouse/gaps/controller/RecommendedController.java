/*
 * Copyright 2019 Jason H House
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jasonhhouse.gaps.controller;

import com.jasonhhouse.gaps.GapsSearch;
import com.jasonhhouse.gaps.GapsService;
import com.jasonhhouse.gaps.Movie;
import com.jasonhhouse.gaps.Payload;
import com.jasonhhouse.gaps.PlexLibrary;
import com.jasonhhouse.gaps.PlexServer;
import com.jasonhhouse.gaps.service.IoService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class RecommendedController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecommendedController.class);

    private final IoService ioService;
    private final GapsService gapsService;
    private final GapsSearch gapsSearch;

    @Autowired
    public RecommendedController(IoService ioService, GapsService gapsService, GapsSearch gapsSearch) {
        this.ioService = ioService;
        this.gapsService = gapsService;
        this.gapsSearch = gapsSearch;
    }

    @RequestMapping(method = RequestMethod.GET, path = "/recommended")
    public ModelAndView getRecommended() {
        LOGGER.info("getRecommended()");

        PlexServer plexServer;
        PlexLibrary plexLibrary;
        if (CollectionUtils.isNotEmpty(gapsService.getPlexSearch().getPlexServers())) {
            //Read first plex servers movies
            plexServer = gapsService.getPlexSearch().getPlexServers().stream().findFirst().orElse(null);
            plexLibrary = plexServer.getPlexLibraries().stream().findFirst().orElse(null);
        } else {
            plexServer = null;
            plexLibrary = null;
        }

        Map<String, PlexServer> plexServerMap = gapsService.getPlexSearch().getPlexServers().stream().collect(Collectors.toMap(PlexServer::getMachineIdentifier, Function.identity()));

        ModelAndView modelAndView = new ModelAndView("recommended");
        modelAndView.addObject("plexServers", plexServerMap);
        modelAndView.addObject("plexSearch", gapsService.getPlexSearch());
        modelAndView.addObject("plexServer", plexServer);
        modelAndView.addObject("plexLibrary", plexLibrary);
        return modelAndView;
    }


    @RequestMapping(method = RequestMethod.GET,
            path = "/recommended/{machineIdentifier}/{key}")
    @ResponseBody
    public ResponseEntity<Payload> getRecommended(@PathVariable("machineIdentifier") final String machineIdentifier, @PathVariable("key") final Integer key) {
        LOGGER.info("getRecommended( " + machineIdentifier + ", " + key + " )");

        final List<Movie> ownedMovies = ioService.readOwnedMovies(machineIdentifier, key);
        Payload payload;

        if (CollectionUtils.isEmpty(ownedMovies)) {
            payload = Payload.PLEX_LIBRARY_MOVIE_NOT_FOUND;
            LOGGER.warn(payload.getReason());
        } else {
            List<Movie> movies = ioService.readRecommendedMovies(machineIdentifier, key);
            if (CollectionUtils.isEmpty(movies)) {
                payload = Payload.RECOMMENDED_MOVIES_NOT_FOUND;
                LOGGER.warn(payload.getReason());
            } else {
                payload = Payload.RECOMMENDED_MOVIES_FOUND;
            }
            payload.setExtras(movies);
        }

        return ResponseEntity.ok().body(payload);
    }

    private List<String> buildUrls(Movie[] movies) {
        LOGGER.info("buildUrls( " + Arrays.toString(movies) + " ) ");
        List<String> urls = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getTvdbId() != -1) {
                urls.add("https://www.themoviedb.org/movie/" + movie.getTvdbId());
                continue;
            }

            if (StringUtils.isNotEmpty(movie.getImdbId())) {
                urls.add("https://www.imdb.com/title/" + movie.getImdbId() + "/");
                continue;
            }

            urls.add(null);
        }

        return urls;
    }

    /**
     * Start Gaps searching for missing movies
     *
     * @param machineIdentifier plex server id
     * @param key               plex library key
     */
    @RequestMapping(value = "/recommended/find/{machineIdentifier}/{key}",
            method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.OK)
    public void putFindRecommencedMovies(@PathVariable("machineIdentifier") final String machineIdentifier, @PathVariable("key") final Integer key) {
        LOGGER.info("putFindRecommencedMovies( " + machineIdentifier + ", " + key + " )");

        ioService.migrateJsonSeedFileIfNeeded();
        gapsSearch.run(machineIdentifier, key);
    }

    /**
     * Cancel Gaps searching for missing movies
     *
     * @param machineIdentifier plex server id
     * @param key               plex library key
     */
    @MessageMapping("/recommended/cancel/{machineIdentifier}/{key}")
    public void cancelSearching(@PathVariable("machineIdentifier") final String machineIdentifier, @PathVariable("key") final Integer key) {
        LOGGER.info("cancelSearching( " + machineIdentifier + ", " + key + " )");
        gapsSearch.cancelSearch();
    }


}
