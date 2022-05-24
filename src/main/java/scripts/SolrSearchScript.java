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
        String fileLocation = "rezultatai.xlsx";
        String address = "localhost";

        ExcelSpreadsheet excelSpreadsheet = new ExcelSpreadsheet(fileLocation);

        SemanticSearchOptions allOptionsTrueWithIndexesEntity = SemanticSearchOptions.newBuilder()
                .setSearchWithSynonyms(true)
                .setSearchWithIsSynonym(true)
                .setSearchWithQuerySynonyms(true)
                .setUseIndexes(true)
                .build();

        SemanticSearchOptions allOptionsTrueWithIndexesTriple = SemanticSearchOptions.newBuilder()
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

        GrpcClient tfidfClient = new GrpcClient(address, 5540);
        GrpcClient bm25Client = new GrpcClient(address,5541);
        GrpcClient dfrClient = new GrpcClient(address, 5542);
        GrpcClient fusekiClient = new GrpcClient(address, 5543);

        Map<String, GrpcClient> solrClients = new HashMap<>();
        solrClients.put("tfidf", tfidfClient);
        solrClients.put("bm25", bm25Client);
        solrClients.put("dfr", dfrClient);

        SearchRequest searchRequest1 = createSearchRequest("velnias", SearchField.DEFINITION, SearchPredicate.OR);
        SearchRequest searchRequest2 = createSearchRequest("velnias", SearchField.LABEL, SearchPredicate.OR);
        SearchRequest semanticRequest1 = createSearchRequest("velnias", SearchField.LABEL, SearchPredicate.OR, allOptionsTrueWithoutIndexes);
        SearchRequest semanticRequest2 = createSearchRequest("velnias", SearchField.LABEL, SearchPredicate.OR, allOptionsTrueWithIndexes);
        SearchRequest semanticRequest3 = createSearchRequest("brolio žmona", SearchField.EVERYWHERE, SearchPredicate.AND, allOptionsTrueWithIndexes);

//        for(Map.Entry<String, GrpcClient> clientEntry : solrClients.entrySet()) {
//            GrpcClient client = clientEntry.getValue();
//            Triplet<Long, List<SearchResult>, Integer> searchResults1 = client.search(searchRequest1);
//            String clientName = clientEntry.getKey();
//            excelSpreadsheet.writeSearchResults(clientName+"1", searchResults1);
//            Triplet<Long, List<SearchResult>, Integer> searchResults2 = client.search(searchRequest2);
//            excelSpreadsheet.writeSearchResults(clientName+"2", searchResults2);
//        }

        Triplet<Long, List<SearchResult>, Integer> searchResults1 = entityClient.search(semanticRequest3);
        excelSpreadsheet.writeSearchResults("semantic1", searchResults1);
        Triplet<Long, List<SearchResult>, Integer> searchResults2 = fusekiTripleClient.search(semanticRequest2);
        excelSpreadsheet.writeSearchResults("semantic2", searchResults2);
        Triplet<Long, List<SearchResult>, Integer> searchResults3 = fusekiUnindexed.search(semanticRequest1);
        excelSpreadsheet.writeSearchResults("semantic3", searchResults3);
    }
}
