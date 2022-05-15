package server;

import client.SolrClient;
import io.grpc.stub.StreamObserver;
import ontology.OntologyModel;
import org.tutorial.search.*;

import java.util.Map;

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
        try {
            long start = System.currentTimeMillis();
            Map<String, String> s = solrClient.query(searchText);
            long stop = System.currentTimeMillis();
            SearchResponse.Builder response = SearchResponse.newBuilder();
            int is = 0;
            for(Map.Entry<String, String> i : s.entrySet()) {
                response.addSearchResults(Result.newBuilder().setLabel(i.getValue()));
            }
            long time = stop - start;
            System.out.println(time +" ms");
            responseStreamObserver.onNext(response.build());
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
