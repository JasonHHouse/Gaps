package com.jasonhhouse.gaps.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jasonhhouse.gaps.GapsService;
import com.jasonhhouse.gaps.PlexSearch;
import com.jasonhhouse.gaps.PlexServer;
import com.jasonhhouse.gaps.service.BindingErrorsService;
import com.jasonhhouse.gaps.service.IoService;
import com.jasonhhouse.gaps.service.PlexQueryImpl;
import com.jasonhhouse.gaps.service.TmdbService;
import java.io.IOException;
import java.util.List;
import javax.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping(value = "/configuration")
public class ConfigurationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final BindingErrorsService bindingErrorsService;
    private final TmdbService tmdbService;
    private final SimpMessagingTemplate template;
    private final PlexQueryImpl plexQuery;
    private final GapsService gapsService;
    private final IoService ioService;

    public ConfigurationController(BindingErrorsService bindingErrorsService, TmdbService tmdbService, SimpMessagingTemplate template, PlexQueryImpl plexQuery, GapsService gapsService, IoService ioService) {
        this.bindingErrorsService = bindingErrorsService;
        this.tmdbService = tmdbService;
        this.template = template;
        this.plexQuery = plexQuery;
        this.gapsService = gapsService;
        this.ioService = ioService;
    }

    @RequestMapping(method = RequestMethod.GET,
            produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getConfiguration() {
        LOGGER.info("getConfiguration()");

        try {
            PlexSearch plexSearch = ioService.readProperties();
            gapsService.updatePlexSearch(plexSearch);

            List<PlexServer> plexServers = ioService.readPlexConfiguration();
            gapsService.getPlexSearch().getPlexServers().addAll(plexServers);
        } catch (IOException e) {
            LOGGER.warn("Failed to read gaps properties.", e);
        }

        ModelAndView modelAndView = new ModelAndView("configuration");
        modelAndView.addObject("plexSearch", gapsService.getPlexSearch());
        return modelAndView;
    }

    @RequestMapping(value = "/add/plex",
            method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseStatus(value = HttpStatus.OK)
    public void postAddPlexServer(@Valid final PlexServer plexServer, BindingResult bindingResult) {
        LOGGER.info("postAddPlexServer( " + plexServer + " )");

/*
        if (bindingErrorsService.hasBindingErrors(bindingResult)) {
            LOGGER.error("Error binding PlexServer object: " + plexServer);
        }
*/
        ObjectNode objectNode = objectMapper.createObjectNode();

        try {
            plexQuery.queryPlexServer(plexServer);
            plexQuery.getLibraries(plexServer);

            int initialCount = gapsService.getPlexSearch().getPlexServers().size();
            gapsService.getPlexSearch().addPlexServer(plexServer);
            if (gapsService.getPlexSearch().getPlexServers().size() == initialCount) {
                template.convertAndSend("/configuration/plex/duplicate", "{}");
            } else {
                ioService.writePlexConfiguration(gapsService.getPlexSearch().getPlexServers());
                template.convertAndSend("/configuration/plex/complete", plexServer);
            }
        } catch (Exception e) {
            LOGGER.error("Could not add plex server", e);
            objectNode.put("failed", true);
            template.convertAndSend("/configuration/plex/complete", objectNode.toString());
        }
    }

    @RequestMapping(value = "/test/plex",
            method = RequestMethod.PUT,
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> putTestPlexServer(@Valid final PlexServer plexServer, BindingResult bindingResult) {
        LOGGER.info("putTestPlexServer( " + plexServer + " )");

        /*if (bindingErrorsService.hasBindingErrors(bindingResult)) {
            LOGGER.error("Error binding PlexServer object: " + plexServer);
        }*/

        ObjectNode objectNode = objectMapper.createObjectNode();
        try {
            plexQuery.queryPlexServer(plexServer);
            objectNode.put("success", true);
        } catch (Exception e) {
            objectNode.put("success", false);
        }

        return ResponseEntity.ok().body(objectNode.toString());
    }

    @RequestMapping(value = "/test/plex/{machineIdentifier}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> putTestPlexServerByMachineId(@PathVariable("machineIdentifier") final String machineIdentifier) {
        LOGGER.info("putTestPlexServerByMachineId( " + machineIdentifier + " )");

        ObjectNode objectNode = objectMapper.createObjectNode();
        PlexServer returnedPlexServer = gapsService.getPlexSearch().getPlexServers().stream().filter(plexServer -> plexServer.getMachineIdentifier().equals(machineIdentifier)).findFirst().orElse(new PlexServer());

        if (StringUtils.isEmpty(returnedPlexServer.getMachineIdentifier())) {
            //Failed to find and delete
            objectNode.put("success", false);
        } else {
            plexQuery.queryPlexServer(returnedPlexServer);
            objectNode.put("success", true);
        }

        return ResponseEntity.ok().body(objectNode.toString());
    }

    @RequestMapping(value = "/delete/plex/{machineIdentifier}",
            method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> deletePlexServer(@PathVariable("machineIdentifier") final String machineIdentifier) {
        LOGGER.info("deletePlexServer( " + machineIdentifier + " )");

        ObjectNode objectNode = objectMapper.createObjectNode();
        PlexServer returnedPlexServer = gapsService.getPlexSearch().getPlexServers().stream().filter(plexServer -> plexServer.getMachineIdentifier().equals(machineIdentifier)).findFirst().orElse(new PlexServer());
        if (StringUtils.isEmpty(returnedPlexServer.getMachineIdentifier())) {
            //Failed to find and delete
            objectNode.put("success", false);
        } else {
            gapsService.getPlexSearch().getPlexServers().remove(returnedPlexServer);
            ioService.writePlexConfiguration(gapsService.getPlexSearch().getPlexServers());
            objectNode.put("success", true);
        }

        return ResponseEntity.ok().body(objectNode.toString());
    }

    @RequestMapping(value = "/test/tmdbKey/{tmdbKey}",
            method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> postTestTmdbKey(@PathVariable("tmdbKey") final String tmdbKey) {
        LOGGER.info("postTestTmdbKey( " + tmdbKey + " )");

        String tmdbResponse = tmdbService.testTmdbKey(tmdbKey);
        return ResponseEntity.ok().body(tmdbResponse);
    }

    @RequestMapping(value = "/save/tmdbKey/{tmdbKey}",
            method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> postSaveTmdbKey(@PathVariable("tmdbKey") final String tmdbKey) {
        LOGGER.info("postSaveTmdbKey( " + tmdbKey + " )");

        ObjectNode objectNode = objectMapper.createObjectNode();

        gapsService.getPlexSearch().setMovieDbApiKey(tmdbKey);
        try {
            ioService.writeProperties(gapsService.getPlexSearch());
            objectNode.put("success", true);
        } catch (IOException e) {
            objectNode.put("success", false);
            LOGGER.warn("Could not save PlexSearch properties");
        }

        return ResponseEntity.ok().body(objectNode.toString());
    }

}
