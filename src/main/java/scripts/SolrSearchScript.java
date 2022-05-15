package scripts;

import commons.SearchResult;
import org.bakalaurinis.search.SearchRequest;

import java.util.List;

public class SolrSearchScript {
    public static void main(String[] args) {
        GrpcClient tfidfClient = new GrpcClient(5540);
        GrpcClient bm25Client = new GrpcClient(5541);
        GrpcClient dfrClient = new GrpcClient(5542);
        GrpcClient semanticClient = new GrpcClient(5543);
        List<SearchResult> searchResults = semanticClient.search("labas", SearchRequest.SearchField.DEFINITION, SearchRequest.SearchPredicate.OR);
        for(SearchResult sr : searchResults){
            System.out.println(sr.getId());
            System.out.println(sr.getLemma());
            System.out.println(sr.getScore());
        }
    }
}
