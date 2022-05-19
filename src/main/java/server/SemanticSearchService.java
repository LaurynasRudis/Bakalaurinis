package server;

import client.Fuseki.FusekiClient;
import commons.SearchResult;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang.NotImplementedException;
import org.bakalaurinis.search.*;
import org.javatuples.Pair;

import java.util.List;

import static commons.ProtoCommons.buildSearchResponse;

public class SemanticSearchService extends SearchServiceGrpc.SearchServiceImplBase {

    private final FusekiClient fusekiClient;

    public SemanticSearchService(FusekiClient fusekiClient) { this.fusekiClient = fusekiClient; }


    @Override
    public void search(
            SearchRequest searchRequest,
            StreamObserver<SearchResponse> responseStreamObserver
    ) {
        String searchQuery = searchRequest.getQuery();
        SearchField searchField = searchRequest.getSearchField();
        SemanticSearchOptions withSynonyms = searchRequest.getWithSynonym();
        Pair<Long, List<SearchResult>> queryTimeAndSearchResults = fusekiClient.execSelectAndProcess(searchQuery, searchField, withSynonyms);
        SearchResponse response = buildSearchResponse(queryTimeAndSearchResults);

        responseStreamObserver.onNext(response);
        responseStreamObserver.onCompleted();
    }

    @Override
    public void index(
            IndexRequest indexRequest,
            StreamObserver<IndexResponse> responseStreamObserver
    ) {
        throw new NotImplementedException("Indeksavimas naudojantis fuseki dar neimplementuotas.");
    }

}
