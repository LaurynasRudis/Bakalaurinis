package client;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrClient {

    private final Http2SolrClient solrClient;

    public SolrClient(String coreName) {
        String SOLR_URL = "http://localhost:8983/solr/"+coreName;
        this.solrClient = new Http2SolrClient
                .Builder(SOLR_URL)
                .build();
        this.solrClient.setParser(new XMLResponseParser());
    }

    public Map<String, String> query(String searchText) throws SolrServerException, IOException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("q", "name:"+searchText);
        MapSolrParams queryParams = new MapSolrParams(queryParamMap);
        Map<String, String> r = new HashMap<>();


        QueryResponse response = solrClient.query(queryParams);
        SolrDocumentList results = response.getResults();
        for (SolrDocument result : results) {
            String id = (String) result.getFirstValue("id");
            String name = (String) result.getFirstValue("name");
            r.put(id, name);
        }
        return r;
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
