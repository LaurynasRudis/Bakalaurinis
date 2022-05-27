package scripts;

import com.google.protobuf.ByteString;
import commons.SearchResult;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.bakalaurinis.search.Result;
import org.bakalaurinis.search.SearchRequest;
import org.bakalaurinis.search.SearchResponse;
import org.bakalaurinis.search.SearchServiceGrpc;
import org.javatuples.Triplet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GrpcClient {
    private final SearchServiceGrpc.SearchServiceBlockingStub stub;


    public GrpcClient(String address, int port) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(address, port)
                .maxInboundMessageSize(6005000)
                .usePlaintext()
                .build();

        stub = SearchServiceGrpc.newBlockingStub(channel);
    }

    public Triplet<Long, List<SearchResult>, Integer> search(SearchRequest searchRequest) {
        SearchResponse searchResponse = stub.search(searchRequest);
        List<SearchResult> searchResults = new ArrayList<>();
        long queryTime = (long) searchResponse.getQueryTime();
        int resultCount = searchResponse.getResultCount();

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
        return new Triplet<>(queryTime, searchResults, resultCount);
    }
}
