package server;

import client.Fuseki.FusekiClient;
import client.SolrClient;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.Properties;

public class GrpcServer {
    public static void main(String[] args) {
        Properties p = new Properties(System.getProperties());
        p.put("jdk.xml.entityExpansionLimit", "0");
        System.setProperties(p);

        SolrClient solrClientTFIDF = new SolrClient("BLKZ_TF_IDF");
        SolrClient solrClientBM25 = new SolrClient("BLKZ_BM25");
        SolrClient solrClientDFR = new SolrClient("BLKZ_DFR");
        FusekiClient fusekiClient = new FusekiClient("blkz");

        Server serverTFIDF = ServerBuilder
                .forPort(5540)
                .addService(new SearchServiceImpl(solrClientTFIDF))
                .build();

        Server serverBM25 = ServerBuilder
                .forPort(5541)
                .addService(new SearchServiceImpl(solrClientBM25))
                .build();

        Server serverDFR = ServerBuilder
                .forPort(5542)
                .addService(new SearchServiceImpl(solrClientDFR))
                .build();

        Server serverSemantic = ServerBuilder
                .forPort(5543)
                .addService(new SemanticSearchService(fusekiClient))
                .build();

        try {
            System.out.println("Starting TF_IDF server...");
            serverTFIDF.start();
            System.out.println("Server TF_IDF started!");
            System.out.println("Starting BM25 server...");
            serverBM25.start();
            System.out.println("Server BM25 started!");
            System.out.println("Starting DFR server...");
            serverDFR.start();
            System.out.println("Server DFR started!");
            System.out.println("Starting Semantic server...");
            serverSemantic.start();
            System.out.println("Server Semantic started!");
            serverTFIDF.awaitTermination();
            serverBM25.awaitTermination();
            serverDFR.awaitTermination();
            serverSemantic.awaitTermination();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Server stopped! Bye!");
        }

    }
}
