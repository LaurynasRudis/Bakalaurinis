package client.Fuseki;

import commons.SparqlQueryBuilder.GraphQueryBuilder;
import commons.SparqlQueryBuilder.UnionQueryBuilder;
import org.bakalaurinis.search.SearchField;

import java.util.Arrays;
import java.util.Iterator;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;

public class UnindexedSearchQueries {

    private static String queryWithPredicates(String query, String searchPredicate) {
        String[] searchWords = query.split("\\s+");
        StringBuilder searchQueryBuilder = new StringBuilder();
        Iterator<String> searchWordsIterator = Arrays.stream(searchWords).iterator();
        while(searchWordsIterator.hasNext()) {
            searchQueryBuilder
                    .append("(?=.*")
                    .append(searchWordsIterator.next())
                    .append(")");
            if(searchWordsIterator.hasNext())
                if(searchPredicate.equals("OR"))
                    searchQueryBuilder.append("|");
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

    private static String labelSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicates = queryWithPredicates(query, searchPredicate);
        partOfQuery.append(triple(lexicalEntryName, RDF_TYPE, LEXICAL_ENTRY));
        partOfQuery.append(triple(lexicalEntryName, LABEL, "?labFilter"));
        partOfQuery.append(filter("?labFilter", queryWithPredicates));
        return partOfQuery.toString();
    }

    private static String lemmaSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicates= queryWithPredicates(query, searchPredicate);
        partOfQuery.append(triple("?lem", RDF_TYPE, LEMMA));
        partOfQuery.append(triple("?lem", WRITTEN_FORM, "?writtenFormFilter"));
        partOfQuery.append(filter("?writtenFormFilter", queryWithPredicates));
        partOfQuery.append(triple("?lem", reverseCombinePredicates(HAS_LEMMA), lexicalEntryName));
        return partOfQuery.toString();
    }

    private static String definitionSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicates = queryWithPredicates(query, searchPredicate);
        partOfQuery.append(triple("?txt", RDF_TYPE, TEXT_REPRESENTATION));
        partOfQuery.append(triple("?txt", WRITTEN_FORM, "?writtenFormFilter"));
        partOfQuery.append(filter("?writtenFormFilter", queryWithPredicates));
        partOfQuery.append(triple("?txt", reverseCombinePredicates(HAS_TEXT_REPRESENTATION, HAS_DEFINITION, HAS_SENSE), lexicalEntryName));
        return partOfQuery.toString();
    }

    private static String senseExampleSearchToLexicalEntry(String query, String searchPredicate, String lexicalEntryName) {
        StringBuilder partOfQuery = new StringBuilder();
        String queryWithPredicates = queryWithPredicates(query, searchPredicate);
        partOfQuery.append(triple("?sen", RDF_TYPE, SENSE_EXAMPLE));
        partOfQuery.append(triple("?sen", TEXT, "?textFilter"));
        partOfQuery.append(filter("?textFilter", queryWithPredicates));
        partOfQuery.append(triple("?sen", reverseCombinePredicates(HAS_SENSE_EXAMPLE, HAS_SENSE), lexicalEntryName));
        return partOfQuery.toString();
    }

    private static void appendToUnionQueryBuilderByField(UnionQueryBuilder unionQueryBuilder, String query, SearchField searchField, String searchPredicate, String lexicalEntryName) {
        switch(searchField) {
            case LABEL:
                unionQueryBuilder.add(labelSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                break;
            case DEFINITION:
                unionQueryBuilder.add(definitionSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                break;
            case LEMMA:
                unionQueryBuilder.add(lemmaSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                break;
            case SENSE_EXAMPLE:
                unionQueryBuilder.add(senseExampleSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                break;
            case EVERYWHERE:
                unionQueryBuilder.add(labelSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                unionQueryBuilder.add(lemmaSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                unionQueryBuilder.add(definitionSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                unionQueryBuilder.add(senseExampleSearchToLexicalEntry(query, searchPredicate, lexicalEntryName));
                break;
            default:
                throw new RuntimeException("Blogas paieškos laukas!");
        }
    }

    public static String unindexSearchQuery(String query, SearchField searchField, String searchPredicate) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        String lexicalEntryName = "?id";
        appendToUnionQueryBuilderByField(unionQueryBuilder,query, searchField, searchPredicate, lexicalEntryName);
        return graphQueryBuilder.add(unionQueryBuilder.build()).build();
    }

    public static String unindexedSearchQueryWithSynonym(String query, SearchField searchField, String searchPredicate) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        String lexicalEntryName = "?tempId";
        appendToUnionQueryBuilderByField(unionQueryBuilder,query, searchField, searchPredicate, lexicalEntryName);
        graphQueryBuilder.add(unionQueryBuilder.build());
        graphQueryBuilder.add(triple("?tempId", LABEL, "?foundLabel"));
        graphQueryBuilder.add(synonymGraph(synonymPredicate));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    public static String unindexedSearchQueryWithIsSynonym(String query, SearchField searchField, String searchPredicate) {
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        String lexicalEntryName = "?tempId";
        appendToUnionQueryBuilderByField(unionQueryBuilder,query, searchField, searchPredicate, lexicalEntryName);
        graphQueryBuilder.add(unionQueryBuilder.build());
        graphQueryBuilder.add(triple("?tempId", LABEL, "?foundLabel"));
        graphQueryBuilder.add(synonymGraph(isSynonymPredicate));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    public static String searchSynonymsForUnindexedTriple(String query, SearchField searchField, String searchPredicate) {
        GraphQueryBuilder synonymGraphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        GraphQueryBuilder blkzGraphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        UnionQueryBuilder unionSynonymQueryBuilder = new UnionQueryBuilder();
        String lexicalEntryName = "?tempId";
        appendToUnionQueryBuilderByField(unionQueryBuilder,query, searchField, searchPredicate, lexicalEntryName);
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
