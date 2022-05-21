package scripts;

import commons.ExcelSpreadsheet;
import commons.SearchResult;
import org.bakalaurinis.search.SearchField;
import org.bakalaurinis.search.SearchPredicate;
import org.bakalaurinis.search.SearchRequest;
import org.bakalaurinis.search.SemanticSearchOptions;
import org.javatuples.Triplet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static commons.ProtoCommons.createSearchRequest;

public class SolrSearchScript {
    public static void main(String[] args) throws IOException {
        String fileLocation = "test.xlsx";

        ExcelSpreadsheet excelSpreadsheet = new ExcelSpreadsheet(fileLocation);

        SemanticSearchOptions allOptionsTrueWithIndexes = SemanticSearchOptions.newBuilder()
                .setSearchWithSynonyms(true)
                .setSearchWithIsSynonym(true)
                .setSearchWithQuerySynonyms(true)
                .setUseIndexes(true)
                .build();

        SemanticSearchOptions allOptionsTrueWithoutIndexes = SemanticSearchOptions.newBuilder()
                .setSearchWithSynonyms(true)
                .setSearchWithIsSynonym(true)
                .setSearchWithQuerySynonyms(true)
                .setUseIndexes(false)
                .build();

        GrpcClient tfidfClient = new GrpcClient(5540);
        GrpcClient bm25Client = new GrpcClient(5541);
        GrpcClient dfrClient = new GrpcClient(5542);
        GrpcClient semanticClient = new GrpcClient(5543);

        Map<String, GrpcClient> solrClients = new HashMap<>();
        solrClients.put("tfidf", tfidfClient);
        solrClients.put("bm25", bm25Client);
        solrClients.put("dfr", dfrClient);

        SearchRequest searchRequest1 = createSearchRequest("velnias", SearchField.DEFINITION, SearchPredicate.OR);
        SearchRequest searchRequest2 = createSearchRequest("velnias", SearchField.LABEL, SearchPredicate.OR);
        SearchRequest semanticRequest1 = createSearchRequest("velnias", SearchField.LABEL, allOptionsTrueWithoutIndexes);
        SearchRequest semanticRequest2 = createSearchRequest("velnias", SearchField.LABEL, allOptionsTrueWithIndexes);

        for(Map.Entry<String, GrpcClient> clientEntry : solrClients.entrySet()) {
            GrpcClient client = clientEntry.getValue();
            Triplet<Long, List<SearchResult>, Integer> searchResults1 = client.search(searchRequest1);
            String clientName = clientEntry.getKey();
            excelSpreadsheet.writeSearchResults(clientName+"1", searchResults1);
            Triplet<Long, List<SearchResult>, Integer> searchResults2 = client.search(searchRequest2);
            excelSpreadsheet.writeSearchResults(clientName+"2", searchResults2);
        }

        Triplet<Long, List<SearchResult>, Integer> searchResults1 = semanticClient.search(semanticRequest1);
        excelSpreadsheet.writeSearchResults("semantic1", searchResults1);
        Triplet<Long, List<SearchResult>, Integer> searchResults2 = semanticClient.search(semanticRequest2);
        excelSpreadsheet.writeSearchResults("semantic2", searchResults2);
    }
}
