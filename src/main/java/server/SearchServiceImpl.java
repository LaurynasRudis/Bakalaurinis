package server;

import client.SolrClient;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import commons.SearchResult;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import ontology.OntologyModel;
import org.bakalaurinis.search.*;
import org.javatuples.Pair;

import java.util.List;

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
        SearchField searchField = searchRequest.getSearchField();
        SearchPredicate searchPredicate = searchRequest.getSearchPredicate();
        String searchPredicateText = "";
        switch(searchPredicate) {
            case OR:
                searchPredicateText = "OR";
                break;
            case AND:
                searchPredicateText = "AND";
                break;
            default:
                com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                        .setCode(Code.INVALID_ARGUMENT.getNumber())
                        .setMessage("Incorrect or unset search predicate")
                        .addDetails(Any.pack(ErrorInfo.newBuilder()
                                .setReason("Bad request")
                                .build())).build();
                responseStreamObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
        try {
            Pair<Long, List<SearchResult>> queryTimeAndSearchResults = solrClient.query(searchText, searchField, searchPredicateText);
            SearchResponse response = buildSearchResponse(queryTimeAndSearchResults);
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
