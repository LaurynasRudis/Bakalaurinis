package client;

import commons.SearchResult;
import org.apache.commons.lang.NotImplementedException;
import org.apache.jena.query.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.bakalaurinis.search.SearchRequest;

import java.util.*;

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


    private String searchLabelQuery (String query) {
        return graph(ZODYNAS,
                searchQuery("( ?id ?score )", LABEL, query, "") +
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?id", LABEL, "?label"));
    }

    private String searchLabelWithSynonymsQuery (String query) {
        return graph(ZODYNAS,
                searchQuery("( " + "?tempLex" + " " + "?scoreTemp" + " )", LABEL, query, "") +
                        bind("?scoreTemp - " + directSynonymReduction, "?score") +
                        triple("?tempLex", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?tempLex", LABEL, "?foundLabel") +
                        graph(SINONIMU_ZODYNAS,
                                triple("?sinLex", LABEL, "?foundLabel") +
                                        triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp")) +
                        triple("?id", LABEL, "?labelTemp") +
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?id", LABEL, "?label")
        );
    }

    private String searchLabelIsSynonymsQuery (String query) {
        return graph(ZODYNAS,
                searchQuery("( " + "?tempLex" + " " + "?scoreTemp" + " )", LABEL, query, "") +
                        bind("?scoreTemp - " + directSynonymReduction, "?score") +
                        triple("?tempLex", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?tempLex", LABEL, "?label2") +
                        graph(SINONIMU_ZODYNAS,
                                triple("?sinLex", LABEL, "?label2") +
                                        triple("?sinLex", isSynonymPredicate+"/"+ LABEL, "?labelTemp")
                        ) +
                        triple("?id", LABEL, "?labelTemp") +
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?id", LABEL, "?label"));
    }

    private String searchSynonymsForLabel(String query) {
        double querySynonymReduction = 3;
        return union(
                graph(SINONIMU_ZODYNAS,
                        searchQuery("( " + "?tempId " + "?scoreTemp" + " )", LABEL, query, "") +
                                bind("?scoreTemp - " + querySynonymReduction, "?score") +
                                triple("?tempId", RDF_TYPE, LEXICAL_ENTRY) +
                                triple("?tempId", LABEL, "?labelTemp2") +
                                triple("?tempId", synonymPredicate+"/"+ LABEL, "?labelTemp")
                ),
                graph(SINONIMU_ZODYNAS,
                        searchQuery("( " + "?tempId " + "?scoreTemp" + " )", LABEL, query, "") +
                                bind("?scoreTemp - " + querySynonymReduction, "?score") +
                                triple("?tempId", RDF_TYPE, LEXICAL_ENTRY) +
                                triple("?tempId", LABEL, "?labelTemp2") +
                                triple("?tempId", isSynonymPredicate+"/"+ LABEL, "?labelTemp") )
        ) +
                graph(ZODYNAS,
                        triple("?id", LABEL, "?labelTemp") +
                                triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                                triple("?id", LABEL, "?label")
                );
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

    public List<SearchResult> execSelectAndProcess(String query, SearchRequest.SearchField searchField){
        switch(searchField){
            case LABEL:
                return searchLabel(query);
            case DEFINITION:
                return searchDefinition(query);
            default:
                throw new RuntimeException("Blogas paieškos laukas!");
        }
    }

    public List<SearchResult> searchDefinition(String query) {
        throw new NotImplementedException("Not implemented yet!");
    }

    private List<SearchResult> getSearchResultsFromQuery(Query query) {
        try {
            List<SearchResult> searchResults = new ArrayList<>();
            HashSet<String> definitions = new HashSet<>();
            HashSet<String> senseExamples = new HashSet<>();
            QueryExecution queryExecution = service.query(query);
            ResultSet results = queryExecution.execSelect();
            String lastNode = "";
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                String id = solution.get("id").toString();
                String label = solution.get("label").toString();
                String score = solution.get("score").toString();
                String lemma = solution.get("lemma").toString();
                String definition = solution.get("definition").toString();
                String senseExample = solution.get("senseExample").toString();
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
            return searchResults;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return List.of();
        }
    }

    public List<SearchResult> searchLabel(String query) {
            Query q = QueryFactory.create(makePrefixString(prefixes) + makeLabelQuerySelectString(query));
            return getSearchResultsFromQuery(q);
        }
}
