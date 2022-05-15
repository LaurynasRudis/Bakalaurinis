package client;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;

import java.util.*;
import java.util.Map.Entry;


public class FusekiClient {
    private final String address = "http://localhost:3030";

    private final RDFConnection service;
    private final Map<String, String> prefixes = new HashMap<>();
    private final String sinonimuZodynas = "lmf:Sinonimu_zodynas";
    private final String zodynas = "lmf:zodynas";
    private final String hasLemma = "lmf:hasLemma";
    private final String hasSense = "lmf:hasSense";
    private final String hasSenseRelation = "lmf:hasSenseRelation";
    private final String senseRelatedTo = "lmf:senseRelatedTo";
    private final String label = "rdfs:label";
    private final String writtenForm = "lmf:writtenForm";
    private final String hasTextRepresentation = "lmf:hasTextRepresentation";
    private final String hasDefinition = "lmf:hasDefinition";
    private final String hasSenseExample = "lmf:hasSenseExample";
    private final String hasEquivalent = "lmf:hasEquivalent";
    private final String type = "lmf:type";
    private final String rdfType = "rdf:type";
    private final String textQuery = "text:query";
    private final String lexicalEntry = "lmf:LexicalEntry";

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

    public void execSelectAndProcess(String query) {
        searchLabel(query);
    }

    private String makePrefixString() {
        StringBuilder prefixesString = new StringBuilder();
        for(Entry entry: prefixes.entrySet()){
            prefixesString.append("PREFIX ")
                    .append(entry.getKey())
                    .append(": <")
                    .append(entry.getValue())
                    .append(">\n");
        }
        System.out.println(prefixesString);
        return prefixesString.toString();
    }


    private String searchLabelQuery (String query) {
        return graph(zodynas,
                searchQuery("( ?id ?score )", label, query, "") +
                        triple("?id", rdfType, lexicalEntry) +
                        triple("?id", label, "?label"));
    }

    private String searchLabelWithSynonymsQuery (String query) {
        return graph(zodynas,
                searchQuery("( " + "?tempLex" + " " + "?scoreTemp" + " )", label, query, "") +
                        bind("?scoreTemp - " + directSynonymReduction, "?score") +
                        triple("?tempLex", rdfType, lexicalEntry) +
                        triple("?tempLex", label, "?foundLabel") +
                        graph(sinonimuZodynas,
                                triple("?sinLex", label, "?foundLabel") +
                                        triple("?sinLex", synonymPredicate+"/"+label, "?labelTemp")) +
                        triple("?id", label, "?labelTemp") +
                        triple("?id", rdfType, lexicalEntry) +
                        triple("?id", label, "?label")
        );
    }

    private String searchLabelIsSynonymsQuery (String query) {
        return graph(zodynas,
                searchQuery("( " + "?tempLex" + " " + "?scoreTemp" + " )", label, query, "") +
                        bind("?scoreTemp - " + directSynonymReduction, "?score") +
                        triple("?tempLex", rdfType, lexicalEntry) +
                        triple("?tempLex", label, "?label2") +
                        graph(sinonimuZodynas,
                                triple("?sinLex", label, "?label2") +
                                        triple("?sinLex", isSynonymPredicate+"/"+label, "?labelTemp")
                        ) +
                        triple("?id", label, "?labelTemp") +
                        triple("?id", rdfType, lexicalEntry) +
                        triple("?id", label, "?label"));
    }

    private String searchSynonyms (String query) {
        double querySynonymReduction = 3;
        return union(
                graph(sinonimuZodynas,
                        searchQuery("( " + "?tempId " + "?scoreTemp" + " )", label, query, "") +
                                bind("?scoreTemp - " + querySynonymReduction, "?score") +
                                triple("?tempId", rdfType, lexicalEntry) +
                                triple("?tempId", label, "?labelTemp2") +
                                triple("?tempId", synonymPredicate+"/"+label, "?labelTemp")
                ),
                graph(sinonimuZodynas,
                        searchQuery("( " + "?tempId " + "?scoreTemp" + " )", label, query, "") +
                                bind("?scoreTemp - " + querySynonymReduction, "?score") +
                                triple("?tempId", rdfType, lexicalEntry) +
                                triple("?tempId", label, "?labelTemp2") +
                                triple("?tempId", isSynonymPredicate+"/"+label, "?labelTemp") )
        ) +
                graph(zodynas,
                        triple("?id", label, "?labelTemp") +
                                triple("?id", rdfType, lexicalEntry) +
                                triple("?id", label, "?label")
                );
    }

    private String getRelevantInformationFromLexicalEntry() {
        return optional(triple("?id", combinePredicates(hasSense, hasDefinition, hasTextRepresentation, writtenForm), "?definition")) +
                optional(triple("?id", combinePredicates(hasSense, hasSenseExample, "lmf:text"), "?senseExample")) +
                optional(triple("?id", combinePredicates(hasLemma, writtenForm), "?lemma"));
    }

    private String combinePredicates(String... predicates) {
        StringBuilder predicate = new StringBuilder();
        Iterator<String> predicatesIterator = Arrays.stream(predicates).iterator();
        while (predicatesIterator.hasNext()) {
            predicate.append(predicatesIterator.next());
            if(predicatesIterator.hasNext()) predicate.append("/");
        }
        return predicate.toString();
    }

    private String reverseCombinePredicates(String... predicates) {
        StringBuilder predicate = new StringBuilder();
        Iterator<String> predicatesIterator = Arrays.stream(predicates).iterator();
        predicate.append("^");
        while (predicatesIterator.hasNext()) {
            predicate.append(predicatesIterator.next());
            if(predicatesIterator.hasNext()) predicate.append("/^");
        }
        return predicate.toString();
    }

    private String makeLabelQuerySelectString(String query) {
        String[] selects = {"?id", "?score", "?label", "?lemma", "?definition", "?senseExample"};

        String graph1 = searchLabelQuery(query);
        String graph2 = searchLabelWithSynonymsQuery(query);
        String graph3 = searchLabelIsSynonymsQuery(query);
        String graph4 = searchSynonyms(query);


        String inside = union(graph1, graph2, graph3, graph4) + getRelevantInformationFromLexicalEntry() ;

        String queryString =
                select(inside, "?id", "?score", "?label", "?lemma", "?definition", "?senseExample");
        System.out.println(queryString);
        return queryString ;
    }

    private String optional(String optionalQuery) {
        return " OPTIONAL {\n" + optionalQuery + "\n}";
    }
    private String graph(String graphName, String graphQuery) {return " GRAPH "+ graphName + "\n{\n" + graphQuery + "}\n";}
    private String bind(String bindQuery, String as) { return "BIND("+bindQuery+" as " + as + ") .\n";}
    private String union(String... graphs) {
        StringBuilder unionString = new StringBuilder();
        Iterator<String> unionIterator = Arrays.stream(graphs).iterator();
        while(unionIterator.hasNext()) {
            unionString.append("{\n")
                    .append(unionIterator.next())
                    .append("\n}\n");
            if (unionIterator.hasNext()) unionString.append(" UNION ");
        }
        return unionString.toString();
    }
    private final String synonymPredicate = combinePredicates(hasSense, hasSenseRelation, senseRelatedTo);
    private final String isSynonymPredicate = reverseCombinePredicates(senseRelatedTo, hasSenseRelation, hasSense);

    private String select(String query, String... selection) {
        StringBuilder selectBuilder = new StringBuilder();
        selectBuilder.append("SELECT ");
        for(String selectText : selection) {
            selectBuilder.append(selectText);
            selectBuilder.append(" ");
        }
        selectBuilder.append("\nWHERE \n{\n");
        selectBuilder.append(query);
        selectBuilder.append(" } ");
        return selectBuilder.toString();
    }
    private String searchQuery(String object,
                               String searchField,
                               String searchQuery,
                               String extraParameters)
    { return object + " " + textQuery + " ( " + searchField + " '" + searchQuery + "' " + extraParameters + " ) . \n" ; }

    private String triple(String subject, String predicate, String object)  { return subject + " " + predicate + " " + object + " .\n"; }

    public void searchLabel(String query) {
        try {
            Query q = QueryFactory.create(makePrefixString() + makeLabelQuerySelectString(query));
            QueryExecution test = service.query(q);
            ResultSet results = test.execSelect();
            int i = 0;
            while(results.hasNext()){
                i++;
                System.out.println(i + ". " + results.next().toString());
            }
        } catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
