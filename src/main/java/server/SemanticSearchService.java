package server;

import client.FusekiClient;
import io.grpc.stub.StreamObserver;
import ontology.OntologyModel;
import org.apache.commons.lang.NotImplementedException;
import org.tutorial.search.*;

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
        SearchResponse.Builder response = SearchResponse.newBuilder();
        switch (searchField) {
            case LABEL:
                fusekiClient.execSelectAndProcess(searchQuery);
                break;
            case DEFINITION:
                fusekiClient.execSelectAndProcess(searchQuery);
                break;
            default:
                throw new RuntimeException("Neteisingas paieškos laukas!");
        }
        responseStreamObserver.onNext(response.build());
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
