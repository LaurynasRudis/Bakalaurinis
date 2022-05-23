package ontology;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.w3c.dom.Text;

import java.util.List;

public class RDFIndexValue {
    public String uri;
    public String label;
    public List<String> senseExamples;
    public String lemma;
    public List<String> definitions;
    public String graph;

    public RDFIndexValue(
            String uri,
            String label,
            List<String> senseExamples,
            String lemma,
            List<String> definitions,
            String graph
    ) {
        this.uri = uri;
        this.label = label;
        this.senseExamples = senseExamples;
        this.lemma = lemma;
        this.definitions = definitions;
        this.graph = graph;
    }


    public Document toDocument() {
        Document document = new Document();
        FieldType rdfType = new FieldType();
        rdfType.setIndexOptions(IndexOptions.DOCS);
        rdfType.setTokenized(false);
        rdfType.setStored(true);
        rdfType.freeze();
        Field uriField = new Field("uri", this.uri, rdfType);
        Field label = new TextField("label", this.label, Field.Store.YES);
        Field lemma = new TextField("lemma", this.lemma, Field.Store.YES);
        Field graph = new Field("graph", this.graph, rdfType);
        document.add(uriField);
        document.add(label);
        document.add(graph);
        document.add(lemma);
        for(String senseExample : senseExamples) {
            Field senseExampleField = new TextField("senseExample", senseExample, Field.Store.YES);
            document.add(senseExampleField);
        }
        for(String definition : definitions) {
            Field definitionField = new TextField("definition", definition, Field.Store.YES);
            document.add(definitionField);
        }
        return document;
    }
}
