package commons.SparqlQueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class UnionQueryBuilder {
    private List<String> graphs = new ArrayList<>();

    public UnionQueryBuilder(){}

    public UnionQueryBuilder add(String graph) {
        graphs.add(graph);
        return this;
    }

    public String build(){
        StringBuilder unionString = new StringBuilder();
        Iterator<String> unionIterator = graphs.listIterator();
        while(unionIterator.hasNext()) {
            unionString.append("{\n")
                    .append(unionIterator.next())
                    .append("\n}\n");
            if (unionIterator.hasNext()) unionString.append(" UNION ");
        }
        return unionString.toString();
    }
}