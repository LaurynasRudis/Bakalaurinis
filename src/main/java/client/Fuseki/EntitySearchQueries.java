package client.Fuseki;

import commons.SparqlQueryBuilder.*;
import org.bakalaurinis.search.SearchField;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static commons.OntologyConstants.*;
import static commons.SparqlQueryBuilder.SparqlQueryBuilder.*;

public class EntitySearchQueries {

    private static void addSearchField(String searchField, String searchWord, double boosting, StringBuilder stringBuilder){
        stringBuilder.append(searchField);
        stringBuilder.append(":");
        stringBuilder.append(searchWord);
        stringBuilder.append("^");
        stringBuilder.append(boosting);
    }

    private static String entitySearchLuceneQuery(String query,
                                                  String searchPredicate,
                                                  SearchField searchField,
                                                  double labelBoosting,
                                                  double lemmaBoosting,
                                                  double definitionBoosting,
                                                  double senseExampleBoosting
                                                  ){
        StringBuilder searchQueryBuilder = new StringBuilder();
        StringBuilder labelQueryBuilder = new StringBuilder("(");
        StringBuilder lemmaQueryBuilder = new StringBuilder("(");
        StringBuilder definitionQueryBuilder = new StringBuilder("(");
        StringBuilder senseExampleQueryBuilder = new StringBuilder("(");
        String[] searchWords = query.split("\\s+");
        Iterator<String> searchWordsIterator = Arrays.stream(searchWords).iterator();
        searchQueryBuilder.append("'");
        while(searchWordsIterator.hasNext()) {
            String searchWord = searchWordsIterator.next();
            addSearchField("label", searchWord, labelBoosting, labelQueryBuilder);
            labelQueryBuilder.append(" ");
            addSearchField("lemma", searchWord, lemmaBoosting, lemmaQueryBuilder);
            lemmaQueryBuilder.append(" ");
            addSearchField("definition", searchWord, definitionBoosting, definitionQueryBuilder);
            definitionQueryBuilder.append(" ");
            addSearchField("senseExample", searchWord, senseExampleBoosting, senseExampleQueryBuilder);
            senseExampleQueryBuilder.append(" ");
            if(searchWordsIterator.hasNext()) {
                labelQueryBuilder.append(searchPredicate);
                labelQueryBuilder.append(" ");
                lemmaQueryBuilder.append(searchPredicate);
                lemmaQueryBuilder.append(" ");
                definitionQueryBuilder.append(searchPredicate);
                definitionQueryBuilder.append(" ");
                senseExampleQueryBuilder.append(searchPredicate);
                senseExampleQueryBuilder.append(" ");
            }
            else{
                labelQueryBuilder.append(")");
                lemmaQueryBuilder.append(")");
                definitionQueryBuilder.append(")");
                senseExampleQueryBuilder.append(")");
            }
        }
        switch(searchField) {
            case LABEL:
                searchQueryBuilder.append(labelQueryBuilder);
                break;
            case DEFINITION:
                searchQueryBuilder.append(definitionQueryBuilder);
                break;
            case LEMMA:
                searchQueryBuilder.append(lemmaQueryBuilder);
                break;
            case SENSE_EXAMPLE:
                searchQueryBuilder.append(senseExampleQueryBuilder);
                break;
            case EVERYWHERE:
                searchQueryBuilder.append(labelQueryBuilder);
                searchQueryBuilder.append(" OR ");
                searchQueryBuilder.append(lemmaQueryBuilder);
                searchQueryBuilder.append(" OR ");
                searchQueryBuilder.append(definitionQueryBuilder);
                searchQueryBuilder.append(" OR ");
                searchQueryBuilder.append(senseExampleQueryBuilder);
                break;
            case UNKNOWN_FIELD:
                throw new RuntimeException("Bad search field");
        }
        searchQueryBuilder.append("'");
        return searchQueryBuilder.toString();
    }

    private static String synonymGraph(String synonymPredicate, String objectName) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        return graphQueryBuilder
                .add(triple("?sinLex", LABEL, objectName))
                .add(triple("?sinLex", synonymPredicate+"/"+ LABEL, "?labelTemp"))
                .build();
    }


     static String entitySearchQueries(String query, String searchPredicate, SearchField searchField) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        graphQueryBuilder.add(triple("(?id ?score)", TEXT_QUERY, entitySearchLuceneQuery(
                query,
                searchPredicate,
                searchField,
                5,
                4,
                2,
                1
        )));
        return graphQueryBuilder.build();
    }

     static String entitySearchQueriesWithSynonym(String query, String searchPredicate, SearchField searchField) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        graphQueryBuilder.add(triple("(?tempId ?score)", TEXT_QUERY, entitySearchLuceneQuery(
                query,
                searchPredicate,
                searchField,
                2.5,
                1.5,
                1,
                0.5)));
        graphQueryBuilder.add(triple("?tempId", LABEL, "?foundLabel"));
        graphQueryBuilder.add(synonymGraph(synonymPredicate, "?foundLabel"));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

     static String entitySearchQueriesWithIsSynonym(String query, String searchPredicate, SearchField searchField) {
        GraphQueryBuilder graphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        graphQueryBuilder.add(triple("(?tempId ?score)", TEXT_QUERY, entitySearchLuceneQuery(
                query,
                searchPredicate,
                searchField,
                2.5,
                1.5,
                1,
                0.5
        )));
        graphQueryBuilder.add(triple("?tempId", LABEL, "?foundLabel"));
        graphQueryBuilder.add(synonymGraph(isSynonymPredicate, "?foundLabel"));
        graphQueryBuilder.add(triple("?id", LABEL, "?labelTemp"));
        graphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        return graphQueryBuilder.build();
    }

    static String searchSynonymsForEntity(String query, String searchPredicate, SearchField searchField) {
        GraphQueryBuilder synonymGraphQueryBuilder = new GraphQueryBuilder(SINONIMU_ZODYNAS);
        GraphQueryBuilder blkzGraphQueryBuilder = new GraphQueryBuilder(ZODYNAS_BE_AKCENTU);
        UnionQueryBuilder unionQueryBuilder = new UnionQueryBuilder();
        synonymGraphQueryBuilder.add(triple("(?tempId ?score)", TEXT_QUERY, entitySearchLuceneQuery(
                query,
                searchPredicate,
                searchField,
                1.25,
                1.25,
                0.1,
                0
        )));
        unionQueryBuilder.add(triple("?tempId", synonymPredicate+"/"+ LABEL, "?querySynonym"));
        unionQueryBuilder.add(triple("?tempId", isSynonymPredicate+"/"+ LABEL, "?querySynonym"));
        synonymGraphQueryBuilder.add(unionQueryBuilder.build());
        blkzGraphQueryBuilder.add(triple("?id", LABEL, "?querySynonym"));
        blkzGraphQueryBuilder.add(triple("?id", RDF_TYPE, LEXICAL_ENTRY));
        synonymGraphQueryBuilder.add(blkzGraphQueryBuilder.build());
        return synonymGraphQueryBuilder.build();
    }
}
