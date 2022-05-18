package commons;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static commons.OntologyConstants.*;
import static commons.OntologyConstants.HAS_SENSE;

public class SparqlQueryBuilder {

    public static String makePrefixString(Map<String, String> prefixes) {
        StringBuilder prefixesString = new StringBuilder();
        for(Map.Entry<String, String> entry: prefixes.entrySet()){
            prefixesString.append("PREFIX ")
                    .append(entry.getKey())
                    .append(": <")
                    .append(entry.getValue())
                    .append(">\n");
        }
        System.out.println(prefixesString);
        return prefixesString.toString();
    }

    public static String select(String query, String... selects) {
        StringBuilder selectBuilder = new StringBuilder();
        selectBuilder.append("SELECT DISTINCT ");
        for(String selectText : selects) {
            selectBuilder.append(selectText);
            selectBuilder.append(" ");
        }
        selectBuilder.append("\nWHERE \n{\n");
        selectBuilder.append(query);
        selectBuilder.append(" } ");
        return selectBuilder.toString();
    }

    public static String triple(String subject, String predicate, String object)  { return subject + " " + predicate + " " + object + " .\n"; }


    public static String searchQuery(String object,
                               String searchField,
                               String searchQuery,
                               String extraParameters)
    { return object + " " + TEXT_QUERY + " ( " + searchField + " '" + searchQuery + "' " + extraParameters + " ) . \n" ; }

    public static String graph(String graphName, String... queries) {
        StringBuilder graphQuery= new StringBuilder();
        for(String query: queries) {
            graphQuery.append(query);
        }
        return " GRAPH "+ graphName + "\n{\n" + graphQuery + "}\n";
    }
    public static String bind(String bindQuery, String as) { return "BIND("+bindQuery+" as " + as + ") .\n";}
    public static String union(String... graphs) {
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
    public static String optional(String optionalQuery) {
        return " OPTIONAL {\n" + optionalQuery + "\n}";
    }

    public static String combinePredicates(String... predicates) {
        StringBuilder predicate = new StringBuilder();
        Iterator<String> predicatesIterator = Arrays.stream(predicates).iterator();
        while (predicatesIterator.hasNext()) {
            predicate.append(predicatesIterator.next());
            if(predicatesIterator.hasNext()) predicate.append("/");
        }
        return predicate.toString();
    }

    public static String reverseCombinePredicates(String... predicates) {
        StringBuilder predicate = new StringBuilder();
        Iterator<String> predicatesIterator = Arrays.stream(predicates).iterator();
        predicate.append("^");
        while (predicatesIterator.hasNext()) {
            predicate.append(predicatesIterator.next());
            if(predicatesIterator.hasNext()) predicate.append("/^");
        }
        return predicate.toString();
    }

    public static String synonymPredicate = combinePredicates(HAS_SENSE, HAS_SENSE_RELATION, SENSE_RELATED_TO);
    public static String isSynonymPredicate = reverseCombinePredicates(SENSE_RELATED_TO, HAS_SENSE_RELATION, HAS_SENSE);
}
