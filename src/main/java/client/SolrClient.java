package client;

import commons.SearchResult;
import ontology.DictionaryValue;
import ontology.OntologyModel;
import org.apache.jena.ontology.Individual;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.MapSolrParams;

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

    public List<SearchResult> query(String searchText, String searchField, String searchPredicate) throws SolrServerException, IOException {
        Map<String, String> queryParamMap = new HashMap<>();
        String[] searchWords = searchText.split("\\s+");
        String searchFieldUnaccented = searchField.toLowerCase()+"_unaccented";
        StringBuilder queryTextBuilder = new StringBuilder();
        for (String searchWord : searchWords) {
            queryTextBuilder.append(searchFieldUnaccented);
            queryTextBuilder.append(":");
            queryTextBuilder.append(searchWord);
            queryTextBuilder.append(" ");
        }
        queryParamMap.put("q", queryTextBuilder.toString());
        queryParamMap.put("q.op", searchPredicate);
        queryParamMap.put("fl", "*, score");
        MapSolrParams queryParams = new MapSolrParams(queryParamMap);
        List<SearchResult> searchResults = new ArrayList<>();


        QueryResponse response = solrClient.query(queryParams);
        SolrDocumentList results = response.getResults();
        for (SolrDocument result : results) {
            String id = (String) result.getFieldValue("id");
            String label = (String) result.getFieldValue("label");
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
        return searchResults;
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
