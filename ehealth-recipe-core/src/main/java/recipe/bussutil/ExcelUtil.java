package recipe.bussutil;


import lombok.Data;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * liumin
 */
@Data
public class ExcelUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

    /**
     * sheet操作行记录
     */
    private int rowCount = 0;

    private RowHandler rowHandler;

    private boolean skipHeaderRow;

    private SheetContentsHandler sheetContentsHandler;

    private OPCPackage pkg;

    public ExcelUtil(RowHandler rowHandler) {
        this(rowHandler,true);
    }

    public ExcelUtil(RowHandler rowHandler, boolean skipHeaderRow) {
        this(rowHandler,skipHeaderRow,null);
    }

    /**
     * 用户必须自定义rowHandler，同时可以指定跳过首行。也可以自定义sheet处理器。
     */
    public ExcelUtil(RowHandler rowHandler, boolean skipHeaderRow, SheetContentsHandler sheetContentsHandler) {
        if(rowHandler == null){
            throw new RuntimeException("rowHandler can not be null");
        }
        if(sheetContentsHandler == null){
            sheetContentsHandler = new DefaultSheetHandler();
        }
        this.rowHandler = rowHandler;
        this.skipHeaderRow = skipHeaderRow;
        this.sheetContentsHandler = sheetContentsHandler;
    }

    public ExcelUtil() {

    }

    /**
     * 使用本工具类的用户必须自己实现行处理器处理逻辑
     *
     * @author zhangrihui
     * @date 2020/6/30 07:45:58
     */
    @FunctionalInterface
    public interface RowHandler{

        void handle(List<String> cells);

    }

    /**
     * excel载入方法。excel操作之前，需要先调用该方法进行载入。
     *
     * @param is excel inputstream
     * @return
     * @throws IOException
     * @throws InvalidFormatException
     */
    public ExcelUtil load(InputStream is) throws IOException, InvalidFormatException {
        setPkg(OPCPackage.open(is));
        return this;
    }

    /**
     * 读取sheet,默认为转换第1个sheet
     */
    public ExcelUtil parse() {
        return parse(1);
    }

    /**
     * 读取sheet
     *
     * @param sheetId 要处理的sheet索引，从1开始
     * @return
     */
    public ExcelUtil parse(int sheetId) {
        if(pkg == null){
            throw new RuntimeException("Before parse ,load is expected");
        }
        //当需要一次性处理多个sheet时，重置sheet行统计器
        rowCount = 0;
        try {
            XSSFReader reader = new XSSFReader(pkg);
            InputStream shellStream = reader.getSheet("rId" + sheetId);
            InputSource sheetSource = new InputSource(shellStream);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            XMLReader parser = getSheetParser(styles, strings);
            parser.parse(sheetSource);
        } catch (Exception e){
            logger.error("excel parse error",e);
        } finally {
            try {
                pkg.close();
            } catch (IOException e) {
                logger.error("pkg close error",e);
            }
        }
        return this;
    }

    /**
     * 获取excel解析器
     *
     * @param styles Table of styles
     * @param strings Table of shared strings
     * @return
     * @throws SAXException SAX异常，用户需自行处理
     */
    private XMLReader getSheetParser(StylesTable styles, ReadOnlySharedStringsTable strings) throws SAXException {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings, sheetContentsHandler, false));
        return parser;
    }

    public void setPkg(OPCPackage pkg) {
        this.pkg = pkg;
    }

    /**
     * 默认sheet内容处理器
     *
     * @author zhangrihui
     * @date 2020/6/30 07:42:26
     */
    private class DefaultSheetHandler implements SheetContentsHandler {

        private List<String> rowData = new ArrayList<>();

        @Override
        public void startRow(int rowNum) {
            rowCount++;
            rowData.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if(skipHeaderRow && rowCount <=1){
                return;
            }
            rowHandler.handle(rowData);
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            rowData.add(formattedValue);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
        }

    }

    public static void main(String[] args) {
        try {
            ExcelUtil ExcelUtil = new ExcelUtil(cells -> {
                System.out.println("cells:"+cells); //直接输出每一行的内容
            });
            ExcelUtil.load(new FileInputStream("D:\\***.xlsx")).parse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
