package client.Fuseki;

import commons.SparqlQueryBuilder.GraphQueryBuilder;
import commons.SparqlQueryBuilder.UnionQueryBuilder;
import org.bakalaurinis.search.SearchField;

import java.util.Arrays;
import java.util.Iterator;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;

public class TripleSearchQueries {

    private static String queryWithPredicatesAndBoosting(String query, String searchPredicate, double boosting) {
        String[] searchWords = query.split("\\s+");
        StringBuilder searchQueryBuilder = new StringBuilder();
        Iterator<String> searchWordsIterator = Arrays.stream(searchWords).iterator();
        while(searchWordsIterator.hasNext()) {
            searchQueryBuilder
                    .append(searchWordsIterator.next())
                    .append("^")
                    .append(boosting)
                    .append(" ");
            if(searchWordsIterator.hasNext())
                searchQueryBuilder
                    .append(searchPredicate)
                    .append(" ");
        }
        return searchQueryBuilder.toString();
    }

    private static String synonymGraph(String synonymPredicate) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        return graphQueryBuilder
                .add(triple("?sinLex", LABEL, "?foundLabel"))
                .add(triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp"))
                .build();
    }

    private static String labelSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName, double boosting) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicatesAndBoosting = queryWithPredicatesAndBoosting(query, searchPredicate, boosting);
        partOfQuery.append(searchQuery("("+lexicalEntryName+ " ?score)", LABEL, queryWithPredicatesAndBoosting));
        partOfQuery.append(triple(lexicalEntryName, RDF_TYPE, LEXICAL_ENTRY));
        return partOfQuery.toString();
    }

    private static String lemmaSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName, double boosting) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicatesAndBoosting = queryWithPredicatesAndBoosting(query, searchPredicate, boosting);
        partOfQuery.append(searchQuery("(?lem ?score)", WRITTEN_FORM, queryWithPredicatesAndBoosting));
        partOfQuery.append(triple("?lem", reverseCombinePredicates(HAS_LEMMA), lexicalEntryName));
        return partOfQuery.toString();
    }

    private static String definitionSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName, double boosting) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicatesAndBoosting = queryWithPredicatesAndBoosting(query, searchPredicate, boosting);
        partOfQuery.append(searchQuery("(?txt ?score)", WRITTEN_FORM, queryWithPredicatesAndBoosting));
        partOfQuery.append(triple("?txt", reverseCombinePredicates(HAS_TEXT_REPRESENTATION, HAS_DEFINITION, HAS_SENSE), lexicalEntryName));
        return partOfQuery.toString();
    }

    private static String senseExampleSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName, double boosting) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicatesAndBoosting = queryWithPredicatesAndBoosting(query, searchPredicate, boosting);
        partOfQuery.append(searchQuery("(?sen ?score)", TEXT, queryWithPredicatesAndBoosting));
        partOfQuery.append(triple("?sen", reverseCombinePredicates(HAS_TEXT_REPRESENTATION, HAS_DEFINITION, HAS_SENSE), lexicalEntryName));
        return partOfQuery.toString();
    }

    private static void appendToUnionQueryBuilderByField(UnionQueryBuilder unionQueryBuilder,
                                                         String query,
                                                         SearchField searchField,
                                                         String searchPredicate,
                                                         String lexicalEntryName,
                                                         double labelBoosting,
                                                         double lemmaBoosting,
                                                         double definitionBoosting,
                                                         double senseExampleBoosting
                                                         ) {
        switch(searchField) {
            case LABEL:
                unionQueryBuilder.add(labelSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, labelBoosting));
                break;
            case DEFINITION:
                unionQueryBuilder.add(definitionSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, definitionBoosting));
                break;
            case LEMMA:
                unionQueryBuilder.add(lemmaSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, lemmaBoosting));
                break;
            case SENSE_EXAMPLE:
                unionQueryBuilder.add(senseExampleSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, senseExampleBoosting));
                break;
            case EVERYWHERE:
                unionQueryBuilder.add(labelSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, labelBoosting));
                unionQueryBuilder.add(lemmaSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, lemmaBoosting));
                unionQueryBuilder.add(definitionSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, definitionBoosting));
                unionQueryBuilder.add(senseExampleSearchToLexicalEntry(query, searchPredicate, lexicalEntryName, senseExampleBoosting));
                break;
            default:
                throw new RuntimeException("Blogas paieškos laukas!");
        }
    }


    public static String tripleSearchQuery(String query, SearchField searchField, String searchPredicate) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        String lexicalEntryName = "?id";
        appendToUnionQueryBuilderByField(unionQueryBuilder,
                query,
                searchField,
                searchPredicate,
                lexicalEntryName,
                5,
                4,
                2,
                1);
       return graphQueryBuilder.add(unionQueryBuilder.build()).build();
    }

    public static String tripleSearchQueryWithSynonym(String query, SearchField searchField, String searchPredicate) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        String lexicalEntryName = "?tempId";
        appendToUnionQueryBuilderByField(unionQueryBuilder,
                query,
                searchField,
                searchPredicate,
                lexicalEntryName,
                2.5,
                1.5,
                1,
                0.5);
        graphQueryBuilder.add(unionQueryBuilder.build());
        graphQueryBuilder.add(triple("?tempId", LABEL, "?foundLabel"));
        graphQueryBuilder.add(synonymGraph(synonymPredicate));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    public static String tripleSearchQueryWithIsSynonym(String query, SearchField searchField, String searchPredicate) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        String lexicalEntryName = "?tempId";
        appendToUnionQueryBuilderByField(unionQueryBuilder,
                query,
                searchField,
                searchPredicate,
                lexicalEntryName,
                2.5,
                1.5,
                1,
                0.5);
        graphQueryBuilder.add(unionQueryBuilder.build());
        graphQueryBuilder.add(triple("?tempId", LABEL, "?foundLabel"));
        graphQueryBuilder.add(synonymGraph(isSynonymPredicate));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    public static String searchSynonymsForTriple(String query, SearchField searchField, String searchPredicate) {
        GraphQueryBuilder synonymGraphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        GraphQueryBuilder blkzGraphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        UnionQueryBuilder unionSynonymQueryBuilder = new UnionQueryBuilder();
        String lexicalEntryName = "?tempId";
        appendToUnionQueryBuilderByField(unionQueryBuilder,
                query,
                searchField,
                searchPredicate,
                lexicalEntryName,
                1.25,
                1.25,
                0.1,
                0);
        unionSynonymQueryBuilder.add(triple("?tempId", synonymPredicate+"/"+ LABEL, "?querySynonym"));
        unionSynonymQueryBuilder.add(triple("?tempId", isSynonymPredicate+"/"+ LABEL, "?querySynonym"));
        synonymGraphQueryBuilder.add(unionQueryBuilder.build());
        synonymGraphQueryBuilder.add(unionSynonymQueryBuilder.build());
        blkzGraphQueryBuilder.add(triple("?id", LABEL, "?querySynonym"));
        blkzGraphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        synonymGraphQueryBuilder.add(blkzGraphQueryBuilder.build());
        return synonymGraphQueryBuilder.build();
    }
}
