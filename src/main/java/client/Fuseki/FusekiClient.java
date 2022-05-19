package client.Fuseki;

import commons.SearchResult;
import commons.SparqlQueryBuilder.GraphQueryBuilder;
import commons.SparqlQueryBuilder.UnionQueryBuilder;
import org.apache.jena.query.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.bakalaurinis.search.SearchField;
import org.bakalaurinis.search.SemanticSearchOptions;
import org.javatuples.Pair;

import java.util.*;

import static client.Fuseki.DefinitionSearchQueries.*;
import static client.Fuseki.LabelSearchQueries.*;
import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;


public class FusekiClient {

    private final RDFConnection service;
    private final Map<String, String> prefixes = new HashMap<>();
    private final String[] selects = {"?id", "(str(?score) as ?s)", "?label", "?lemma", "?definition", "?senseExample"};

    public FusekiClient(String serviceName) {
        String address = "http://localhost:3030";
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(address +"/"+serviceName).queryEndpoint("sparql");
        service = builder.build();
        prefixes.put("te", "http://www.w3.org/2006/time-entry#");
        prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prefixes.put("lmf", "http://www.lexinfo.net/lmf#");
        prefixes.put("text", "http://jena.apache.org/text#");
    }

    private String getInformationFromBLKZLexicalEntry() {
        return graph(ZODYNAS,
                triple("?id", LABEL, "?label"),
                optional(triple("?id", combinePredicates(HAS_SENSE, HAS_DEFINITION, HAS_TEXT_REPRESENTATION, WRITTEN_FORM), "?definition")),
                optional(triple("?id", combinePredicates(HAS_SENSE, HAS_SENSE_EXAMPLE, "lmf:text"), "?senseExample")),
                optional(triple("?id", combinePredicates(HAS_LEMMA, WRITTEN_FORM), "?lemma"))
        );
    }

    private String makeLabelQuerySelectString(String query, SemanticSearchOptions searchOptions) {

        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        boolean withIndex = searchOptions.getUseIndexes();
        String mainGraph = searchLabelQuery(query, withIndex);
        unionQueryBuilder.add(mainGraph);
        if(searchOptions.getSearchWithSynonyms()){
            String graph = searchLabelWithSynonymsQuery(query, withIndex);
            unionQueryBuilder.add(graph);
        }
        if(searchOptions.getSearchWithIsSynonym()){
            String graph = searchLabelIsSynonymsQuery(query, withIndex);
            unionQueryBuilder.add(graph);
        }
        if(searchOptions.getSearchWithQuerySynonyms()){
            String graph = searchSynonymsForLabel(query, withIndex);
            unionQueryBuilder.add(graph);
        }

        String inside = unionQueryBuilder.build() + getInformationFromBLKZLexicalEntry() ;

        String queryString =
                select(inside, selects);
        System.out.println(queryString);
        return queryString ;
    }

    private String makeDefinitionQuerySelectString(String query, SemanticSearchOptions searchOptions) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        boolean withIndex = searchOptions.getUseIndexes();

        String mainGraph = searchDefinitionQuery(query, withIndex);
        unionQueryBuilder.add(mainGraph);

        if(searchOptions.getSearchWithSynonyms()) {
            String graph = searchDefinitionWithSynonymsQuery(query, withIndex);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithIsSynonym()) {
            String graph = searchDefinitionIsSynonymsQuery(query, withIndex);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithQuerySynonyms()) {
            String graph = searchSynonymsForDefinition(query, withIndex);
            unionQueryBuilder.add(graph);
        }

        String inside = unionQueryBuilder.build() + getInformationFromBLKZLexicalEntry();

        String queryString =
                select(inside, selects);
        System.out.println(queryString);
        return queryString;
    }

    public Pair<Long, List<SearchResult>> execSelectAndProcess(String query, SearchField searchField, SemanticSearchOptions searchOptions){
        switch(searchField){
            case LABEL:
                return searchLabel(query, searchOptions);
            case DEFINITION:
                return searchDefinition(query, searchOptions);
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
            String lastId = "";
            String lemma = "";
            String label = "";
            String score = "";
            while(results.hasNext()) {
                QuerySolution solution = results.next();
                String id = Objects.toString(solution.get("id"), "");
                if (lastId.isBlank()) {
                    lemma = Objects.toString(solution.get("lemma"), "");
                    label = Objects.toString(solution.get("label"), "");
                    score = Objects.toString(solution.get("s"), "");
                    lastId = id;
                }
                if (!lastId.equals(id)) {
                    searchResults.add(new SearchResult(lastId, label, lemma, score, new ArrayList<>(definitions), new ArrayList<>(senseExamples)));
                    lemma = Objects.toString(solution.get("lemma"), "");
                    label = Objects.toString(solution.get("label"), "");
                    score = Objects.toString(solution.get("s"), "");
                    lastId = id;
                    definitions.clear();
                    senseExamples.clear();
                }
                definitions.add(Objects.toString(solution.get("definition"), ""));
                senseExamples.add(Objects.toString(solution.get("senseExample"), ""));
                if (!results.hasNext()) {
                    searchResults.add(new SearchResult(id, label, lemma, score, new ArrayList<>(definitions), new ArrayList<>(senseExamples)));
                }
            }
            return new Pair<>(queryTime, searchResults);
        } catch (Exception e) {
            System.out.println("FAILED:" + e.getMessage());
            return new Pair<>(0L, List.of());
        }
    }

    private Pair<Long, List<SearchResult>> searchLabel(String query, SemanticSearchOptions searchOptions) {
            Query q = QueryFactory.create(makePrefixString(prefixes) + makeLabelQuerySelectString(query, searchOptions));
            return getSearchResultsFromQuery(q);
        }

    private Pair<Long, List<SearchResult>> searchDefinition(String query, SemanticSearchOptions searchOptions) {
        Query q = QueryFactory.create(makePrefixString(prefixes) + makeDefinitionQuerySelectString(query, searchOptions));
        return getSearchResultsFromQuery(q);
    }

}
