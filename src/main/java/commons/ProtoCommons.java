package commons;

import org.bakalaurinis.search.*;

import java.util.ArrayList;
import java.util.List;

public class ProtoCommons {

    public static SearchResponse buildSearchResponse(List<SearchResult> searchResults) {
        SearchResponse.Builder response = SearchResponse.newBuilder();
        List<Result> results = new ArrayList<>();
        for(SearchResult searchResult : searchResults) {
                    results.add(Result.newBuilder()
                            .setId(searchResult.getId())
                            .setLabel(searchResult.getLabel())
                            .setLemma(searchResult.getLemma())
                            .addAllDefinition(searchResult.getDefinition())
                            .addAllSenseExample(searchResult.getSenseExample())
                            .setScore(searchResult.getScore())
                            .build());
        }
        return response.addAllSearchResults(results).build();
    }
}
