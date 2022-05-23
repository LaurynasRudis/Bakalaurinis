package ontology;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.tdb2.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.lt.LithuanianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LuceneIndexer {
    private String databaseLocation = "C:\\Users\\Laurynas\\OneDrive\\Desktop\\fuseki\\DB2";
    private String indexLoc = "C:\\Users\\Laurynas\\OneDrive\\Desktop\\fuseki\\run\\configuration\\index\\lucene-index";

    public LuceneIndexer(){}

    public void indexDoc() throws IOException {
        Dataset dataset = TDB2Factory.connectDataset(databaseLocation);
        dataset.begin();
        Iterator<Resource> das = dataset.listModelNames();
        List <Document> documents = new ArrayList<>();
        while (das.hasNext()) {
            Resource graphURI = das.next();
            Model a = dataset.getNamedModel(graphURI);
            OntologyModel ontologyModel = new OntologyModel(a, graphURI.getURI());
            List <RDFIndexValue> rdfIndexValues = ontologyModel.getRDFIndexValues();
            for(RDFIndexValue rdfIndexValue : rdfIndexValues) {
                documents.add(rdfIndexValue.toDocument());
            }
        }
        indexDocuments(documents);
    }

    private void indexDocuments(List<Document> documents) throws IOException {
        Analyzer analyzer = new LithuanianAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        Path path = Paths.get(indexLoc);
        Directory directory = FSDirectory.open(path);
        IndexWriter writer = new IndexWriter(directory, config);
        writer.addDocuments(documents);
        writer.commit();
        writer.close();
    }

}
