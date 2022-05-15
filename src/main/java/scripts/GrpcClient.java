package scripts;

import com.google.protobuf.ByteString;
import commons.SearchResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bakalaurinis.search.Result;
import org.bakalaurinis.search.SearchRequest;
import org.bakalaurinis.search.SearchResponse;
import org.bakalaurinis.search.SearchServiceGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GrpcClient {
    private int port;
    private final String address = "localhost";
    private ManagedChannel channel;
    private SearchServiceGrpc.SearchServiceBlockingStub stub;


    public GrpcClient(int port) {
        channel = ManagedChannelBuilder
                .forAddress(address, port)
                .usePlaintext()
                .build();

        stub = SearchServiceGrpc.newBlockingStub(channel);
    }

    public List<SearchResult> search(String query, SearchRequest.SearchField searchField, SearchRequest.SearchPredicate searchPredicate){
        SearchRequest searchRequest = SearchRequest.newBuilder()
                .setQuery(query)
                .setSearchField(searchField)
                .setSearchPredicate(searchPredicate)
                .build();
        SearchResponse searchResponse = stub.search(searchRequest);
        List<SearchResult> searchResults = new ArrayList<>();
        for(Result r : searchResponse.getSearchResultsList()) {
            searchResults.add(new SearchResult(
                    r.getIdBytes().toStringUtf8(),
                    r.getLabelBytes().toStringUtf8(),
                    r.getLemmaBytes().toStringUtf8(),
                    r.getScore(),
                    r.getDefinitionList().asByteStringList().stream()
                            .map(ByteString::toStringUtf8)
                            .collect(Collectors.toList()),
                    r.getSenseExampleList().asByteStringList().stream()
                            .map(ByteString::toStringUtf8)
                            .collect(Collectors.toList())
            ));
        }
        return searchResults;
    }
}
