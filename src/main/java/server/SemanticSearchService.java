package server;

import client.FusekiClient;
import commons.SearchResult;
import io.grpc.stub.StreamObserver;
import ontology.OntologyModel;
import org.apache.commons.lang.NotImplementedException;
import org.bakalaurinis.search.*;

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
        SearchRequest.SearchField searchField = searchRequest.getSearchField();
        List<SearchResult> searchResults = fusekiClient.execSelectAndProcess(searchQuery, searchField);
        SearchResponse response = buildSearchResponse(searchResults);

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
