package client.Fuseki;

import commons.SearchResult;
import org.apache.commons.lang.NotImplementedException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.bakalaurinis.search.SearchField;
import org.bakalaurinis.search.SearchRequest;
import org.javatuples.Pair;

import java.util.*;

import static client.Fuseki.DefinitionSearchQueries.*;
import static client.Fuseki.LabelSearchQueries.*;
import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.*;


public class FusekiClient {
    private final String address = "http://localhost:3030";

    private final RDFConnection service;
    private final Map<String, String> prefixes = new HashMap<>();


    private final double directSynonymReduction = 0.5 ;

    public FusekiClient(String serviceName) {
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(address+"/"+serviceName).queryEndpoint("sparql");
        service = builder.build();
        prefixes.put("te", "http://www.w3.org/2006/time-entry#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("lmf", "http://www.lexinfo.net/lmf#");
        prefixes.put("text", "http://jena.apache.org/text#");
    }

    private String getExtraInformationFromLexicalEntry() {
        return optional(triple("?id", combinePredicates(HAS_SENSE, HAS_DEFINITION, HAS_TEXT_REPRESENTATION, WRITTEN_FORM), "?definition")) +
                optional(triple("?id", combinePredicates(HAS_SENSE, HAS_SENSE_EXAMPLE, "lmf:text"), "?senseExample")) +
                optional(triple("?id", combinePredicates(HAS_LEMMA, WRITTEN_FORM), "?lemma"));
    }

    private String makeLabelQuerySelectString(String query) {
        String[] selects = {"?id", "?score", "?label", "?lemma", "?definition", "?senseExample"};

        String graph1 = searchLabelQuery(query);
        String graph2 = searchLabelWithSynonymsQuery(query);
        String graph3 = searchLabelIsSynonymsQuery(query);
        String graph4 = searchSynonymsForLabel(query);


        String inside = union(graph1, graph2, graph3, graph4) + getExtraInformationFromLexicalEntry() ;

        String queryString =
                select(inside, selects);
        System.out.println(queryString);
        return queryString ;
    }

    private String makeDefinitionQuerySelectString(String query) {
        String[] selects = {"?id", "?score", "?label", "?lemma", "?definition", "?senseExample"};

        String graph1 = searchDefinitionQuery(query);

        String graph2 = searchDefinitionWithSynonymsQuery(query);

        String graph3 = searchDefinitionIsSynonymsQuery(query);

        String graph4 = searchSynonymsForDefinition(query);

        String inside = union(graph1, graph2, graph3, graph4) + getExtraInformationFromLexicalEntry() ;

        String queryString =
                select(inside, selects);
        System.out.println(queryString);
        return queryString;
    }

    public Pair<Long, List<SearchResult>> execSelectAndProcess(String query, SearchField searchField){
        switch(searchField){
            case LABEL:
                return searchLabel(query);
            case DEFINITION:
                return searchDefinition(query);
            default:
                throw new RuntimeException("Blogas paieškos laukas!");
        }
    }


    private Pair<Long, List<SearchResult>> getSearchResultsFromQuery(Query query) {
        try {
            List<SearchResult> searchResults = new ArrayList<>();
            HashSet<String> definitions = new HashSet<>();
            HashSet<String> senseExamples = new HashSet<>();
            QueryExecution queryExecution = service.query(query);
            long queryStart = System.currentTimeMillis();
            ResultSet results = queryExecution.execSelect();
            long queryFinished = System.currentTimeMillis();
            long queryTime = queryFinished - queryStart;
            String lastNode = "";
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                String id = Objects.toString(solution.get("id"), "");
                String label = Objects.toString(solution.get("label"), "");
                String score = Objects.toString(solution.get("score"), "");
                String lemma = Objects.toString(solution.get("lemma"), "");
                String definition = Objects.toString(solution.get("definition"), "");
                String senseExample = Objects.toString(solution.get("senseExample"), "");
                if (lastNode.equals(id) && results.hasNext()) {
                    definitions.add(definition);
                    senseExamples.add(senseExample);
                } else {
                    lastNode = id;
                    definitions.add(definition);
                    senseExamples.add(senseExample);
                    searchResults.add(new SearchResult(id, label, lemma, score, new ArrayList<>(definitions), new ArrayList<>(senseExamples)));
                    definitions.clear();
                    senseExamples.clear();
                }
            }
            return new Pair<>(queryTime, searchResults);
        } catch (Exception e) {
            System.out.println("FAILED:" + e.getMessage());
            return new Pair<>(0L, List.of());
        }
    }

    private Pair<Long, List<SearchResult>> searchLabel(String query) {
            Query q = QueryFactory.create(makePrefixString(prefixes) + makeLabelQuerySelectString(query));
            return getSearchResultsFromQuery(q);
        }

    private Pair<Long, List<SearchResult>> searchDefinition(String query) {
        Query q = QueryFactory.create(makePrefixString(prefixes) + makeDefinitionQuerySelectString(query));
        return getSearchResultsFromQuery(q);
    }

}
