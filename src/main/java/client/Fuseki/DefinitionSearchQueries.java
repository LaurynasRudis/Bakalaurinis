package client.Fuseki;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.*;

public final class DefinitionSearchQueries {

    static String searchDefinitionQuery(String query) {
        return graph(ZODYNAS,
                searchQuery("(?textRepresentation ?score)", WRITTEN_FORM, query, "") +
                        triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION) +
                        triple("?textRepresentation",
                                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                        HAS_DEFINITION,
                                        HAS_SENSE),
                                "?id") +
                        triple("?id", LABEL, "?label")
        );
    }

    static String searchDefinitionWithSynonymsQuery(String query) {
        return graph(ZODYNAS,
                searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, "") +
                        bind("?scoreTemp -" + DIRECT_SYNONYM_SCORE_REDUCTION, "?score") +
                        triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION) +
                        triple("?textRepresentation",
                                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                        HAS_DEFINITION,
                                        HAS_SENSE),
                                "?tempLex") +
                        triple("?tempLex", LABEL, "?foundLabel") +
                        graph(SINONIMU_ZODYNAS,
                                triple("?sinLex", LABEL, "?foundLabel") +
                                        triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp")) +
                        triple("?id", LABEL, "?labelTemp") +
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?id", LABEL, "?label")
        );
    }

    static  String searchDefinitionIsSynonymsQuery(String query) { //Ar nereikia dar atvirkstinio varianto? (Tipo sinonimo sinonimai)
        return graph(ZODYNAS,
                searchQuery("(?textRepresentation ?scoreTemp)", WRITTEN_FORM, query, "") +
                        bind("?scoreTemp - " + DIRECT_SYNONYM_SCORE_REDUCTION, "?score") +
                        triple("?textRepresentation", RDF_TYPE, TEXT_REPRESENTATION) +
                        triple("?textRepresentation",
                                reverseCombinePredicates(HAS_TEXT_REPRESENTATION,
                                        HAS_DEFINITION,
                                        HAS_SENSE),
                                "?tempLex") +
                        triple("?tempLex", LABEL, "?label2") +
                        graph(SINONIMU_ZODYNAS,
                                triple("?sinLex", LABEL, "?label2") +
                                        triple("?sinLex", isSynonymPredicate+"/"+ LABEL, "?labelTemp")
                        ) +
                        triple("?id", LABEL, "?labelTemp") +
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?id", LABEL, "?label"));
    }
}
