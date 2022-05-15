package server;

import client.SolrClient;
import commons.SearchResult;
import io.grpc.stub.StreamObserver;
import ontology.OntologyModel;
import org.bakalaurinis.search.*;

import java.util.List;
import java.util.Map;

import static commons.ProtoCommons.buildSearchResponse;

public class SearchServiceImpl extends SearchServiceGrpc.SearchServiceImplBase {

    private final SolrClient solrClient;

    public SearchServiceImpl(SolrClient solrClient){
        this.solrClient = solrClient;
    }


    @Override
    public void search(
            SearchRequest searchRequest,
            StreamObserver<SearchResponse> responseStreamObserver
    ) {
        String searchText = searchRequest.getQuery();
        String searchField = searchRequest.getSearchField().toString();
        String searchPredicate = searchRequest.getSearchPredicate().toString();
        try {
            List<SearchResult> searchResults = solrClient.query(searchText, searchField, searchPredicate);
            SearchResponse response = buildSearchResponse(searchResults);
            responseStreamObserver.onNext(response);
            responseStreamObserver.onCompleted();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void index(
            IndexRequest indexRequest,
            StreamObserver<IndexResponse> responseStreamObserver
    ) {
        String fileLocation = indexRequest.getFileLocation();
        OntologyModel model = new OntologyModel(fileLocation);
        solrClient.indexOntologyModel(model);
        IndexResponse.Builder response = IndexResponse.newBuilder();
        responseStreamObserver.onNext(response.build());
        responseStreamObserver.onCompleted();
    }
}
