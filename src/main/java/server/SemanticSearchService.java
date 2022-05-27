package server;

import client.Fuseki.FusekiClient;
import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import commons.SearchResult;
import io.grpc.protobuf.StatusProto;
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
        SemanticSearchOptions withSynonyms = searchRequest.getWithSynonym();
        Pair<Long, List<SearchResult>> queryTimeAndSearchResults = fusekiClient.execSelectAndProcess(searchQuery, searchField, withSynonyms, searchPredicateText);
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
