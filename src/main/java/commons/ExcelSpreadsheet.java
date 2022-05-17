package commons;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class ExcelSpreadsheet {
    private final XSSFWorkbook workbook;
    private final File file;

    public ExcelSpreadsheet(String fileLocation) {
        workbook = new XSSFWorkbook();
        file = new File(fileLocation);
    }

    public void writeSearchResults(String spreadsheetName, List<SearchResult> searchResults) {
        XSSFSheet spreadsheet = workbook.createSheet(spreadsheetName);
        String[] columnNames = {"Id", "Label", "Lemma", "SenseExample", "Definition", "Score"};
        int rowId = 0;
        int cellId = 0;
        XSSFRow row = spreadsheet.createRow(rowId++);
        for(String columnName : columnNames) {
            Cell cell = row.createCell(cellId++);
            cell.setCellValue(columnName);
        }
        for(SearchResult searchResult : searchResults) {
            row = spreadsheet.createRow(rowId++);
            String[] results = {
                    searchResult.getId(),
                    searchResult.getLabel(),
                    searchResult.getLemma(),
                    searchResult.getSenseExample().toString(),
                    searchResult.getDefinition().toString(),
                    searchResult.getScore()
            };
            cellId = 0;
            for (String result : results) {
                Cell cell = row.createCell(cellId++);
                cell.setCellValue(result);
            }
        }
        spreadsheet.createRow(rowId);
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (Exception e) {
            System.out.println("Nepavyko įrašyti duomenų į failą! Gauta klaida: "+e.getMessage());
        }
    }

}
