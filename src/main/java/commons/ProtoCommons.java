package commons;

import org.bakalaurinis.search.*;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class ProtoCommons {

    public static SearchResponse buildSearchResponse(Pair<Long, List<SearchResult>> queryTimeAndSearchResults) {
        SearchResponse.Builder response = SearchResponse.newBuilder();
        List<Result> results = new ArrayList<>();
        long queryTime = queryTimeAndSearchResults.getValue0();
        List<SearchResult> searchResults = queryTimeAndSearchResults.getValue1();

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

        return response
                .addAllSearchResults(results)
                .setQueryTime(queryTime)
                .setResultCount(results.size())
                .build();
    }

    public static SearchRequest createSearchRequest(String query, SearchField searchField, SearchPredicate searchPredicate) {
        return SearchRequest.newBuilder()
                .setQuery(query)
                .setSearchField(searchField)
                .setSearchPredicate(searchPredicate)
                .build();
    }

    public static SearchRequest createSearchRequest(String query, SearchField searchField, SearchPredicate searchPredicate, SemanticSearchOptions searchOptions) {
        return SearchRequest.newBuilder()
                .setQuery(query)
                .setSearchField(searchField)
                .setWithSynonym(searchOptions)
                .setSearchPredicate(searchPredicate)
                .build();
    }

    public static SearchRequest createSearchRequest(String query, SearchField searchField, SemanticSearchOptions searchOptions) {
        return SearchRequest.newBuilder()
                .setQuery(query)
                .setSearchField(searchField)
                .setWithSynonym(searchOptions)
                .build();
    }
}
