//package client.Fuseki;
//
//import commons.SparqlQueryBuilder.GraphQueryBuilder;
//import commons.SparqlQueryBuilder.UnionQueryBuilder;
//
//import static commons.OntologyConstants.*;
//import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;
//
//public final class LemmaSearchQueries {
//
//    private static void filterSearchQuery(GraphQueryBuilder graphQueryBuilder, String query) {
//       graphQueryBuilder
//               .add(triple("?lemmaTemp", RDF_TYPE, LEMMA))
//               .add(triple("?lemmaTemp", WRITTEN_FORM, "?writtenForm"))
//               .add(filter("?writtenForm", query));
//    }
//
//    private static void indexSearchQuery(GraphQueryBuilder graphQueryBuilder, String query, double scoreReduction) {
//        graphQueryBuilder
//                .add(searchQuery("(?lemmaTemp ?scoreTemp)", WRITTEN_FORM, query, ""))
//                .add(bind("?scoreTemp -" + scoreReduction, "?score"))
//                .add(triple("?lemmaTemp", RDF_TYPE, LEMMA));
//    }
//
//    private static void indexSearchQuery(GraphQueryBuilder graphQueryBuilder, String query) {
//        graphQueryBuilder
//                .add(searchQuery("(?lemmaTemp ?score)", WRITTEN_FORM, query, ""))
//                .add(triple("?lemmaTemp", RDF_TYPE, LEMMA));
//    }
//
//    private static void lexicalEntryFromLemma(GraphQueryBuilder graphQueryBuilder) {
//        graphQueryBuilder.add(triple("?lemmaTemp",
//                reverseCombinePredicates(HAS_LEMMA),
//                "?id"));
//    }
//
//    private static void lexicalEntryFromLemma(GraphQueryBuilder graphQueryBuilder, String object, String lemmaName) {
//        graphQueryBuilder.add(triple(lemmaName,
//                reverseCombinePredicates(HAS_LEMMA),
//                object));
//    }
//
//    private static String synonymGraph(String synonymPredicate, String objectName) {
//        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
//        return graphQueryBuilder
//                .add(triple("?sinLemma", WRITTEN_FORM, objectName))
//                .add(triple("?sinLex", HAS_LEMMA, "?sinLemma"))
//                .add(triple("?sinLex", synonymPredicate+"/"+ HAS_LEMMA + "/" + WRITTEN_FORM, "?writtenLemma"))
//                .build();
//    }
//
//    private static String synonymGraphWithSearch(String synonymPredicate, String query, boolean withIndex) {
//        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
//        if(withIndex) indexSearchQuery(graphQueryBuilder, query, QUERY_SYNONYM_SCORE_REDUCTION);
//        else filterSearchQuery(graphQueryBuilder, query);
//        lexicalEntryFromLemma(graphQueryBuilder, "?tempId", "?lemmaTemp");
//        graphQueryBuilder.add(triple("?tempId", synonymPredicate+"/"+ HAS_LEMMA + "/" + WRITTEN_FORM, "?writtenTemp3"));
//        return graphQueryBuilder.build();
//    }
//
//    static String searchLemmaQuery(String query, boolean withIndex) {
//        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
//        if(withIndex) {
//            indexSearchQuery(graphQueryBuilder, query);
//        }
//        else {
//            filterSearchQuery(graphQueryBuilder, query);
//        }
//        lexicalEntryFromLemma(graphQueryBuilder);
//        return graphQueryBuilder.build();
//    }
//
//    static String searchLemmaWithSynonymsQuery(String query, boolean withIndex) {
//        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
//        if (withIndex) indexSearchQuery(graphQueryBuilder, query, DIRECT_SYNONYM_SCORE_REDUCTION);
//        else filterSearchQuery(graphQueryBuilder, query);
//        graphQueryBuilder.add(triple("?lemmaTemp", WRITTEN_FORM, "?writTemp"));
//        graphQueryBuilder.add(synonymGraph(synonymPredicate, "?writTemp"));
//        graphQueryBuilder.add(triple("?lemma3", WRITTEN_FORM, "?writtenLemma"));
//        graphQueryBuilder.add(triple("?id", combinePredicates(HAS_LEMMA, WRITTEN_FORM), "?lemma3"));
//        return graphQueryBuilder.build();
//    }
//
//    static String searchLemmaIsSynonymsQuery(String query, boolean withIndex) {
//        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
//        if(withIndex) indexSearchQuery(graphQueryBuilder, query, DIRECT_SYNONYM_SCORE_REDUCTION);
//        else filterSearchQuery(graphQueryBuilder, query);
//        graphQueryBuilder.add(triple("?lemmaTemp", WRITTEN_FORM, "?writTemp"));
//        graphQueryBuilder.add(synonymGraph(isSynonymPredicate, "?writTemp"));
//        graphQueryBuilder.add(triple("?lemma3", WRITTEN_FORM, "?writtenLemma"));
//        graphQueryBuilder.add(triple("?id", combinePredicates(HAS_LEMMA, WRITTEN_FORM), "?lemma3"));
//        return graphQueryBuilder.build();
//    }
//
//    static String searchSynonymsForLemma(String query, boolean withIndex) {
//        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
//        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
//        unionQueryBuilder
//                .add(synonymGraphWithSearch(synonymPredicate, query, withIndex))
//                .add(synonymGraphWithSearch(isSynonymPredicate, query, withIndex));
//        graphQueryBuilder.add(triple("?id", combinePredicates(HAS_LEMMA, WRITTEN_FORM), "?writtenTemp3"));
//        return unionQueryBuilder.build() + graphQueryBuilder.build();
//    }
//}
