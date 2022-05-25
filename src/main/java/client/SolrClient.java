package client;

import commons.SearchResult;
import ontology.DictionaryValue;
import ontology.OntologyModel;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.MapSolrParams;
import org.bakalaurinis.search.SearchField;
import org.javatuples.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SolrClient {

    private final Http2SolrClient solrClient;

    public SolrClient(String coreName) {
        String SOLR_URL = "http://localhost:8983/solr/"+coreName;
        this.solrClient = new Http2SolrClient
                .Builder(SOLR_URL)
                .build();
        this.solrClient.setParser(new XMLResponseParser());
    }

    private Map<String, String> dismaxQueryParameters(String queryText, String searchPredicate, String... fields) {
        HashMap<String, String> queryParamMap = new HashMap<>();
        StringBuilder queryFieldBuilder = new StringBuilder();
        for(String field : fields) {
            queryFieldBuilder.append(field);
            queryFieldBuilder.append(" ");
        }
        queryParamMap.put("q", queryText);
        queryParamMap.put("q.op", searchPredicate);
        queryParamMap.put("fl", "*, score");
        queryParamMap.put("qf", queryFieldBuilder.toString());
        queryParamMap.put("defType", "dismax");
        System.out.println(queryParamMap);
        return queryParamMap;
    }

    public Pair<Long, List<SearchResult>> query(String searchText, SearchField searchField, String searchPredicate) throws SolrServerException, IOException {
        double labelBoosting = 5;
        double lemmaBoosting = 4;
        double definitionBoosting = 2;
        double senseExampleBoosting = 1;
        String[] searchFields = {"label_unaccented^"+labelBoosting,
                "lemma_unaccented^"+lemmaBoosting,
                "definition_unaccented^"+definitionBoosting,
                "senseExample_unaccented^"+senseExampleBoosting};
        Map<String, String> queryParamMap;
        switch(searchField) {
            case LABEL:
                queryParamMap = dismaxQueryParameters(searchText, searchPredicate, searchFields[0]);
                break;
            case LEMMA:
                queryParamMap = dismaxQueryParameters(searchText, searchPredicate, searchFields[1]);
                break;
            case DEFINITION:
                queryParamMap = dismaxQueryParameters(searchText, searchPredicate, searchFields[2]);
                break;
            case SENSE_EXAMPLE:
                queryParamMap = dismaxQueryParameters(searchText, searchPredicate, searchFields[3]);
                break;
            case EVERYWHERE:
                queryParamMap = dismaxQueryParameters(searchText, searchPredicate, searchFields);
                break;
            default:
                throw new RuntimeException("Bad search field!");
        }
        MapSolrParams queryParams = new MapSolrParams(queryParamMap);
        System.out.println(queryParams);
        List<SearchResult> searchResults = new ArrayList<>();

        long startQuery = System.currentTimeMillis();
        QueryResponse response = solrClient.query(queryParams);
        long finishQuery = System.currentTimeMillis();
        long queryTime = finishQuery - startQuery;
        System.out.println(response);
        SolrDocumentList results = response.getResults();
        for (SolrDocument result : results) {
            System.out.println(result);
            String id = Objects.toString(result.getFieldValue("id"), "");
            String label = Objects.toString(result.getFieldValue("label"),"");
            String lemma = (String) result.getFieldValue("lemma");
            List<String> senseExample = result.getFieldValues("senseExample").stream()
                    .map(object -> Objects.toString(object, ""))
                    .collect(Collectors.toList());
            List<String> definition = result.getFieldValues("definition").stream()
                    .map(object -> Objects.toString(object, ""))
                    .collect(Collectors.toList());
            String score = String.valueOf(result.getFieldValue("score"));
            searchResults.add(new SearchResult(id, label, lemma, score, definition, senseExample));
        }
        return new Pair<>(queryTime, searchResults);
    }

    public void indexOntologyModel(OntologyModel model) {
        try {
            List<DictionaryValue> dictionaryValues = model.getDictionaryValues();
            for (DictionaryValue dv : dictionaryValues) {
                solrClient.addBean(dv);
            }
            UpdateResponse updateResponse = solrClient.commit();
            System.out.println(updateResponse.toString());
        }
        catch (Exception e) {
            System.out.println("Couldn't index. Exception: " + e.getMessage());
        }
    }
}
