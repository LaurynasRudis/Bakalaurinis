package commons.SparqlQueryBuilder;

import java.util.ArrayList;
import java.util.List;

public class GraphQueryBuilder {
    private List<String> queries = new ArrayList<>();
    private String graphName;

    public GraphQueryBuilder(String graphName) {
        this.graphName = graphName;
    }

    public GraphQueryBuilder add(String triple) {
        queries.add(triple);
        return this;
    }

    public String build() {
        StringBuilder graphQuery = new StringBuilder();
        for(String query : queries) {
            graphQuery.append(query);
            graphQuery.append("\n");
        }
        return "GRAPH " + graphName + "\n{\n" + graphQuery + "}\n";
    }
}
