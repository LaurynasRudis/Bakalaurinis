package scripts;

import commons.ExcelSpreadsheet;
import commons.SearchResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bakalaurinis.search.SearchField;
import org.bakalaurinis.search.SearchPredicate;
import org.bakalaurinis.search.SearchRequest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static commons.ProtoCommons.createSearchRequest;

public class SolrSearchScript {
    public static void main(String[] args) throws IOException {
        String fileLocation = "test.xlsx";

        ExcelSpreadsheet excelSpreadsheet = new ExcelSpreadsheet(fileLocation);

        GrpcClient tfidfClient = new GrpcClient(5540);
        GrpcClient bm25Client = new GrpcClient(5541);
        GrpcClient dfrClient = new GrpcClient(5542);
        GrpcClient semanticClient = new GrpcClient(5543);

        Map<String, GrpcClient> clients = new HashMap<>();
        clients.put("tfidf", tfidfClient);
        clients.put("bm25", bm25Client);
        clients.put("dfr", dfrClient);
        clients.put("semantic", semanticClient);

        SearchRequest searchRequest1 = createSearchRequest("velnias", SearchField.DEFINITION, SearchPredicate.OR);
        SearchRequest searchRequest2 = createSearchRequest("velnias", SearchField.LABEL, SearchPredicate.OR);

        for(Map.Entry<String, GrpcClient> clientEntry : clients.entrySet()) {
            GrpcClient client = clientEntry.getValue();
            List<SearchResult> searchResults1 = client.search(searchRequest1);
            String clientName = clientEntry.getKey();
            excelSpreadsheet.writeSearchResults(clientName+"1", searchResults1);
            List<SearchResult> searchResults2 = client.search(searchRequest2);
            excelSpreadsheet.writeSearchResults(clientName+"2", searchResults2);
        }
    }
}
