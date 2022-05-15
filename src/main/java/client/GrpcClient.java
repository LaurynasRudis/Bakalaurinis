package client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.tutorial.search.*;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;

public class GrpcClient {
    public static void main(String[] args) throws IOException {
        String fileLocation = "C:\\Users\\Laurynas\\codebase\\test-grpc-maven\\src\\main\\resources\\BLKZ_all_individuals.rdf";
        ManagedChannel channel =
                ManagedChannelBuilder.forAddress("localhost", 5540)
                        .usePlaintext()
                        .build();

        ManagedChannel channel2 =
                ManagedChannelBuilder.forAddress("localhost", 5541)
                        .usePlaintext()
                        .build();

        ManagedChannel channel3 =
                ManagedChannelBuilder.forAddress("localhost", 5542)
                        .usePlaintext()
                        .build();

        SearchServiceGrpc.SearchServiceBlockingStub stub =
                SearchServiceGrpc.newBlockingStub(channel);

        SearchServiceGrpc.SearchServiceBlockingStub stub2 =
                SearchServiceGrpc.newBlockingStub(channel2);

        SearchServiceGrpc.SearchServiceBlockingStub stub3 =
                SearchServiceGrpc.newBlockingStub(channel3);

        FusekiClient fc = new FusekiClient("blkz");

        // Enter data using BufferReader
        String what = "";
        System.out.println("1. Index \n 2. Search 3. Fuseki Search");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        what = reader.readLine();
//        what = "3";
        switch (what) {
            case "1" :
                IndexResponse response = stub.index(IndexRequest.newBuilder().setFileLocation(fileLocation).build());
                IndexResponse response2 = stub2.index(IndexRequest.newBuilder().setFileLocation(fileLocation).build());
                IndexResponse response3 = stub3.index(IndexRequest.newBuilder().setFileLocation(fileLocation).build());
                System.out.println(response.toString());
                System.out.println("Done. I hope.");
                break;
            case "2" :
                // Reading data using readLine
                System.out.print("Search> ");
                String search = reader.readLine();


                SearchResponse sr = stub.search(SearchRequest.newBuilder()
                        .setQuery(search)
                        .build()
                );

                for(Result s : sr.getSearchResultsList()) {
                    System.out.println(s);
                }
                break;
            case "3":
                fc.execSelectAndProcess("velnias");
                break;
            default:
                System.out.println(SearchRequest.SearchField.LABEL.toString());

        }
        channel.shutdown();
    }

}