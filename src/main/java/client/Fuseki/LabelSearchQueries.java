package client.Fuseki;

import commons.SparqlQueryBuilder.GraphQueryBuilder;
import commons.SparqlQueryBuilder.UnionQueryBuilder;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;

public final class LabelSearchQueries {

    private static void filterSearchQuery(GraphQueryBuilder graphQueryBuilder, String query, String lexicalEntryName) {
        graphQueryBuilder
                .add(triple(lexicalEntryName, RDF_TYPE, LEXICAL_ENTRY))
                .add(triple(lexicalEntryName, LABEL, "?foundLabel"))
                .add(filter("?foundLabel", query));
    }

    private static void indexSearchQuery(GraphQueryBuilder graphQueryBuilder, String query, double scoreReduction) {
        graphQueryBuilder
                .add(searchQuery("(?tempLex ?scoreTemp)", LABEL, query, ""))
                .add(bind("?scoreTemp -" + scoreReduction, "?score"))
                .add(triple("?tempLex", RDF_TYPE, LEXICAL_ENTRY))
                .add(triple("?tempLex", LABEL, "?foundLabel"));
    }

    private static void indexSearchQuery(GraphQueryBuilder graphQueryBuilder, String query) {
        graphQueryBuilder
                .add(searchQuery("(?id ?score)", LABEL, query, ""))
                .add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
    }

    private static String synonymGraph(String synonymPredicate) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        return graphQueryBuilder
                .add(triple("?sinLex", LABEL, "?foundLabel"))
                .add(triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp"))
                .build();
    }

    private static void findLexicalEntryByLabel(GraphQueryBuilder graphQueryBuilder) {
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
    }

    private static String synonymGraphWithSearch(String synonymPredicate, String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query, QUERY_SYNONYM_SCORE_REDUCTION);
        else filterSearchQuery(graphQueryBuilder, query, "?tempLex");
        graphQueryBuilder.add(triple("?tempLex", synonymPredicate+"/"+ LABEL, "?labelTemp"));
        return graphQueryBuilder.build();
    }

     static String searchLabelQuery(String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query);
        else filterSearchQuery(graphQueryBuilder, query, "?id");
        return graphQueryBuilder.build();
    }

    static String searchLabelWithSynonymsQuery(String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query, DIRECT_SYNONYM_SCORE_REDUCTION);
        else filterSearchQuery(graphQueryBuilder, query, "?tempLex");
        graphQueryBuilder.add(synonymGraph(synonymPredicate));
        findLexicalEntryByLabel(graphQueryBuilder);
        return graphQueryBuilder.build();
    }

    static String searchLabelIsSynonymsQuery(String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query, DIRECT_SYNONYM_SCORE_REDUCTION);
        else filterSearchQuery(graphQueryBuilder, query, "?tempLex");
        graphQueryBuilder.add(synonymGraph(isSynonymPredicate));
        findLexicalEntryByLabel(graphQueryBuilder);
        return graphQueryBuilder.build();
    }

    static String searchSynonymsForLabel(String query, boolean withIndex) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        unionQueryBuilder
                .add(synonymGraphWithSearch(synonymPredicate, query, withIndex))
                .add(synonymGraphWithSearch(isSynonymPredicate, query, withIndex));
        findLexicalEntryByLabel(graphQueryBuilder);
        return unionQueryBuilder.build() + graphQueryBuilder.build();
    }
}
