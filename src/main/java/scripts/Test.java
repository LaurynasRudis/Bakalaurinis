package scripts;

import ontology.LuceneIndexer;

import java.io.IOException;

public class Test {
    public static void main(String[] args) throws IOException {
        LuceneIndexer luceneIndexer = new LuceneIndexer();
        luceneIndexer.indexDoc();
    }
}
