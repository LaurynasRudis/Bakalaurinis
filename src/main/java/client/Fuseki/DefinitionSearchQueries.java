package client.Fuseki;

import commons.SparqlQueryBuilder.GraphQueryBuilder;
import commons.SparqlQueryBuilder.UnionQueryBuilder;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.synonymPredicate;

public final class DefinitionSearchQueries {

    private static void filterSearchQuery(GraphQueryBuilder graphQueryBuilder, String query) {
       graphQueryBuilder
               .add(triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION))
               .add(triple("?textRepresentation", WRITTEN_FORM, "?writtenForm"))
               .add(filter("?writtenForm", query));
    }

    private static void indexSearchQuery(GraphQueryBuilder graphQueryBuilder, String query, double scoreReduction) {
        graphQueryBuilder
                .add(searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, ""))
                .add(bind("?scoreTemp -" + scoreReduction, "?score"))
                .add(triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION));
    }

    private static void indexSearchQuery(GraphQueryBuilder graphQueryBuilder, String query) {
        graphQueryBuilder
                .add(searchQuery("(?textRepresentation ?score)", WRITTEN_FORM, query, ""))
                .add(triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION));
    }

    private static void lexicalEntryFromTextRepresentation(GraphQueryBuilder graphQueryBuilder) {
        graphQueryBuilder.add(triple("?textRepresentation",
                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                        HAS_DEFINITION,
                        HAS_SENSE),
                "?id"));
    }

    private static void lexicalEntryFromTextRepresentation(GraphQueryBuilder graphQueryBuilder, String object, String labelName) {
        graphQueryBuilder.add(triple("?textRepresentation",
                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                        HAS_DEFINITION,
                        HAS_SENSE),
                object));
        graphQueryBuilder.add(triple(object, LABEL, labelName));
    }

    private static String synonymGraph(String synonymPredicate, String objectName) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        return graphQueryBuilder
                .add(triple("?sinLex", LABEL, objectName))
                .add(triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp"))
                .build();
    }

    private static String synonymGraphWithSearch(String synonymPredicate, String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query, QUERY_SYNONYM_SCORE_REDUCTION);
        else filterSearchQuery(graphQueryBuilder, query);
        lexicalEntryFromTextRepresentation(graphQueryBuilder, "?tempId", "?labelTemp");
        graphQueryBuilder.add(triple("?tempId", synonymPredicate+"/"+ LABEL, "?labelTemp"));
        return graphQueryBuilder.build();
    }

    static String searchDefinitionQuery(String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        if(withIndex) {
            indexSearchQuery(graphQueryBuilder, query);
        }
        else {
            filterSearchQuery(graphQueryBuilder, query);
        }
        lexicalEntryFromTextRepresentation(graphQueryBuilder);
        return graphQueryBuilder.build();
    }

    static String searchDefinitionWithSynonymsQuery(String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query, DIRECT_SYNONYM_SCORE_REDUCTION);
        else filterSearchQuery(graphQueryBuilder, query);
        lexicalEntryFromTextRepresentation(graphQueryBuilder, "?tempLex", "?foundLabel");
        graphQueryBuilder.add(synonymGraph(synonymPredicate, "?foundLabel"));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    static String searchDefinitionIsSynonymsQuery(String query, boolean withIndex) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        if(withIndex) indexSearchQuery(graphQueryBuilder, query, DIRECT_SYNONYM_SCORE_REDUCTION);
        else filterSearchQuery(graphQueryBuilder, query);
        lexicalEntryFromTextRepresentation(graphQueryBuilder, "?tempLex", "?label2");
        graphQueryBuilder.add(synonymGraph(isSynonymPredicate, "?label2"));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    static String searchSynonymsForDefinition(String query, boolean withIndex) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        unionQueryBuilder
                .add(synonymGraphWithSearch(synonymPredicate, query, withIndex))
                .add(synonymGraphWithSearch(isSynonymPredicate, query, withIndex));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return unionQueryBuilder.build() + graphQueryBuilder.build();
    }
}
