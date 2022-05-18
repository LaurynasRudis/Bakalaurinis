package client.Fuseki;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.*;

public final class DefinitionSearchQueries {

    static String searchDefinitionQuery(String query) {
        return graph(ZODYNAS_BE_AKCENTU,
                searchQuery("(?textRepresentation ?score)", WRITTEN_FORM, query, ""),
                        triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION),
                        triple("?textRepresentation",
                                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                        HAS_DEFINITION,
                                        HAS_SENSE),
                                "?id")
        );
    }

    static String searchDefinitionWithSynonymsQuery(String query) {
        return graph(ZODYNAS_BE_AKCENTU,
                searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, ""),
                        bind("?scoreTemp -" + DIRECT_SYNONYM_SCORE_REDUCTION, "?score"),
                        triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION) +
                        triple("?textRepresentation",
                                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                        HAS_DEFINITION,
                                        HAS_SENSE),
                                "?tempLex") +
                        triple("?tempLex", LABEL, "?foundLabel"),
                        graph(SINONIMU_ZODYNAS,
                                triple("?sinLex", LABEL, "?foundLabel") +
                                        triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp")),
                        triple("?id", LABEL, "?labelTemp"),
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY)
        );
    }

    static  String searchDefinitionIsSynonymsQuery(String query) {
        return graph(ZODYNAS_BE_AKCENTU,
                searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, ""),
                        bind("?scoreTemp - " + DIRECT_SYNONYM_SCORE_REDUCTION, "?score"),
                        triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION),
                        triple("?textRepresentation",
                                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                        HAS_DEFINITION,
                                        HAS_SENSE),
                                "?tempLex"),
                        triple("?tempLex", LABEL, "?label2"),
                        graph(SINONIMU_ZODYNAS,
                                triple("?sinLex", LABEL, "?label2") +
                                        triple("?sinLex", isSynonymPredicate+"/"+ LABEL, "?labelTemp")
                        ),
                        triple("?id", LABEL, "?labelTemp"),
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY)
        );
    }

    static String searchSynonymsForDefinition(String query) {
        return union(
                graph(SINONIMU_ZODYNAS,
                        searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, ""),
                                bind("?scoreTemp - " + QUERY_SYNONYM_SCORE_REDUCTION, "?score"),
                                triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION),
                                triple("?textRepresentation",
                                        reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                                HAS_DEFINITION,
                                                HAS_SENSE),
                                        "?tempId"),
                                triple("?tempId", LABEL, "?labelTemp2"),
                                triple("?tempId", synonymPredicate+"/"+ LABEL, "?labelTemp")
                ),
                graph(SINONIMU_ZODYNAS,
                        searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, ""),
                                bind("?scoreTemp - " + QUERY_SYNONYM_SCORE_REDUCTION, "?score"),
                                triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION),
                                triple("?textRepresentation",
                                        reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                                HAS_DEFINITION,
                                                HAS_SENSE),
                                        "?tempId") +
                                triple("?tempId", LABEL, "?labelTemp2"),
                                triple("?tempId", isSynonymPredicate+"/"+ LABEL, "?labelTemp") )
        ) +
                graph(ZODYNAS_BE_AKCENTU,
                        triple("?id", LABEL, "?labelTemp"),
                                triple("?id", RDF_TYPE, LEXICAL_ENTRY)
                );
    }
}
