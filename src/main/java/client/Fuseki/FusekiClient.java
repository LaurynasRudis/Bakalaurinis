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
import org.bakalaurinis.search.SemanticSearchService;
import org.javatuples.Pair;

import java.util.*;
import java.util.concurrent.Callable;

import static client.Fuseki.EntitySearchQueries.*;
import static client.Fuseki.TripleSearchQueries.*;
import static client.Fuseki.UnindexedSearchQueries.*;
import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;


public class FusekiClient {

    private RDFConnection service;
    private final Map<String, String> prefixes = new HashMap<>();
    private final String[] selects = {"?id", "?(sample(str(?score)) as ?s)", "?label", "?lemma", "?definition", "?senseExample"};

    public FusekiClient() {
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

    private void buildService(String serviceName) {
        String address = "http://localhost:3030";
        RDFConnectionRemoteBuilder builder = RDFConnectionFuseki.create().destination(address +"/"+serviceName).queryEndpoint("sparql");
        service = builder.build();
    }

    private String makeEntityQuerySelectString(String query, SemanticSearchOptions searchOptions, String searchPredicate, SearchField searchField) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        String mainGraph = entitySearchQueries(query, searchPredicate, searchField);
        unionQueryBuilder.add(mainGraph);

        if(searchOptions.getSearchWithSynonyms()) {
            String graph = entitySearchQueriesWithSynonym(query, searchPredicate, searchField);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithIsSynonym()) {
            String graph = entitySearchQueriesWithIsSynonym(query, searchPredicate, searchField);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithQuerySynonyms()) {
            String graph = searchSynonymsForEntity(query, searchPredicate, searchField);
            unionQueryBuilder.add(graph);
        }

        String inside = unionQueryBuilder.build() + getInformationFromBLKZLexicalEntry();

        String queryString =
                select(inside, "?id ?label ?lemma ?definition ?senseExample", "?s", selects);
        System.out.println(queryString);
        return queryString;
    }

    private String makeTripleQuerySelectString(String query, SemanticSearchOptions searchOptions, String searchPredicate, SearchField searchField) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        String mainGraph = tripleSearchQuery(query, searchField, searchPredicate);
        unionQueryBuilder.add(mainGraph);

        if(searchOptions.getSearchWithSynonyms()) {
            String graph = tripleSearchQueryWithSynonym(query, searchField, searchPredicate);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithIsSynonym()) {
            String graph = tripleSearchQueryWithIsSynonym(query, searchField, searchPredicate);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithQuerySynonyms()) {
            String graph = searchSynonymsForTriple(query, searchField, searchPredicate);
            unionQueryBuilder.add(graph);
        }

        String inside = unionQueryBuilder.build() + getInformationFromBLKZLexicalEntry();

        String queryString =
                select(inside, "?id ?label ?lemma ?definition ?senseExample", "?s", selects);
        System.out.println(queryString);
        return queryString;
    }

    private String makeUnindexedQuerySelectString(String query, SemanticSearchOptions searchOptions, String searchPredicate, SearchField searchField) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        String mainGraph = unindexSearchQuery(query, searchField, searchPredicate);
        unionQueryBuilder.add(mainGraph);

        if(searchOptions.getSearchWithSynonyms()) {
            String graph = unindexedSearchQueryWithSynonym(query, searchField, searchPredicate);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithIsSynonym()) {
            String graph = unindexedSearchQueryWithIsSynonym(query, searchField, searchPredicate);
            unionQueryBuilder.add(graph);
        }

        if(searchOptions.getSearchWithQuerySynonyms()) {
            String graph = searchSynonymsForUnindexedTriple(query, searchField, searchPredicate);
            unionQueryBuilder.add(graph);
        }

        String inside = unionQueryBuilder.build() + getInformationFromBLKZLexicalEntry();

        String queryString =
                select(inside, "?id ?label ?lemma ?definition ?senseExample", "?s", selects);
        System.out.println(queryString);
        return queryString;
    }

    public Pair<Long, List<SearchResult>> execSelectAndProcess(String query, SearchField searchField, SemanticSearchOptions searchOptions, String searchPredicate){
        SemanticSearchService searchService = searchOptions.getSemanticSearchService();
        switch(searchService){
            case BLKZ_UNINDEXED:
                buildService("blkz_unindexed");
                return searchUnindexed(query, searchOptions, searchPredicate, searchField);
            case BLKZ_TRIPLE:
                buildService("blkz_triple");
                return searchTriple(query, searchOptions, searchPredicate, searchField);
            case BLKZ_ENTITY:
                buildService("blkz");
                return searchEntity(query, searchOptions, searchPredicate, searchField);
            default:
                throw new RuntimeException("Blogas paieskos serviso pasirinkimas!");
        }
    }

    private Pair<Long, List<SearchResult>> searchTriple(String query, SemanticSearchOptions searchOptions, String searchPredicate, SearchField searchField) {
        Query q = QueryFactory.create(makePrefixString(prefixes) + makeTripleQuerySelectString(query, searchOptions, searchPredicate, searchField));
        return getSearchResultsFromQuery(q);
    }

    private Pair<Long, List<SearchResult>> searchUnindexed(String query, SemanticSearchOptions searchOptions, String searchPredicate, SearchField searchField) {
        Query q = QueryFactory.create(makePrefixString(prefixes) + makeUnindexedQuerySelectString(query, searchOptions, searchPredicate, searchField));
        return getSearchResultsFromQuery(q);
    }

    private Pair<Long, List<SearchResult>> searchEntity(String query, SemanticSearchOptions searchOptions, String searchPredicate, SearchField searchField) {
        Query q = QueryFactory.create(makePrefixString(prefixes) + makeEntityQuerySelectString(query, searchOptions, searchPredicate, searchField));
        return getSearchResultsFromQuery(q);
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

}
