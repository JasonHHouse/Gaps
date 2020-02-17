/*
 * Copyright 2020 Jason H House
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.jasonhhouse.gaps.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.jasonhhouse.gaps.Movie;
import java.io.IOException;

public class MovieDeserializer extends StdDeserializer<Movie> {
    public MovieDeserializer() {
        this(null);
    }

    protected MovieDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Movie deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int tvdbId = (Integer) node.get(Movie.TVDB_ID).numberValue();
        String imdbId = node.get(Movie.IMDB_ID).asText();
        String name = node.get(Movie.NAME).asText();
        int year = (Integer) node.get(Movie.YEAR).numberValue();


        int collectionId = -1;
        if (node.has(Movie.COLLECTION_ID)) {
            collectionId = (Integer) node.get(Movie.COLLECTION_ID).numberValue();
        }

        String collection = "";
        if (node.has(Movie.COLLECTION)) {
            collection = node.get(Movie.COLLECTION).asText();
        }

        String posterUrl = "";
        if (node.has(Movie.POSTER)) {
            posterUrl = node.get(Movie.POSTER).asText();
        }

        String language = "";
        if (node.has(Movie.LANGUAGE)) {
            language = node.get(Movie.LANGUAGE).asText();
        }

        String overview = "";
        if (node.has(Movie.OVERVIEW)) {
            overview = node.get(Movie.OVERVIEW).asText();
        }

        Movie movie = new Movie(name, year);
        movie.setTvdbId(tvdbId);
        movie.setImdbId(imdbId);
        movie.setCollectionId(collectionId);
        movie.setCollection(collection);
        movie.setPosterUrl(posterUrl);
        movie.setLanguage(language);
        movie.setOverview(overview);

        return movie;
    }
}
