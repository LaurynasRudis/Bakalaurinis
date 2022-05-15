package client.Fuseki;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.*;

public final class LabelSearchQueries {

     static String searchLabelQuery(String query) {
        return graph(ZODYNAS,
                searchQuery("( ?id ?score )", LABEL, query, "") +
                        triple("?id", RDF_TYPE, LEXICAL_ENTRY) +
                        triple("?id", LABEL, "?label"));
    }

    static String searchLabelWithSynonymsQuery(String query) {
        return graph(ZODYNAS,
                searchQuery("( " + "?tempLex" + " " + "?scoreTemp" + " )", LABEL, query, "") +
                        bind("?scoreTemp - " + DIRECT_SYNONYM_SCORE_REDUCTION, "?score") +
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

    static String searchLabelIsSynonymsQuery(String query) {
        return graph(ZODYNAS,
                searchQuery("( " + "?tempLex" + " " + "?scoreTemp" + " )", LABEL, query, "") +
                        bind("?scoreTemp - " + DIRECT_SYNONYM_SCORE_REDUCTION, "?score") +
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

    static String searchSynonymsForLabel(String query) {
        return union(
                graph(SINONIMU_ZODYNAS,
                        searchQuery("( " + "?tempId " + "?scoreTemp" + " )", LABEL, query, "") +
                                bind("?scoreTemp - " + QUERY_SYNONYM_SCORE_REDUCTION, "?score") +
                                triple("?tempId", RDF_TYPE, LEXICAL_ENTRY) +
                                triple("?tempId", LABEL, "?labelTemp2") +
                                triple("?tempId", synonymPredicate+"/"+ LABEL, "?labelTemp")
                ),
                graph(SINONIMU_ZODYNAS,
                        searchQuery("( " + "?tempId " + "?scoreTemp" + " )", LABEL, query, "") +
                                bind("?scoreTemp - " + QUERY_SYNONYM_SCORE_REDUCTION, "?score") +
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
}
